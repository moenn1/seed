package com.seed.interp;

public class Return extends RuntimeException {
    public final Value value;
    public Return(Value value) { super(null, null, false, false); this.value = value; }
}
