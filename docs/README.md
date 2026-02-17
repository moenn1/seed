# Seed Compiler Project Documentation

Audience: You know Java well, decent C/C++, some ARM64 assembly, understand JIT vs AOT (HotSpot, Graal Native Image), GC, and some memory. Objective: Learn compilers end-to-end with a hands-on language “Seed”, connecting each step to JVM/HotSpot/Graal concepts. This doc is your living guide: definitions, design choices, how to run each step, and how it maps to production VMs and toolchains.

Repository layout
- Java frontend (fast iteration, learning parsing/interpreting):
  - seed/java/src/main/java/com/seed/lexer: Token, TokenType, Lexer
  - seed/java/src/main/java/com/seed/cli: CLI tools (LexDump now; AST/Run later)
- C++ backend (systems/VM/GC/codegen/LLVM):
  - seed/src/include/seed: public headers (gc.h, vm.h)
  - seed/src/cpp/runtime: runtime stubs
  - seed/src/cpp/vm: VM/GC stubs
  - seed/src/cpp/codegen: codegen (future)
  - seed/tests/cpp: GoogleTest suite
  - seed/scripts: helper scripts (disasm.sh)
- seed/examples: example programs
- Top-level CMake in seed/CMakeLists.txt

How to build and run (current functionality)
- Build C++ + run tests:
  - cd seed && cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug && cmake --build build -j && ctest --test-dir build -j
  - Run the VM stub: ./build/seedvm (prints “seedvm: OK”)
  - Disassemble an executable (requires llvm-objdump): ./scripts/disasm.sh ./build/seedvm
- Build Java frontend and dump tokens for the example:
  - cd seed/java && mvn -q -DskipTests package
  - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.LexDump ../examples/hello.seed

High-level compiler pipeline
source → lexer → parser → AST → resolver/type-check → IR or bytecode → (A) interpret on VM or (B) lower to native code → link → execute. Optional JIT tier (LLVM ORC) compiles hot paths at runtime.

Links to JVM/HotSpot/Graal:
- Frontend (lexer/parser/AST/resolution) parallels javac and Graal’s frontends.
- IR/optimization mirrors Graal’s high-tier and low-tier IR pipelines.
- VM interpreter parallels HotSpot’s template interpreter.
- AOT (our ARM64 backend) parallels javac+native compilers or Graal Native Image.
- JIT (LLVM ORC) parallels HotSpot C1/C2/Graal JIT with tiered compilation.
- GC runtime maps to HotSpot collectors (generational, barriers, safepoints).


0) Environment & sanity checks (done)
Definitions
- Assembler: translates assembly to machine code, produces an object/binary (clang -arch arm64 file.s -o prog).
- Disassembler: dumps machine code as assembly (llvm-objdump -d).
- Mach-O: macOS object/executable format.

Execute now
- Assemble/disassemble any binary:
  - ./scripts/disasm.sh ./build/seedvm

What to learn/observe
- Function prologues/epilogues, call/ret, branches, literal pools, symbol names.


1) Lexer (tokenizer) [Implemented in Java]
Definitions
- Token: (type, lexeme, position). Types: identifiers, literals, operators, delimiters, keywords, EOF.
- Lexing: deterministic scan that categorizes characters into tokens; ignores whitespace/comments.

Our design
- Hand-written lexer with line/column tracking.
- Supports: identifiers, int literals, operators (+ - * / ! = < <= > >= == !=), delimiters ((), {}, , ;), keywords (let, fn, if, else, while, return, true, false, print), // comments.

Execute now
- cd seed/java && mvn -q -DskipTests package
- java -cp target/seed-frontend-0.0.1.jar com.seed.cli.LexDump ../examples/hello.seed
- You will see a token stream including positions.

Notes/next improvements
- Add hex/binary literals, string literals, block comments, error handling strategies.
- Add JUnit tests (Milestone 1 tests) with golden token sequences and fuzz/property tests.

Mapping to JVM
- Mirrors the initial stage of javac or bytecode parser that tokenizes input streams for syntactic analysis.


2) Parser + AST (planned; implement next)
Definitions
- Grammar: defines structure of programs using productions (expressions/statements).
- Recursive descent: hand-written parser; easy to control error messages.
- Pratt parsing: precedence-based expression parser for concise operator handling.
- AST: Abstract Syntax Tree; semantic structure independent of concrete syntax.

Proposed Seed grammar (informal)
- program     → declaration* EOF
- declaration → funDecl | varDecl | statement
- funDecl     → "fn" IDENT "(" parameters? ")" block
- parameters  → IDENT ("," IDENT)*
- varDecl     → "let" IDENT ("=" expression)? ";"
- statement   → exprStmt | ifStmt | whileStmt | returnStmt | block
- exprStmt    → expression ";"
- ifStmt      → "if" "(" expression ")" block ( "else" block )?
- whileStmt   → "while" "(" expression ")" block
- returnStmt  → "return" expression? ";"
- block       → "{" declaration* "}"
- expression  → assignment
- assignment  → IDENT "=" assignment | logic_or
- logic_or    → logic_and ( "||" logic_and )*
- logic_and   → equality ( "&&" equality )*
- equality    → comparison ( ( "==" | "!=" ) comparison )*
- comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )*
- term        → factor ( ( "-" | "+" ) factor )*
- factor      → unary ( ( "/" | "*" ) unary )*
- unary       → ( "!" | "-" ) unary | call
- call        → primary ( "(" arguments? ")" )*
- arguments   → expression ( "," expression )*
- primary     → INT | TRUE | FALSE | IDENT | "(" expression ")"

AST (Java packages)
- com.seed.ast.Expr:
  - Literal(Int, Bool), Var(name), Unary(op, expr), Binary(op, left, right), Call(callee, args)
- com.seed.ast.Stmt:
  - Let(name, init), ExprStmt(expr), Block(stmts), If(cond, then, else), While(cond, body), Return(value)
- com.seed.parser.Parser: recursive descent with Pratt for Expr, good diagnostics.
- com.seed.cli.AstDump: parses file and pretty-prints AST.

How to execute (after implementation)
- mvn -q -DskipTests package
- java -cp target/seed-frontend-0.0.1.jar com.seed.cli.AstDump ../examples/hello.seed

Mapping to JVM
- Mirrors javac’s parse tree and initial AST structure; sets up for semantic analysis and code generation.


3) Interpreter (baseline semantics; planned)
Definitions
- Environment: scope chain mapping identifiers to values.
- Closure: function value capturing outer lexical bindings.

Design
- Evaluate AST nodes to produce values (int, boolean, function).
- Lexically scoped environments; call frames for functions; return handling via exceptions or explicit result.
- Short-circuit boolean ops; first-class functions (optional next).

CLI
- com.seed.cli.Run: interpret Seed source and print outputs.

Execute (after implementation)
- java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Run ../examples/hello.seed

Links to JVM
- Mirrors HotSpot’s bytecode interpreter tier for baseline semantics and fast startup.


4) Static semantics (planned)
Definitions
- Name resolution: link identifiers to declarations across lexical scopes.
- Type checking: optionally ensure operations are type-safe (int vs bool, arity checks).
- Diagnostics: helpful error messages with locations.

Design
- Resolver pass to build symbol tables, detect shadowing, undefined vars, duplicate params.
- Optional simple type system (monomorphic int/bool), or gradual with annotations: let x: int = ...

Execution
- com.seed.cli.Check: runs resolver/type checker and prints diagnostics; exits non-zero on errors.

Mapping to JVM
- Mirrors javac’s symbol resolution and basic type checks before IR generation.


5) IR or Bytecode design (planned)
Definitions
- IR: Intermediate Representation, often in SSA or 3-address code (TAC).
- Bytecode: compact stack-machine instructions, easy to interpret.

Choice
- Start with a simple stack-based bytecode for Track A VM (fast to implement).
- Optionally design a 3-address IR (TAC) as a stepping stone to native code/LLVM.

Proposed bytecode (sketch)
- Constants/vars: CONST k, LOAD n, STORE n
- Arithmetic/logic: ADD, SUB, MUL, DIV, NOT, EQ, NE, LT, LE, GT, GE
- Control: JMP label, JMP_IF_FALSE label
- Calls: CALL argc, RET
- Stack ops: POP, DUP
- Frames: ENTER nlocals, LEAVE

Lowering
- Compile AST to bytecode with a verifier and pretty-printer for dumps.

Execution (after implementation)
- A Java tool to dump bytecode; C++ VM to execute bytecode.


6) Bytecode VM + GC (Track A; planned)
Definitions
- Interpreter loop: fetch-decode-execute; direct or computed gotos (threaded).
- Heap: area for objects (strings, arrays, closures).
- GC mark-sweep: mark reachable objects then sweep unmarked ones.

Design
- seed/src/cpp/vm: implement a fast interpreter loop for stack bytecode.
- Runtime values: tagged union or pointer tagging; start with boxed heap objects.
- GC 1.0: stop-the-world mark-sweep; roots from VM stack + globals.
- Diagnostics: GC stats (allocations, time).

Execution (after implementation)
- Build: cd seed && cmake -S . -B build && cmake --build build -j
- Run: ./build/seedvm program.seed
- Compare perf vs AST interpreter.

Mapping to HotSpot
- Interpreter + simple GC analogous to HotSpot’s interpreter and early collectors (without gen/barriers yet).


7) AOT ARM64 codegen (Track A; planned)
Definitions
- Instruction selection: map IR ops to target instructions.
- Register allocation (RA): assign virtual registers to physical; start with Linear Scan.
- Calling convention (AArch64 macOS):
  - Arguments: x0–x7; return in x0.
  - Caller-saved: x0–x18 (subset); callee-saved: x19–x28, fp(x29), lr(x30).
  - Stack 16-byte aligned; frame pointer often x29.

Lowering plan
- From 3-address IR or selected form to ARM64:
  - Prologue: stp x29, x30, [sp, -frame]!; mov x29, sp; sub sp, sp, framesz
  - Epilogue: add sp, sp, framesz; ldp x29, x30, [sp], frame; ret
- Implement linear scan RA; spill to stack slots when needed.

Execution (after implementation)
- seedc -S file.seed → file.s (emit assembly)
- clang -arch arm64 file.s -o a.out && ./a.out
- Inspect with llvm-objdump -d -arch arm64 a.out

Mapping to HotSpot/Graal
- Similar to low-tier codegen and RA; introduces stack maps if integrating GC later.


8) LLVM backend + ORC JIT (Track B; planned)
Definitions
- LLVM IR: SSA-based IR used by clang/LLVM toolchain.
- ORC JIT: modern LLVM JIT API (LLJIT/LazyJIT) supporting on-the-fly compilation and symbol resolution.

Setup
- brew install llvm
- export PATH="/opt/homebrew/opt/llvm/bin:$PATH"
- For CMake: -DENABLE_LLVM=ON -DLLVM_DIR=/opt/homebrew/opt/llvm/lib/cmake/llvm

Plan
- Emit LLVM IR from frontend (or from your own IR).
- AOT: use llc/clang through LLVM APIs or shell-out.
- JIT: LLJIT; compile hot functions at runtime; add a tiny profiler to trigger JIT.

Execution (after implementation)
- seed-llvm -emit-llvm file.seed → file.ll
- seed-llvm -o a.out file.seed; ./a.out
- seed-jit run file.seed


9) Optimization passes (planned)
Definitions
- Constant folding, DCE (dead code elimination), CSE (common subexpression elimination), GVN, SCCP, LICM.
- Inlining: replacing call sites with callee bodies.

Plan
- Start with simple local peepholes on bytecode or TAC IR.
- Add CFG-based DCE and copy propagation.
- Implement a small pass pipeline with flags and before/after dumps.

Execution (after implementation)
- seed-opt -O1/-O2 file.seed → optimized IR dump and performance comparison.


10) GC upgrades & runtime integration (planned)
Definitions
- Generational GC: nursery for young objects; tenured space for survivors.
- Write barrier: records intergenerational pointers (card table).
- Safepoints: places where threads can be stopped for GC; stack maps needed for precise roots in native code.

Plan
- Add a copying nursery + mark-sweep tenured; implement card marking barrier in interpreter.
- For JITed code, emit barrier calls and safepoint polls (design notes first; implement later).

Execution (after implementation)
- GC flags for tuning; stress tests with randomized allocation patterns.


11) Linkers, loaders, debug info (planned)
Definitions
- Relocation: placeholders that the linker patches to final addresses.
- GOT/PLT: indirection for dynamic linking (ELF-centric concept; Mach-O analogues: stubs/Lazy Binding).
- DWARF: debug info format.

Practice
- llvm-objdump -h -r -d ./build/seedvm
- lldb ./build/seedvm (break main, step instructions)


12) Capstone and report (planned)
- Summarize features, performance, and relate each component to JVM/HotSpot/Graal.
- Include annotated disassembly and IR dumps.
- Provide a benchmark suite and results.


Implementation roadmap from here
Short-term (next commits)
- Milestone 1 tests: Add JUnit tests for the lexer (goldens + error tests).
- Milestone 2: Implement parser/AST in Java; add AstDump CLI; unit tests for precedence and errors.
- Milestone 3: Implement Java interpreter; differential tests (interpreter vs future VM).

Mid-term
- Milestone 5/6: Design bytecode + build C++ VM + mark-sweep GC; add execution of compiled bytecode emitted by Java compiler.
- Milestone 7: Add simple AOT ARM64 codegen from TAC IR with linear-scan RA.

Long-term
- Milestone 8: LLVM backend + ORC JIT tier; tiering policy (interpret → JIT).
- Milestone 9–11: Opt passes, GC upgrades, linking/debugging exploration.

Testing strategies
- Unit tests: Tokenization, parsing, interpreter results.
- Golden files: Token streams, AST prints, IR dumps, bytecode.
- Differential: Interpreter vs VM/native output equivalence.
- Property/fuzz: Grammar-based random programs; interpreter should not crash; error paths yield diagnostics.
- Performance: microbenchmarks; compare tiers (AST vs VM vs native).

Learning references (high-signal)
- Crafting Interpreters — craftinginterpreters.com
- Engineering a Compiler (Cooper & Torczon)
- Modern Compiler Implementation (Appel)
- CS 6120: Advanced Compilers (Cornell) — cs.cornell.edu/courses/cs6120/2023fa/
- LLVM Kaleidoscope Tutorial — llvm.org/docs/tutorial
- Linkers and Loaders (Levine)
- AOSA: LuaJIT — aosabook.org/en/luajit.html
- “Linear Scan Register Allocation for Java” (Poletto & Sarkar)