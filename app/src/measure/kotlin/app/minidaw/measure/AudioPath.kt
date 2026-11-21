package app.minidaw.measure

import app.minidaw.engine.AudioPathProbe
import org.json.JSONObject

/**
 * The check V-03 (TK-005, technology_stack.md Section 14): rate, Burst, kind of path, Underruns over
 * 10 min, and the times to open both streams. The native part lies in the module :engine.
 */
internal object AudioPath {
    const val RUN_SECONDS = 600
    private const val ID = "V-03"
    private val UNITS = mapOf(
        "openMicros" to "µs",
        "firstCallbackMicros" to "µs",
        "sampleRate" to "Hz",
        "framesPerBurst" to "frames",
        "bufferCapacityInFrames" to "frames",
        "bufferSizeInFrames" to "frames",
        "underruns" to "count",
        "latencyMicros" to "µs",
        "seconds" to "s"
    )

    /** Opens and starts both streams once: rate, Burst, path and the times to open. Needs the microphone. */
    fun streams(): List<ResultRow> {
        val rows = mutableListOf<ResultRow>()
        rows += rowsOf(ID, "Output stream", AudioPathProbe.probeOutput(false, 0)) { UNITS[it] ?: "" }
        rows += rowsOf(ID, "Input stream", AudioPathProbe.probeInput()) { UNITS[it] ?: "" }
        return rows
    }

    /** Plays silence for 10 min with a buffer of 2 Bursts and counts the Underruns. */
    fun run(performanceHint: Boolean): List<ResultRow> {
        val hint = if (performanceHint) "with" else "without"
        val json = AudioPathProbe.probeOutput(performanceHint, RUN_SECONDS)
        return rowsOf(ID, "Output for 10 min $hint performance hints", json) { UNITS[it] ?: "" }
    }

    /** One row per value of the JSON object of a probe. */
    internal fun rowsOf(id: String, stream: String, json: String, unitOf: (String) -> String): List<ResultRow> {
        val values = JSONObject(json)
        val rows = mutableListOf<ResultRow>()
        for (key in values.keys()) {
            rows += ResultRow(id, "measurement", "$stream: $key", values.get(key), unitOf(key))
        }
        return rows
    }
}
