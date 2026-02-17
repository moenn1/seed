# Milestone 4: Static Semantics

Definitions
- Resolution: link each identifier use to its declaration across lexical scopes.
- Shadowing: inner scope redeclares a name, hiding an outer one.
- Diagnostics: structured messages (line:col + message) emitted by static checks.
- (Optional) Types: assign basic types (int/bool) to expressions to validate operators and conditions.

What we implemented
- A Resolver that:
  - Builds a scope stack for blocks and functions
  - Pre-declares top-level functions to check call arities
  - Checks:
    - Undefined identifiers
    - Duplicate declarations in the same scope
    - Duplicate function parameters
    - Arity mismatch for named calls
    - Operator/condition checks (ints for arithmetic/comparisons; bools for !, &&, ||; bool for if/while conditions)
  - Returns a list of Diagnostic entries; the CLI prints them and exits with failure if any exist

How to run
- Build Java:
  - cd seed/java && mvn -q -DskipTests package
- Check a file:
  - java -cp target/seed-frontend-0.0.1.jar com.seed.cli.Check ../examples/hello.seed
  - Expected: "OK"
- Introduce an error (e.g., print(x); without let x) and re-run to see diagnostics.

Mapping to JVM/HotSpot/Graal
- Similar to javac symbol resolution and basic type checks that run before IR generation in production compilers/VMs.
