package com.seed.parser;

import com.seed.ast.AstPrinter;
import com.seed.ast.Stmt;
import com.seed.lexer.Lexer;
import com.seed.lexer.Token;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private List<Stmt> parse(String src) {
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.scanTokens();
        Parser p = new Parser(toks);
        return p.parseProgram();
    }

    @Test
    void precedence_termVsFactor() {
        String src = "1 + 2 * 3;";
        List<Stmt> prog = parse(src);
        String printed = new AstPrinter().print(prog);
        assertEquals("(+ 1 (* 2 3))", printed);
    }

    @Test
    void functionDecl_andReturn() {
        String src = "fn add(a, b) { return a + b; }";
        List<Stmt> prog = parse(src);
        String printed = new AstPrinter().print(prog).replaceAll("\\s+", " ");
        assertTrue(printed.contains("(fn add (a b) (return (+ a b)))"),
                "Printed: " + printed);
    }

    @Test
    void ifElse_block() {
        String src = "if (1 < 2) { return 3; } else { return 4; }";
        List<Stmt> prog = parse(src);
        String printed = new AstPrinter().print(prog).replaceAll("\\s+", " ");
        assertTrue(printed.startsWith("(if (< 1 2) (block (return 3)) (block (return 4)))"),
                "Printed: " + printed);
    }
}
