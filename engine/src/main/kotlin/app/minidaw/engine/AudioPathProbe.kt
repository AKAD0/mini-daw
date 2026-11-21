package app.minidaw.engine

/**
 * The check V-03 (TK-005): opens the streams of Oboe as the engine will (D-035) and reports what the
 * phone grants. Each function returns one JSON object. The output keeps playing silence for `seconds`.
 */
object AudioPathProbe {
    init {
        System.loadLibrary("minidaw-engine")
    }

    external fun probeOutput(performanceHint: Boolean, seconds: Int): String

    external fun probeInput(): String
}
