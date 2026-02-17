package com.seed.bytecode;

import java.util.ArrayList;
import java.util.List;

public final class Function {
    public final String name;
    public final int arity;
    public int nlocals;
    public final List<Instr> code = new ArrayList<>();

    public Function(String name, int arity, int nlocals) {
        this.name = name;
        this.arity = arity;
        this.nlocals = nlocals;
    }
}
