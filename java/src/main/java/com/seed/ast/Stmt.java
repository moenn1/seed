package com.seed.ast;

import com.seed.lexer.Token;
import java.util.List;

public interface Stmt {
    final class Let implements Stmt {
        public final Token name;
        public final Expr init; // may be null
        public Let(Token name, Expr init) { this.name = name; this.init = init; }
    }

    final class ExprStmt implements Stmt {
        public final Expr expr;
        public ExprStmt(Expr expr) { this.expr = expr; }
    }

    final class Print implements Stmt {
        public final Expr value;
        public Print(Expr value) { this.value = value; }
    }

    final class Block implements Stmt {
        public final List<Stmt> stmts;
        public Block(List<Stmt> stmts) { this.stmts = stmts; }
    }

    final class If implements Stmt {
        public final Expr cond;
        public final Stmt thenBranch;
        public final Stmt elseBranch; // may be null
        public If(Expr cond, Stmt thenBranch, Stmt elseBranch) { this.cond = cond; this.thenBranch = thenBranch; this.elseBranch = elseBranch; }
    }

    final class While implements Stmt {
        public final Expr cond;
        public final Stmt body;
        public While(Expr cond, Stmt body) { this.cond = cond; this.body = body; }
    }

    final class Return implements Stmt {
        public final Token keyword;
        public final Expr value; // may be null
        public Return(Token keyword, Expr value) { this.keyword = keyword; this.value = value; }
    }

    final class Fun implements Stmt {
        public final Token name;
        public final List<Token> params;
        public final List<Stmt> body;
        public Fun(Token name, List<Token> params, List<Stmt> body) { this.name = name; this.params = params; this.body = body; }
    }
}