#pragma once
#include <string>
#include "seed/bytecode.h"

namespace seed::codegen::llvmjit {

// JIT-compile and run a tiny recognized subset of .sbc:
// - Finds main and add functions
// - Infers constants for add(a,b) in a print site (hello-like shape)
// - Builds an LLVM IR main that calls printf("%d\n", a+b)
// Returns true on success and captures stdout via the host process; on failure,
// sets err and returns false.
bool run_hello_like_program(const seed::bc::Module& mod, std::string& err);

}  // namespace seed::codegen::llvmjit