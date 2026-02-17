package com.seed.lexer;

public enum TokenType {
    // Single-char
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, SEMICOLON,
    PLUS, MINUS, STAR, SLASH, BANG, EQUAL,
    // One or two char
    BANG_EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
    AND_AND, OR_OR,
    // Literals/identifiers
    IDENT, INT, TRUE, FALSE,
    // Keywords
    LET, FN, IF, ELSE, WHILE, RETURN, PRINT,
    EOF
}
