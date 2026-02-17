#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace seed::bc {

enum class Op {
  ENTER, LEAVE,
  CONST, LOAD, STORE, POP, DUP,
  ADD, SUB, MUL, DIV,
  NOT,
  EQ, NE, LT, LE, GT, GE,
  JMP, JMP_IF_FALSE,
  CALL, RET,
  PRINT
};

struct Instr {
  Op op;
  int a{0};
  int b{0};
};

struct Function {
  std::string name;
  int arity{0};
  int nlocals{0};
  std::vector<Instr> code;
};

struct Module {
  std::vector<long long> consts;
  std::vector<Function> funcs;

  int findFuncIndex(const std::string& name, int arity) const {
    for (int i = 0; i < static_cast<int>(funcs.size()); ++i) {
      if (funcs[i].name == name && funcs[i].arity == arity) return i;
    }
    return -1;
  }
  int findFuncByName(const std::string& name) const {
    for (int i = 0; i < static_cast<int>(funcs.size()); ++i) {
      if (funcs[i].name == name) return i;
    }
    return -1;
  }
};

// Load textual .sbc (as produced by Java TextWriter)
bool loadTextModule(const std::string& path, Module& out, std::string& err);

} // namespace seed::bc
