package com.seed.lexer;

import java.util.*;

public class Lexer {
    private final String src;
    private final int n;
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, TokenType> keywords = Map.ofEntries(
            Map.entry("let", TokenType.LET),
            Map.entry("fn", TokenType.FN),
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("while", TokenType.WHILE),
            Map.entry("return", TokenType.RETURN),
            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE),
            Map.entry("print", TokenType.PRINT)
    );

    public Lexer(String src) {
        this.src = src;
        this.n = src.length();
    }

    public List<Token> scanTokens() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            start = current;
            Token t = scanToken();
            if (t != null) tokens.add(t);
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    private Token scanToken() {
        char c = advance();
        switch (c) {
            case '(': return make(TokenType.LEFT_PAREN);
            case ')': return make(TokenType.RIGHT_PAREN);
            case '{': return make(TokenType.LEFT_BRACE);
            case '}': return make(TokenType.RIGHT_BRACE);
            case ',': return make(TokenType.COMMA);
            case ';': return make(TokenType.SEMICOLON);
            case '+': return make(TokenType.PLUS);
            case '-': return make(TokenType.MINUS);
            case '*': return make(TokenType.STAR);
            case '/':
                if (match('/')) { // comment
                    while (!isAtEnd() && peek() != '\n') advance();
                    return null;
                }
                return make(TokenType.SLASH);
            case '!': return make(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=': return make(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '<': return make(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>': return make(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '&':
                if (match('&')) return make(TokenType.AND_AND);
                throw unexpected('&');
            case '|':
                if (match('|')) return make(TokenType.OR_OR);
                throw unexpected('|');
            case ' ':
            case '\r':
            case '\t':
                return null;
            case '\n':
                line++; col = 1;
                return null;
            default:
                if (isDigit(c)) return number();
                if (isAlpha(c)) return identifier();
                throw unexpected(c);
        }
    }

    private RuntimeException unexpected(char c) {
        return new RuntimeException("Unexpected char '" + c + "' at " + line + ":" + (col-1));
    }

    private Token number() {
        while (isDigit(peek())) advance();
        String lex = src.substring(start, current);
        return new Token(TokenType.INT, lex, line, colAtStart());
    }

    private Token identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = src.substring(start, current);
        TokenType type = keywords.getOrDefault(text, TokenType.IDENT);
        return new Token(type, text, line, colAtStart());
    }

    private int colAtStart() {
        return col - (current - start);
    }

    private boolean isAtEnd() { return current >= n; }

    private char advance() {
        char c = src.charAt(current++);
        col++;
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (src.charAt(current) != expected) return false;
        current++; col++;
        return true;
    }

    private char peek() { return isAtEnd() ? '\0' : src.charAt(current); }

    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }
    private static boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }

    private Token make(TokenType type) {
        String lex = src.substring(start, current);
        return new Token(type, lex, line, colAtStart());
    }
}
