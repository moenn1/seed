#include <iostream>
#include "seed/bytecode.h"
#include "seed/vm.h"

int main(int argc, char** argv) {
  if (argc != 2) {
    std::cerr << "usage: seedvm <program.sbc>\n";
    return 2;
  }
  std::string path = argv[1];
  seed::bc::Module mod;
  std::string err;
  if (!seed::bc::loadTextModule(path, mod, err)) {
    std::cerr << "load error: " << err << "\n";
    return 1;
  }
  seed::VM vm;
  if (!vm.run(mod, "main", std::cout, err)) {
    std::cerr << "vm error: " << err << "\n";
    return 1;
  }
  return 0;
}
