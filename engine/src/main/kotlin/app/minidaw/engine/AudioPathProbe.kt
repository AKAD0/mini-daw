package app.minidaw.engine

/**
 * The checks V-03 (TK-005) and V-07 (TK-006): opens the streams of Oboe as the engine will (D-035)
 * and reports what the phone grants. Each function returns one JSON object. The output keeps playing
 * silence for `seconds`.
 */
object AudioPathProbe {
    init {
        System.loadLibrary("minidaw-engine")
    }

    external fun probeOutput(performanceHint: Boolean, seconds: Int): String

    external fun probeInput(): String

    /** Plays the click 8 times, once per second, and records it: the offset of a Take by both methods of D-040. */
    external fun probeTakeLatency(click: FloatArray): String
}
