package com.seed.cli;

import com.seed.lexer.*;
import com.seed.parser.*;
import com.seed.ast.*;
import com.seed.bytecode.*;

import java.nio.file.*;
import java.util.*;

public class BytecodeDump {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: bytecode-dump <file.seed>");
            System.exit(2);
        }
        String src = Files.readString(Path.of(args[0]));
        Lexer lexer = new Lexer(src);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> program = parser.parseProgram();

        com.seed.bytecode.Compiler c = new com.seed.bytecode.Compiler();
        com.seed.bytecode.Module m = c.compile(program);
        Verifier v = new Verifier();
        var probs = v.verify(m);
        if (!probs.isEmpty()) {
            System.err.println("Verification problems:");
            for (var p : probs) System.err.println(p);
            System.exit(1);
        }
        System.out.println(TextWriter.write(m));
    }
}
