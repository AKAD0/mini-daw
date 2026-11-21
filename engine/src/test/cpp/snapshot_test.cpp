#include "minidaw/engine/snapshot.h"

#include <gtest/gtest.h>

#include <cstdint>
#include <cstring>
#include <vector>

#include "minidaw/engine/load_bench.h"

namespace minidaw::engine::snapshot {
namespace {

// The same numbers stand in the test of the writer of :engine-api: both writers give the same bytes.
constexpr uint64_t kMvpFingerprint = 0xD87D14E4F99FE61CULL;
constexpr uint64_t kVersion10Fingerprint = 0xC27A518E15BABED3ULL;

uint32_t sectionOffset(const std::vector<uint8_t>& bytes, Section s) {
  uint32_t offset = 0;
  std::memcpy(&offset, &bytes[kHeaderSize + s * kSectionEntrySize], sizeof(offset));
  return offset;
}

TEST(Snapshot, TheStandardTestProjectOfVersion10LoadsWithItsCounts) {
  const std::vector<uint8_t> bytes = writeStandard(version10Counts());
  Loaded loaded;
  ASSERT_EQ(load(bytes, loaded), Error::kNone);
  EXPECT_EQ(loaded.samples.size(), 24U);
  EXPECT_EQ(loaded.instruments.size(), 16U);
  EXPECT_EQ(loaded.notes.size(), 6144U);
  EXPECT_EQ(loaded.patterns.size(), 16U);
  EXPECT_EQ(loaded.clips.size(), 368U);
  EXPECT_EQ(loaded.tracks.size(), 16U);
  EXPECT_EQ(loaded.channels.size(), 16U);
  EXPECT_EQ(loaded.channels[15].slots[0].kind, 3);
  EXPECT_FLOAT_EQ(loaded.instruments[2].pan, 0.25F);
  // About 100 KB at the limits of version 1.0 (architecture.md Section 5.2).
  EXPECT_GT(bytes.size(), 50000U);
  EXPECT_LT(bytes.size(), 150000U);
}

TEST(Snapshot, BothWritersGiveTheSameBytes) {
  EXPECT_EQ(fingerprint(writeStandard(mvpCounts())), kMvpFingerprint);
  EXPECT_EQ(fingerprint(writeStandard(version10Counts())), kVersion10Fingerprint);
}

TEST(Snapshot, BadBytesAreRefusedAndTheOldSnapshotStays) {
  const std::vector<uint8_t> good = writeStandard(mvpCounts());
  Loaded loaded;
  ASSERT_EQ(load(good, loaded), Error::kNone);
  const size_t notes = loaded.notes.size();

  std::vector<uint8_t> bytes = good;
  bytes[0] = 'X';
  EXPECT_EQ(load(bytes, loaded), Error::kMagic);

  bytes = good;
  bytes.pop_back();
  EXPECT_EQ(load(bytes, loaded), Error::kSize);

  bytes = std::vector<uint8_t>(good.begin(), good.begin() + 40);
  EXPECT_EQ(load(bytes, loaded), Error::kTooShort);

  bytes = good;
  const uint16_t missing = 0xFFFFU;
  std::memcpy(&bytes[sectionOffset(bytes, kInstruments)], &missing, sizeof(missing));
  EXPECT_EQ(load(bytes, loaded), Error::kReference);

  bytes = good;
  const uint32_t late = 1000000U;
  std::memcpy(&bytes[sectionOffset(bytes, kNotes)], &late, sizeof(late));
  EXPECT_EQ(load(bytes, loaded), Error::kOrder);

  bytes = good;
  const uint16_t itself = 0;
  std::memcpy(&bytes[sectionOffset(bytes, kChannels)], &itself, sizeof(itself));
  EXPECT_EQ(load(bytes, loaded), Error::kOrder);

  EXPECT_EQ(loaded.notes.size(), notes);
}

}  // namespace
}  // namespace minidaw::engine::snapshot
