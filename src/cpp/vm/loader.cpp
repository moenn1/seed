#include "seed/bytecode.h"
#include <fstream>
#include <sstream>
#include <unordered_map>
#include <cctype>

using namespace seed::bc;

static Op parseOp(const std::string& s) {
  static const std::unordered_map<std::string, Op> M = {
    {"ENTER", Op::ENTER}, {"LEAVE", Op::LEAVE},
    {"CONST", Op::CONST}, {"LOAD", Op::LOAD}, {"STORE", Op::STORE}, {"POP", Op::POP}, {"DUP", Op::DUP},
    {"ADD", Op::ADD}, {"SUB", Op::SUB}, {"MUL", Op::MUL}, {"DIV", Op::DIV},
    {"NOT", Op::NOT},
    {"EQ", Op::EQ}, {"NE", Op::NE}, {"LT", Op::LT}, {"LE", Op::LE}, {"GT", Op::GT}, {"GE", Op::GE},
    {"JMP", Op::JMP}, {"JMP_IF_FALSE", Op::JMP_IF_FALSE},
    {"CALL", Op::CALL}, {"RET", Op::RET},
    {"PRINT", Op::PRINT}
  };
  auto it = M.find(s);
  if (it == M.end()) throw std::runtime_error("Unknown opcode: " + s);
  return it->second;
}

bool seed::bc::loadTextModule(const std::string& path, Module& out, std::string& err) {
  std::ifstream in(path);
  if (!in) { err = "Cannot open " + path; return false; }
  std::string line;
  enum class State { Start, Consts, Funcs, InFunc } st = State::Start;
  Function curf;

  auto trim = [](std::string s) {
    size_t i = 0; while (i < s.size() && std::isspace(static_cast<unsigned char>(s[i]))) ++i;
    size_t j = s.size(); while (j > i && std::isspace(static_cast<unsigned char>(s[j-1]))) --j;
    return s.substr(i, j - i);
  };

  try {
    while (std::getline(in, line)) {
      std::string t = trim(line);
      if (t.empty() || t[0] == ';') continue;

      if (st == State::Start) {
        if (t.rfind(".consts", 0) == 0) { st = State::Consts; continue; }
        else if (t.rfind(".funcs", 0) == 0) { st = State::Funcs; continue; }
      } else if (st == State::Consts) {
        if (t.rfind(".funcs", 0) == 0) { st = State::Funcs; continue; }
        // "i: value"
        auto colon = t.find(':');
        if (colon == std::string::npos) continue;
        std::string sval = trim(t.substr(colon + 1));
        if (sval == "true") out.consts.push_back(1);
        else if (sval == "false") out.consts.push_back(0);
        else if (sval == "nil" || sval == "null") out.consts.push_back(0);
        else out.consts.push_back(std::stoll(sval));
      } else if (st == State::Funcs) {
        if (t.rfind(".func ", 0) == 0) {
          // .func <idx> <name> arity=X locals=Y
          std::istringstream ss(t);
          std::string dotfunc;
          int idx;
          std::string name;
          std::string arityEq, localsEq;
          ss >> dotfunc >> idx >> name >> arityEq >> localsEq;
          auto parseKV = [](const std::string& kv, const char* key)->int{
            auto p = kv.find('=');
            if (p == std::string::npos) return 0;
            return std::stoi(kv.substr(p+1));
          };
          curf = Function();
          curf.name = name;
          curf.arity = parseKV(arityEq, "arity");
          curf.nlocals = parseKV(localsEq, "locals");
          st = State::InFunc;
          continue;
        }
      } else if (st == State::InFunc) {
        if (t == ".end") {
          out.funcs.push_back(curf);
          st = State::Funcs;
          continue;
        }
        // "<pc>  OPC [a [b]]"
        std::istringstream ss(t);
        int pc; ss >> pc;
        std::string opstr; ss >> opstr;
        Instr ins{};
        ins.op = parseOp(opstr);
        if (ins.op == Op::CONST || ins.op == Op::LOAD || ins.op == Op::STORE ||
            ins.op == Op::JMP || ins.op == Op::JMP_IF_FALSE) {
          ss >> ins.a;
        } else if (ins.op == Op::CALL) {
          ss >> ins.a >> ins.b;
        }
        curf.code.push_back(ins);
      }
    }
  } catch (const std::exception& ex) {
    err = std::string("Parse error: ") + ex.what();
    return false;
  }
  return true;
}
