# Milestone 3: AST Interpreter

Definitions
- Environment: lexical scope chain mapping identifiers to runtime values.
- Closure: function value that captures the surrounding environment at definition time.
- Return (non-local exit): mechanism to leave a function early with a value.

What we implemented
- Runtime Value model: Int, Bool, Fun, and Nil.
- Lexical Environment with parent links.
- Interpreter that executes Stmt/Expr:
  - let binds a new name in current environment
  - print evaluates and prints value
  - blocks introduce inner scopes
  - if/else and while use a truthiness rule (booleans and non-zero ints are true)
  - functions close over their defining environment; calls create a new frame and support return
  - arithmetic/comparison/equality and logical &&, ||

How to run
- Tests:
  - cd seed/java
  - mvn -q test
- Run a Seed program:
  - mvn -q -DskipTests package
  - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Run ../examples/hello.seed
  - Expected output (for examples/hello.seed): 8

Mapping to HotSpot/Graal
- Mirrors the interpreter tier in HotSpot (template interpreter) and Graal’s baseline execution.
- Establishes semantics before IR/bytecode and JIT tiers; later we’ll add profiling and tiered compilation.

Next
- Add assignment and closures with upvalues (optional), then lower to bytecode for the C++ VM (Milestone 5/6).
