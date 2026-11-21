#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include "minidaw/engine/load_bench.h"

namespace minidaw::engine::snapshot {

// The prototype of the bytes of a Snapshot for the check V-04 (architecture.md Section 5.2). All
// numbers are little-endian. A header of 32 bytes, a table of 7 sections (offset, count, size of a
// record; 12 bytes each), then the records of each section. docs/architecture.md Section 5.2 and
// the writer in :engine-api describe the same layout.
constexpr uint32_t kMagic = 0x4E53444DU;  // "MDSN"
constexpr uint16_t kVersion = 1;
constexpr uint32_t kHeaderSize = 32;
constexpr uint32_t kSectionEntrySize = 12;
constexpr uint16_t kMaster = 0xFFFFU;
constexpr int kSlots = 4;
constexpr int kParameters = 8;

enum Section : uint8_t { kSamples, kInstruments, kNotes, kPatterns, kClips, kTracks, kChannels };
constexpr uint32_t kSections = 7;
constexpr std::array<uint32_t, kSections> kRecordSizes{32, 24, 12, 8, 20, 12, 160};

struct Instrument {
  uint16_t sample = 0;
  uint8_t rootNote = 60;
  uint8_t playMode = 0;
  float volume = 1.0F;
  float pan = 0.0F;
  float attack = 0.0F;
  float release = 0.0F;
  bool audible = true;
};

struct Note {
  uint32_t start = 0;
  uint32_t length = 0;
  uint8_t pitch = 0;
  uint8_t velocity = 0;
  uint16_t instrument = 0;
};

struct Pattern {
  uint32_t firstNote = 0;
  uint32_t noteCount = 0;
};

struct Clip {
  uint32_t start = 0;
  uint32_t length = 0;
  uint8_t kind = 0;  // 0: Audio clip, 1: Pattern clip
  uint16_t source = 0;
  uint32_t offset = 0;
  float gain = 1.0F;
};

struct Track {
  uint32_t firstClip = 0;
  uint32_t clipCount = 0;
  uint16_t channel = 0;
  bool audible = true;
};

struct Slot {
  uint8_t kind = 0;  // 0: empty, 1: eq3, 2: delay, 3: reverb
  std::array<float, kParameters> values{};
};

struct Channel {
  uint16_t target = kMaster;
  uint16_t order = 0;
  float level = 1.0F;
  float pan = 0.0F;
  bool audible = true;
  std::array<Slot, kSlots> slots{};
};

// A Snapshot in the memory of the engine.
struct Loaded {
  float tempo = 120.0F;
  uint8_t beatsPerBar = 4;
  uint8_t beatUnit = 4;
  std::vector<std::array<uint8_t, 32>> samples;
  std::vector<Instrument> instruments;
  std::vector<Note> notes;
  std::vector<Pattern> patterns;
  std::vector<Clip> clips;
  std::vector<Track> tracks;
  std::vector<Channel> channels;
};

enum class Error : uint8_t {
  kNone,
  kTooShort,
  kMagic,
  kVersion,
  kSize,
  kSection,
  kReference,
  kOrder,
};

const char* errorText(Error error);

// Checks the bytes and builds the Snapshot (steps 3 and 4 of Section 5.2). On an error, `out`
// stays unchanged.
Error load(std::span<const uint8_t> bytes, Loaded& out);

// Writes a Snapshot with the counts of a standard test Project. The writer of :engine-api writes
// the same bytes for the same counts.
std::vector<uint8_t> writeStandard(const TestCounts& counts);

// FNV-1a with 64 bits: a short fingerprint of bytes for the tests of both writers.
uint64_t fingerprint(std::span<const uint8_t> bytes);

}  // namespace minidaw::engine::snapshot
