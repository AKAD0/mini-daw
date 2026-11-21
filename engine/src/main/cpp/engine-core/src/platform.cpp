#include "minidaw/engine/platform.h"

namespace minidaw::engine {

bool audioAtomicsAreLockFree() noexcept {
  const std::atomic<std::uint64_t> frames{0};
  const std::atomic<float> level{0.0F};
  return frames.is_lock_free() && level.is_lock_free();
}

}  // namespace minidaw::engine
