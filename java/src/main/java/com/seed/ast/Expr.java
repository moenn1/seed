package com.seed.ast;

import com.seed.lexer.Token;
import java.util.List;

public interface Expr {
    final class Literal implements Expr {
        public final Object value;
        public Literal(Object value) { this.value = value; }
    }

    final class Variable implements Expr {
        public final Token name;
        public Variable(Token name) { this.name = name; }
    }

    final class Unary implements Expr {
        public final Token op;
        public final Expr right;
        public Unary(Token op, Expr right) { this.op = op; this.right = right; }
    }

    final class Binary implements Expr {
        public final Expr left;
        public final Token op;
        public final Expr right;
        public Binary(Expr left, Token op, Expr right) { this.left = left; this.op = op; this.right = right; }
    }

    final class Grouping implements Expr {
        public final Expr expr;
        public Grouping(Expr expr) { this.expr = expr; }
    }

    final class Call implements Expr {
        public final Expr callee;
        public final Token paren; // closing paren for error location
        public final List<Expr> args;
        public Call(Expr callee, Token paren, List<Expr> args) { this.callee = callee; this.paren = paren; this.args = args; }
    }
}
