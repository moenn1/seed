# Seed: a teaching compiler/VM

This repo hosts:
- Java frontend (lexer/parser/interpreter) in `seed/java`
- C++ backend/runtime/VM in `seed/src/cpp` with CMake build
- Track A: custom VM + AOT ARM64
- Track B: optional LLVM backend + ORC JIT (enable with -DENABLE_LLVM=ON once LLVM is installed)

## Build C++ (Track A skeleton)
cd seed
cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug
cmake --build build -j
ctest --test-dir build -j

Run:
./build/seedvm

## Build Java frontend
cd seed/java
mvn -q -DskipTests package
java -cp target/seed-frontend-0.0.1.jar com.seed.cli.LexDump ../examples/hello.seed

## Disassemble a binary (requires llvm-objdump)
./scripts/disasm.sh ./build/seedvm
