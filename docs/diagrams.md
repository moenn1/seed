# Seed Compiler Diagrams (PlantUML)

This document provides PlantUML diagrams to visualize the architecture and data flow across the Seed project: frontend (Java), IR/bytecode, VM/runtime (C++), AOT ARM64, and LLVM JIT. You can render these with:
- VS Code “PlantUML” extension (with Graphviz installed), or
- plantuml.jar + Graphviz: java -jar plantuml.jar -tpng diagrams.md
- Or copy a single @startuml…@enduml block to an online PlantUML server.

1) Overview: End-to-End Pipeline
```plantuml
@startuml
skinparam monochrome true
title Seed Pipeline Overview

rectangle "Java Frontend" {
  [Lexer] --> [Tokens]
  [Parser] --> [AST]
  [Resolver/Checks]
  [AST] --> [IR/Bytecode]
}

rectangle "Tooling" {
  [BytecodeDump CLI]
  [Compile CLI (to .sbc)]
}

rectangle "C++ Backend" {
  [VM Loader] --> [VM Interpreter]
  [VM Interpreter] --> [Runtime/GC]
  [AOT seedc] --> [ARM64 Asm] --> [Native Binary]
}

rectangle "LLVM Path" {
  [LLVM IR Builder] --> [ORC JIT]
}

[Source (.seed)] --> [Lexer]
[Tokens] --> [Parser]
[Parser] --> [AST]
[AST] --> [Resolver/Checks]
[Resolver/Checks] --> [IR/Bytecode]
[IR/Bytecode] --> [BytecodeDump CLI]
[IR/Bytecode] --> [Compile CLI (to .sbc)]
[SBC (.sbc)] --> [VM Loader]
[VM Loader] --> [VM Interpreter]
[IR/Bytecode] --> [AOT seedc]
[IR/Bytecode] --> [LLVM IR Builder]
[ORC JIT] --> [print: 8]
[Native Binary] --> [print: 8]
[VM Interpreter] --> [print: 8]
@enduml
```

2) Java Frontend: Core Classes
```plantuml
@startuml
skinparam monochrome true
title Java Frontend: Lexer, Parser, AST

package com.seed.lexer {
  class Lexer
  class Token {
    +TokenType type
    +String lexeme
    +int line
    +int col
  }
  enum TokenType
}

package com.seed.ast {
  interface Expr
  class "Expr.Literal" as ExprLiteral
  class "Expr.Variable" as ExprVariable
  class "Expr.Unary" as ExprUnary
  class "Expr.Binary" as ExprBinary
  class "Expr.Grouping" as ExprGrouping
  class "Expr.Call" as ExprCall

  interface Stmt
  class "Stmt.Let" as StmtLet
  class "Stmt.ExprStmt" as StmtExprStmt
  class "Stmt.Print" as StmtPrint
  class "Stmt.Block" as StmtBlock
  class "Stmt.If" as StmtIf
  class "Stmt.While" as StmtWhile
  class "Stmt.Return" as StmtReturn
  class "Stmt.Fun" as StmtFun

  class AstPrinter
}

package com.seed.parser {
  class Parser
  class ParseError
}

Lexer --> Token
Parser --> "builds" Expr
Parser --> "builds" Stmt
AstPrinter --> Expr
AstPrinter --> Stmt
@enduml
```

3) Static Semantics (Resolver)
```plantuml
@startuml
skinparam monochrome true
title Resolver Flow (Static Semantics)

actor Developer
Developer -> Resolver: resolve(program)
Resolver -> "Scope Stack": push(global)
Resolver -> Resolver: predeclare top-level functions
loop For each Stmt
  Resolver -> Resolver: stmt(s)
  alt let x = init
    Resolver -> Resolver: expr(init)
    Resolver -> "Scope Stack": declare x
  else if/while
    Resolver -> Resolver: expr(condition)
    Resolver -> "Diagnostics": condition must be bool
  else return
    Resolver -> Resolver: (optional) expr(value)
  else fun f(params) { ... }
    Resolver -> "Scope Stack": push(fn scope)
    Resolver -> "Scope Stack": declare params
    Resolver -> Resolver: stmt(body...)
    Resolver -> "Scope Stack": pop
  end
end
Resolver -> "Diagnostics": report (undefined, duplicate, arity, types)
@enduml
```

4) Bytecode and VM Execution (C++)
```plantuml
@startuml
skinparam monochrome true
title Bytecode VM Execution

participant "Module(.sbc)" as M
participant "VM Loader" as L
participant "VM Interpreter" as V
participant "Frame/Locals" as F
participant "Stack" as S
participant "Runtime/PRINT" as R

M -> L: parse textual .sbc
L -> V: Module{consts, funcs}
V -> V: run(entry="main")
V -> F: create Frame(main)
loop fetch-decode-execute
  V -> S: push/pop for CONST/LOAD/STORE
  V -> V: ALU (ADD/SUB/…)
  alt JMP_IF_FALSE
    V -> S: pop cond
    V -> V: branch if false
  end
  alt CALL f(argc)
    V -> F: push new Frame(f), map args to locals
  else RET
    V -> F: pop frame, push return on caller stack
  end
  alt PRINT
    V -> S: pop value
    V -> R: print value + newline
  end
end
@enduml
```

5) AOT ARM64: Minimal Lowering Slice
```plantuml
@startuml
skinparam monochrome true
title AOT ARM64 Minimal Flow (seedc)

participant "IR/Bytecode" as IR
participant "Pattern Recognizer" as P
participant "A64 Emitter" as E
participant "clang" as Clang
participant "Native Binary" as Bin
participant "OS/Loader" as OS

IR -> P: find hello.sbc pattern (main, add, args)
P -> E: emit __TEXT + cstring + _add/_main
E -> Clang: file.s
Clang -> Bin: link Mach-O
OS -> Bin: run
Bin -> OS: printf("%d\\n", result)
@enduml
```

6) LLVM JIT (ORC/LLJIT)
```plantuml
@startuml
skinparam monochrome true
title LLVM ORC JIT (LLJIT) Slice

participant "IR/Bytecode" as IR
participant "Recognizer" as Rec
participant "LLVM IR Builder" as B
participant "LLJIT" as J
participant "main()" as Main

IR -> Rec: infer a,b
Rec -> B: define add(i64,i64), main()
B -> J: add ThreadSafeModule
J -> J: compile + link
J -> Main: lookup "main"
Main -> Main: printf("%d\\n", (a+b))
@enduml
```

7) Data Model: Values/Frames/GC (future GC details)
```plantuml
@startuml
skinparam monochrome true
title Runtime Data Model (VM)

class Value <<union-like>> {
  +int64 i
  -- future --
  +ptr  obj
  +tag  type
}

class Frame {
  +Function* fn
  +int pc
  +locals: vector<Value>
}
class Stack {
  +vector<Value> data
}
class GC <<planned>> {
  +mark()
  +sweep()
  +roots: {Stack + Frames + Globals}
}

Frame "1" *-- "n" Value : locals
Stack "1" o-- "n" Value : data
@enduml
```

Rendering Tips
- VS Code: Install “PlantUML” + “Graphviz Preview” and reopen this file; or
- CLI: java -jar plantuml.jar -tpng diagrams.md
- For .puml, isolate a single @startuml…@enduml block per file and run plantuml on it.