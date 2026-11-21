#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <vector>

#include "minidaw/engine/format.h"

namespace minidaw::engine::test {

// Writes a Snapshot in the format of feature_specifications.md Section 6.2, for the tests only. The writer
// of the product is SnapshotWriter of :engine-api; the fixtures come from it.
class SnapshotBytes {
 public:
  static std::vector<uint8_t> write(const format::Snapshot& s) {
    SnapshotBytes w;
    const std::array<size_t, format::kTables> counts{
        s.samples.size(), s.instruments.size(), s.envelopes.size(), s.patterns.size(),
        s.parts.size(),   s.notes.size(),       s.tracks.size(),    s.clips.size(),
        s.channels.size(), s.slots.size(),      s.parameters.size(), s.order.size()};
    size_t end = format::kHeaderSize + format::kDirectorySize;
    std::array<size_t, format::kTables> offsets{};
    for (size_t t = 0; t < format::kTables; ++t) {
      offsets[t] = end;
      end += counts[t] * format::kRecordSizes[t];
    }
    w.u32(format::kMark);
    w.u16(format::kVersion);
    w.u16(0);
    w.u32(static_cast<uint32_t>(end));
    w.u32(s.tempoMilliBpm);
    w.u8(s.numerator);
    w.u8(s.denominator);
    w.u16(0);
    for (size_t t = 0; t < format::kTables; ++t) {
      w.u32(static_cast<uint32_t>(offsets[t]));
      w.u32(static_cast<uint32_t>(counts[t]));
    }
    w.tables(s);
    return w.bytes_;
  }

 private:
  void tables(const format::Snapshot& s) {
    for (const auto& hash : s.samples) {
      bytes_.insert(bytes_.end(), hash.begin(), hash.end());
    }
    for (const format::Instrument& x : s.instruments) {
      u32(x.id);
      u32(x.sample);
      u8(x.rootNote);
      u8(x.playMode);
      u8(x.audible ? 1 : 0);
      u8(0);
      f32(x.gain);
      f32(x.pan);
      f32(x.fineCents);
      u32(static_cast<uint32_t>(x.ownChannel));
      u32(static_cast<uint32_t>(x.envelope));
      u32(x.startFrame);
      u32(x.endFrame);
      u32(x.loopStart);
      u32(x.loopEnd);
    }
    for (const format::Envelope& e : s.envelopes) {
      f32(e.attackMs);
      f32(e.decayMs);
      f32(e.sustain);
      f32(e.releaseMs);
    }
    for (const format::Pattern& p : s.patterns) {
      u32(p.id);
      u32(p.lengthTicks);
      u32(p.firstPart);
      u32(p.partCount);
    }
    for (const format::Part& p : s.parts) {
      u32(p.instrument);
      u32(p.firstNote);
      u32(p.noteCount);
    }
    for (const format::Note& n : s.notes) {
      u32(n.start);
      u32(n.length);
      u8(n.pitch);
      u8(n.velocity);
      u16(0);
    }
    for (const format::Track& t : s.tracks) {
      u32(t.id);
      u32(t.channel);
      u32(t.firstClip);
      u32(t.clipCount);
      u8(t.audible ? 1 : 0);
      pad(3);
    }
    for (const format::Clip& c : s.clips) {
      u32(c.id);
      u8(c.kind);
      pad(3);
      u32(c.start);
      u32(c.source);
      u32(c.offset);
      u32(c.length);
      f32(c.gain);
    }
    for (const format::Channel& c : s.channels) {
      u32(c.id);
      u32(static_cast<uint32_t>(c.target));
      f32(c.gain);
      f32(c.pan);
      u8(c.audible ? 1 : 0);
      pad(3);
      u32(c.firstSlot);
    }
    for (const format::Slot& slot : s.slots) {
      u32(slot.id);
      u16(slot.kind);
      u8(slot.bypass ? 1 : 0);
      u8(0);
      u32(slot.firstParameter);
      u32(slot.parameterCount);
    }
    for (const float p : s.parameters) {
      f32(p);
    }
    for (const uint32_t o : s.order) {
      u32(o);
    }
  }

  template <typename T>
  void put(T value) {
    const size_t at = bytes_.size();
    bytes_.resize(at + sizeof(T));
    std::memcpy(&bytes_[at], &value, sizeof(T));
  }

  void u8(uint8_t value) { bytes_.push_back(value); }
  void u16(uint16_t value) { put(value); }
  void u32(uint32_t value) { put(value); }
  void f32(float value) { put(value); }
  void pad(size_t count) { bytes_.insert(bytes_.end(), count, 0); }

  std::vector<uint8_t> bytes_;
};

// A small valid Snapshot: one Sample, two Sampler instruments with one envelope, one Pattern with two
// Parts, two Tracks with three Clips, two Mixer channels (the first sends into the second), an
// equalizer, and the Master channel.
inline format::Snapshot smallSnapshot() {
  format::Snapshot s;
  s.samples.resize(1);
  s.samples[0].fill(0x11);
  s.instruments = {{.id = 3, .sample = 0, .envelope = 0, .endFrame = 24000},
                   {.id = 10, .sample = 0, .playMode = 1, .gain = 0.5F, .endFrame = 24000}};
  s.envelopes = {{.attackMs = 5.0F, .decayMs = 100.0F, .sustain = 0.5F, .releaseMs = 200.0F}};
  s.patterns = {{.id = 4, .lengthTicks = 3840, .firstPart = 0, .partCount = 2}};
  s.parts = {{.instrument = 0, .firstNote = 0, .noteCount = 2},
             {.instrument = 1, .firstNote = 2, .noteCount = 1}};
  s.notes = {{.start = 0, .length = 240, .pitch = 60, .velocity = 100},
             {.start = 960, .length = 240, .pitch = 64, .velocity = 90},
             {.start = 0, .length = 480, .pitch = 48, .velocity = 127}};
  s.tracks = {{.id = 1, .channel = 0, .firstClip = 0, .clipCount = 2},
              {.id = 7, .channel = 1, .firstClip = 2, .clipCount = 1}};
  s.clips = {{.id = 5, .kind = 1, .start = 0, .source = 0, .length = 7680},
             {.id = 6, .kind = 0, .start = 7680, .source = 0, .length = 24000},
             {.id = 9, .kind = 0, .start = 0, .source = 0, .offset = 100, .length = 2000}};
  s.channels = {{.id = 2, .target = 1, .firstSlot = 0},
                {.id = 8, .gain = 0.7F, .pan = -0.5F, .firstSlot = 4},
                {.id = 0, .firstSlot = 8}};
  s.slots.assign(12, format::Slot{});
  s.slots[0] = {.id = 12, .kind = 1, .firstParameter = 0, .parameterCount = 7};
  s.parameters = {3.0F, 120.0F, 0.0F, 1000.0F, 0.9F, 0.0F, 8000.0F};
  s.order = {0, 1};
  return s;
}

}  // namespace minidaw::engine::test
