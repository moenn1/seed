#include <iostream>
#include "seed/bytecode.h"
#include "seed/codegen/llvm_jit.h"

int main(int argc, char** argv) {
  if (argc != 2) {
    std::cerr << "usage: seedjit <program.sbc>\n";
    return 2;
  }
  std::string path = argv[1];
  seed::bc::Module mod;
  std::string err;
  if (!seed::bc::loadTextModule(path, mod, err)) {
    std::cerr << "load error: " << err << "\n";
    return 1;
  }
  if (!seed::codegen::llvmjit::run_hello_like_program(mod, err)) {
    std::cerr << "jit error: " << err << "\n";
    return 1;
  }
  return 0;
}