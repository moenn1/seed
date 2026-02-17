# Milestone 6: Bytecode VM (Track A) — Now runnable

Definitions
- Interpreter loop: fetch → decode → execute the next instruction.
- Frame: per-call activation record with locals and program counter (PC).
- Truthiness: our VM treats 0 as false and non-zero as true; booleans are integers 0/1.

What we implemented (minimal)
- Textual .sbc loader (parses output of the Java BytecodeDump/Compile tools)
- VM that executes a subset used by examples:
  - Stack ops: CONST, LOAD, STORE, POP, DUP
  - Arithmetic/logic: ADD, SUB, MUL, DIV, NOT, EQ, NE, LT, LE, GT, GE
  - Control flow: JMP, JMP_IF_FALSE (pops the condition)
  - Calls/returns: CALL, RET with locals and arity checks
  - Frames: ENTER/LEAVE initialize/signal locals lifetime (no shrinking yet)
  - IO: PRINT prints an integer followed by newline

How to run (end-to-end)
1) Build C++:
   - cd seed
   - cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug
   - cmake --build build -j
2) Ensure bytecode exists (from Java):
   - cd seed/java
   - mvn -q -DskipTests package
   - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Compile ../examples/hello.seed ../out/hello.sbc
3) Run VM:
   - cd seed
   - ./build/seedvm ./out/hello.sbc
   - Expected output: 8

Mapping to HotSpot
- Mirrors the template interpreter conceptually (though vastly simpler).
- Establishes runtime calling convention, locals/frames, and truthiness semantics that later inform GC, stack maps, and safepoints.

Next (GC stub to full GC)
- Introduce a Value representation that can hold heap pointers (for strings/arrays later).
- Implement a basic mark-sweep collector:
  - Roots = VM operand stack + locals of all frames
  - Mark = DFS from roots; Sweep = reclaim unmarked
- Add GC stats and stress tests (allocation patterns).
