package app.minidaw.engine.api.snapshot

import app.minidaw.engine.api.EngineResult
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// The expected bytes come from a second writer of feature_specifications.md Section 6.2, written apart from this
// one in Python with the module struct (TK-015). They are the "fixed inputs" of the criterion of TK-015.
class SnapshotWriterTest {
    private val master = ChannelRecord(id = 0, target = -1, gain = 1f, pan = 0f, audible = true, firstSlot = 0)

    /** Only the Master channel with its four empty slots. */
    private val minimal = SnapshotContent(
        tempoMilliBpm = 120_000,
        numerator = 4,
        denominator = 4,
        channels = listOf(master),
        slots = List(SnapshotFormat.SLOTS_PER_CHANNEL) { SlotRecord.EMPTY }
    )

    /**
     * One Sample, one Sampler instrument with an envelope, one Pattern with two Notes, one Track with an Audio clip
     * and a Pattern clip, one channel with an equalizer, and the Master channel.
     */
    private val small = SnapshotContent(
        tempoMilliBpm = 120_500,
        numerator = 3,
        denominator = 4,
        samples = listOf(SampleHash.of(ByteArray(SampleHash.BYTES) { it.toByte() })),
        instruments = listOf(
            InstrumentRecord(
                id = 7,
                sample = 0,
                rootNote = 60,
                playMode = PlayMode.ONE_SHOT,
                audible = true,
                gain = 0.5f,
                pan = -0.25f,
                fineTuningCents = 12.5f,
                envelope = 0,
                startFrame = 0,
                endFrame = 48_000
            )
        ),
        envelopes = listOf(EnvelopeRecord(attackMs = 5f, decayMs = 100f, sustain = 0.8f, releaseMs = 250f)),
        patterns = listOf(PatternRecord(id = 9, lengthTicks = 3840, firstPart = 0, partCount = 1)),
        parts = listOf(PartRecord(instrument = 0, firstNote = 0, noteCount = 2)),
        notes = listOf(
            NoteRecord(start = 0, length = 480, pitch = 60, velocity = 100),
            NoteRecord(start = 960, length = 240, pitch = 64, velocity = 127)
        ),
        tracks = listOf(TrackRecord(id = 11, channel = 0, firstClip = 0, clipCount = 2, audible = true)),
        clips = listOf(
            ClipRecord(id = 12, kind = ClipKind.AUDIO, start = 0, source = 0, offset = 0, length = 96_000, gain = 1f),
            ClipRecord(id = 13, kind = ClipKind.PATTERN, start = 3840, source = 0, offset = 0, length = 3840, 0.75f)
        ),
        channels = listOf(
            ChannelRecord(id = 20, target = -1, gain = 0.8f, pan = 0.1f, audible = true, firstSlot = 0),
            master.copy(firstSlot = 4)
        ),
        slots = listOf(SlotRecord(21, EffectKind.EQ3, bypass = false, firstParameter = 0, parameterCount = 8)) +
            List(7) { SlotRecord.EMPTY },
        parameters = listOf(3f, 120f, -2f, 1000f, 0.9f, 2f, 8000f, 0f),
        order = listOf(0)
    )

    @Test
    fun theMinimalSnapshotHasTheBytesOfTheSpecification() {
        val expected = listOf(
            "4d44534e01000000cc000000c0d40100", // MDSN, version 1, size 204, tempo 120000
            "04040000740000000000000074000000", // 4/4; directory: offsets 116, counts 0 ...
            "00000000740000000000000074000000",
            "00000000740000000000000074000000",
            "00000000740000000000000074000000",
            "0000000074000000010000008c000000", // ... channels: one record at 116; slots at 140
            "04000000cc00000000000000cc000000", // four slots; parameters and order empty at 204
            "0000000000000000ffffffff0000803f", // Master: ID 0, target -1, gain 1.0
            "00000000010000000000000000000000", // pan 0, audible, first slot 0; slots start
            "00000000000000000000000000000000",
            "00000000000000000000000000000000",
            "00000000000000000000000000000000",
            "000000000000000000000000"
        ).joinToString("")
        assertEquals(expected, hex(SnapshotWriter.write(minimal)))
    }

    @Test
    fun theSmallSnapshotHasTheBytesOfTheSecondWriter() {
        val bytes = SnapshotWriter.write(small)
        assertEquals(552, bytes.size)
        assertEquals(0xE42845C1AB993F59uL, Fingerprint.of(bytes))
        val directory = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val pairs = (0 until SnapshotFormat.TABLES).map { t ->
            val at = SnapshotFormat.HEADER_SIZE + 8 * t
            directory.getInt(at) to directory.getInt(at + 4)
        }
        assertEquals(
            listOf(
                116 to 1, 148 to 1, 196 to 1, 212 to 1, 228 to 1, 240 to 2,
                264 to 1, 284 to 2, 340 to 2, 388 to 8, 516 to 8, 548 to 1
            ),
            pairs
        )
    }

    @Test
    fun theSameContentGivesTheSameBytes() {
        assertContentEquals(SnapshotWriter.write(small), SnapshotWriter.write(small.copy()))
    }

    @Test
    fun aFieldOutsideItsRangeIsRefused() {
        val instrument = small.instruments.single()
        assertFailsWith<IllegalArgumentException> {
            SnapshotWriter.write(small.copy(instruments = listOf(instrument.copy(rootNote = 256))))
        }
        assertFailsWith<IllegalArgumentException> {
            SnapshotWriter.write(small.copy(instruments = listOf(instrument.copy(ownChannel = -2))))
        }
        assertFailsWith<IllegalArgumentException> {
            SnapshotWriter.write(small.copy(notes = listOf(NoteRecord(-1, length = 1, pitch = 60, velocity = 1))))
        }
        assertFailsWith<IllegalArgumentException> { SnapshotWriter.write(small.copy(tempoMilliBpm = 0)) }
    }

    @Test
    fun theCheckAcceptsTheBytesOfTheWriter() {
        assertEquals(EngineResult.OK, SnapshotCheck.check(SnapshotWriter.write(minimal)))
        assertEquals(EngineResult.OK, SnapshotCheck.check(SnapshotWriter.write(small)))
    }

    @Test
    fun theCheckRefusesChangedBytes() {
        val good = SnapshotWriter.write(small)
        assertEquals(EngineResult.TOO_SHORT, SnapshotCheck.check(good.copyOf(100)))
        assertEquals(EngineResult.BAD_MARK, SnapshotCheck.check(good.copyOf().also { it[0] = 'X'.code.toByte() }))
        assertEquals(EngineResult.BAD_VERSION, SnapshotCheck.check(good.copyOf().also { it[4] = 2 }))
        assertEquals(EngineResult.BAD_SIZE, SnapshotCheck.check(good.copyOf(good.size - 4)))
        // The count of the Notes grows by one record past the end of the block.
        val notesCount = SnapshotFormat.HEADER_SIZE + 8 * SnapshotFormat.NOTES + 4
        assertEquals(EngineResult.TABLE_OUTSIDE, SnapshotCheck.check(good.copyOf().also { it[notesCount] = 100 }))
    }

    @Test
    fun aHashKeepsItsBytes() {
        val bytes = ByteArray(SampleHash.BYTES) { (255 - 7 * it).toByte() }
        assertContentEquals(bytes, SampleHash.of(bytes).toBytes())
        assertEquals("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", small.samples[0].hex)
        assertFailsWith<IllegalArgumentException> { SampleHash("AB".repeat(SampleHash.BYTES)) }
        assertFailsWith<IllegalArgumentException> { SampleHash("ab") }
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
