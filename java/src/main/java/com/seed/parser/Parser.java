package com.seed.parser;

import com.seed.ast.Expr;
import com.seed.ast.Stmt;
import com.seed.lexer.Token;
import com.seed.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

import static com.seed.lexer.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parseProgram() {
        List<Stmt> stmts = new ArrayList<>();
        while (!isAtEnd()) {
            stmts.add(declaration());
        }
        return stmts;
    }

    private Stmt declaration() {
        try {
            if (match(FN)) return funDecl();
            if (match(LET)) return varDecl();
            return statement();
        } catch (ParseError e) {
            synchronize();
            return new Stmt.ExprStmt(new Expr.Literal(null));
        }
    }

    private Stmt funDecl() {
        Token name = consume(IDENT, "Expect function name.");
        consume(LEFT_PAREN, "Expect '(' after function name.");
        List<Token> params = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                params.add(consume(IDENT, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before function body.");
        List<Stmt> body = blockItems();
        return new Stmt.Fun(name, params, body);
    }

    private Stmt varDecl() {
        Token name = consume(IDENT, "Expect variable name.");
        Expr init = null;
        if (match(EQUAL)) {
            init = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Let(name, init);
    }

    private Stmt statement() {
        if (match(IF)) return ifStmt();
        if (match(WHILE)) return whileStmt();
        if (match(RETURN)) return returnStmt();
        if (match(PRINT)) return printStmt();
        if (match(LEFT_BRACE)) return new Stmt.Block(blockItems());
        return exprStmt();
    }

    private Stmt ifStmt() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr cond = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(cond, thenBranch, elseBranch);
    }

    private Stmt whileStmt() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr cond = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(cond, body);
    }

    private Stmt returnStmt() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt exprStmt() {
        Expr e = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.ExprStmt(e);
    }

    private Stmt printStmt() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private List<Stmt> blockItems() {
        List<Stmt> stmts = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            stmts.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return stmts;
    }

    // Expressions (no assignment for now; use let x = ...;)
    private Expr expression() { return or(); }

    private Expr or() {
        Expr expr = and();
        while (match(OR_OR)) {
            Token op = previous();
            Expr right = and();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND_AND)) {
            Token op = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token op = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token op = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(PLUS, MINUS)) {
            Token op = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(STAR, SLASH)) {
            Token op = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token op = previous();
            Expr right = unary();
            return new Expr.Unary(op, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> args = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                args.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, args);
    }

    private Expr primary() {
        if (match(INT)) {
            int v = Integer.parseInt(previous().lexeme);
            return new Expr.Literal(v);
        }
        if (match(TRUE)) return new Expr.Literal(Boolean.TRUE);
        if (match(FALSE)) return new Expr.Literal(Boolean.FALSE);
        if (match(IDENT)) return new Expr.Variable(previous());
        if (match(LEFT_PAREN)) {
            Expr e = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(e);
        }
        throw error(peek(), "Expect expression.");
    }

    // Helpers
    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        String where = token.type == EOF ? "at end" : "at '" + token.lexeme + "'";
        return new ParseError("Parse error " + where + " line " + token.line + ":" + token.col + ": " + message);
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case LET, IF, WHILE, RETURN, FN -> { return; }
                default -> {}
            }
            advance();
        }
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
}