package app.minidaw.core.model

import app.minidaw.engine.api.snapshot.PlayMode
import app.minidaw.engine.api.snapshot.SampleHash
import java.time.Instant

// The state of a Project (architecture.md Section 4.1, D-059). Every class is immutable. An Edit copies only the
// lists on the way to the changed item; all other parts are shared with the state before (Section 4.2).
// Positions and lengths in Ticks are whole numbers (Section 4.3). Fields of version 1.0 exist from the first
// build on and hold their defaults in the MVP.

/** The version of the schema of the document (feature_specifications.md Section 6.1). */
const val SCHEMA = 1

data class TimeSignature(val numerator: Int, val denominator: Int) {
    companion object {
        val FOUR_FOUR = TimeSignature(4, 4)
    }
}

data class Project(
    val id: String,
    val name: String,
    val created: Instant,
    val modified: Instant,
    val tempoMilliBpm: Int,
    val timeSignature: TimeSignature,
    val nextId: Int,
    val tracks: List<Track>,
    val patterns: List<Pattern>,
    val instruments: List<SamplerInstrument>,
    val samples: List<UsedSample>,
    val mixer: Mixer,
    val schema: Int = SCHEMA
)

/** A Track holds its Clips sorted by start. Solo is used from version 1.0 on. */
data class Track(
    val id: Int,
    val name: String,
    val color: Int,
    val channel: Int,
    val clips: List<Clip> = emptyList(),
    val mute: Boolean = false,
    val solo: Boolean = false
)

sealed interface Clip {
    val id: Int
    val start: Int
    val gainDb: Float
}

/** Offset and length are frames of the Sample. It plays at its original speed (D-008). */
data class AudioClip(
    override val id: Int,
    val sample: SampleHash,
    override val start: Int,
    val offset: Int,
    val length: Int,
    override val gainDb: Float = 0f
) : Clip

/** Start, length and offset are Ticks. The Pattern repeats when the Clip is longer (D-007). */
data class PatternClip(
    override val id: Int,
    val pattern: Int,
    override val start: Int,
    val length: Int,
    val offset: Int = 0,
    override val gainDb: Float = 0f
) : Clip

/** A Note of a Part. Inside its Part it is identified by its start and its pitch. */
data class Note(val start: Int, val length: Int, val pitch: Int, val velocity: Int)

/** The Notes of one Sampler instrument in a Pattern, sorted by start and then by pitch (D-054). */
data class Part(val instrument: Int, val notes: List<Note> = emptyList())

/** The length is in Ticks; the UI offers whole bars (FR-PT-01). */
data class Pattern(
    val id: Int,
    val name: String,
    val color: Int,
    val length: Int,
    val parts: List<Part> = emptyList(),
    val take: SampleHash? = null
)

/** Attack, decay and release in milliseconds; sustain as a level from 0 to 1. Used from version 1.0 on. */
data class Envelope(val attackMs: Float, val decayMs: Float, val sustain: Float, val releaseMs: Float)

/** A loop region of a Sample in frames. Used from version 1.0 on. */
data class LoopRegion(val startFrame: Int, val endFrame: Int)

data class SamplerInstrument(
    val id: Int,
    val name: String,
    val color: Int,
    val sample: SampleHash,
    val rootNote: Int,
    val playMode: PlayMode,
    val volumeDb: Float = 0f,
    val mute: Boolean = false,
    val pan: Float = 0f,
    val solo: Boolean = false,
    val fineCents: Float = 0f,
    val envelope: Envelope? = null,
    val startFrame: Int? = null,
    val endFrame: Int? = null,
    val loop: LoopRegion? = null,
    val channel: Int? = null
)

/** An effect in a slot. Its parameters are named as in feature_specifications.md Section 6.1. */
data class Effect(val id: Int, val kind: String, val bypass: Boolean, val params: Map<String, Float>)

/** A Mixer channel. The target null means the Master channel. It has four slots; null is an empty slot. */
data class MixerChannel(
    val id: Int,
    val name: String,
    val color: Int,
    val volumeDb: Float = 0f,
    val pan: Float = 0f,
    val mute: Boolean = false,
    val solo: Boolean = false,
    val target: Int? = null,
    val slots: List<Effect?> = EMPTY_SLOTS
) {
    companion object {
        const val SLOTS = 4
        val EMPTY_SLOTS: List<Effect?> = List(SLOTS) { null }
    }
}

data class MasterChannel(val volumeDb: Float = 0f, val slots: List<Effect?> = MixerChannel.EMPTY_SLOTS)

data class Mixer(val channels: List<MixerChannel>, val master: MasterChannel = MasterChannel())

/** An entry of the table of used Samples: a Project opens without the index of the Library. */
data class UsedSample(val hash: SampleHash, val name: String, val frames: Int, val channels: Int)
