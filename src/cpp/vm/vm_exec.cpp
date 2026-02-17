#include "seed/vm.h"
#include "seed/value.h"
#include "seed/gc.h"
#include <vector>
#include <iostream>
#include <stdexcept>
#include <cstdlib>

using namespace seed;

namespace {
static const char* opname(bc::Op op) {
  using Op = bc::Op;
  switch (op) {
    case Op::ENTER: return "ENTER";
    case Op::LEAVE: return "LEAVE";
    case Op::CONST: return "CONST";
    case Op::LOAD: return "LOAD";
    case Op::STORE: return "STORE";
    case Op::POP: return "POP";
    case Op::DUP: return "DUP";
    case Op::ADD: return "ADD";
    case Op::SUB: return "SUB";
    case Op::MUL: return "MUL";
    case Op::DIV: return "DIV";
    case Op::MOD: return "MOD";
    case Op::NOT: return "NOT";
    case Op::EQ: return "EQ";
    case Op::NE: return "NE";
    case Op::LT: return "LT";
    case Op::LE: return "LE";
    case Op::GT: return "GT";
    case Op::GE: return "GE";
    case Op::JMP: return "JMP";
    case Op::JMP_IF_FALSE: return "JMP_IF_FALSE";
    case Op::CALL: return "CALL";
    case Op::RET: return "RET";
    case Op::PRINT: return "PRINT";
    default: return "UNKNOWN";
  }
}

struct Frame {
  const bc::Function* fn{};
  int pc{0};
  std::vector<Value> locals; // tagged values
  Frame() = default;
  explicit Frame(const bc::Function* f): fn(f), locals(f->nlocals, Value::Nil()) {}
};

inline bool truthy(const Value& v) { return v.truthy(); }

} // namespace

bool VM::run(const bc::Module& mod, const std::string& entry, std::ostream& out, std::string& err) {
  int entryIdx = mod.findFuncByName(entry);
  if (entryIdx < 0) { err = "Entry function '" + entry + "' not found"; return false; }

  std::vector<Value> stack;
  std::vector<Frame> callstack;
  callstack.emplace_back(&mod.funcs[entryIdx]);

  // Debug trace (stderr) if set
  bool trace = false;
  if (const char* t = std::getenv("SEED_TRACE")) {
    std::string tv(t);
    trace = (!tv.empty() && tv != "0" && tv != "false" && tv != "FALSE");
  }

  auto pop = [&](){
    if (stack.empty()) throw std::runtime_error("Stack underflow");
    Value v = stack.back(); stack.pop_back(); return v;
  };
  auto push = [&](const Value& v){ stack.push_back(v); };

  // Optional periodic GC: collect every N executed instructions using current roots
  std::size_t op_count = 0;
  long gc_every = 0;
  if (const char* g = std::getenv("SEED_GC_EVERY")) {
    try { gc_every = std::stol(std::string(g)); } catch (...) { gc_every = 0; }
    if (gc_every < 0) gc_every = 0;
  }

  auto collect_roots = [&](){
    if (gc_every <= 0) return;
    std::vector<void*> roots;
    roots.reserve(stack.size());
    // Stack roots
    for (const auto& v : stack) {
      if (v.isObj() && v.asObj() != nullptr) roots.push_back(v.asObj());
    }
    // Frame local roots
    for (const auto& fr2 : callstack) {
      for (const auto& lv : fr2.locals) {
        if (lv.isObj() && lv.asObj() != nullptr) roots.push_back(lv.asObj());
      }
    }
    gc::collect(roots);
  };

  try {
    while (!callstack.empty()) {
      Frame& fr = callstack.back();
      if (fr.pc < 0 || fr.pc >= (int)fr.fn->code.size()) {
        throw std::runtime_error("PC out of range");
      }
      const bc::Instr& ins = fr.fn->code[fr.pc++];
      using Op = bc::Op;
      ++op_count;
      if (gc_every > 0 && (op_count % static_cast<std::size_t>(gc_every) == 0)) {
        collect_roots();
      }

      if (trace) {
        std::cerr << "[pc=" << (fr.pc - 1) << "] " << opname(ins.op)
                  << " a=" << ins.a << " b=" << ins.b
                  << " stack=" << stack.size() << "\n";
      }
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
          push(Value::fromInt(mod.consts[idx]));
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
          Value v = pop();
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
        case Op::ADD: { Value b = pop(), a = pop(); push(Value::fromInt(a.asInt() + b.asInt())); break; }
        case Op::SUB: { Value b = pop(), a = pop(); push(Value::fromInt(a.asInt() - b.asInt())); break; }
        case Op::MUL: { Value b = pop(), a = pop(); push(Value::fromInt(a.asInt() * b.asInt())); break; }
        case Op::DIV: { Value b = pop(), a = pop(); push(Value::fromInt(a.asInt() / b.asInt())); break; }
        case Op::MOD: { Value b = pop(), a = pop(); push(Value::fromInt(a.asInt() % b.asInt())); break; }
        case Op::NOT: { Value a = pop(); push(Value::fromBool(!truthy(a))); break; }
        case Op::EQ:  { Value b = pop(), a = pop(); push(Value::fromBool(a.asInt() == b.asInt())); break; }
        case Op::NE:  { Value b = pop(), a = pop(); push(Value::fromBool(a.asInt() != b.asInt())); break; }
        case Op::LT:  { Value b = pop(), a = pop(); push(Value::fromBool(a.asInt() <  b.asInt())); break; }
        case Op::LE:  { Value b = pop(), a = pop(); push(Value::fromBool(a.asInt() <= b.asInt())); break; }
        case Op::GT:  { Value b = pop(), a = pop(); push(Value::fromBool(a.asInt() >  b.asInt())); break; }
        case Op::GE:  { Value b = pop(), a = pop(); push(Value::fromBool(a.asInt() >= b.asInt())); break; }
        case Op::JMP: {
          fr.pc = fr.pc + ins.a;
          break;
        }
        case Op::JMP_IF_FALSE: {
          Value c = pop();
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
          std::vector<Value> args(argc);
          for (int i = argc - 1; i >= 0; --i) { args[i] = pop(); }
          // push new frame
          callstack.emplace_back(cal);
          Frame& nf = callstack.back();
          // params in locals [0..arity-1]
          for (int i = 0; i < argc; ++i) nf.locals[i] = args[i];
          break;
        }
        case Op::RET: {
          Value rv = stack.empty() ? Value::Nil() : pop();
          callstack.pop_back();
          if (!callstack.empty()) {
            push(rv);
          } else {
            // returning from entry: print nothing; return success
          }
          break;
        }
        case Op::PRINT: {
          Value v = pop();
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