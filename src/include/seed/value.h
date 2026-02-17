#pragma once
#include <cstdint>
#include <ostream>

namespace seed {

// Minimal tagged value for the VM. Today we support:
// - INT  (64-bit signed)
// - BOOL (0/1 stored in i_)
// - NIL  (unit)
// - OBJ  (reserved for future GC-managed heap objects; not used yet)
class Value {
public:
  enum class Tag : uint8_t { INT, BOOL, NIL, OBJ };

  static Value fromInt(int64_t v) { Value x; x.tag_ = Tag::INT; x.i_ = v; return x; }
  static Value fromBool(bool v)   { Value x; x.tag_ = Tag::BOOL; x.i_ = v ? 1 : 0; return x; }
  static Value Nil()              { Value x; x.tag_ = Tag::NIL;  x.i_ = 0; return x; }
  static Value fromObj(void* p)   { Value x; x.tag_ = Tag::OBJ;  x.p_ = p; return x; }

  bool isInt()  const { return tag_ == Tag::INT; }
  bool isBool() const { return tag_ == Tag::BOOL; }
  bool isNil()  const { return tag_ == Tag::NIL; }
  bool isObj()  const { return tag_ == Tag::OBJ; }

  int64_t asInt() const { return i_; }
  bool    asBool() const { return i_ != 0; }
  void*   asObj()  const { return p_;  }

  // Truthiness: false for NIL and BOOL(false) and INT(0), true otherwise
  bool truthy() const {
    if (isNil())  return false;
    if (isBool()) return asBool();
    if (isInt())  return asInt() != 0;
    // For future object types: non-null object is truthy
    return p_ != nullptr;
  }

  friend std::ostream& operator<<(std::ostream& os, const Value& v) {
    if (v.isInt())  return os << v.asInt();
    if (v.isBool()) return os << (v.asBool() ? 1 : 0);
    if (v.isNil())  return os << 0; // keep old semantics for now
    // OBJ printing (future): default to address
    return os << reinterpret_cast<std::uintptr_t>(v.asObj());
  }

private:
  Tag tag_{Tag::NIL};
  union {
    int64_t i_;
    void*   p_;
  };
};

} // namespace seed