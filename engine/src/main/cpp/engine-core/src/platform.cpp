#include "minidaw/engine/platform.h"

namespace minidaw::engine {

bool audioAtomicsAreLockFree() noexcept {
  // The constants of the type need no call into libatomic. A call of is_lock_free() on an object
  // would need it in a build without optimization.
  return std::atomic<std::uint64_t>::is_always_lock_free && std::atomic<float>::is_always_lock_free;
}

}  // namespace minidaw::engine
