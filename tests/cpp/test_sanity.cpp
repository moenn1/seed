#include <gtest/gtest.h>
#include "seed/vm.h"

TEST(Sanity, Basic) {
  seed::VM vm;
  SUCCEED();
  EXPECT_EQ(1 + 1, 2);
}
