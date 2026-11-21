#include "minidaw/engine/engine.h"

#include <algorithm>
#include <cmath>
#include <limits>
#include <memory>
#include <utility>

namespace minidaw::engine {
namespace {

constexpr float kUnknown = std::numeric_limits<float>::quiet_NaN();
constexpr uint8_t kPatternMode = 1;
constexpr int32_t kMaxPitch = 127;
constexpr int32_t kMinTempo = 30000;
constexpr int32_t kMaxTempo = 300000;

// The index of the item with this ID, or the size of the list when none has it.
template <typename Item>
size_t indexOf(const std::vector<Item>& items, uint32_t id) {
  size_t i = 0;
  while (i < items.size() && items[i].id != id) {
    ++i;
  }
  return i;
}

std::unique_ptr<Loaded> prepare(format::Snapshot&& snapshot) {
  auto loaded = std::make_unique<Loaded>();
  loaded->snapshot = std::move(snapshot);
  const format::Snapshot& s = loaded->snapshot;
  for (const format::Channel& c : s.channels) {
    loaded->channelGain.push_back(c.gain);
    loaded->channelPan.push_back(c.pan);
  }
  for (const format::Instrument& x : s.instruments) {
    loaded->instrumentGain.push_back(x.gain);
  }
  loaded->parameters = s.parameters;
  return loaded;
}

// The rule for values at a swap (architecture.md Section 5.2): a value of the new Snapshot replaces the
// running value only when it differs from the same value of the Snapshot before. So a value that a Live
// command has set stays until an Edit changes it.
void carryOver(const Loaded& old, Loaded& next) {
  const format::Snapshot& a = old.snapshot;
  const format::Snapshot& b = next.snapshot;
  for (size_t i = 0; i < b.channels.size(); ++i) {
    const size_t j = indexOf(a.channels, b.channels[i].id);
    if (j < a.channels.size()) {
      if (b.channels[i].gain == a.channels[j].gain) {
        next.channelGain[i] = old.channelGain[j];
      }
      if (b.channels[i].pan == a.channels[j].pan) {
        next.channelPan[i] = old.channelPan[j];
      }
    }
  }
  for (size_t i = 0; i < b.instruments.size(); ++i) {
    const size_t j = indexOf(a.instruments, b.instruments[i].id);
    if (j < a.instruments.size() && b.instruments[i].gain == a.instruments[j].gain) {
      next.instrumentGain[i] = old.instrumentGain[j];
    }
  }
  for (const format::Slot& slot : b.slots) {
    const size_t j = slot.id == 0 ? a.slots.size() : indexOf(a.slots, slot.id);
    if (j >= a.slots.size() || a.slots[j].kind != slot.kind) {
      continue;
    }
    const format::Slot& before = a.slots[j];
    const uint32_t count = std::min(slot.parameterCount, before.parameterCount);
    for (uint32_t p = 0; p < count; ++p) {
      if (b.parameters[slot.firstParameter + p] == a.parameters[before.firstParameter + p]) {
        next.parameters[slot.firstParameter + p] = old.parameters[before.firstParameter + p];
      }
    }
  }
}

}  // namespace

Engine::~Engine() {
  delete latest_.exchange(nullptr);
  collect();
  delete current_;
}

format::Error Engine::loadSnapshot(std::span<const uint8_t> bytes, const format::Limits& limits) {
  collect();
  format::Snapshot snapshot;
  const format::Error error = format::load(bytes, snapshot, limits);
  if (error != format::Error::kNone) {
    return error;
  }
  // A Snapshot that the audio thread has not taken yet was never seen by it, so it can be freed here.
  delete latest_.exchange(prepare(std::move(snapshot)).release(), std::memory_order_acq_rel);
  return format::Error::kNone;
}

void Engine::collect() {
  Loaded* old = nullptr;
  while (retired_.pop(old)) {
    delete old;
  }
}

bool Engine::send(const LiveCommand& command) {
  if (commands_.push(command)) {
    return true;
  }
  dropped_.fetch_add(1);
  return false;
}

void Engine::swap() {
  // The old Snapshot goes back to the Control thread. When the ring is full, the swap waits a block.
  if (retired_.full()) {
    return;
  }
  Loaded* next = latest_.exchange(nullptr, std::memory_order_acq_rel);
  if (next == nullptr) {
    return;
  }
  if (current_ == nullptr) {
    tempo_ = next->snapshot.tempoMilliBpm;
  } else {
    carryOver(*current_, *next);
    if (next->snapshot.tempoMilliBpm != current_->snapshot.tempoMilliBpm) {
      tempo_ = next->snapshot.tempoMilliBpm;
    }
    retired_.push(current_);
  }
  current_ = next;
  swaps_.fetch_add(1);
}

void Engine::setValue(const LiveCommand& command) {
  if (current_ == nullptr || !std::isfinite(command.value)) {
    dropped_.fetch_add(1);
    return;
  }
  const format::Snapshot& s = current_->snapshot;
  const auto id = static_cast<uint32_t>(command.a);
  const float value = command.value;
  if (const size_t c = indexOf(s.channels, id); c < s.channels.size()) {
    if (command.b == kParameterGain) {
      current_->channelGain[c] = std::clamp(value, 0.0F, format::kMaxGain);
      return;
    }
    if (command.b == kParameterPan) {
      current_->channelPan[c] = std::clamp(value, -1.0F, 1.0F);
      return;
    }
  }
  if (const size_t x = indexOf(s.instruments, id); x < s.instruments.size()) {
    if (command.b == kParameterGain) {
      current_->instrumentGain[x] = std::clamp(value, 0.0F, format::kMaxGain);
      return;
    }
  }
  if (const size_t slot = id == 0 ? s.slots.size() : indexOf(s.slots, id); slot < s.slots.size()) {
    const format::Slot& item = s.slots[slot];
    if (command.b >= 0 && static_cast<uint32_t>(command.b) < item.parameterCount) {
      current_->parameters[item.firstParameter + static_cast<uint32_t>(command.b)] = value;
      return;
    }
  }
  dropped_.fetch_add(1);
}

void Engine::apply(const LiveCommand& command) {
  using Kind = LiveCommand::Kind;
  const bool known = current_ != nullptr;
  switch (command.kind) {
    case Kind::kPlay:
      running_ = true;
      break;
    case Kind::kStop:
      running_ = false;
      break;
    case Kind::kSeek:
      if (command.a >= 0) {
        position_ = command.a;
      } else {
        dropped_.fetch_add(1);
      }
      break;
    case Kind::kSetMode: {
      const bool song = command.a == 0;
      const bool pattern = command.a == kPatternMode && known &&
                           indexOf(current_->snapshot.patterns, static_cast<uint32_t>(command.b)) <
                               current_->snapshot.patterns.size();
      if (song || pattern) {
        mode_ = static_cast<uint8_t>(command.a);
      } else {
        dropped_.fetch_add(1);
      }
      break;
    }
    case Kind::kSetLoop:
      if (command.a >= 0 && command.b > command.a) {
        loopStart_ = static_cast<uint32_t>(command.a);
        loopEnd_ = static_cast<uint32_t>(command.b);
        loopOn_ = command.c != 0;
      } else {
        dropped_.fetch_add(1);
      }
      break;
    case Kind::kSetTempo:
      if (command.a >= kMinTempo && command.a <= kMaxTempo) {
        tempo_ = static_cast<uint32_t>(command.a);
      } else {
        dropped_.fetch_add(1);
      }
      break;
    case Kind::kSetValue:
      setValue(command);
      break;
    case Kind::kNoteOn:
    case Kind::kNoteOff: {
      const bool instrument = known && indexOf(current_->snapshot.instruments,
                                               static_cast<uint32_t>(command.a)) <
                                           current_->snapshot.instruments.size();
      if (!instrument || command.b < 0 || command.b > kMaxPitch) {
        dropped_.fetch_add(1);
      }
      break;
    }
    case Kind::kMetronome:
      metronome_ = command.a != 0;
      break;
    case Kind::kStopPreview:
    case Kind::kClearClipFlag:
      break;
  }
}

void Engine::advance(size_t frames) {
  if (!running_) {
    return;
  }
  position_ += static_cast<double>(frames) * tempo_ / kFramesPerTickTimesTempo;
  if (loopOn_ && mode_ == 0 && position_ >= loopEnd_) {
    const double span = static_cast<double>(loopEnd_ - loopStart_);
    position_ = loopStart_ + std::fmod(position_ - loopStart_, span);
  }
}

void Engine::render(std::span<float> left, std::span<float> right) {
  swap();
  LiveCommand command;
  while (commands_.pop(command)) {
    apply(command);
  }
  const size_t frames = std::min(left.size(), right.size());
  std::fill(left.begin(), left.end(), 0.0F);
  std::fill(right.begin(), right.end(), 0.0F);
  advance(frames);
  positionOut_.store(position_);
  playingOut_.store(running_);
  tempoOut_.store(tempo_);
}

float Engine::channelGain(uint32_t id) const {
  if (current_ == nullptr) {
    return kUnknown;
  }
  const size_t c = indexOf(current_->snapshot.channels, id);
  return c < current_->channelGain.size() ? current_->channelGain[c] : kUnknown;
}

float Engine::channelPan(uint32_t id) const {
  if (current_ == nullptr) {
    return kUnknown;
  }
  const size_t c = indexOf(current_->snapshot.channels, id);
  return c < current_->channelPan.size() ? current_->channelPan[c] : kUnknown;
}

float Engine::instrumentGain(uint32_t id) const {
  if (current_ == nullptr) {
    return kUnknown;
  }
  const size_t x = indexOf(current_->snapshot.instruments, id);
  return x < current_->instrumentGain.size() ? current_->instrumentGain[x] : kUnknown;
}

}  // namespace minidaw::engine
