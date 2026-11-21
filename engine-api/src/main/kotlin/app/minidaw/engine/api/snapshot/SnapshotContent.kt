package app.minidaw.engine.api.snapshot

// The records of the twelve tables of a Snapshot (feature_specifications.md Section 6.2). A reference is the
// number of a record in its table, counted from 0. Whole numbers of the type u32 must not be negative.

/** Table 2. Times of the Sample are frames. */
data class InstrumentRecord(
    val id: Int,
    val sample: Int,
    val rootNote: Int,
    val playMode: PlayMode,
    val audible: Boolean,
    val gain: Float,
    val pan: Float,
    val fineTuningCents: Float,
    val ownChannel: Int = SnapshotFormat.NONE,
    val envelope: Int = SnapshotFormat.NONE,
    val startFrame: Int,
    val endFrame: Int,
    val loopStart: Int = 0,
    val loopEnd: Int = 0
)

/** Table 3. Attack, decay and release in milliseconds; sustain as a level from 0 to 1. */
data class EnvelopeRecord(val attackMs: Float, val decayMs: Float, val sustain: Float, val releaseMs: Float)

/** Table 4. */
data class PatternRecord(val id: Int, val lengthTicks: Int, val firstPart: Int, val partCount: Int)

/** Table 5. */
data class PartRecord(val instrument: Int, val firstNote: Int, val noteCount: Int)

/** Table 6. Start and length in Ticks. */
data class NoteRecord(val start: Int, val length: Int, val pitch: Int, val velocity: Int)

/** Table 7. */
data class TrackRecord(val id: Int, val channel: Int, val firstClip: Int, val clipCount: Int, val audible: Boolean)

/**
 * Table 8. The start is in Ticks. `source` refers to a Sample for an Audio clip and to a Pattern for a Pattern
 * clip. Offset and length are frames for an Audio clip and Ticks for a Pattern clip.
 */
data class ClipRecord(
    val id: Int,
    val kind: ClipKind,
    val start: Int,
    val source: Int,
    val offset: Int,
    val length: Int,
    val gain: Float
)

/** Table 9. The target −1 is the Master channel. The last record is the Master channel, with the ID 0. */
data class ChannelRecord(
    val id: Int,
    val target: Int,
    val gain: Float,
    val pan: Float,
    val audible: Boolean,
    val firstSlot: Int
)

/** Table 10. `kind` is one of [EffectKind]. An empty slot has the ID 0. */
data class SlotRecord(
    val id: Int,
    val kind: Int,
    val bypass: Boolean,
    val firstParameter: Int,
    val parameterCount: Int
) {
    companion object {
        /** An empty slot. */
        val EMPTY = SlotRecord(SnapshotFormat.EMPTY_ID, EffectKind.EMPTY, false, 0, 0)
    }
}

/** Everything that a Snapshot holds. Gains are plain factors (meaning 10). */
data class SnapshotContent(
    val tempoMilliBpm: Int,
    val numerator: Int,
    val denominator: Int,
    val samples: List<SampleHash> = emptyList(),
    val instruments: List<InstrumentRecord> = emptyList(),
    val envelopes: List<EnvelopeRecord> = emptyList(),
    val patterns: List<PatternRecord> = emptyList(),
    val parts: List<PartRecord> = emptyList(),
    val notes: List<NoteRecord> = emptyList(),
    val tracks: List<TrackRecord> = emptyList(),
    val clips: List<ClipRecord> = emptyList(),
    val channels: List<ChannelRecord> = emptyList(),
    val slots: List<SlotRecord> = emptyList(),
    val parameters: List<Float> = emptyList(),
    val order: List<Int> = emptyList()
) {
    /** The number of records of each table, in the order of the directory. */
    fun counts(): List<Int> = listOf(
        samples.size,
        instruments.size,
        envelopes.size,
        patterns.size,
        parts.size,
        notes.size,
        tracks.size,
        clips.size,
        channels.size,
        slots.size,
        parameters.size,
        order.size
    )
}
