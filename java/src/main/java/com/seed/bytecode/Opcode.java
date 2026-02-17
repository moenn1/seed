package com.seed.bytecode;

public enum Opcode {
    // Stack and locals
    ENTER,     // ENTER nlocals
    LEAVE,     // LEAVE
    CONST,     // CONST kIndex
    LOAD,      // LOAD localIndex
    STORE,     // STORE localIndex
    POP,       // POP
    DUP,       // DUP
    // Arithmetic / logic
    ADD, SUB, MUL, DIV,
    NOT,
    EQ, NE, LT, LE, GT, GE,
    // Control flow
    JMP,           // JMP relOffset
    JMP_IF_FALSE,  // JMP_IF_FALSE relOffset
    // Calls and return
    CALL,      // CALL funcIndex argc
    RET,       // RET
    // IO / runtime
    PRINT      // PRINT (pops one)
}
