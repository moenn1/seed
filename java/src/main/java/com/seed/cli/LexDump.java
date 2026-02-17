package com.seed.cli;

import com.seed.lexer.*;
import java.nio.file.*;
import java.util.*;

public class LexDump {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: lex-dump <file.seed>");
            System.exit(1);
        }
        String src = Files.readString(Path.of(args[0]));
        Lexer lexer = new Lexer(src);
        List<Token> tokens = lexer.scanTokens();
        for (Token t : tokens) {
            System.out.println(t);
        }
    }
}
