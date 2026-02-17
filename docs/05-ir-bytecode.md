# Milestone 5: IR/Bytecode Design and Lowering

Definitions
- IR (Intermediate Representation): a machine-independent representation enabling multiple backends.
- Stack bytecode: a compact instruction set operating on an implicit operand stack.
- Verifier: a checker to ensure well-formedness (e.g., jump targets in range, call indices valid).

Instruction set (subset implemented)
- ENTER nlocals, LEAVE: setup/teardown for local frame
- CONST k, LOAD i, STORE i, POP, DUP
- ADD, SUB, MUL, DIV, NOT
- EQ, NE, LT, LE, GT, GE
- JMP rel, JMP_IF_FALSE rel (relative to the instruction after the jump)
- CALL funcIndex argc, RET
- PRINT

Lowering highlights
- program compiles to a synthetic main() with arity 0
- let x = e; compiles to (eval e) STORE slot(x)
- if (c) then [else]: compiles with JMP_IF_FALSE and patched labels
- while (c) body: loop start label, conditional branch to end, body, back edge
- Direct calls only: call by function name (variable callee not yet supported in bytecode compiler)
- Logical &&/|| currently evaluate both sides; truthiness is a runtime concern for the VM tier

CLI tools
- Bytecode dump (verifies, then prints):
  - cd seed/java && mvn -q -DskipTests package
  - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.BytecodeDump ../examples/hello.seed
- Compile to .sbc (textual):
  - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Compile ../examples/hello.seed ../out/hello.sbc
  - Result is human-readable; future C++ VM loader will consume .sbc

Sample output (abridged)
- For examples/hello.seed:

  ; Seed Bytecode (textual)
  .consts N
    ...
  .funcs 2

  .func 0 main arity=0 locals=...
     0  ENTER
     ..
     x  JMP_IF_FALSE k
     ..
     y  CALL 1 2   ; call add(a,b)
     z  PRINT
     .. LEAVE
     .. RET
  .end

  .func 1 add arity=2 locals=2
     0  ENTER
     1  LOAD 0
     2  LOAD 1
     3  ADD
     4  LEAVE
     5  RET
  .end

Mapping to JVM/HotSpot/Graal
- Comparable to a low-tier IR or interpreter-friendly bytecode enabling both interpretation and native code generation.
- Forms a bridge to the next milestones: C++ VM + GC (06), AOT ARM64 (07), and LLVM JIT (08).
