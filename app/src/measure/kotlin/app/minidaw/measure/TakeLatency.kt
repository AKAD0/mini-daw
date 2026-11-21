package app.minidaw.measure

import android.content.Context
import app.minidaw.engine.AudioPathProbe

/**
 * The check V-07 (TK-006, D-040): the offset of a Take by the timestamps of the streams and by the
 * calibration by loopback. The speaker or the wired headphones play the Sample "Click high" 8 times.
 */
internal object TakeLatency {
    private const val ID = "V-07"

    /** Plays the click and records it; takes about 12 s. Needs the microphone. */
    fun run(context: Context, route: String): List<ResultRow> {
        val json = AudioPathProbe.probeTakeLatency(ClickSample.load(context))
        return AudioPath.rowsOf(ID, "Loopback with $route", json) { if (it.endsWith("Ms")) "ms" else "" }
    }
}
