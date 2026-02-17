package com.seed.cli;

import com.seed.lexer.*;
import com.seed.parser.*;
import com.seed.ast.*;
import com.seed.interp.*;

import java.nio.file.*;
import java.util.*;

public class Run {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: run <file.seed>");
            System.exit(1);
        }
        String src = Files.readString(Path.of(args[0]));
        Lexer lexer = new Lexer(src);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> program = parser.parseProgram();

        Interpreter interp = new Interpreter(System.out);
        interp.interpret(program);
    }
}
