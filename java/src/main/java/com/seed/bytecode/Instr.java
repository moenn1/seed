package com.seed.bytecode;

public final class Instr {
    public final Opcode op;
    public final int a;
    public final int b;

    public Instr(Opcode op) { this(op, 0, 0); }
    public Instr(Opcode op, int a) { this(op, a, 0); }
    public Instr(Opcode op, int a, int b) { this.op = op; this.a = a; this.b = b; }

    @Override public String toString() {
        return switch (op) {
            case ENTER, LEAVE, ADD, SUB, MUL, DIV, NOT, EQ, NE, LT, LE, GT, GE, POP, DUP, RET, PRINT -> op.name();
            case CONST, LOAD, STORE, JMP, JMP_IF_FALSE -> op.name() + " " + a;
            case CALL -> op.name() + " " + a + " " + b; // funcIndex argc
        };
    }
}
