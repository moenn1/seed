# C++ Backend: File-by-File Walkthrough (Verbose)

This document explains each C++ file in the project in detail: what it does, the key data structures and control flow, platform/ABI notes, pitfalls, and suggested exercises. Read this along with docs/06-vm-gc.md, docs/07-aot-arm64.md, and docs/08-llvm-jit.md.

Index
- Headers
  - src/include/seed/bytecode.h
  - src/include/seed/vm.h
  - src/include/seed/codegen/a64.h
  - src/include/seed/codegen/llvm_jit.h
- VM and Loader (Track A)
  - src/cpp/vm/loader.cpp
  - src/cpp/vm/vm_exec.cpp
  - src/cpp/vm/gc.cpp (stub)
  - src/cpp/apps/seedvm.cpp
- AOT ARM64 (Track A)
  - src/cpp/codegen/a64_emit.cpp
  - src/cpp/apps/seedc.cpp
- LLVM ORC JIT (Track B)
  - src/cpp/codegen/llvm_jit.cpp
  - src/cpp/apps/seedjit.cpp
- Build files
  - CMakeLists.txt (root)
  - src/cpp/CMakeLists.txt

Conventions
- Values are currently 64-bit integers in the VM (long long). Booleans use 0/1. This keeps the interpreter simple until we introduce a boxed representation for GC-able objects.
- The bytecode is a stack machine: most instructions pop their operands and push results.

==================================================
Headers
==================================================

src/include/seed/bytecode.h
Purpose
- Declares the in-memory representation for bytecode modules loaded from the textual .sbc produced by the Java frontend.
- Types: enum Op, Instr{op,a,b}, Function{name,arity,nlocals,code}, Module{consts,funcs}.
- Declares loadTextModule() for parsing the textual .sbc into Module.

Key Points
- Op enumerates the VM instruction set: stack ops (CONST/LOAD/STORE/POP/DUP), arithmetic/logic (ADD/SUB/…/NOT/EQ/NE/…), control flow (JMP/JMP_IF_FALSE), calls/returns (CALL/RET), and I/O (PRINT).
- Function.nlocals pre-sizes the locals vector per frame. The interpreter reserves that many local slots.
- Module.findFuncByName enables name-based call resolution if the compiler emitted direct-name calls.

Pitfalls
- CONST indices must be in range of Module.consts; loader and VM both check this and throw errors on out-of-range.
- nlocals needs to be accurate to avoid LOAD/STORE range errors in the interpreter.

Exercises
- Add a new opcode (e.g., MOD) end-to-end: extend enum Op, teach VM, teach compiler, and update docs.

src/include/seed/vm.h
Purpose
- Declares the VM class (seed::VM) that executes a Module starting at an entry function (default “main”).
- VM::run returns false with an error string on runtime failures (under/overflow, bad indices).

Key Points
- The VM implementation (vm_exec.cpp) owns the stack and call stack.

Pitfalls
- Currently integer-only; introducing objects requires a tagged representation and GC.

Exercises
- Add a flag to VM::run to toggle debug traces (print each decoded instruction).

src/include/seed/codegen/a64.h
Purpose
- Declares a minimal ARM64 emitter for the AOT slice (seedc). Its first version recognizes the hello.sbc pattern and emits assembly.

Key Points
- This is intentionally small to focus learning on sections, symbol addressing, and calling conventions. You’ll generalize it by mapping more bytecode sequences to AArch64.

Pitfalls
- Format strings and vararg calls require exact register setup (x0=fmt; integer arg in w1; stack alignment on more complex cases).

Exercises
- Make emitter handle print of a literal without calling add().

src/include/seed/codegen/llvm_jit.h
Purpose
- Declares the initial LLVM JIT slice entry point. It recognizes the hello.sbc shape and builds minimal LLVM IR for add + main, then runs via ORC LLJIT.

Key Points
- JIT uses ThreadSafeModule to hand an LLVM Module/Context to the JIT compiler.

Pitfalls
- Make sure ENABLE_LLVM and LLVM_DIR are set; otherwise the build guards stub the function with an “LLVM JIT not enabled” error.

Exercises
- Add a JIT option to print both a and b separately before printing the sum.

==================================================
VM and Loader (Track A)
==================================================

src/cpp/vm/loader.cpp
Purpose
- Parses the textual .sbc bytecode file into a Module (const pool + function table + instructions).

Flow
- The loader scans lines:
  - .consts N starts reading constants until .funcs
  - .func i name arity=X locals=Y starts a function block
  - Each instruction line includes a program counter, mnemonic, and operands
  - .end terminates a function block

Key Points
- parseOp maps string mnemonics to Op enum.
- CONST true/false map to 1/0; nil/null map to 0 to support initial semantics.
- For CALL, a stores function index, and b stores argc.

Pitfalls
- The textual format is simple; any change in text layout needs matching parser changes.
- No type info: VM trusts INT semantics (future GC/boxed values will extend this).

Exercises
- Validate that each function has at least one RET or add an implicit one at end for safety.

src/cpp/vm/vm_exec.cpp
Purpose
- Implements the interpreter loop for bytecode. It manages:
  - Operand stack (vector<long long>)
  - Call stack (vector<Frame>), each Frame has fn, pc, and locals

Flow
- run() looks up entry function by name; pushes its Frame
- Loop:
  - Fetch ins = fn.code[pc++]
  - Switch on ins.op:
    - CONST/LOAD/STORE/POP/DUP manipulates stack/locals
    - ADD/SUB/MUL/DIV/NOT/EQ/NE/LT/LE/GT/GE do arithmetic/logic; inputs are popped, results pushed
    - JMP adjusts pc relatively; JMP_IF_FALSE pops a condition and jumps if zero
    - CALL creates a new Frame for the callee and maps args to its locals 0..arity-1
    - RET pops return value (default 0) and returns to caller (or exits run if unwinding last frame)
    - PRINT pops and writes integer plus newline to the provided ostream

Key Points
- truthy() uses 0 = false, non-zero = true.
- Error handling throws std::runtime_error on underflow or out-of-range accesses and propagates back via err string.

Pitfalls
- Stack underflow: always ensure push/pop balance when you add new ops.
- Locals out of range: ensure nlocals in bytecode matches loads/stores used.

Exercises
- Add a trace option that prints “[pc] OP a [stack: …]” per step to visualize execution.
- Add a simple microbenchmark to compare AST interpreter (Java) vs VM bytecode (C++).

src/cpp/vm/gc.cpp (stub)
Purpose
- Placeholder for GC; currently exposes small heap counters and no-op collect().

Design notes for later
- Transition to a tagged representation (e.g., low bit tag or NaN-boxing) to store pointers vs ints.
- GC roots: the operand stack and all frames’ locals; mark from these roots, sweep the heap.

src/cpp/apps/seedvm.cpp
Purpose
- CLI entry for the VM: seedvm program.sbc
- Loads Module via loadTextModule() and calls VM::run(mod, "main", std::cout, err)

Pitfalls
- Non-zero exit on load or runtime error prints the cause to stderr.

Exercises
- Add a flag “--trace” to enable trace mode (when implemented in VM).

==================================================
AOT ARM64 (Track A)
==================================================

src/cpp/codegen/a64_emit.cpp
Purpose
- Emitter for a minimal AArch64 “hello.sbc” pattern:
  - Finds main and add
  - Infers constants a1 and a2
  - Emits:
    - _add: returns x0 + x1
    - _main: moves immediates to x0/x1, calls _add, prepares printf and prints result

Mach-O / AArch64 Notes (macOS)
- Sectioning:
  - __TEXT,__text for code
  - __TEXT,__cstring for literal strings (format strings)
- Symbol resolution:
  - adrp x0, Lfmt@PAGE
  - add  x0, x0, Lfmt@PAGEOFF
  This is the canonical way to materialize an address to a cstring symbol on Mach-O.
- Vararg call (printf):
  - x0: points to format string
  - subsequent integer arg: w1
  - Stack alignment must be maintained for more complex scenarios (here our simple case is fine)

Pitfalls
- Passing values in the wrong registers for varargs produces garbage output.
- PIE and linker warnings: on recent macOS/arm64 -no_pie is deprecated; prefer correct sectioning and symbol emission.

Exercises
- Generalize to handle printing a loaded local (LOAD) or a CONST without add().

src/cpp/apps/seedc.cpp
Purpose
- CLI entry for AOT emission: seedc -S input.sbc -o out.s
- Loads Module, runs the emitter, writes assembly.

Pitfalls
- This minimal slice does not cover linking of external runtime helpers; it calls libc printf directly.

Exercises
- Add a “-run” mode to assemble/link and run, printing stdout and saving the assembly alongside.

==================================================
LLVM ORC JIT (Track B)
==================================================

src/cpp/codegen/llvm_jit.cpp
Purpose
- Initial LLVM JIT pipeline:
  - Recognizes hello-like pattern in bytecode (finds add and constants)
  - Builds an LLVM Module in-memory:
    - i64 @add(i64,i64) returns sum
    - i32 @main() prints (int)sum via printf("%d\n")
  - Uses orc::LLJIT to compile and run main()

Key APIs
- LLVMContext / Module / IRBuilder: constructs IR
- GlobalVariable for a cstring, accessed via GEP
- FunctionType/Function for add and main
- orc::LLJITBuilder, ThreadSafeModule, lookup() for execution

Pitfalls
- Ensure ENABLE_LLVM=ON and valid LLVM_DIR; otherwise the build uses the stub (returns error).
- printf declaration uses varargs (i32 (i8*, ...)) and GEP to get fmt pointer.

Exercises
- Add a JIT option to call into a runtime helper (e.g., int add1(int x)) to illustrate symbol resolution.

src/cpp/apps/seedjit.cpp
Purpose
- CLI entry: seedjit program.sbc
- Loads Module; calls run_hello_like_program(); returns nonzero with error on failure.

==================================================
Build Files
==================================================

CMakeLists.txt (root)
Purpose
- Project setup and ENABLE_LLVM flag. When ENABLE_LLVM=ON and LLVM_DIR is provided, defines -DENABLE_LLVM_JIT and configures includes/definitions.

Gotchas
- Tip message reminds you to set LLVM_DIR=/opt/homebrew/opt/llvm/lib/cmake/llvm on Apple Silicon.
- Tests enabled via enable_testing(); GoogleTest is fetched in tests/cpp/CMakeLists.txt.

src/cpp/CMakeLists.txt
Purpose
- Defines static libraries (seed_runtime, seed_vm, seed_codegen) and apps (seedvm, seeddasm, seedc, seedjit).
- Links LLVM into seedjit only when ENABLE_LLVM is set at configure time.

Exercises
- Add an install(TARGETS seedc seedjit RUNTIME DESTINATION bin) if you want to install these tools.

==================================================
Platform/ABI Crib Notes (AArch64 macOS)
==================================================
- Function prologue/epilogue: callee saves x29(fp)/x30(lr) with stp/ldp; ensure 16-byte stack alignment.
- Calling convention: args in x0–x7 (32-bit if the argument is i32 as wN), return in x0.
- Varargs: x0 must be the format pointer; subsequent args in order (w1 for %d). Be very careful with widths (w-registers vs x-registers).

==================================================
Suggested Learning/Modification Exercises
==================================================
1) Add an opcode: MOD
   - Extend Op enum, VM switch, and compiler lowering (Java side) so “a % b” works in the VM.

2) Trace mode in VM
   - Add a boolean to VM::run to print each instruction with stack state; use it to debug simple programs.

3) Generalize AOT emission
   - Recognize CONST → PRINT without calling add.
   - Emit code that preserves the result across fmt materialization (adrp/add) with the correct vararg register setup.

4) Extend LLVM JIT
   - Generate LLVM IR directly from bytecode sequences (not just hello pattern).
   - Insert optimization passes (O1/O2) and compare runtime vs VM/AOT on microbenchmarks.

5) GC Prototype
   - Replace Value with a tagged union, add a simple heap for boxed strings, and implement mark + sweep. Add GC stats per run and stress it with allocation-heavy programs.

==================================================
FAQ / Troubleshooting
==================================================
- Disassembly shows unknown -c arg in llvm-objdump:
  - Our disasm.sh falls back to otool -tvV automatically on macOS.

- seedjit reports “LLVM JIT not enabled at build time”:
  - Reconfigure with -DENABLE_LLVM=ON and a valid -DLLVM_DIR path (Homebrew: /opt/homebrew/opt/llvm/lib/cmake/llvm).

- seedvm errors with “Stack underflow” or LOAD/STORE out of range:
  - Re-check nlocals in the Function metadata and ensure compiler lowering sets it high enough for locals used.

- printf prints garbage:
  - For AOT/varargs: confirm x0 points to cstring (adrp/add), integer arg placed in w1 (not x1), and that you preserve values around the format address materialization.

With this document and the milestone docs, you should have enough verbosity to understand the C++ backend in depth and modify it confidently.