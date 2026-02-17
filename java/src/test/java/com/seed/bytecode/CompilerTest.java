package com.seed.bytecode;

import com.seed.ast.Stmt;
import com.seed.lexer.Lexer;
import com.seed.lexer.Token;
import com.seed.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompilerTest {

    private Module compile(String src) {
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.scanTokens();
        Parser p = new Parser(toks);
        List<Stmt> prog = p.parseProgram();
        Compiler c = new Compiler();
        return c.compile(prog);
    }

    @Test
    void helloCompiles_andVerifies() {
        String src = ""
                + "let x = 3;\n"
                + "fn add(a, b) { return a + b; }\n"
                + "if (x < 10) { print(add(x, 5)); }\n";
        Module m = compile(src);
        Verifier v = new Verifier();
        var probs = v.verify(m);
        assertTrue(probs.isEmpty(), "Verifier problems: " + probs);
        // Ensure main and add exist
        assertTrue(m.findFunctionByName("main") >= 0);
        assertTrue(m.findFunctionByName("add") >= 0);
        // Ensure CALL to add is present in main or then-block
        boolean hasCall = m.funcs.stream().anyMatch(f ->
                f.code.stream().anyMatch(ins -> ins.op == Opcode.CALL));
        assertTrue(hasCall, "Expected a CALL in bytecode");
    }

    @Test
    void functionArity_andLocals() {
        String src = "fn add(a, b) { return a + b; }";
        Module m = compile(src);
        int idx = m.findFunctionByName("add");
        assertTrue(idx >= 0);
        Function f = m.funcs.get(idx);
        assertEquals(2, f.arity);
        assertTrue(f.nlocals >= 2);
    }
}
