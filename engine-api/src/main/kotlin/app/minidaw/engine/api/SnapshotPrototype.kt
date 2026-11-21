// The numbers of this file are the layout of the bytes and the values of the standard test Projects.
// engine/src/main/cpp/engine-core/include/minidaw/engine/snapshot.h names each of them.
@file:Suppress("MagicNumber")

package app.minidaw.engine.api

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The prototype of the writer of a Snapshot for the check V-04 (TK-007, architecture.md Section 5.2).
 * It writes a standard test Project with the layout of `snapshot.h` in engine-core, whose loader reads
 * the same bytes. PH-01 and PH-02 turn it into the compiler of :core.
 */
object SnapshotPrototype {
    /** The counts of a standard test Project (requirements.md Section 8). */
    data class Counts(
        val audioTracks: Int,
        val patternTracks: Int,
        val channels: Int,
        val equalizers: Int,
        val delays: Int,
        val reverbs: Int
    )

    /** The standard test Project of the MVP: 8 Tracks, 16 Voices, 8 Mixer channels, 8 effects. */
    val mvp = Counts(audioTracks = 4, patternTracks = 4, channels = 8, equalizers = 4, delays = 2, reverbs = 2)

    /** The standard test Project of version 1.0: the doubled counts. */
    val version10 = Counts(audioTracks = 8, patternTracks = 8, channels = 16, equalizers = 8, delays = 4, reverbs = 4)

    private const val MAGIC = 0x4E53444D
    private const val VERSION: Short = 1
    private const val HEADER_SIZE = 32
    private const val SECTIONS = 7
    private const val SECTION_ENTRY_SIZE = 12
    private val recordSizes = intArrayOf(32, 24, 12, 8, 20, 12, 160)
    private const val NOTES_PER_PATTERN = 384
    private const val CLIPS_PER_PATTERN_TRACK = 45
    private const val PATTERN_CLIP_LENGTH = 7680
    private const val AUDIO_CLIP_LENGTH = 345600
    private const val SLOTS = 4
    private const val MASTER: Short = -1
    private val equalizerValues = floatArrayOf(3f, 120f, -2f, 1000f, 0.9f, 2f, 8000f, 0f)
    private val delayValues = floatArrayOf(350f, 0.35f, 6000f, 0.3f, 0f, 0f, 0f, 0f)
    private val reverbValues = floatArrayOf(0.5f, 0.5f, 1f, 0.25f, 0f, 0f, 0f, 0f)
    private val emptyValues = FloatArray(8)

    /** Writes the Snapshot of a standard test Project. */
    fun write(counts: Counts): ByteArray {
        val instruments = 2 * counts.patternTracks
        val patterns = 2 * counts.patternTracks
        val sizes = intArrayOf(
            counts.audioTracks + instruments,
            instruments,
            patterns * NOTES_PER_PATTERN,
            patterns,
            counts.audioTracks + counts.patternTracks * CLIPS_PER_PATTERN_TRACK,
            counts.audioTracks + counts.patternTracks,
            counts.channels
        )
        val offsets = IntArray(SECTIONS)
        var total = HEADER_SIZE + SECTIONS * SECTION_ENTRY_SIZE
        for (s in 0 until SECTIONS) {
            offsets[s] = total
            total += sizes[s] * recordSizes[s]
        }
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(MAGIC).putShort(VERSION).putShort(HEADER_SIZE.toShort()).putFloat(120f)
        out.put(4).put(4).putShort(0).putInt(SECTIONS).putInt(total)
        out.position(HEADER_SIZE)
        for (s in 0 until SECTIONS) {
            out.putInt(offsets[s]).putInt(sizes[s]).putInt(recordSizes[s])
        }
        writeSamples(out, sizes[0])
        writeInstruments(out, counts, instruments)
        writeNotes(out, patterns, instruments)
        for (p in 0 until patterns) {
            out.putInt(p * NOTES_PER_PATTERN).putInt(NOTES_PER_PATTERN)
        }
        writeClips(out, counts, patterns)
        writeTracks(out, counts)
        writeChannels(out, counts)
        check(out.position() == total) { "the layout of the Snapshot is wrong" }
        return out.array()
    }

    /** FNV-1a with 64 bits: the same fingerprint as in engine-core. */
    fun fingerprint(bytes: ByteArray): ULong {
        var hash = 14695981039346656037uL
        for (byte in bytes) {
            hash = (hash xor byte.toUByte().toULong()) * 1099511628211uL
        }
        return hash
    }

    private fun writeSamples(out: ByteBuffer, samples: Int) {
        for (i in 0 until samples) {
            for (k in 0 until 32) {
                out.put(((i * 31 + k * 7 + 1) and 0xFF).toByte())
            }
        }
    }

    private fun writeInstruments(out: ByteBuffer, counts: Counts, instruments: Int) {
        for (i in 0 until instruments) {
            out.putShort((counts.audioTracks + i).toShort()).put(60).put((i % 2).toByte())
            out.putFloat(0.8f).putFloat(((i % 3) - 1) * 0.25f).putFloat(0.005f).putFloat(0.2f)
            out.put(1).put(0).put(0).put(0)
        }
    }

    private fun writeNotes(out: ByteBuffer, patterns: Int, instruments: Int) {
        for (p in 0 until patterns) {
            for (j in 0 until NOTES_PER_PATTERN) {
                out.putInt(j * 240).putInt(200)
                out.put((36 + (j * 7 + p * 5) % 48).toByte()).put((64 + (j * 13) % 64).toByte())
                out.putShort(((p * 2 + j % 2) % instruments).toShort())
            }
        }
    }

    private fun writeClips(out: ByteBuffer, counts: Counts, patterns: Int) {
        for (t in 0 until counts.audioTracks) {
            out.putInt(0).putInt(AUDIO_CLIP_LENGTH).put(0).put(0).putShort(t.toShort()).putInt(0).putFloat(1f)
        }
        for (q in 0 until counts.patternTracks) {
            for (c in 0 until CLIPS_PER_PATTERN_TRACK) {
                out.putInt(c * PATTERN_CLIP_LENGTH).putInt(PATTERN_CLIP_LENGTH).put(1).put(0)
                out.putShort(((q * 2 + c) % patterns).toShort()).putInt(0).putFloat(1f)
            }
        }
    }

    private fun writeTracks(out: ByteBuffer, counts: Counts) {
        var clip = 0
        for (t in 0 until counts.audioTracks + counts.patternTracks) {
            val clips = if (t < counts.audioTracks) 1 else CLIPS_PER_PATTERN_TRACK
            out.putInt(clip).putInt(clips).putShort((t % counts.channels).toShort()).put(1).put(0)
            clip += clips
        }
    }

    private fun writeChannels(out: ByteBuffer, counts: Counts) {
        for (i in 0 until counts.channels) {
            out.putShort(MASTER).putShort(i.toShort()).putFloat(0.8f).putFloat(0f)
            out.put(1).put(0).put(0).put(0)
            for (s in 0 until SLOTS) {
                val (kind, values) = slot(s * counts.channels + i, counts)
                out.put(kind.toByte()).put(0).put(0).put(0)
                values.forEach { out.putFloat(it) }
            }
        }
    }

    private fun slot(assignment: Int, counts: Counts): Pair<Int, FloatArray> = when {
        assignment < counts.equalizers -> 1 to equalizerValues
        assignment < counts.equalizers + counts.delays -> 2 to delayValues
        assignment < counts.equalizers + counts.delays + counts.reverbs -> 3 to reverbValues
        else -> 0 to emptyValues
    }
}
