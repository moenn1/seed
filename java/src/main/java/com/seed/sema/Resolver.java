package com.seed.sema;

import com.seed.ast.Expr;
import com.seed.ast.Stmt;
import com.seed.lexer.Token;
import com.seed.lexer.TokenType;

import java.util.*;

import static com.seed.lexer.TokenType.*;

public class Resolver {
    private final List<Diagnostic> diags = new ArrayList<>();
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public List<Diagnostic> resolve(List<Stmt> program) {
        scopes.clear();
        diags.clear();
        beginScope(); // global
        // First pass: predeclare all top-level functions so calls can see arity
        for (Stmt s : program) {
            if (s instanceof Stmt.Fun f) {
                declare(sloc(f.name), f.name.lexeme, Symbol.fun(f.params.size()));
            }
        }
        // Full pass
        for (Stmt s : program) {
            stmt(s);
        }
        endScope();
        return diags;
    }

    private record SLoc(int line, int col) {}
    private SLoc sloc(Token t) { return new SLoc(t.line, t.col); }
    private void error(SLoc sl, String msg) { diags.add(new Diagnostic(sl.line, sl.col, msg)); }

    private void beginScope() { scopes.push(new HashMap<>()); }
    private void endScope() { scopes.pop(); }

    private boolean declare(SLoc where, String name, Symbol sym) {
        Map<String, Symbol> cur = scopes.peek();
        if (cur.containsKey(name)) {
            error(where, "Duplicate declaration: '" + name + "'");
            return false;
        }
        cur.put(name, sym);
        return true;
    }

    private Symbol lookup(String name) {
        for (Map<String, Symbol> m : scopes) {
            Symbol s = m.get(name);
            if (s != null) return s;
        }
        return null;
    }

    // Statements
    private void stmt(Stmt s) {
        if (s instanceof Stmt.Let v) {
            // Infer initializer type if available
            Symbol.Type t = Symbol.Type.UNKNOWN;
            if (v.init != null) {
                t = expr(v.init);
            }
            declare(new SLoc(v.name.line, v.name.col), v.name.lexeme, Symbol.var(t));
            return;
        }
        if (s instanceof Stmt.ExprStmt es) {
            expr(es.expr);
            return;
        }
        if (s instanceof Stmt.Print p) {
            expr(p.value);
            return;
        }
        if (s instanceof Stmt.Block b) {
            beginScope();
            for (Stmt st : b.stmts) stmt(st);
            endScope();
            return;
        }
        if (s instanceof Stmt.If iff) {
            Symbol.Type ct = expr(iff.cond);
            checkIsBool(iff, ct, "if condition should be boolean");
            stmt(iff.thenBranch);
            if (iff.elseBranch != null) stmt(iff.elseBranch);
            return;
        }
        if (s instanceof Stmt.While w) {
            Symbol.Type ct = expr(w.cond);
            checkIsBool(w, ct, "while condition should be boolean");
            stmt(w.body);
            return;
        }
        if (s instanceof Stmt.Return r) {
            if (r.value != null) expr(r.value);
            return;
        }
        if (s instanceof Stmt.Fun f) {
            // Function body scope with params
            beginScope();
            Set<String> seen = new HashSet<>();
            for (Token p : f.params) {
                if (seen.contains(p.lexeme)) {
                    error(sloc(p), "Duplicate parameter: '" + p.lexeme + "'");
                }
                seen.add(p.lexeme);
                declare(sloc(p), p.lexeme, Symbol.var(Symbol.Type.UNKNOWN));
            }
            for (Stmt st : f.body) stmt(st);
            endScope();
            return;
        }
        throw new IllegalArgumentException("Unknown Stmt: " + s.getClass());
    }

    private void checkIsBool(Object node, Symbol.Type t, String msg) {
        if (t != Symbol.Type.BOOL && t != Symbol.Type.UNKNOWN) {
            // Attempt to get location
            if (node instanceof Stmt.If iff) {
                error(new SLoc(0,0), msg);
            } else if (node instanceof Stmt.While w) {
                error(new SLoc(0,0), msg);
            } else {
                error(new SLoc(0,0), msg);
            }
        }
    }

    // Expressions: return a best-effort static type
    private Symbol.Type expr(Expr e) {
        if (e instanceof Expr.Literal l) {
            if (l.value instanceof Integer) return Symbol.Type.INT;
            if (l.value instanceof Boolean) return Symbol.Type.BOOL;
            if (l.value == null) return Symbol.Type.NIL;
            return Symbol.Type.UNKNOWN;
        }
        if (e instanceof Expr.Variable v) {
            Symbol s = lookup(v.name.lexeme);
            if (s == null) {
                error(sloc(v.name), "Undefined identifier: '" + v.name.lexeme + "'");
                return Symbol.Type.UNKNOWN;
            }
            return s.kind == Symbol.Kind.FUN ? Symbol.Type.FUNCTION : s.type;
        }
        if (e instanceof Expr.Unary u) {
            Symbol.Type rt = expr(u.right);
            if (u.op.type == BANG) {
                // logical not expects bool
                if (rt != Symbol.Type.BOOL && rt != Symbol.Type.UNKNOWN) {
                    error(sloc(u.op), "Operator '!' expects boolean");
                }
                return Symbol.Type.BOOL;
            }
            if (u.op.type == MINUS) {
                if (rt != Symbol.Type.INT && rt != Symbol.Type.UNKNOWN) {
                    error(sloc(u.op), "Unary '-' expects int");
                }
                return Symbol.Type.INT;
            }
            return Symbol.Type.UNKNOWN;
        }
        if (e instanceof Expr.Binary b) {
            Symbol.Type lt = expr(b.left);
            Symbol.Type rt = expr(b.right);
            TokenType op = b.op.type;
            if (op == PLUS || op == MINUS || op == STAR || op == SLASH) {
                if (!isIntLike(lt) || !isIntLike(rt)) {
                    error(sloc(b.op), "Arithmetic '" + b.op.lexeme + "' expects int operands");
                }
                return Symbol.Type.INT;
            }
            if (op == LESS || op == LESS_EQUAL || op == GREATER || op == GREATER_EQUAL) {
                if (!isIntLike(lt) || !isIntLike(rt)) {
                    error(sloc(b.op), "Comparison '" + b.op.lexeme + "' expects int operands");
                }
                return Symbol.Type.BOOL;
            }
            if (op == EQUAL_EQUAL || op == BANG_EQUAL) {
                // allow int==int or bool==bool
                if (!((lt == Symbol.Type.INT && rt == Symbol.Type.INT) ||
                      (lt == Symbol.Type.BOOL && rt == Symbol.Type.BOOL) ||
                      lt == Symbol.Type.UNKNOWN || rt == Symbol.Type.UNKNOWN)) {
                    error(sloc(b.op), "Equality expects operands of same basic type");
                }
                return Symbol.Type.BOOL;
            }
            if (op == OR_OR || op == AND_AND) {
                if (!(lt == Symbol.Type.BOOL || lt == Symbol.Type.UNKNOWN) ||
                    !(rt == Symbol.Type.BOOL || rt == Symbol.Type.UNKNOWN)) {
                    error(sloc(b.op), "Logical operator '" + b.op.lexeme + "' expects boolean operands");
                }
                return Symbol.Type.BOOL;
            }
            return Symbol.Type.UNKNOWN;
        }
        if (e instanceof Expr.Grouping g) {
            return expr(g.expr);
        }
        if (e instanceof Expr.Call c) {
            // If callee is a bare variable referring to a known function, check arity
            if (c.callee instanceof Expr.Variable v) {
                Symbol s = lookup(v.name.lexeme);
                if (s == null) {
                    // will be reported by variable lookup
                } else if (s.kind == Symbol.Kind.FUN) {
                    if (c.args.size() != s.arity) {
                        error(sloc(v.name), "Arity mismatch for function '" + v.name.lexeme + "': expected " + s.arity + " got " + c.args.size());
                    }
                }
            }
            for (Expr a : c.args) expr(a);
            return Symbol.Type.UNKNOWN;
        }
        return Symbol.Type.UNKNOWN;
    }

    private boolean isIntLike(Symbol.Type t) { return t == Symbol.Type.INT || t == Symbol.Type.UNKNOWN; }
}
