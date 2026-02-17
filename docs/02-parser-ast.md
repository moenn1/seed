# Milestone 2: Parser + AST

Definitions
- Grammar: formal structure of programs; we implement it via hand-written recursive descent (expressions with precedence).
- AST: Abstract Syntax Tree; captures program structure without concrete syntax.
- Precedence/associativity: define how operators group; e.g., * binds tighter than +.

What we implemented
- AST node classes (Expr, Stmt) in Java.
- Recursive descent parser (Parser) for Seed:
  - Declarations: let, fn
  - Statements: if/else, while, return, block, expression statements
  - Expressions: unary (!, -), binary (+, -, *, /, comparisons, ==, !=), logical &&, ||, calls f(a,b), grouping (...)
- CLI: AstDump to pretty print AST.
- Tests for precedence and constructs.

How to run
- Build and run tests:
  - cd seed/java
  - mvn -q test
- Pretty-print AST for the example:
  - mvn -q -DskipTests package
  - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.AstDump ../examples/hello.seed

Sample output (abridged)
- For examples/hello.seed:
  (let x 3)
  (fn add (a b) (return (+ a b)))
  (if (< x 10) (block (print (call add (x 5)))) )

Notes
- We added token types for && and || and support in the lexer.
- Assignment outside of let (e.g., x = x + 1;) is intentionally not yet implemented; we’ll add later when designing the bytecode/IR and environments.
- Error recovery is basic; we synchronize on ; and block boundaries.

Mapping to JVM/HotSpot/Graal
- Comparable to javac/Graal frontends producing an AST/IR for later optimization tiers.
