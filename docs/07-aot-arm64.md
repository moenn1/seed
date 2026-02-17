# Milestone 7: ARM64 AOT Codegen (initial slice)

Definitions
- Instruction selection: map IR/bytecode to target instructions.
- Calling convention (AArch64 macOS):
  - Args: x0–x7, return in x0
  - Callee-saved: x19–x28, fp(x29), lr(x30)
  - 16‑byte stack alignment; typical stp/ldp prologue/epilogue
- Page relocations:
  - adrp reg, sym@PAGE / add reg, reg, sym@PAGEOFF to materialize addresses (e.g., format strings)

What’s implemented in this slice
- A minimal recognizer over the hello.sbc program:
  - Finds main and add functions
  - Infers constants for add(a,b) call in the if-then branch
  - Emits AArch64 assembly for:
    - _add: returns a+b
    - _main: computes add(a,b), calls printf("%lld\n", result), returns 0
- This provides an end-to-end AOT path you can inspect/disassemble and tweak.

How to run
1) Ensure bytecode exists (from Java Milestone 5):
   - cd seed/java
   - mvn -q -DskipTests package
   - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Compile ../examples/hello.seed ../out/hello.sbc
2) Build seedc:
   - cd seed
   - cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug
   - cmake --build build -j
3) Generate assembly and build native binary:
   - ./build/src/cpp/seedc -S ./out/hello.sbc -o ./out/hello.s
   - clang -arch arm64 ./out/hello.s -o ./out/hello_aot
4) Run and disassemble:
   - ./out/hello_aot          # expected output: 8
   - ./scripts/disasm.sh ./out/hello_aot

Discussion and next steps
- This initial lowering is intentionally narrow to establish the toolchain and ABI details.
- Next iterations:
  - Generalize selection to handle common op sequences from bytecode (CONST/LOAD/ALU/CMP/JMP_IF_FALSE/CALL/PRINT)
  - Introduce a 3-address IR to simplify register allocation
  - Implement a linear scan register allocator for locals/temporaries
  - Add a small runtime shim (e.g., print) instead of calling libc directly
