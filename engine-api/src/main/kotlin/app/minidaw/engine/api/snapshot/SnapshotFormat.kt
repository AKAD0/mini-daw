package app.minidaw.engine.api.snapshot

/** The constants of the bytes of a Snapshot (feature_specifications.md Section 6.2, D-089). */
object SnapshotFormat {
    /** The letters `MDSN`, read as one little-endian u32. */
    const val MARK = 0x4E53444D

    /** The version of the format. The prototype of TK-007 had its own layout. */
    const val VERSION = 1

    /** Mark, version, reserved, size, tempo, time signature, reserved. */
    const val HEADER_SIZE = 20

    /** The number of tables. */
    const val TABLES = 12

    /** Twelve pairs of an offset and a count, each a u32. */
    const val DIRECTORY_SIZE = TABLES * 8

    /** "None" for an own channel and an envelope; "the Master channel" for the target of a channel. */
    const val NONE = -1

    /** Each channel owns this many records in a row in the table of slots. */
    const val SLOTS_PER_CHANNEL = 4

    /** The ID of the Master channel and of an empty slot. */
    const val EMPTY_ID = 0

    // The numbers of the tables, in the order of the directory.
    const val SAMPLES = 0
    const val INSTRUMENTS = 1
    const val ENVELOPES = 2
    const val PATTERNS = 3
    const val PARTS = 4
    const val NOTES = 5
    const val TRACKS = 6
    const val CLIPS = 7
    const val CHANNELS = 8
    const val SLOTS = 9
    const val PARAMETERS = 10
    const val ORDER = 11

    /** The size in bytes of one record of each table, in the order of the directory. */
    @Suppress("MagicNumber") // The sizes of the records of Section 6.2.
    val RECORD_SIZES: List<Int> = listOf(32, 48, 16, 16, 12, 12, 20, 28, 24, 16, 4, 4)
}

/** The kinds of effects of a slot (feature_specifications.md Section 6.2, meaning 7). 0 marks an empty slot. */
object EffectKind {
    const val EMPTY = 0
    const val EQ3 = 1
    const val DELAY = 2
    const val REVERB = 3
    const val COMPRESSOR = 4
    const val LIMITER = 5
    const val SATURATION = 6
    const val FILTER = 7
    const val CHORUS = 8
}

/** Play mode of a Sampler instrument (meaning 5). */
enum class PlayMode(val code: Int) {
    GATE(0),
    ONE_SHOT(1)
}

/** Kind of a Clip (meaning 5). */
enum class ClipKind(val code: Int) {
    AUDIO(0),
    PATTERN(1)
}
