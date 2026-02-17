package com.seed.cli;

import com.seed.lexer.*;
import com.seed.parser.*;
import com.seed.ast.*;

import java.nio.file.*;
import java.util.*;

public class AstDump {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: ast-dump <file.seed>");
            System.exit(1);
        }
        String src = Files.readString(Path.of(args[0]));
        Lexer lexer = new Lexer(src);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> program = parser.parseProgram();

        AstPrinter printer = new AstPrinter();
        System.out.println(printer.print(program));
    }
}
