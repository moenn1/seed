#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." &>/dev/null && pwd)"
DOCS="$ROOT_DIR/docs"
mkdir -p "$DOCS"

# Update top-level project README to point to docs
cat > "$ROOT_DIR/README.md" << 'EOF'
# Seed: a teaching compiler/VM

Seed is a learn-by-building project to connect parsing, interpretation, bytecode VMs, native codegen (ARM64), JIT (LLVM), and GC — and relate them to HotSpot/Graal concepts.

Quick start
- Build C++ scaffold + tests:
  - cd seed && cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug && cmake --build build -j && ctest --test-dir build -j
- Build Java frontend:
  - cd seed/java && mvn -q -DskipTests package
- Tools:
  - Lex dump: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.LexDump ../examples/hello.seed
  - AST dump: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.AstDump ../examples/hello.seed
  - Run program: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Run ../examples/hello.seed

Documentation
See docs index: ./docs/README.md

Tracks
- Track A: custom bytecode VM + basic GC + ARM64 AOT
- Track B: LLVM IR backend + ORC JIT (tiered)
EOF

# Docs index with links and status
cat > "$DOCS/README.md" << 'EOF'
# Seed Documentation Index

This index links every milestone with definitions, design notes, how-to-run commands, and mapping to HotSpot/Graal.

Foundation
- 00-overview.md — the full learning roadmap and mapping to JVM/HotSpot/Graal
- 01-lexer.md — tokens, lexing, tests, CLI

Frontend (Java)
- 02-parser-ast.md — grammar, AST, parser design, AST printer
- 03-interpreter.md — AST interpreter, values, environments, return

Static Semantics
- 04-static-semantics.md — resolver, symbol tables, diagnostics, optional types

IR and Execution
- 05-ir-bytecode.md — bytecode/IR design, lowering, verifier, dumps
- 06-vm-gc.md — C++ bytecode VM, value repr, mark-sweep GC, perf/GC stats

Native and JIT
- 07-aot-arm64.md — ARM64 codegen, register allocation, calling convention
- 08-llvm-jit.md — LLVM IR, ORC/LLJIT, tiering

Optimization and Runtime
- 09-optimizations.md — pass suite (CSE/DCE/const-fold/LICM/etc.), benchmarking
- 10-gc-advanced.md — generational GC, barriers, safepoints, stack maps

Binary Toolchain
- 11-linking-debug.md — relocations, Mach-O sections, llvm-objdump, lldb

Capstone
- 12-capstone.md — feature checklist, perf summary, reflections

Execution cheat-sheet
- Build C++: cd seed && cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug && cmake --build build -j && ctest --test-dir build -j
- Build Java: cd seed/java && mvn -q -DskipTests package
- Lex dump: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.LexDump ../examples/hello.seed
- AST dump: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.AstDump ../examples/hello.seed
- Run program: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Run ../examples/hello.seed
EOF

# 00 Overview (synthesized from learning-plan with tighter execution notes)
cat > "$DOCS/00-overview.md" << 'EOF'
# Overview: Learning Roadmap and JVM/HotSpot/Graal Mapping

Goal: build a small language “Seed” and connect frontend → IR → optimization → VM → native codegen → JIT → GC, with explicit parallels to HotSpot/Graal.

Core parallels
- Frontend: Seed lexer/parser/AST ↔ javac/Graal frontend
- IR: Seed bytecode or TAC/SSA ↔ Graal’s high-tier/mid/low-tier IRs
- Interpreter: Seed AST/bytecode interpreter ↔ HotSpot template interpreter
- AOT: ARM64 backend ↔ static native compilers / Graal Native Image
- JIT: LLVM ORC tier ↔ HotSpot C1/C2/Graal JIT tiers (tiered compilation)
- GC: mark-sweep/generational ↔ HotSpot’s collectors (G1/ZGC/Shenandoah)

Current status
- Implemented: 01 Lexer, 02 Parser+AST, 03 Interpreter (Java)
- Next up: 04 Static Semantics, 05 IR/Bytecode, 06 C++ VM+GC, 07 ARM64, 08 LLVM JIT

How to run key tools
- See docs/README.md cheat-sheet and each milestone’s “How to run” section.
EOF

# 01 Lexer
cat > "$DOCS/01-lexer.md" << 'EOF'
# Milestone 1: Lexer

Definitions
- Token: a pair of (type, lexeme) with source position.
- Lexing: categorizing characters into tokens, skipping whitespace/comments.

Design
- Hand-written lexer in Java with line/col, keywords, identifiers, int literals, operators (+ - * / ! = < <= > >= == !=), delimiters, // comments.

Run and test
- Build: cd seed/java && mvn -q -DskipTests package
- Lex dump: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.LexDump ../examples/hello.seed
- Tests: mvn -q test (see LexerTest)

Mapping to HotSpot/Graal
- Comparable to the first stage in javac/Graal frontends; turns raw source into a structured stream for parsing.
EOF

# Ensure previously generated files exist; re-save 02 & 03 with small notes to cross-link
if [ -f "$DOCS/02-parser-ast.md" ]; then
  sed -i '' '1s/^/# Milestone 2: Parser + AST\n/' "$DOCS/02-parser-ast.md" || true
  printf '\nSee also: 01-lexer.md, 03-interpreter.md.\n' >> "$DOCS/02-parser-ast.md"
fi
if [ -f "$DOCS/03-interpreter.md" ]; then
  sed -i '' '1s/^/# Milestone 3: AST Interpreter\n/' "$DOCS/03-interpreter.md" || true
  printf '\nSee also: 02-parser-ast.md, 05-ir-bytecode.md.\n' >> "$DOCS/03-interpreter.md"
fi

# 04 Static Semantics
cat > "$DOCS/04-static-semantics.md" << 'EOF'
# Milestone 4: Static Semantics (Resolver and Diagnostics)

Definitions
- Resolution: map identifier uses to their declarations across lexical scopes.
- Shadowing: inner scope redeclares a name, hiding outer binding.
- Arity/type checks: basic validations like parameter counts and operator types.

Design plan
- Build a Resolver pass that walks the AST:
  - Tracks scope stack (blocks, functions).
  - Reports undefined variables and duplicate declarations.
  - Optionally checks int/bool for operations and conditionals.
- CLI: com.seed.cli.Check to run the resolver and print diagnostics; exit non-zero on error.

How to run (after implementation)
- Build Java: cd seed/java && mvn -q -DskipTests package
- Check: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Check ../examples/hello.seed

Mapping to HotSpot/javac
- Similar to javac symbol resolution and basic type checking before IR generation.
EOF

# 05 IR/Bytecode
cat > "$DOCS/05-ir-bytecode.md" << 'EOF'
# Milestone 5: IR/Bytecode Design and Lowering

Definitions
- IR: intermediate representation; TAC/SSA or stack machine bytecode.
- Verifier: checks well-formedness (no underflow, valid jumps, types per op).

Choice and design
- Start with a stack bytecode for fast Track A prototyping.
- Sketch instruction set:
  - CONST k | LOAD slot | STORE slot
  - ADD SUB MUL DIV | NOT | EQ NE LT LE GT GE
  - JMP label | JMP_IF_FALSE label
  - CALL argc | RET
  - ENTER nlocals | LEAVE | POP | DUP | PRINT
- Layout:
  - Constant pool; function table (name, arity, code offset); flat code array.

Lowering
- From AST to bytecode (Java pass) + textual dump for debugging + verifier.

How to run (after implementation)
- Dump bytecode: java -cp target/seed-frontend-0.0.1.jar com.seed.cli.BytecodeDump file.seed
- Save as .sbc (Seed ByteCode) for the C++ VM to consume.

Mapping to HotSpot/Graal
- Comparable to bytecode or low-tier IR that enables multiple backends (VM, AOT, JIT).
EOF

# 06 VM + GC
cat > "$DOCS/06-vm-gc.md" << 'EOF'
# Milestone 6: Bytecode VM and Basic GC (C++)

Definitions
- Interpreter loop: fetch-decode-execute.
- Heap and GC: allocate runtime objects; mark-sweep collector.

Plan
- Implement a compact value representation (e.g., tagged union).
- Interpreter for core bytecodes (stack, control flow, calls).
- Runtime library: print, small standard ops.
- GC 1.0: stop-the-world mark-sweep; roots from VM stack and globals; stats.

How to run (after implementation)
- Build: cd seed && cmake -S . -B build && cmake --build build -j
- Execute bytecode: ./build/seedvm program.sbc
- Disassemble native exe when AOT arrives: ./scripts/disasm.sh ./build/seedvm

Mapping to HotSpot
- Mirrors template interpreter and a simple collector; a base to add generational GC and barriers later.
EOF

# 07 ARM64 AOT
cat > "$DOCS/07-aot-arm64.md" << 'EOF'
# Milestone 7: ARM64 AOT Codegen

Definitions
- Instruction selection, register allocation (linear scan), calling convention.

AArch64 macOS calling convention (high-level)
- Args in x0–x7; return in x0.
- Callee-saved: x19–x28, fp(x29), lr(x30); 16-byte stack align.
- Typical prologue/epilogue with frame pointer (x29) and link register (x30).

Plan
- Lower 3-address form to ARM64 asm.
- Implement linear scan RA; spill to stack slots.
- Generate prologue/epilogue and calls to runtime (e.g., print).

How to run (after implementation)
- seedc -S file.seed > file.s
- clang -arch arm64 file.s -o a.out && ./a.out
- Inspect: llvm-objdump -d -arch arm64 a.out
EOF

# 08 LLVM + ORC JIT
cat > "$DOCS/08-llvm-jit.md" << 'EOF'
# Milestone 8: LLVM Backend and ORC JIT

Definitions
- LLVM IR (SSA), ORC (LLJIT), tiered compilation.

Plan
- Emit LLVM IR from Seed IR/AST.
- AOT via llc/clang or lib APIs.
- JIT via ORC/LLJIT; minimal profiler to mark hot functions; optional OSR/deopt notes.

Setup
- brew install llvm
- export PATH="/opt/homebrew/opt/llvm/bin:$PATH"
- CMake: -DENABLE_LLVM=ON -DLLVM_DIR=/opt/homebrew/opt/llvm/lib/cmake/llvm

How to run (after implementation)
- seed-llvm -emit-llvm file.seed > file.ll
- seed-llvm -o a.out file.seed && ./a.out
- seed-jit run file.seed
EOF

# 09 Optimizations
cat > "$DOCS/09-optimizations.md" << 'EOF'
# Milestone 9: Optimizations and Benchmarks

Definitions
- Const-fold, DCE, CSE/GVN, SCCP, LICM, inlining.

Plan
- Start with local peepholes; add CFG-based passes.
- Flags: -O0/-O1/-O2 to control pipeline.
- Before/after IR dumps; microbenchmarks and perf counters.

How to run (after implementation)
- seed-opt -O2 file.seed > file.opt.ir
- Compare run time vs baseline interpreter/VM/AOT.
EOF

# 10 Advanced GC
cat > "$DOCS/10-gc-advanced.md" << 'EOF'
# Milestone 10: Advanced GC (Generational, Barriers, Safepoints)

Definitions
- Generational hypothesis; card table; remembered sets; write barriers.
- Safepoints: instrumentation to stop threads for GC; stack maps.

Plan
- Add nursery + tenured spaces; copying nursery; remembered sets with card marking.
- For JIT code, emit barriers and safepoint polls (design and metadata layout).

How to run (after implementation)
- Tune GC flags; run randomized alloc tests; report pause-time and throughput.
EOF

# 11 Linking / Debugging
cat > "$DOCS/11-linking-debug.md" << 'EOF'
# Milestone 11: Linking, Loading, and Debugging

Definitions
- Relocations, symbol resolution, Mach-O sections, stubs/lazy binding.
- DWARF basics; mapping source to machine code.

Practice
- llvm-objdump -h -r -d ./build/seedvm
- lldb ./build/seedvm (break main, step, inspect frames)
EOF

# 12 Capstone
cat > "$DOCS/12-capstone.md" << 'EOF'
# Capstone: Feature Checklist and Performance Summary

Checklist
- Frontend (lexer/parser/AST) — implemented
- Interpreter — implemented
- Static semantics — TBD
- IR/bytecode — TBD
- VM + GC — TBD
- ARM64 AOT — TBD
- LLVM JIT — TBD
- Optimizations — TBD
- Advanced GC — TBD
- Linking/debug — TBD

Deliverables
- Engineering report connecting Seed components to HotSpot/Graal.
- Benchmarks across tiers (interp vs VM vs AOT vs JIT) with narrative.
EOF

echo "Docs updated:
- Updated seed/README.md and docs/README.md
- Added 00-overview.md, 01-lexer.md, 04..12 milestone docs
- Cross-linked existing 02/03 docs"