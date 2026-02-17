#pragma once
#include <string>
#include "seed/bytecode.h"

namespace seed::codegen::a64 {

// Emit macOS ARM64 assembly text for a tiny recognized subset of Module:
// - main calls add(a,b) and prints result via printf("%lld\n", result)
// - add is a 2-arg integer addition
// Returns true on success; false and sets err on failure.
bool emit_hello_like_program(const seed::bc::Module& mod, std::string& out_asm, std::string& err);

} // namespace seed::codegen::a64
