package com.seed.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private static String joinTypes(List<Token> toks) {
        StringBuilder sb = new StringBuilder();
        for (Token t : toks) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t.type.name());
        }
        return sb.toString();
    }

    @Test
    void helloExample_tokenSequence() {
        String src = ""
                + "let x = 3;\n"
                + "fn add(a, b) { return a + b; }\n"
                + "if (x < 10) { print(add(x, 5)); }\n";
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.scanTokens();

        // Quick signature check (types only, in order)
        String types = joinTypes(toks);
        assertTrue(types.startsWith(
                "LET IDENT EQUAL INT SEMICOLON " +
                "FN IDENT LEFT_PAREN IDENT COMMA IDENT RIGHT_PAREN LEFT_BRACE RETURN IDENT PLUS IDENT SEMICOLON RIGHT_BRACE " +
                "IF LEFT_PAREN IDENT LESS INT RIGHT_PAREN LEFT_BRACE PRINT LEFT_PAREN IDENT LEFT_PAREN IDENT COMMA INT RIGHT_PAREN RIGHT_PAREN SEMICOLON RIGHT_BRACE"
        ), "Unexpected token type sequence:\n" + types);

        // Last token must be EOF
        assertEquals(TokenType.EOF, toks.get(toks.size() - 1).type);
    }

    @Test
    void commentsAndWhitespace_areIgnored() {
        String src = ""
                + "// comment only line\n"
                + "let a = 1; // trailing comment\n"
                + "   \t  // spaces + tabs comment\n"
                + "let b=2;//no space\n";
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.scanTokens();

        // Ensure we see tokens for two var statements + EOF
        // Sequence (types only):
        // LET IDENT EQUAL INT SEMICOLON LET IDENT EQUAL INT SEMICOLON EOF
        String types = joinTypes(toks);
        assertTrue(types.matches("^LET IDENT EQUAL INT SEMICOLON LET IDENT EQUAL INT SEMICOLON EOF$"),
                "Got: " + types);
    }

    @Test
    void unexpectedCharacter_throws() {
        String src = "let x = @;";
        Lexer lx = new Lexer(src);
        RuntimeException ex = assertThrows(RuntimeException.class, lx::scanTokens);
        assertTrue(ex.getMessage().contains("Unexpected char"),
                "Message: " + ex.getMessage());
    }
}