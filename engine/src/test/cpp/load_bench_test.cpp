#include "minidaw/engine/load_bench.h"

#include <gtest/gtest.h>

#include <cmath>
#include <cstddef>
#include <vector>

namespace minidaw::engine {
namespace {

std::vector<float> clickLike() {
  std::vector<float> values(1440);
  for (size_t i = 0; i < values.size(); ++i) {
    values[i] = static_cast<float>(0.5 * std::sin(0.26 * static_cast<double>(i)));
  }
  return values;
}

TEST(LoadBench, TheCountsFollowTheStandardTestProjects) {
  const LoadBench mvp(mvpCounts(), clickLike());
  EXPECT_EQ(mvp.voices(), 16U);
  EXPECT_EQ(mvp.streamers(), 4U);
  EXPECT_EQ(mvp.effects(), 8U);
  const LoadBench full(version10Counts(), clickLike());
  EXPECT_EQ(full.voices(), 32U);
  EXPECT_EQ(full.streamers(), 8U);
  EXPECT_EQ(full.effects(), 16U);
}

TEST(LoadBench, ARenderGivesFiniteValues) {
  LoadBench bench(version10Counts(), clickLike());
  std::vector<float> left(1000, 0.0F);
  std::vector<float> right(1000, 0.0F);
  for (int round = 0; round < 50; ++round) {
    bench.render(left.data(), right.data(), 1000);
  }
  double energy = 0.0;
  for (size_t i = 0; i < left.size(); ++i) {
    ASSERT_TRUE(std::isfinite(left[i]) && std::isfinite(right[i]));
    energy += left[i] * left[i];
  }
  EXPECT_GT(energy, 0.0);
}

TEST(LoadBench, EachKernelHasACost) {
  const std::vector<KernelCost> costs = measureKernels(clickLike(), 20);
  ASSERT_EQ(costs.size(), 5U);
  for (const KernelCost& cost : costs) {
    EXPECT_GT(cost.microsPerBlock, 0.0);
  }
}

}  // namespace
}  // namespace minidaw::engine
