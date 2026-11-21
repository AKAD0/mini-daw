#include "minidaw/engine/snapshot.h"

#include <array>
#include <bit>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <span>
#include <utility>
#include <vector>

#include "minidaw/engine/load_bench.h"

namespace minidaw::engine::snapshot {
namespace {

static_assert(std::endian::native == std::endian::little, "the bytes are read as they lie");

constexpr uint32_t kNotesPerPattern = 384;
constexpr uint32_t kNoteDistance = 240;
constexpr uint32_t kNoteLength = 200;
constexpr uint32_t kClipsPerPatternTrack = 45;
constexpr uint32_t kPatternClipLength = 7680;
constexpr uint32_t kAudioClipLength = 345600;
constexpr uint8_t kLowestPitch = 36;
constexpr uint32_t kPitchRange = 48;
constexpr uint8_t kVelocityBase = 64;
constexpr uint32_t kVelocityRange = 64;
constexpr uint8_t kMaxPitch = 127;
constexpr uint8_t kLastKind = 3;
constexpr uint64_t kFnvOffset = 14695981039346656037ULL;
constexpr uint64_t kFnvPrime = 1099511628211ULL;
constexpr size_t kHashSize = 32;
constexpr uint32_t kSlotSize = 36;
constexpr uint32_t kChannelHead = 16;

class Writer {
 public:
  explicit Writer(size_t size) : bytes_(size, 0) {}

  void u8(size_t at, uint8_t value) { bytes_[at] = value; }
  void u16(size_t at, uint16_t value) { std::memcpy(&bytes_[at], &value, sizeof(value)); }
  void u32(size_t at, uint32_t value) { std::memcpy(&bytes_[at], &value, sizeof(value)); }
  void f32(size_t at, float value) { std::memcpy(&bytes_[at], &value, sizeof(value)); }

  std::vector<uint8_t> take() { return std::move(bytes_); }

 private:
  std::vector<uint8_t> bytes_;
};

class Reader {
 public:
  explicit Reader(std::span<const uint8_t> bytes) : bytes_(bytes) {}

  [[nodiscard]] uint8_t u8(size_t at) const { return bytes_[at]; }
  [[nodiscard]] uint16_t u16(size_t at) const { return read<uint16_t>(at); }
  [[nodiscard]] uint32_t u32(size_t at) const { return read<uint32_t>(at); }
  [[nodiscard]] float f32(size_t at) const { return read<float>(at); }

 private:
  template <typename T>
  [[nodiscard]] T read(size_t at) const {
    T value{};
    std::memcpy(&value, &bytes_[at], sizeof(T));
    return value;
  }

  std::span<const uint8_t> bytes_;
};

struct Layout {
  std::array<uint32_t, kSections> offsets{};
  std::array<uint32_t, kSections> counts{};
  uint32_t total = 0;
};

Layout layoutOf(const std::array<uint32_t, kSections>& counts) {
  Layout layout;
  layout.counts = counts;
  uint32_t at = kHeaderSize + kSections * kSectionEntrySize;
  for (size_t s = 0; s < kSections; ++s) {
    layout.offsets[s] = at;
    at += counts[s] * kRecordSizes[s];
  }
  layout.total = at;
  return layout;
}

// The values of the effect in slot `slot` of a standard test Project.
Slot standardSlot(int assignment, const TestCounts& counts) {
  Slot slot;
  if (assignment < counts.equalizers) {
    slot.kind = 1;
    slot.values = {3.0F, 120.0F, -2.0F, 1000.0F, 0.9F, 2.0F, 8000.0F, 0.0F};
  } else if (assignment < counts.equalizers + counts.delays) {
    slot.kind = 2;
    slot.values = {350.0F, 0.35F, 6000.0F, 0.3F, 0.0F, 0.0F, 0.0F, 0.0F};
  } else if (assignment < counts.equalizers + counts.delays + counts.reverbs) {
    slot.kind = 3;
    slot.values = {0.5F, 0.5F, 1.0F, 0.25F, 0.0F, 0.0F, 0.0F, 0.0F};
  }
  return slot;
}

bool inside(const Layout& layout, size_t s, size_t size) {
  const uint64_t end = uint64_t{layout.offsets[s]} + uint64_t{layout.counts[s]} * kRecordSizes[s];
  const bool afterTable = layout.offsets[s] >= kHeaderSize + kSections * kSectionEntrySize;
  const bool aligned = layout.offsets[s] % 4 == 0;
  return afterTable && aligned && end <= size;
}

}  // namespace

const char* errorText(Error error) {
  switch (error) {
    case Error::kNone:
      return "none";
    case Error::kTooShort:
      return "too short";
    case Error::kMagic:
      return "no Snapshot";
    case Error::kVersion:
      return "unknown version";
    case Error::kSize:
      return "wrong size";
    case Error::kSection:
      return "wrong section";
    case Error::kReference:
      return "wrong reference";
    case Error::kOrder:
      return "wrong order";
  }
  return "unknown";
}

std::vector<uint8_t> writeStandard(const TestCounts& counts) {
  const auto audioTracks = static_cast<uint32_t>(counts.audioTracks);
  const auto patternTracks = static_cast<uint32_t>(counts.patternTracks);
  const auto channels = static_cast<uint32_t>(counts.channels);
  const uint32_t instruments = 2 * patternTracks;
  const uint32_t patterns = 2 * patternTracks;
  std::array<uint32_t, kSections> sizes{};
  sizes[kSamples] = audioTracks + instruments;
  sizes[kInstruments] = instruments;
  sizes[kNotes] = patterns * kNotesPerPattern;
  sizes[kPatterns] = patterns;
  sizes[kClips] = audioTracks + patternTracks * kClipsPerPatternTrack;
  sizes[kTracks] = audioTracks + patternTracks;
  sizes[kChannels] = channels;
  const Layout layout = layoutOf(sizes);
  Writer w(layout.total);
  w.u32(0, kMagic);
  w.u16(4, kVersion);
  w.u16(6, static_cast<uint16_t>(kHeaderSize));
  w.f32(8, 120.0F);
  w.u8(12, 4);
  w.u8(13, 4);
  w.u32(16, kSections);
  w.u32(20, layout.total);
  for (size_t s = 0; s < kSections; ++s) {
    const size_t entry = kHeaderSize + s * kSectionEntrySize;
    w.u32(entry, layout.offsets[s]);
    w.u32(entry + 4, layout.counts[s]);
    w.u32(entry + 8, kRecordSizes[s]);
  }
  for (uint32_t i = 0; i < sizes[kSamples]; ++i) {
    const size_t at = layout.offsets[kSamples] + i * kRecordSizes[kSamples];
    for (uint32_t k = 0; k < kHashSize; ++k) {
      w.u8(at + k, static_cast<uint8_t>((i * 31 + k * 7 + 1) & 0xFFU));
    }
  }
  for (uint32_t i = 0; i < instruments; ++i) {
    const size_t at = layout.offsets[kInstruments] + i * kRecordSizes[kInstruments];
    w.u16(at, static_cast<uint16_t>(audioTracks + i));
    w.u8(at + 2, 60);
    w.u8(at + 3, static_cast<uint8_t>(i % 2));
    w.f32(at + 4, 0.8F);
    w.f32(at + 8, static_cast<float>(static_cast<int>(i % 3) - 1) * 0.25F);
    w.f32(at + 12, 0.005F);
    w.f32(at + 16, 0.2F);
    w.u8(at + 20, 1);
  }
  for (uint32_t p = 0; p < patterns; ++p) {
    const size_t pattern = layout.offsets[kPatterns] + p * kRecordSizes[kPatterns];
    w.u32(pattern, p * kNotesPerPattern);
    w.u32(pattern + 4, kNotesPerPattern);
    for (uint32_t j = 0; j < kNotesPerPattern; ++j) {
      const size_t at = layout.offsets[kNotes] + (p * kNotesPerPattern + j) * kRecordSizes[kNotes];
      w.u32(at, j * kNoteDistance);
      w.u32(at + 4, kNoteLength);
      w.u8(at + 8, static_cast<uint8_t>(kLowestPitch + (j * 7 + p * 5) % kPitchRange));
      w.u8(at + 9, static_cast<uint8_t>(kVelocityBase + (j * 13) % kVelocityRange));
      w.u16(at + 10, static_cast<uint16_t>((p * 2 + j % 2) % instruments));
    }
  }
  uint32_t clip = 0;
  for (uint32_t t = 0; t < audioTracks + patternTracks; ++t) {
    const bool audio = t < audioTracks;
    const uint32_t count = audio ? 1 : kClipsPerPatternTrack;
    const size_t track = layout.offsets[kTracks] + t * kRecordSizes[kTracks];
    w.u32(track, clip);
    w.u32(track + 4, count);
    w.u16(track + 8, static_cast<uint16_t>(t % channels));
    w.u8(track + 10, 1);
    for (uint32_t c = 0; c < count; ++c, ++clip) {
      const size_t at = layout.offsets[kClips] + clip * kRecordSizes[kClips];
      const uint32_t q = t - audioTracks;
      w.u32(at, audio ? 0 : c * kPatternClipLength);
      w.u32(at + 4, audio ? kAudioClipLength : kPatternClipLength);
      w.u8(at + 8, static_cast<uint8_t>(audio ? 0 : 1));
      w.u16(at + 10, static_cast<uint16_t>(audio ? t : (q * 2 + c) % patterns));
      w.f32(at + 16, 1.0F);
    }
  }
  for (uint32_t i = 0; i < channels; ++i) {
    const size_t at = layout.offsets[kChannels] + i * kRecordSizes[kChannels];
    w.u16(at, kMaster);
    w.u16(at + 2, static_cast<uint16_t>(i));
    w.f32(at + 4, 0.8F);
    w.u8(at + 12, 1);
    for (uint32_t s = 0; s < kSlots; ++s) {
      const Slot slot = standardSlot(static_cast<int>(s * channels + i), counts);
      const size_t slotAt = at + kChannelHead + s * kSlotSize;
      w.u8(slotAt, slot.kind);
      for (size_t v = 0; v < kParameters; ++v) {
        w.f32(slotAt + 4 + v * 4, slot.values[v]);
      }
    }
  }
  return w.take();
}

Error load(std::span<const uint8_t> bytes, Loaded& out) {
  if (bytes.size() < kHeaderSize + kSections * kSectionEntrySize) {
    return Error::kTooShort;
  }
  const Reader r(bytes);
  if (r.u32(0) != kMagic) {
    return Error::kMagic;
  }
  if (r.u16(4) != kVersion || r.u16(6) != kHeaderSize || r.u32(16) != kSections) {
    return Error::kVersion;
  }
  if (r.u32(20) != bytes.size()) {
    return Error::kSize;
  }
  Layout layout;
  for (size_t s = 0; s < kSections; ++s) {
    const size_t entry = kHeaderSize + s * kSectionEntrySize;
    layout.offsets[s] = r.u32(entry);
    layout.counts[s] = r.u32(entry + 4);
    if (r.u32(entry + 8) != kRecordSizes[s] || !inside(layout, s, bytes.size())) {
      return Error::kSection;
    }
  }
  const auto at = [&](Section s, uint32_t i) {
    return size_t{layout.offsets[s]} + size_t{i} * kRecordSizes[s];
  };
  Loaded loaded;
  loaded.tempo = r.f32(8);
  loaded.beatsPerBar = r.u8(12);
  loaded.beatUnit = r.u8(13);
  loaded.samples.resize(layout.counts[kSamples]);
  for (uint32_t i = 0; i < layout.counts[kSamples]; ++i) {
    std::memcpy(loaded.samples[i].data(), &bytes[at(kSamples, i)], kHashSize);
  }
  loaded.instruments.resize(layout.counts[kInstruments]);
  for (uint32_t i = 0; i < layout.counts[kInstruments]; ++i) {
    Instrument& item = loaded.instruments[i];
    const size_t a = at(kInstruments, i);
    item.sample = r.u16(a);
    item.rootNote = r.u8(a + 2);
    item.playMode = r.u8(a + 3);
    item.volume = r.f32(a + 4);
    item.pan = r.f32(a + 8);
    item.attack = r.f32(a + 12);
    item.release = r.f32(a + 16);
    item.audible = r.u8(a + 20) != 0;
    if (item.sample >= loaded.samples.size()) {
      return Error::kReference;
    }
  }
  loaded.notes.resize(layout.counts[kNotes]);
  for (uint32_t i = 0; i < layout.counts[kNotes]; ++i) {
    const size_t a = at(kNotes, i);
    loaded.notes[i] = {r.u32(a), r.u32(a + 4), r.u8(a + 8), r.u8(a + 9), r.u16(a + 10)};
    const Note& note = loaded.notes[i];
    if (note.pitch > kMaxPitch || note.instrument >= loaded.instruments.size()) {
      return Error::kReference;
    }
  }
  loaded.patterns.resize(layout.counts[kPatterns]);
  for (uint32_t i = 0; i < layout.counts[kPatterns]; ++i) {
    const size_t a = at(kPatterns, i);
    const Pattern pattern{r.u32(a), r.u32(a + 4)};
    if (uint64_t{pattern.firstNote} + pattern.noteCount > loaded.notes.size()) {
      return Error::kReference;
    }
    for (uint32_t j = 1; j < pattern.noteCount; ++j) {
      const Note& before = loaded.notes[pattern.firstNote + j - 1];
      if (loaded.notes[pattern.firstNote + j].start < before.start) {
        return Error::kOrder;
      }
    }
    loaded.patterns[i] = pattern;
  }
  loaded.clips.resize(layout.counts[kClips]);
  for (uint32_t i = 0; i < layout.counts[kClips]; ++i) {
    const size_t a = at(kClips, i);
    Clip& item = loaded.clips[i];
    item = {r.u32(a), r.u32(a + 4), r.u8(a + 8), r.u16(a + 10), r.u32(a + 12), r.f32(a + 16)};
    const size_t sources = item.kind == 0 ? loaded.samples.size() : loaded.patterns.size();
    if (item.kind > 1 || item.source >= sources) {
      return Error::kReference;
    }
  }
  loaded.channels.resize(layout.counts[kChannels]);
  for (uint32_t i = 0; i < layout.counts[kChannels]; ++i) {
    const size_t a = at(kChannels, i);
    Channel& item = loaded.channels[i];
    item.target = r.u16(a);
    item.order = r.u16(a + 2);
    item.level = r.f32(a + 4);
    item.pan = r.f32(a + 8);
    item.audible = r.u8(a + 12) != 0;
    for (size_t s = 0; s < kSlots; ++s) {
      const size_t slotAt = a + kChannelHead + s * kSlotSize;
      item.slots[s].kind = r.u8(slotAt);
      if (item.slots[s].kind > kLastKind) {
        return Error::kReference;
      }
      for (size_t v = 0; v < kParameters; ++v) {
        item.slots[s].values[v] = r.f32(slotAt + 4 + v * 4);
      }
    }
  }
  // The order of processing: each channel exactly once, and every channel before its target.
  std::vector<bool> seen(loaded.channels.size(), false);
  for (size_t i = 0; i < loaded.channels.size(); ++i) {
    const Channel& item = loaded.channels[i];
    if (item.order >= seen.size() || seen[item.order]) {
      return Error::kOrder;
    }
    seen[item.order] = true;
    if (item.target == kMaster) {
      continue;
    }
    const bool known = item.target < loaded.channels.size() && item.target != i;
    if (!known || loaded.channels[item.target].order <= item.order) {
      return Error::kOrder;
    }
  }
  loaded.tracks.resize(layout.counts[kTracks]);
  for (uint32_t i = 0; i < layout.counts[kTracks]; ++i) {
    const size_t a = at(kTracks, i);
    Track& item = loaded.tracks[i];
    item = {r.u32(a), r.u32(a + 4), r.u16(a + 8), r.u8(a + 10) != 0};
    const bool clipsInside = uint64_t{item.firstClip} + item.clipCount <= loaded.clips.size();
    if (!clipsInside || item.channel >= loaded.channels.size()) {
      return Error::kReference;
    }
    for (uint32_t c = 1; c < item.clipCount; ++c) {
      const Clip& before = loaded.clips[item.firstClip + c - 1];
      if (loaded.clips[item.firstClip + c].start < before.start + before.length) {
        return Error::kOrder;
      }
    }
  }
  out = std::move(loaded);
  return Error::kNone;
}

uint64_t fingerprint(std::span<const uint8_t> bytes) {
  uint64_t hash = kFnvOffset;
  for (const uint8_t byte : bytes) {
    hash ^= byte;
    hash *= kFnvPrime;
  }
  return hash;
}

}  // namespace minidaw::engine::snapshot
