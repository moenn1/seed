#include "seed/vm.h"
#include <vector>
#include <iostream>
#include <stdexcept>

using namespace seed;

namespace {
struct Frame {
  const bc::Function* fn{};
  int pc{0};
  std::vector<long long> locals; // int-only for now
  Frame() = default;
  explicit Frame(const bc::Function* f): fn(f), locals(f->nlocals, 0) {}
};

inline bool truthy(long long v) { return v != 0; }

} // namespace

bool VM::run(const bc::Module& mod, const std::string& entry, std::ostream& out, std::string& err) {
  int entryIdx = mod.findFuncByName(entry);
  if (entryIdx < 0) { err = "Entry function '" + entry + "' not found"; return false; }

  std::vector<long long> stack;
  std::vector<Frame> callstack;
  callstack.emplace_back(&mod.funcs[entryIdx]);

  auto pop = [&](){
    if (stack.empty()) throw std::runtime_error("Stack underflow");
    long long v = stack.back(); stack.pop_back(); return v;
  };
  auto push = [&](long long v){ stack.push_back(v); };

  try {
    while (!callstack.empty()) {
      Frame& fr = callstack.back();
      if (fr.pc < 0 || fr.pc >= (int)fr.fn->code.size()) {
        throw std::runtime_error("PC out of range");
      }
      const bc::Instr& ins = fr.fn->code[fr.pc++];
      using Op = bc::Op;
      switch (ins.op) {
        case Op::ENTER: {
          // locals already sized to nlocals
          break;
        }
        case Op::LEAVE: {
          // nothing for now (no shrinking)
          break;
        }
        case Op::CONST: {
          int idx = ins.a;
          if (idx < 0 || idx >= (int)mod.consts.size()) throw std::runtime_error("CONST out of range");
          push(mod.consts[idx]);
          break;
        }
        case Op::LOAD: {
          int i = ins.a;
          if (i < 0 || i >= (int)fr.locals.size()) throw std::runtime_error("LOAD out of range");
          push(fr.locals[i]);
          break;
        }
        case Op::STORE: {
          int i = ins.a;
          if (i < 0 || i >= (int)fr.locals.size()) throw std::runtime_error("STORE out of range");
          long long v = pop();
          fr.locals[i] = v;
          break;
        }
        case Op::POP: {
          (void)pop();
          break;
        }
        case Op::DUP: {
          if (stack.empty()) throw std::runtime_error("DUP underflow");
          push(stack.back());
          break;
        }
        case Op::ADD: { long long b = pop(), a = pop(); push(a + b); break; }
        case Op::SUB: { long long b = pop(), a = pop(); push(a - b); break; }
        case Op::MUL: { long long b = pop(), a = pop(); push(a * b); break; }
        case Op::DIV: { long long b = pop(), a = pop(); push(a / b); break; }
        case Op::NOT: { long long a = pop(); push(truthy(a) ? 0 : 1); break; }
        case Op::EQ:  { long long b = pop(), a = pop(); push(a == b); break; }
        case Op::NE:  { long long b = pop(), a = pop(); push(a != b); break; }
        case Op::LT:  { long long b = pop(), a = pop(); push(a <  b); break; }
        case Op::LE:  { long long b = pop(), a = pop(); push(a <= b); break; }
        case Op::GT:  { long long b = pop(), a = pop(); push(a >  b); break; }
        case Op::GE:  { long long b = pop(), a = pop(); push(a >= b); break; }
        case Op::JMP: {
          fr.pc = fr.pc + ins.a;
          break;
        }
        case Op::JMP_IF_FALSE: {
          long long c = pop();
          if (!truthy(c)) {
            fr.pc = fr.pc + ins.a;
          }
          break;
        }
        case Op::CALL: {
          int fidx = ins.a;
          int argc = ins.b;
          if (fidx < 0 || fidx >= (int)mod.funcs.size()) throw std::runtime_error("CALL out of range");
          const bc::Function* cal = &mod.funcs[fidx];
          if (argc != cal->arity) throw std::runtime_error("CALL arity mismatch");
          // collect args from stack (rightmost on top)
          std::vector<long long> args(argc);
          for (int i = argc - 1; i >= 0; --i) { args[i] = pop(); }
          // push new frame
          callstack.emplace_back(cal);
          Frame& nf = callstack.back();
          // params in locals [0..arity-1]
          for (int i = 0; i < argc; ++i) nf.locals[i] = args[i];
          break;
        }
        case Op::RET: {
          long long rv = stack.empty() ? 0 : pop();
          callstack.pop_back();
          if (!callstack.empty()) {
            push(rv);
          } else {
            // returning from entry: print nothing; return success
          }
          break;
        }
        case Op::PRINT: {
          long long v = pop();
          out << v << "\n";
          break;
        }
        default:
          throw std::runtime_error("Unknown opcode at runtime");
      }
    }
  } catch (const std::exception& e) {
    err = e.what();
    return false;
  }
  return true;
}
