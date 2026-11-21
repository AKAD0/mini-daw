package app.minidaw.engine.api

import app.minidaw.engine.api.snapshot.SampleHash

/**
 * The result of a call into the engine. Errors cross the border between Kotlin and C++ as these codes
 * (architecture.md Section 12.2). Codes 1 to 7 follow `snapshot::Error` of engine-core; codes 1 to 9 are the
 * checks before a swap (feature_specifications.md Section 6.2).
 */
@Suppress("MagicNumber") // The codes are the numbers that cross the border; the native library uses the same.
enum class EngineResult(val code: Int) {
    OK(0),
    TOO_SHORT(1),
    BAD_MARK(2),
    BAD_VERSION(3),
    BAD_SIZE(4),
    TABLE_OUTSIDE(5),
    BAD_REFERENCE(6),
    BAD_ORDER(7),
    UNSORTED(8),
    OVER_LIMIT(9),
    UNKNOWN_SAMPLE(10),
    FILE_FAILED(11),
    STREAM_FAILED(12),
    BUSY(13),
    UNKNOWN(-1);

    companion object {
        /** The result of a code from the native library. */
        fun of(code: Int): EngineResult = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/** How the engine keeps a Sample (architecture.md Section 5.7): in memory, or read by a Streamer. */
enum class SampleUse {
    MEMORY,
    STREAM
}

/** Why a Take ended without a tap on Stop (FR-REC-05). */
enum class TakeStop {
    LIMIT,
    INPUT_LOST,
    STORAGE_LOW
}

/** The events that the audio thread leaves in its ring buffer for the Control thread. */
sealed interface EngineEvent {
    /** A Streamer had no audio in time; the Clip played silence (architecture.md Section 5.10). */
    data class StreamerLate(val clipId: Int) : EngineEvent

    /** A Take ended by itself. Its file holds the audio up to the end. */
    data class TakeStopped(val reason: TakeStop) : EngineEvent

    /** A stream reported an error (architecture.md Section 5.11). */
    data class StreamFailed(val result: EngineResult) : EngineEvent
}

/**
 * The engine as Kotlin sees it (architecture.md Sections 5.3 and 12.1). [send] runs on the main thread and never
 * blocks. Every other call runs on the Control thread. `:engine` implements it; [FakeAudioEngine] serves the tests.
 * The offline render of an export comes with its task.
 */
interface AudioEngine {
    /** The values of the last block (architecture.md Section 5.10). */
    val telemetry: Telemetry

    /** Passes a Live command to the audio thread. */
    fun send(command: LiveCommand)

    /** Checks and loads a Snapshot (architecture.md Section 5.2). On an error the old Snapshot stays. */
    fun loadSnapshot(bytes: ByteArray): EngineResult

    /** Loads a Sample into memory or prepares a Streamer for it. */
    fun registerSample(hash: SampleHash, path: String, use: SampleUse): EngineResult

    /** Frees a Sample once no Snapshot uses it. */
    fun releaseSample(hash: SampleHash)

    /** Plays a Sample after the Master channel (FR-LIB-03). */
    fun startPreview(hash: SampleHash): EngineResult

    /** Starts a Take into the file at `path` (architecture.md Section 5.8). */
    fun startTake(path: String): EngineResult

    /** Ends the running Take. */
    fun stopTake(): EngineResult

    /** Opens the output stream (architecture.md Section 5.11). */
    fun openStreams(): EngineResult

    /** Closes both streams. */
    fun closeStreams()

    /** Empties the ring buffer of events of the audio thread. */
    fun readEvents(): List<EngineEvent>
}
