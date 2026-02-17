#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include "seed/bytecode.h"
#include "seed/codegen/a64.h"

static void usage() {
  std::cerr << "usage: seedc -S <input.sbc> -o <out.s>\n";
}

int main(int argc, char** argv) {
  std::string in, out;
  bool dashS = false;
  for (int i = 1; i < argc; ++i) {
    std::string a = argv[i];
    if (a == "-S") dashS = true;
    else if (a == "-o" && i + 1 < argc) { out = argv[++i]; }
    else if (a[0] != '-') { in = a; }
    else { usage(); return 2; }
  }
  if (!dashS || in.empty() || out.empty()) { usage(); return 2; }

  seed::bc::Module mod;
  std::string err;
  if (!seed::bc::loadTextModule(in, mod, err)) {
    std::cerr << "load error: " << err << "\n";
    return 1;
  }

  std::string asmtext;
  if (!seed::codegen::a64::emit_hello_like_program(mod, asmtext, err)) {
    std::cerr << "codegen error: " << err << "\n";
    return 1;
  }

  std::ofstream ofs(out);
  ofs << asmtext;
  ofs.close();
  if (!ofs) {
    std::cerr << "write error for " << out << "\n";
    return 1;
  }
  std::cout << "Wrote " << out << "\n";
  return 0;
}
