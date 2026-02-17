package com.seed.bytecode;

import java.util.stream.Collectors;

public final class TextWriter {
    public static String write(Module m) {
        StringBuilder sb = new StringBuilder();
        sb.append("; Seed Bytecode (textual)\n");
        // Consts
        sb.append(".consts ").append(m.consts.size()).append("\n");
        for (int i = 0; i < m.consts.size(); i++) {
            Object c = m.consts.get(i);
            sb.append("  ").append(i).append(": ").append(String.valueOf(c)).append("\n");
        }
        // Functions
        sb.append(".funcs ").append(m.funcs.size()).append("\n");
        for (int i = 0; i < m.funcs.size(); i++) {
            Function f = m.funcs.get(i);
            sb.append("\n.func ").append(i).append(" ").append(f.name)
              .append(" arity=").append(f.arity).append(" locals=").append(f.nlocals).append("\n");
            for (int pc = 0; pc < f.code.size(); pc++) {
                sb.append(String.format("%4d  %s\n", pc, f.code.get(pc).toString()));
            }
            sb.append(".end\n");
        }
        return sb.toString();
    }
}
