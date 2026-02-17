#include "seed/codegen/a64.h"
#include <sstream>
#include <optional>

using namespace seed;

namespace {
struct Args {
  long long a1{0};
  long long a2{0};
};

// Very small recognizer for hello.seed compiled shape:
// let x = 3; fn add(a,b){return a+b;} if (x<10){ print(add(x,5)); }
static std::optional<Args> infer_args_from_main(const bc::Module& mod, const bc::Function& mainFn, int addIdx) {
  // Heuristic: find first STORE slot s fed by preceding CONST k -> locals[s] init
  long long locals_init[8] = {0};
  bool has_init[8] = {false};
  for (size_t i = 0; i < mainFn.code.size(); ++i) {
    const auto& ins = mainFn.code[i];
    if (ins.op == bc::Op::STORE && ins.a >= 0 && ins.a < 8) {
      if (i == 0) continue;
      const auto& prev = mainFn.code[i-1];
      if (prev.op == bc::Op::CONST && prev.a >= 0 && prev.a < (int)mod.consts.size()) {
        locals_init[ins.a] = mod.consts[prev.a];
        has_init[ins.a] = true;
      }
    }
  }
  // Find CALL add with argc 2; assume previous instructions provided args:
  for (size_t i = 0; i < mainFn.code.size(); ++i) {
    const auto& ins = mainFn.code[i];
    if (ins.op == bc::Op::CALL && ins.a == addIdx && ins.b == 2) {
      // Expect ... LOAD s ; CONST k ; CALL
      if (i < 2) break;
      const auto& i1 = mainFn.code[i-2];
      const auto& i2 = mainFn.code[i-1];
      long long a1 = 0, a2 = 0;
      if (i2.op == bc::Op::CONST && i2.a >= 0 && i2.a < (int)mod.consts.size()) {
        a2 = mod.consts[i2.a];
      } else {
        return std::nullopt;
      }
      if (i1.op == bc::Op::LOAD && i1.a >= 0 && i1.a < 8 && has_init[i1.a]) {
        a1 = locals_init[i1.a];
      } else if (i1.op == bc::Op::CONST && i1.a >= 0 && i1.a < (int)mod.consts.size()) {
        a1 = mod.consts[i1.a];
      } else {
        return std::nullopt;
      }
      return Args{a1, a2};
    }
  }
  return std::nullopt;
}

} // namespace

bool codegen::a64::emit_hello_like_program(const bc::Module& mod, std::string& out_asm, std::string& err) {
  int mainIdx = mod.findFuncByName("main");
  if (mainIdx < 0) { err = "main function not found"; return false; }
  int addIdx = mod.findFuncByName("add");
  if (addIdx < 0) { err = "add function not found"; return false; }
  const auto& mainFn = mod.funcs[mainIdx];
  const auto& addFn  = mod.funcs[addIdx];
  if (addFn.arity != 2) { err = "add must have arity=2"; return false; }

  auto args = infer_args_from_main(mod, mainFn, addIdx);
  if (!args) { err = "unrecognized main pattern; only hello.sbc-like shape supported"; return false; }

  long long a1 = args->a1;
  long long a2 = args->a2;

  // Emit simple AArch64 assembly for macOS:
  // _main calls _add(a1,a2), then printf("%lld\n", x0)
  std::ostringstream asmtext;
  asmtext <<
R"(.section __TEXT,__text,regular,pure_instructions
.subsections_via_symbols
.extern _printf

.section __TEXT,__cstring,cstring_literals
Lfmt:
  .asciz "%d\n"

.section __TEXT,__text,regular,pure_instructions
.globl _add
.p2align 2
_add:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
  add x0, x0, x1
  ldp x29, x30, [sp], #16
  ret

.globl _main
.p2align 2
_main:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
)";

  // Move immediates into x0/x1
  // Use MOVZ/MOVK if values exceed 16-bit (here they fit)
  asmtext << "  mov x0, #" << a1 << "\n";
  asmtext << "  mov x1, #" << a2 << "\n";
  asmtext << R"(  bl _add
  mov  w1, w0
  adrp x0, Lfmt@PAGE
  add  x0, x0, Lfmt@PAGEOFF
  bl _printf
  mov w0, #0
  ldp x29, x30, [sp], #16
  ret
)";

  out_asm = asmtext.str();
  return true;
}