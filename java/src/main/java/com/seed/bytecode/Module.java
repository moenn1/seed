package com.seed.bytecode;

import java.util.ArrayList;
import java.util.List;

public final class Module {
    public final List<Object> consts = new ArrayList<>();
    public final List<Function> funcs = new ArrayList<>();

    public int addConst(Object o) {
        consts.add(o);
        return consts.size() - 1;
    }

    public int addFunction(Function f) {
        funcs.add(f);
        return funcs.size() - 1;
    }

    public int findFunction(String name, int arity) {
        for (int i = 0; i < funcs.size(); i++) {
            Function f = funcs.get(i);
            if (f.name.equals(name) && f.arity == arity) return i;
        }
        return -1;
    }

    public int findFunctionByName(String name) {
        for (int i = 0; i < funcs.size(); i++) {
            if (funcs.get(i).name.equals(name)) return i;
        }
        return -1;
    }
}
