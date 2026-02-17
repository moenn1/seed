#pragma once
#include <iosfwd>
#include <string>
#include "seed/bytecode.h"

namespace seed {

class VM {
public:
  VM() = default;

  // Run entry function (default "main") and print using provided stream.
  // Returns true on success; false and sets err on error.
  bool run(const bc::Module& mod, const std::string& entry, std::ostream& out, std::string& err);
};

} // namespace seed
