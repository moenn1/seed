package com.seed.cli;

import com.seed.lexer.*;
import com.seed.parser.*;
import com.seed.ast.*;
import com.seed.sema.*;

import java.nio.file.*;
import java.util.*;

public class Check {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: check <file.seed>");
            System.exit(2);
        }
        String src = Files.readString(Path.of(args[0]));
        Lexer lexer = new Lexer(src);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> program = parser.parseProgram();

        Resolver resolver = new Resolver();
        List<Diagnostic> diags = resolver.resolve(program);
        if (diags.isEmpty()) {
            System.out.println("OK");
            return;
        }
        for (Diagnostic d : diags) {
            System.out.println(d);
        }
        System.exit(1);
    }
}
