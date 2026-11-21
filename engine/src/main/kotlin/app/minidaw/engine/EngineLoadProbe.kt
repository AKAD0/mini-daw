package app.minidaw.engine

/**
 * The check V-04 (TK-007): the load of the standard test Projects on the audio thread, the cost of
 * each kernel, and the load of a Snapshot. Each function returns one JSON object. The streams play
 * silence.
 */
object EngineLoadProbe {
    init {
        System.loadLibrary("minidaw-engine")
    }

    /** Renders a standard test Project (0: MVP, 1: version 1.0) in the audio callback for [seconds]. */
    external fun probeLoad(project: Int, seconds: Int, sample: FloatArray): String

    /** Measures each kernel alone on the calling thread. */
    external fun probeKernels(sample: FloatArray): String

    /** Loads the bytes of a Snapshot [runs] times. */
    external fun probeSnapshotLoad(bytes: ByteArray, runs: Int): String
}
