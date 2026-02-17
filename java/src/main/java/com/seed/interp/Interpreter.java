package com.seed.interp;

import com.seed.ast.Expr;
import com.seed.ast.Stmt;
import com.seed.lexer.Token;
import com.seed.lexer.TokenType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.seed.lexer.TokenType.*;

public class Interpreter {
    private final PrintStream out;
    private final Environment globals;

    public Interpreter(PrintStream out) {
        this.out = out;
        this.globals = new Environment();
    }

    public void interpret(List<Stmt> program) {
        // Execute top-level in globals
        for (Stmt s : program) {
            exec(s, globals);
        }
    }

    private void exec(Stmt s, Environment env) {
        if (s instanceof Stmt.Let v) {
            Value init = (v.init == null) ? Value.Nil.INSTANCE : eval(v.init, env);
            env.define(v.name.lexeme, init);
            return;
        }
        if (s instanceof Stmt.ExprStmt es) {
            eval(es.expr, env);
            return;
        }
        if (s instanceof Stmt.Print p) {
            Value val = eval(p.value, env);
            out.println(Value.show(val));
            return;
        }
        if (s instanceof Stmt.Block b) {
            Environment inner = new Environment(env);
            for (Stmt st : b.stmts) exec(st, inner);
            return;
        }
        if (s instanceof Stmt.If iff) {
            if (truthy(eval(iff.cond, env))) {
                exec(iff.thenBranch, env);
            } else if (iff.elseBranch != null) {
                exec(iff.elseBranch, env);
            }
            return;
        }
        if (s instanceof Stmt.While w) {
            while (truthy(eval(w.cond, env))) {
                exec(w.body, env);
            }
            return;
        }
        if (s instanceof Stmt.Return r) {
            Value v = (r.value == null) ? Value.Nil.INSTANCE : eval(r.value, env);
            throw new Return(v);
        }
        if (s instanceof Stmt.Fun f) {
            Value.FunVal fn = new Value.FunVal(f.name, f.params, f.body, env);
            env.define(f.name.lexeme, fn);
            return;
        }
        throw new IllegalArgumentException("Unknown Stmt: " + s.getClass());
    }

    private Value eval(Expr e, Environment env) {
        if (e instanceof Expr.Literal l) {
            if (l.value == null) return Value.Nil.INSTANCE;
            if (l.value instanceof Integer i) return new Value.IntVal(i);
            if (l.value instanceof Boolean b) return new Value.BoolVal(b);
            throw new RuntimeException("Unsupported literal: " + l.value);
        }
        if (e instanceof Expr.Variable v) {
            return env.get(v.name.lexeme);
        }
        if (e instanceof Expr.Unary u) {
            Value rv = eval(u.right, env);
            if (u.op.type == BANG) return new Value.BoolVal(!truthy(rv));
            if (u.op.type == MINUS) return new Value.IntVal(-asInt(rv, u.op));
            throw opError("unary", u.op);
        }
        if (e instanceof Expr.Binary b) {
            Value lv = eval(b.left, env);
            Value rv = eval(b.right, env);
            TokenType op = b.op.type;
            // arithmetic
            if (op == PLUS) return new Value.IntVal(asInt(lv, b.op) + asInt(rv, b.op));
            if (op == MINUS) return new Value.IntVal(asInt(lv, b.op) - asInt(rv, b.op));
            if (op == STAR) return new Value.IntVal(asInt(lv, b.op) * asInt(rv, b.op));
            if (op == SLASH) return new Value.IntVal(asInt(lv, b.op) / asInt(rv, b.op));
            // comparison
            if (op == LESS) return new Value.BoolVal(asInt(lv, b.op) < asInt(rv, b.op));
            if (op == LESS_EQUAL) return new Value.BoolVal(asInt(lv, b.op) <= asInt(rv, b.op));
            if (op == GREATER) return new Value.BoolVal(asInt(lv, b.op) > asInt(rv, b.op));
            if (op == GREATER_EQUAL) return new Value.BoolVal(asInt(lv, b.op) >= asInt(rv, b.op));
            // equality
            if (op == EQUAL_EQUAL) return new Value.BoolVal(equalsVal(lv, rv));
            if (op == BANG_EQUAL) return new Value.BoolVal(!equalsVal(lv, rv));
            // logical short-circuit
            if (op == OR_OR) {
                boolean ltb = truthy(lv);
                return new Value.BoolVal(ltb || truthy(rv));
            }
            if (op == AND_AND) {
                boolean ltb = truthy(lv);
                return new Value.BoolVal(ltb && truthy(rv));
            }
            throw opError("binary", b.op);
        }
        if (e instanceof Expr.Grouping g) {
            return eval(g.expr, env);
        }
        if (e instanceof Expr.Call c) {
            Value callee = eval(c.callee, env);
            if (!(callee instanceof Value.FunVal f)) {
                throw new RuntimeException("Attempting to call non-function");
            }
            if (c.args.size() != f.params.size()) {
                throw new RuntimeException("Arity mismatch: expected " + f.params.size() + " got " + c.args.size());
            }
            // Prepare call env
            Environment callEnv = new Environment(f.closure);
            for (int i = 0; i < c.args.size(); i++) {
                Value av = eval(c.args.get(i), env);
                callEnv.define(f.params.get(i).lexeme, av);
            }
            try {
                for (Stmt s : f.body) exec(s, callEnv);
            } catch (Return r) {
                return r.value;
            }
            return Value.Nil.INSTANCE;
        }
        throw new IllegalArgumentException("Unknown Expr: " + e.getClass());
    }

    private boolean truthy(Value v) {
        if (v instanceof Value.BoolVal b) return b.v;
        if (v instanceof Value.IntVal i) return i.v != 0;
        return v != Value.Nil.INSTANCE; // functions are truthy, nil is falsey
    }

    private int asInt(Value v, Token opSite) {
        if (v instanceof Value.IntVal i) return i.v;
        throw new RuntimeException("Expected int at " + opSite.line + ":" + opSite.col);
    }

    private boolean equalsVal(Value a, Value b) {
        if (a instanceof Value.IntVal ai && b instanceof Value.IntVal bi) return ai.v == bi.v;
        if (a instanceof Value.BoolVal ab && b instanceof Value.BoolVal bb) return ab.v == bb.v;
        return a == b; // nil equality and same function object
    }

    private RuntimeException opError(String kind, Token op) {
        return new RuntimeException("Unsupported " + kind + " op '" + op.lexeme + "' at " + op.line + ":" + op.col);
    }
}
