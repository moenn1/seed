package com.seed.interp;

import com.seed.ast.Stmt;
import com.seed.lexer.Lexer;
import com.seed.lexer.Token;
import com.seed.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterpreterTest {

    private String run(String src) {
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.scanTokens();
        Parser p = new Parser(toks);
        List<Stmt> prog = p.parseProgram();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Interpreter itp = new Interpreter(ps);
        itp.interpret(prog);
        return baos.toString();
    }

    @Test
    void addAndPrint_functionCall() {
        String src = ""
                + "let x = 3;\n"
                + "fn add(a, b) { return a + b; }\n"
                + "print(add(x, 5));\n";
        String out = run(src);
        assertEquals("8\n", out);
    }

    @Test
    void ifElse_printsElse() {
        String src = "if (1 > 2) { print(1); } else { print(2); }";
        String out = run(src);
        assertEquals("2\n", out);
    }

    @Test
    void logicalAndOr_shortCircuitShape() {
        String src = "print( (1 < 2) && (2 < 3) ); print( (1 > 2) || (2 < 3) );";
        String out = run(src).replace("\r\n", "\n");
        assertEquals("true\ntrue\n", out);
    }
}
