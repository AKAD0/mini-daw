#include "minidaw/engine/platform.h"

#include <gtest/gtest.h>

namespace minidaw::engine {
namespace {

TEST(Platform, AtomicValuesOfTheAudioThreadAreLockFree) { EXPECT_TRUE(audioAtomicsAreLockFree()); }

}  // namespace
}  // namespace minidaw::engine
