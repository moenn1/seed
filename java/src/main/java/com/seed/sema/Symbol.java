package com.seed.sema;

public class Symbol {
    public enum Kind { VAR, FUN }
    public enum Type { INT, BOOL, NIL, UNKNOWN, FUNCTION }

    public final Kind kind;
    public final Type type;   // For VAR: static type if known; For FUN: FUNCTION
    public final int arity;   // For FUN
    public Symbol(Kind kind, Type type, int arity) {
        this.kind = kind;
        this.type = type;
        this.arity = arity;
    }

    public static Symbol var(Type t) { return new Symbol(Kind.VAR, t, -1); }
    public static Symbol fun(int arity) { return new Symbol(Kind.FUN, Type.FUNCTION, arity); }
}
