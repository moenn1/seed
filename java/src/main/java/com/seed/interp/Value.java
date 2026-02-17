package com.seed.interp;

public interface Value {
    final class IntVal implements Value {
        public final int v;
        public IntVal(int v) { this.v = v; }
    }
    final class BoolVal implements Value {
        public final boolean v;
        public BoolVal(boolean v) { this.v = v; }
    }
    final class FunVal implements Value {
        public final com.seed.lexer.Token name; // may be null for anon
        public final java.util.List<com.seed.lexer.Token> params;
        public final java.util.List<com.seed.ast.Stmt> body;
        public final Environment closure;
        public FunVal(com.seed.lexer.Token name,
                      java.util.List<com.seed.lexer.Token> params,
                      java.util.List<com.seed.ast.Stmt> body,
                      Environment closure) {
            this.name = name; this.params = params; this.body = body; this.closure = closure;
        }
    }
    final class Nil implements Value {
        public static final Nil INSTANCE = new Nil();
        private Nil() {}
    }

    static String show(Value v) {
        if (v instanceof IntVal i) return Integer.toString(i.v);
        if (v instanceof BoolVal b) return Boolean.toString(b.v);
        if (v instanceof FunVal f) {
            String n = (f.name == null) ? "<anon>" : f.name.lexeme;
            return "<fn " + n + "/" + f.params.size() + ">";
        }
        if (v instanceof Nil) return "nil";
        return "<unknown>";
    }
}
