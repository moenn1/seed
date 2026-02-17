package com.seed.sema;

import com.seed.ast.Stmt;
import com.seed.lexer.Lexer;
import com.seed.lexer.Token;
import com.seed.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResolverTest {

    private List<Diagnostic> check(String src) {
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.scanTokens();
        Parser p = new Parser(toks);
        List<Stmt> prog = p.parseProgram();
        Resolver r = new Resolver();
        return r.resolve(prog);
    }

    @Test
    void undefinedVariable_reported() {
        String src = "print(x);";
        List<Diagnostic> ds = check(src);
        assertTrue(ds.stream().anyMatch(d -> d.message.contains("Undefined identifier")), "Expected undefined identifier diag");
    }

    @Test
    void duplicateDeclaration_reported() {
        String src = "let x = 1; let x = 2;";
        List<Diagnostic> ds = check(src);
        assertTrue(ds.stream().anyMatch(d -> d.message.contains("Duplicate declaration")), "Expected duplicate declaration diag");
    }

    @Test
    void duplicateParam_reported() {
        String src = "fn f(a, a) { return 0; }";
        List<Diagnostic> ds = check(src);
        assertTrue(ds.stream().anyMatch(d -> d.message.contains("Duplicate parameter")), "Expected duplicate parameter diag");
    }

    @Test
    void arityMismatch_forNamedCall_reported() {
        String src = "fn add(a,b) { return a+b; } print(add(1));";
        List<Diagnostic> ds = check(src);
        assertTrue(ds.stream().anyMatch(d -> d.message.contains("Arity mismatch")), "Expected arity mismatch diag");
    }

    @Test
    void typeChecks_arithAndLogic() {
        String src = "print(true + 1); print(1 < false); if (1) { print(1); }";
        List<Diagnostic> ds = check(src);
        String joined = String.join("\n", ds.stream().map(d -> d.message).toList());
        assertTrue(joined.contains("expects int operands"), "Arithmetic int check missing");
        assertTrue(joined.contains("expects int operands"), "Comparison int check missing");
        assertTrue(joined.contains("condition should be boolean"), "If condition bool check missing");
    }

    @Test
    void okProgram_noDiagnostics() {
        String src = ""
                + "let x = 3;\n"
                + "fn add(a, b) { return a + b; }\n"
                + "if (x < 10) { print(add(x, 5)); }\n";
        List<Diagnostic> ds = check(src);
        assertTrue(ds.isEmpty(), "Expected no diagnostics, got: " + ds);
    }
}
