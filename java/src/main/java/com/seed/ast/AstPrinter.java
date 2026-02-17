package com.seed.ast;

import com.seed.lexer.Token;

import java.util.List;
import java.util.stream.Collectors;

public final class AstPrinter {
    public String print(List<Stmt> stmts) {
        return stmts.stream().map(this::print).collect(Collectors.joining("\n"));
    }

    public String print(Stmt s) {
        if (s instanceof Stmt.Let v) {
            String init = v.init == null ? "nil" : print(v.init);
            return "(let " + v.name.lexeme + " " + init + ")";
        }
        if (s instanceof Stmt.ExprStmt es) {
            return print(es.expr);
        }
        if (s instanceof Stmt.Print p) {
            return "(print " + print(p.value) + ")";
        }
        if (s instanceof Stmt.Block b) {
            return "(block " + b.stmts.stream().map(this::print).collect(Collectors.joining(" ")) + ")";
        }
        if (s instanceof Stmt.If iff) {
            String elsePart = iff.elseBranch == null ? "" : " " + print(iff.elseBranch);
            return "(if " + print(iff.cond) + " " + print(iff.thenBranch) + elsePart + ")";
        }
        if (s instanceof Stmt.While w) {
            return "(while " + print(w.cond) + " " + print(w.body) + ")";
        }
        if (s instanceof Stmt.Return r) {
            return r.value == null ? "(return)" : "(return " + print(r.value) + ")";
        }
        if (s instanceof Stmt.Fun f) {
            String params = f.params.stream().map(t -> t.lexeme).collect(Collectors.joining(" "));
            String body = f.body.stream().map(this::print).collect(Collectors.joining(" "));
            return "(fn " + f.name.lexeme + " (" + params + ") " + body + ")";
        }
        throw new IllegalArgumentException("Unknown Stmt: " + s.getClass());
    }

    public String print(Expr e) {
        if (e instanceof Expr.Literal l) {
            if (l.value instanceof String s) return "\"" + s + "\"";
            return String.valueOf(l.value);
        }
        if (e instanceof Expr.Variable v) {
            return v.name.lexeme;
        }
        if (e instanceof Expr.Unary u) {
            return "(" + u.op.lexeme + " " + print(u.right) + ")";
        }
        if (e instanceof Expr.Binary b) {
            return "(" + b.op.lexeme + " " + print(b.left) + " " + print(b.right) + ")";
        }
        if (e instanceof Expr.Grouping g) {
            return "(group " + print(g.expr) + ")";
        }
        if (e instanceof Expr.Call c) {
            String args = c.args.stream().map(this::print).collect(Collectors.joining(" "));
            return "(call " + print(c.callee) + " (" + args + "))";
        }
        throw new IllegalArgumentException("Unknown Expr: " + e.getClass());
    }
}