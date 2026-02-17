#include "seed/vm.h"
#include "seed/gc.h"
#include <iostream>
namespace seed {
VM::VM() { gc::init(); }
void VM::print_hello() { std::cout << "seedvm: OK" << std::endl; }
} // namespace seed
