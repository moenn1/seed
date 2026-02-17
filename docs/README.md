# Seed Documentation Index

Start here if you want a structured, end-to-end learning path. Each milestone includes definitions, design notes, “how to run” commands, and mapping to HotSpot/Graal.

1) Orientation and Getting Started
- 00-overview.md — Learning roadmap and mappings to JVM/HotSpot/Graal (frontend → IR → VM → AOT → JIT → GC)
- diagrams.md — PlantUML diagrams of the whole system (pipeline, classes, VM, AOT, JIT, runtime model)
- Quick run targets
  - Java tools:
    - cd seed/java && mvn -q -DskipTests package
    - Lex: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.LexDump ../examples/hello.seed
    - AST: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.AstDump ../examples/hello.seed
    - Run: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Run ../examples/hello.seed
    - Compile .sbc: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Compile ../examples/hello.seed ../out/hello.sbc
  - C++ VM:
    - cd seed && cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug && cmake --build build -j
    - ./build/src/cpp/seedvm ./out/hello.sbc  (prints 8)
    - Disassemble: ./scripts/disasm.sh ./build/src/cpp/seedvm (otool fallback auto)
  - AOT ARM64 (initial slice):
    - ./build/src/cpp/seedc -S ./out/hello.sbc -o ./out/hello.s
    - clang -arch arm64 ./out/hello.s -o ./out/hello_aot
    - ./out/hello_aot
  - LLVM JIT (if LLVM installed):
    - brew install llvm
    - cmake -S . -B build_llvm -DCMAKE_BUILD_TYPE=Debug -DENABLE_LLVM=ON -DLLVM_DIR=/opt/homebrew/opt/llvm/lib/cmake/llvm
    - cmake --build build_llvm -j
    - ./build_llvm/src/cpp/seedjit ./out/hello.sbc

2) Frontend (Java)
- 01-lexer.md — Tokens, lexing, tests, LexDump CLI
- 02-parser-ast.md — Grammar, AST, recursive descent, precedence, AstDump CLI
- 03-interpreter.md — AST Interpreter (Value/Environment/Return), Run CLI

3) Static Semantics
- 04-static-semantics.md — Resolver, symbol tables, diagnostics (undefined, duplicate names, arity/type checks)

4) IR and Execution
- 05-ir-bytecode.md — Stack bytecode design, compiler (AST→bytecode), verifier, writer, CLI tools

5) VM and GC
- 06-vm-gc.md — C++ bytecode VM (loader + interpreter) now runnable; roadmap to add GC

6) Native and JIT
- 07-aot-arm64.md — ARM64 AOT (seedc) minimal emitter, assembly linking/run, disassembly
- 08-llvm-jit.md — LLVM IR + ORC JIT (seedjit) initial slice; how-to-run with Homebrew LLVM

Binary Tooling and Debug
- scripts/disasm.sh — Disassembles a Mach-O binary
  - Prefers llvm-objdump -d -arch arm64 if available, otherwise falls back to otool -tvV
  - This avoids unsupported flags on your macOS llvm-objdump toolchain

Learning checklist by milestone
- Milestone 1 (Lexer): understand tokens, trivia, error reporting with line/col
- Milestone 2 (Parser+AST): grammar, precedence, AST structure/printing
- Milestone 3 (Interpreter): runtime values, environments, closures, control
- Milestone 4 (Static semantics): symbol tables, shadowing, arity/type checks, diagnostics
- Milestone 5 (IR/Bytecode): instruction set, stack discipline, verifier
- Milestone 6 (VM): fetch/decode/execute, frames/locals, I/O, branch and calls
- Milestone 7 (AOT): intro to AArch64 sections, calling convention, format string addressing, prologue/epilogue
- Milestone 8 (LLVM JIT): emitting minimal LLVM IR, linking printf, JIT main()

PlantUML (diagrams.md) rendering
- VS Code: Install “PlantUML” + “Graphviz Preview”
- CLI: java -jar plantuml.jar -tpng diagrams.md
- Or copy any @startuml…@enduml block into an online PlantUML renderer

HotSpot/Graal mapping crib sheet
- Frontend → javac/Graal frontends
- IR/Bytecode → intermediate/low-tier IRs
- VM interpreter → HotSpot template interpreter
- AOT → analog to native-image/static compilers
- JIT (ORC) → analogous to Graal/HotSpot tiered JIT
- GC (planned) → design towards generational/remembered sets (barriers, safepoints)

Status and expectations
- VM path (Milestone 6) is fully correct and prints 8 for hello.sbc; ideal for semantics learning and debugging.
- AOT (Milestone 7) produces runnable binaries and correct disassembly flows; the initial emitter is intentionally narrow and will be broadened, with stabilized output formatting as a follow-up.
- JIT (Milestone 8) compiles an IR slice via ORC LLJIT; expanders will generalize lowering, add optimization passes, and perf comparisons.