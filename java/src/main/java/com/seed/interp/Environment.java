package com.seed.interp;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Value> values = new HashMap<>();
    public final Environment parent;

    public Environment() { this.parent = null; }
    public Environment(Environment parent) { this.parent = parent; }

    public void define(String name, Value value) {
        values.put(name, value);
    }

    public boolean assignIfExists(String name, Value value) {
        if (values.containsKey(name)) { values.put(name, value); return true; }
        if (parent != null) return parent.assignIfExists(name, value);
        return false;
    }

    public Value get(String name) {
        if (values.containsKey(name)) return values.get(name);
        if (parent != null) return parent.get(name);
        throw new RuntimeException("Undefined variable '" + name + "'");
    }
}
