#include "minidaw/engine/format.h"

#include <bit>
#include <cmath>
#include <cstring>
#include <utility>

namespace minidaw::engine::format {
namespace {

static_assert(std::endian::native == std::endian::little,
              "the engine reads the little-endian numbers of a Snapshot directly");

constexpr uint32_t kMinTempo = 30000;
constexpr uint32_t kMaxTempo = 300000;

class Reader {
 public:
  explicit Reader(std::span<const uint8_t> bytes) : bytes_(bytes) {}

  [[nodiscard]] uint8_t u8(size_t at) const { return bytes_[at]; }
  [[nodiscard]] uint16_t u16(size_t at) const { return read<uint16_t>(at); }
  [[nodiscard]] uint32_t u32(size_t at) const { return read<uint32_t>(at); }
  [[nodiscard]] int32_t i32(size_t at) const { return read<int32_t>(at); }
  [[nodiscard]] float f32(size_t at) const { return read<float>(at); }

 private:
  template <typename T>
  [[nodiscard]] T read(size_t at) const {
    T value{};
    std::memcpy(&value, bytes_.subspan(at, sizeof(T)).data(), sizeof(T));
    return value;
  }

  std::span<const uint8_t> bytes_;
};

struct Directory {
  std::array<uint32_t, kTables> offsets{};
  std::array<uint32_t, kTables> counts{};

  [[nodiscard]] size_t at(Table table, uint32_t index) const {
    return size_t{offsets[table]} + size_t{index} * size_t{kRecordSizes[table]};
  }
};

// Check 2: every table lies inside the block, behind the directory.
bool readDirectory(const Reader& r, size_t size, Directory& d) {
  for (size_t t = 0; t < kTables; ++t) {
    const size_t entry = kHeaderSize + t * 8;
    d.offsets[t] = r.u32(entry);
    d.counts[t] = r.u32(entry + 4);
    const uint64_t end = uint64_t{d.offsets[t]} + uint64_t{d.counts[t]} * uint64_t{kRecordSizes[t]};
    if (d.offsets[t] < kHeaderSize + kDirectorySize || end > size) {
      return false;
    }
  }
  return true;
}

void readInstruments(const Reader& r, const Directory& d, Snapshot& s) {
  s.instruments.resize(d.counts[kInstrumentTable]);
  for (uint32_t i = 0; i < d.counts[kInstrumentTable]; ++i) {
    const size_t a = d.at(kInstrumentTable, i);
    Instrument& x = s.instruments[i];
    x.id = r.u32(a);
    x.sample = r.u32(a + 4);
    x.rootNote = r.u8(a + 8);
    x.playMode = r.u8(a + 9);
    x.audible = r.u8(a + 10) != 0;
    x.gain = r.f32(a + 12);
    x.pan = r.f32(a + 16);
    x.fineCents = r.f32(a + 20);
    x.ownChannel = r.i32(a + 24);
    x.envelope = r.i32(a + 28);
    x.startFrame = r.u32(a + 32);
    x.endFrame = r.u32(a + 36);
    x.loopStart = r.u32(a + 40);
    x.loopEnd = r.u32(a + 44);
  }
}

void readSequences(const Reader& r, const Directory& d, Snapshot& s) {
  s.envelopes.resize(d.counts[kEnvelopeTable]);
  for (uint32_t i = 0; i < d.counts[kEnvelopeTable]; ++i) {
    const size_t a = d.at(kEnvelopeTable, i);
    s.envelopes[i] = {r.f32(a), r.f32(a + 4), r.f32(a + 8), r.f32(a + 12)};
  }
  s.patterns.resize(d.counts[kPatternTable]);
  for (uint32_t i = 0; i < d.counts[kPatternTable]; ++i) {
    const size_t a = d.at(kPatternTable, i);
    s.patterns[i] = {r.u32(a), r.u32(a + 4), r.u32(a + 8), r.u32(a + 12)};
  }
  s.parts.resize(d.counts[kPartTable]);
  for (uint32_t i = 0; i < d.counts[kPartTable]; ++i) {
    const size_t a = d.at(kPartTable, i);
    s.parts[i] = {r.u32(a), r.u32(a + 4), r.u32(a + 8)};
  }
  s.notes.resize(d.counts[kNoteTable]);
  for (uint32_t i = 0; i < d.counts[kNoteTable]; ++i) {
    const size_t a = d.at(kNoteTable, i);
    s.notes[i] = {r.u32(a), r.u32(a + 4), r.u8(a + 8), r.u8(a + 9)};
  }
}

void readArrangement(const Reader& r, const Directory& d, Snapshot& s) {
  s.tracks.resize(d.counts[kTrackTable]);
  for (uint32_t i = 0; i < d.counts[kTrackTable]; ++i) {
    const size_t a = d.at(kTrackTable, i);
    s.tracks[i] = {r.u32(a), r.u32(a + 4), r.u32(a + 8), r.u32(a + 12), r.u8(a + 16) != 0};
  }
  s.clips.resize(d.counts[kClipTable]);
  for (uint32_t i = 0; i < d.counts[kClipTable]; ++i) {
    const size_t a = d.at(kClipTable, i);
    s.clips[i] = {r.u32(a),      r.u8(a + 4),      r.u32(a + 8), r.u32(a + 12),
                  r.u32(a + 16), r.u32(a + 20), r.f32(a + 24)};
  }
}

void readMixer(const Reader& r, const Directory& d, Snapshot& s) {
  s.channels.resize(d.counts[kChannelTable]);
  for (uint32_t i = 0; i < d.counts[kChannelTable]; ++i) {
    const size_t a = d.at(kChannelTable, i);
    s.channels[i] = {r.u32(a),          r.i32(a + 4), r.f32(a + 8),
                     r.f32(a + 12),     r.u8(a + 16) != 0, r.u32(a + 20)};
  }
  s.slots.resize(d.counts[kSlotTable]);
  for (uint32_t i = 0; i < d.counts[kSlotTable]; ++i) {
    const size_t a = d.at(kSlotTable, i);
    s.slots[i] = {r.u32(a), r.u16(a + 4), r.u8(a + 6) != 0, r.u32(a + 8), r.u32(a + 12)};
  }
  s.parameters.resize(d.counts[kParameterTable]);
  for (uint32_t i = 0; i < d.counts[kParameterTable]; ++i) {
    s.parameters[i] = r.f32(d.at(kParameterTable, i));
  }
  s.order.resize(d.counts[kOrderTable]);
  for (uint32_t i = 0; i < d.counts[kOrderTable]; ++i) {
    s.order[i] = r.u32(d.at(kOrderTable, i));
  }
}

bool fits(uint32_t first, uint32_t count, size_t size) {
  return uint64_t{first} + uint64_t{count} <= uint64_t{size};
}

bool optional(int32_t reference, size_t size) {
  return reference == kNone || (reference >= 0 && static_cast<size_t>(reference) < size);
}

// Check 3: every reference points inside its table. The last channel is the Master channel.
bool referencesHold(const Snapshot& s) {
  if (s.channels.empty() || s.channels.back().id != 0 || s.channels.back().target != kNone) {
    return false;
  }
  const size_t mixerChannels = s.channels.size() - 1;
  for (const Instrument& x : s.instruments) {
    if (x.sample >= s.samples.size() || x.playMode > 1 || x.rootNote > kMaxPitch ||
        !optional(x.ownChannel, mixerChannels) || !optional(x.envelope, s.envelopes.size())) {
      return false;
    }
  }
  for (const Pattern& p : s.patterns) {
    if (p.lengthTicks == 0 || !fits(p.firstPart, p.partCount, s.parts.size())) {
      return false;
    }
  }
  for (const Part& p : s.parts) {
    if (p.instrument >= s.instruments.size() || !fits(p.firstNote, p.noteCount, s.notes.size())) {
      return false;
    }
  }
  for (const Note& n : s.notes) {
    if (n.pitch > kMaxPitch || n.velocity == 0 || n.velocity > kMaxPitch || n.length == 0) {
      return false;
    }
  }
  for (const Track& t : s.tracks) {
    if (t.channel >= s.channels.size() || !fits(t.firstClip, t.clipCount, s.clips.size())) {
      return false;
    }
  }
  for (const Clip& c : s.clips) {
    const size_t sources = c.kind == 0 ? s.samples.size() : s.patterns.size();
    if (c.kind > 1 || c.source >= sources || c.length == 0) {
      return false;
    }
  }
  for (size_t i = 0; i < s.channels.size(); ++i) {
    const Channel& c = s.channels[i];
    const bool self = c.target >= 0 && static_cast<size_t>(c.target) == i;
    if (self || !optional(c.target, mixerChannels) ||
        !fits(c.firstSlot, kSlotsPerChannel, s.slots.size())) {
      return false;
    }
  }
  for (const Slot& slot : s.slots) {
    if (slot.kind > kMaxEffectKind ||
        !fits(slot.firstParameter, slot.parameterCount, s.parameters.size())) {
      return false;
    }
  }
  return true;
}

bool level(float gain) { return std::isfinite(gain) && gain >= 0.0F && gain <= kMaxGain; }

bool pan(float value) { return std::isfinite(value) && value >= -1.0F && value <= 1.0F; }

// Every number is finite and in its range, so that no invalid number reaches the audio.
bool valuesHold(const Snapshot& s) {
  if (s.tempoMilliBpm < kMinTempo || s.tempoMilliBpm > kMaxTempo || s.numerator == 0 ||
      s.denominator == 0) {
    return false;
  }
  for (const Instrument& x : s.instruments) {
    if (!level(x.gain) || !pan(x.pan) || !std::isfinite(x.fineCents)) {
      return false;
    }
  }
  for (const Envelope& e : s.envelopes) {
    if (!(e.attackMs >= 0.0F) || !(e.decayMs >= 0.0F) || !(e.releaseMs >= 0.0F) ||
        !std::isfinite(e.attackMs + e.decayMs + e.releaseMs) || !level(e.sustain)) {
      return false;
    }
  }
  for (const Clip& c : s.clips) {
    if (!level(c.gain)) {
      return false;
    }
  }
  for (const Channel& c : s.channels) {
    if (!level(c.gain) || !pan(c.pan)) {
      return false;
    }
  }
  for (const float p : s.parameters) {
    if (!std::isfinite(p)) {
      return false;
    }
  }
  return true;
}

// Check 4: the Notes of a Part and the Clips of a Track are sorted by start.
bool sorted(const Snapshot& s) {
  for (const Part& p : s.parts) {
    for (uint32_t i = 1; i < p.noteCount; ++i) {
      if (s.notes[p.firstNote + i].start < s.notes[p.firstNote + i - 1].start) {
        return false;
      }
    }
  }
  for (const Track& t : s.tracks) {
    for (uint32_t i = 1; i < t.clipCount; ++i) {
      if (s.clips[t.firstClip + i].start < s.clips[t.firstClip + i - 1].start) {
        return false;
      }
    }
  }
  return true;
}

// Check 5: the table "Order" names every channel but the Master channel once, and every channel comes
// before its target.
bool orderHolds(const Snapshot& s) {
  const size_t mixerChannels = s.channels.size() - 1;
  if (s.order.size() != mixerChannels) {
    return false;
  }
  std::vector<size_t> place(mixerChannels, mixerChannels);
  for (size_t i = 0; i < s.order.size(); ++i) {
    const uint32_t channel = s.order[i];
    if (channel >= mixerChannels || place[channel] != mixerChannels) {
      return false;
    }
    place[channel] = i;
  }
  for (size_t c = 0; c < mixerChannels; ++c) {
    const int32_t target = s.channels[c].target;
    if (target != kNone && place[static_cast<size_t>(target)] <= place[c]) {
      return false;
    }
  }
  return true;
}

// Check 6: the limits of requirements.md Section 8.
bool limitsHold(const Snapshot& s, const Limits& limits) {
  if (s.tracks.size() > limits.tracks || s.channels.size() - 1 > limits.channels) {
    return false;
  }
  for (const Pattern& p : s.patterns) {
    if (p.partCount > limits.instrumentsPerPattern) {
      return false;
    }
  }
  for (const Part& p : s.parts) {
    if (p.noteCount > limits.notesPerPart) {
      return false;
    }
  }
  size_t effects = 0;
  for (const Slot& slot : s.slots) {
    effects += slot.kind != 0 ? 1 : 0;
  }
  return effects <= limits.activeEffects;
}

}  // namespace

const char* errorText(Error error) {
  switch (error) {
    case Error::kNone:
      return "none";
    case Error::kTooShort:
      return "too short";
    case Error::kMark:
      return "wrong mark";
    case Error::kVersion:
      return "wrong version";
    case Error::kSize:
      return "wrong size";
    case Error::kTable:
      return "a table lies outside the block";
    case Error::kReference:
      return "a reference points outside its table";
    case Error::kOrder:
      return "wrong order of the channels";
    case Error::kUnsorted:
      return "Notes or Clips are not sorted";
    case Error::kLimit:
      return "a value or a count passes its limit";
  }
  return "unknown";
}

Error load(std::span<const uint8_t> bytes, Snapshot& out, const Limits& limits) {
  if (bytes.size() < kHeaderSize + kDirectorySize) {
    return Error::kTooShort;
  }
  const Reader r(bytes);
  if (r.u32(0) != kMark) {
    return Error::kMark;
  }
  if (r.u16(4) != kVersion) {
    return Error::kVersion;
  }
  if (r.u32(8) != bytes.size()) {
    return Error::kSize;
  }
  Directory d;
  if (!readDirectory(r, bytes.size(), d)) {
    return Error::kTable;
  }
  Snapshot s;
  s.tempoMilliBpm = r.u32(12);
  s.numerator = r.u8(16);
  s.denominator = r.u8(17);
  s.samples.resize(d.counts[kSampleTable]);
  for (uint32_t i = 0; i < d.counts[kSampleTable]; ++i) {
    std::memcpy(s.samples[i].data(), bytes.subspan(d.at(kSampleTable, i), kHashSize).data(),
                kHashSize);
  }
  readInstruments(r, d, s);
  readSequences(r, d, s);
  readArrangement(r, d, s);
  readMixer(r, d, s);
  Error error = Error::kNone;
  if (!referencesHold(s)) {
    error = Error::kReference;
  } else if (!valuesHold(s)) {
    error = Error::kLimit;
  } else if (!sorted(s)) {
    error = Error::kUnsorted;
  } else if (!orderHolds(s)) {
    error = Error::kOrder;
  } else if (!limitsHold(s, limits)) {
    error = Error::kLimit;
  }
  if (error == Error::kNone) {
    out = std::move(s);
  }
  return error;
}

}  // namespace minidaw::engine::format
