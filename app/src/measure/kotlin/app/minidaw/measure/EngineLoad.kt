package app.minidaw.measure

import android.content.Context
import app.minidaw.engine.EngineLoadProbe
import app.minidaw.engine.api.SnapshotPrototype

/**
 * The check V-04 (TK-007, technology_stack.md Section 14): the load of the Voices and of the effects
 * for both standard test Projects, the cost of each kernel, and the times of a compile and of a load
 * of a Snapshot. Every run plays silence.
 */
internal object EngineLoad {
    const val RUN_SECONDS = 60
    private const val ID = "V-04"
    private const val WARM_UP = 5
    private const val RUNS = 21
    private const val NANOS_PER_MICRO = 1000.0

    /** Renders a standard test Project on the audio thread for 60 s. */
    fun load(context: Context, version10: Boolean): List<ResultRow> {
        val name = if (version10) "version 1.0" else "MVP"
        val json = EngineLoadProbe.probeLoad(if (version10) 1 else 0, RUN_SECONDS, ClickSample.load(context))
        return AudioPath.rowsOf(ID, "Load of the test Project of the $name", json) { unitOf(it) }
    }

    /** The kernels alone, and the compile and the load of the Snapshot of version 1.0. */
    fun kernelsAndSnapshot(context: Context): List<ResultRow> {
        val rows = mutableListOf<ResultRow>()
        val kernels = EngineLoadProbe.probeKernels(ClickSample.load(context))
        rows += AudioPath.rowsOf(ID, "Kernels, for 128 frames", kernels) { if (it.endsWith("Micros")) "µs" else "" }
        repeat(WARM_UP) { SnapshotPrototype.write(SnapshotPrototype.version10) }
        val times = DoubleArray(RUNS)
        var bytes = ByteArray(0)
        for (i in times.indices) {
            val start = System.nanoTime()
            bytes = SnapshotPrototype.write(SnapshotPrototype.version10)
            times[i] = (System.nanoTime() - start) / NANOS_PER_MICRO
        }
        times.sort()
        val title = "Snapshot of version 1.0: median compile on the JVM of the phone"
        rows += ResultRow(ID, "measurement", title, times[times.size / 2], "µs")
        val loaded = EngineLoadProbe.probeSnapshotLoad(bytes, RUNS)
        rows += AudioPath.rowsOf(ID, "Snapshot of version 1.0", loaded) { if (it.endsWith("Micros")) "µs" else "" }
        return rows
    }

    private fun unitOf(key: String): String = when {
        key.endsWith("Percent") -> "%"
        key == "underruns" || key == "callbacks" -> "count"
        key.endsWith("Frames") || key == "framesPerBurst" -> "frames"
        key == "sampleRate" -> "Hz"
        else -> ""
    }
}
