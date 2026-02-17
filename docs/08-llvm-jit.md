# Milestone 8: LLVM Backend + ORC JIT (initial slice)

Definitions
- LLVM IR: SSA-based intermediate representation used by clang/LLVM.
- ORC JIT (LLJIT): modern, composable JIT APIs in LLVM for compiling and executing IR at runtime.
- ThreadSafeModule: wrapper for LLVM Module + Context safe to hand into JIT.

What’s implemented in this slice
- A recognizer for hello.sbc (same pattern as AOT slice): finds main/add and infers (a,b).
- In-memory LLVM IR:
  - define i64 @add(i64, i64) { ret (a+b) }
  - define i32 @main() { printf("%d\n", trunc.i32(add(a,b))); ret 0 }
- JIT via ORC LLJIT; looks up main and runs it.

Build and run (Apple Silicon; Homebrew LLVM)
1) brew install llvm
2) Configure & build:
   cmake -S . -B build_llvm -DCMAKE_BUILD_TYPE=Debug \
     -DENABLE_LLVM=ON \
     -DLLVM_DIR=/opt/homebrew/opt/llvm/lib/cmake/llvm
   cmake --build build_llvm -j
3) Ensure bytecode exists:
   (cd java && mvn -q -DskipTests package)
   java -cp java/target/seed-frontend-0.0.1.jar com.seed.cli.Compile examples/hello.seed out/hello.sbc
4) Run JIT:
   ./build_llvm/src/cpp/seedjit ./out/hello.sbc
Expected: prints a single line with the result (8).

Next steps
- Generalize lowering from your IR/bytecode to LLVM IR
- Add basic optimization passes (O1/O2) and compare with VM/AOT
- Expand runtime linkage/shims as needed
