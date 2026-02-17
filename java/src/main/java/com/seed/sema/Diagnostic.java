package com.seed.sema;

public class Diagnostic {
    public final int line;
    public final int col;
    public final String message;

    public Diagnostic(int line, int col, String message) {
        this.line = line;
        this.col = col;
        this.message = message;
    }

    @Override public String toString() {
        return line + ":" + col + " " + message;
    }
}
