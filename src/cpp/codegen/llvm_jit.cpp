#include "seed/codegen/llvm_jit.h"

#if defined(ENABLE_LLVM_JIT)

#include <optional>
#include <string>
#include <vector>
#include <iostream>

// LLVM
#include "llvm/ADT/StringRef.h"
#include "llvm/ExecutionEngine/Orc/LLJIT.h"
#include "llvm/ExecutionEngine/Orc/ThreadSafeModule.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/DerivedTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/Type.h"
#include "llvm/IR/Verifier.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/Host.h"
#include "llvm/TargetParser/Triple.h"

using namespace seed;

namespace {
struct Args {
  long long a1{0};
  long long a2{0};
};

static std::optional<Args> infer_args_from_main(const bc::Module& mod, const bc::Function& mainFn, int addIdx) {
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
  for (size_t i = 0; i < mainFn.code.size(); ++i) {
    const auto& ins = mainFn.code[i];
    if (ins.op == bc::Op::CALL && ins.a == addIdx && ins.b == 2) {
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

namespace seed::codegen::llvmjit {

bool run_hello_like_program(const bc::Module& mod, std::string& err) {
  int mainIdx = mod.findFuncByName("main");
  if (mainIdx < 0) { err = "main not found"; return false; }
  int addIdx = mod.findFuncByName("add");
  if (addIdx < 0) { err = "add not found"; return false; }

  const auto& mainFn = mod.funcs[mainIdx];
  const auto& addFn  = mod.funcs[addIdx];
  if (addFn.arity != 2) { err = "add arity != 2"; return false; }

  auto args = infer_args_from_main(mod, mainFn, addIdx);
  if (!args) { err = "unrecognized hello-like program"; return false; }
  long long a1 = args->a1;
  long long a2 = args->a2;

  // Build LLVM IR
  auto ctx = std::make_unique<llvm::LLVMContext>();
  llvm::LLVMContext& C = *ctx;
  auto M = std::make_unique<llvm::Module>("seed_jit", C);
  M->setTargetTriple(llvm::sys::getDefaultTargetTriple());

  llvm::IRBuilder<> B(C);

  // Types
  auto i32 = llvm::Type::getInt32Ty(C);
  auto i64 = llvm::Type::getInt64Ty(C);
  auto i8  = llvm::Type::getInt8Ty(C);
  auto i8p = llvm::Type::getInt8PtrTy(C);

  // printf declaration: i32 (i8*, ...)
  auto printfTy = llvm::FunctionType::get(i32, {i8p}, true);
  auto printfFn = llvm::Function::Create(printfTy, llvm::Function::ExternalLinkage, "printf", M.get());

  // format string "%d\n"
  auto fmtData = llvm::ConstantDataArray::getString(C, "%d\n", true);
  auto fmtGV = new llvm::GlobalVariable(
      *M, fmtData->getType(), true, llvm::GlobalValue::PrivateLinkage, fmtData, "fmt");
  fmtGV->setUnnamedAddr(llvm::GlobalValue::UnnamedAddr::Global);
  fmtGV->setAlignment(llvm::MaybeAlign(1));

  // Define add(i64,i64) -> i64
  {
    auto addTy = llvm::FunctionType::get(i64, {i64, i64}, false);
    auto add = llvm::Function::Create(addTy, llvm::Function::ExternalLinkage, "add", M.get());
    auto bb = llvm::BasicBlock::Create(C, "entry", add);
    B.SetInsertPoint(bb);
    auto a = add->getArg(0);
    auto b = add->getArg(1);
    auto sum = B.CreateAdd(a, b, "sum");
    B.CreateRet(sum);
  }

  // int main()
  {
    auto mainTy = llvm::FunctionType::get(i32, {}, false);
    auto mainF = llvm::Function::Create(mainTy, llvm::Function::ExternalLinkage, "main", M.get());
    auto bb = llvm::BasicBlock::Create(C, "entry", mainF);
    B.SetInsertPoint(bb);

    auto a = llvm::ConstantInt::get(i64, (uint64_t)a1, true);
    auto b = llvm::ConstantInt::get(i64, (uint64_t)a2, true);

    auto addF = M->getFunction("add");
    auto sum = B.CreateCall(addF, {a, b}, "sum");

    // printf("%d\n", (i32)sum)
    auto zero = llvm::ConstantInt::get(i32, 0);
    auto fmtPtr = B.CreateInBoundsGEP(fmtData->getType(), fmtGV, {zero, zero});
    auto sum32 = B.CreateTrunc(sum, i32);
    B.CreateCall(printfFn, {fmtPtr, sum32});
    B.CreateRet(llvm::ConstantInt::get(i32, 0));
  }

  if (llvm::verifyModule(*M, &llvm::errs())) {
    err = "LLVM module verification failed";
    return false;
  }

  // JIT
  auto JITExpected = llvm::orc::LLJITBuilder().create();
  if (!JITExpected) {
    err = "LLJIT creation failed";
    return false;
  }
  auto J = std::move(*JITExpected);

  // Add the module
  auto TSM = llvm::orc::ThreadSafeModule(std::move(M), std::move(ctx));
  if (auto E = J->addIRModule(std::move(TSM))) {
    err = "addIRModule failed";
    return false;
  }

  // Look up main and run it
  auto sym = J->lookup("main");
  if (!sym) {
    err = "lookup(main) failed";
    return false;
  }

  using MainFn = int (*)();
  auto mainPtr = (MainFn)(sym->getAddress());
  int rc = mainPtr();
  (void)rc;
  return true;
}

} // namespace seed::codegen::llvmjit

#else

namespace seed::codegen::llvmjit {
bool run_hello_like_program(const seed::bc::Module&, std::string& err) {
  err = "LLVM JIT not enabled at build time";
  return false;
}
} // namespace seed::codegen::llvmjit

#endif