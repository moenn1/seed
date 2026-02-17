package com.seed.bytecode;

import java.util.ArrayList;
import java.util.List;

public final class Verifier {
    public static final class Problem {
        public final String where;
        public final String msg;
        public Problem(String where, String msg) { this.where = where; this.msg = msg; }
        public String toString() { return where + ": " + msg; }
    }

    public List<Problem> verify(Module m) {
        List<Problem> probs = new ArrayList<>();
        for (int i = 0; i < m.funcs.size(); i++) {
            Function f = m.funcs.get(i);
            // Minimal checks: CALL indices in range; JMP targets in range (approximate)
            for (int pc = 0; pc < f.code.size(); pc++) {
                Instr ins = f.code.get(pc);
                switch (ins.op) {
                    case CALL -> {
                        if (ins.a < 0 || ins.a >= m.funcs.size()) {
                            probs.add(new Problem(site(f, pc), "CALL funcIndex out of range"));
                        }
                    }
                    case JMP, JMP_IF_FALSE -> {
                        int tgt = pc + 1 + ins.a;
                        if (tgt < 0 || tgt > f.code.size()) {
                            probs.add(new Problem(site(f, pc), "Jump target out of range"));
                        }
                    }
                    default -> {}
                }
            }
        }
        return probs;
    }

    private String site(Function f, int pc) { return f.name + ":" + pc; }
}
