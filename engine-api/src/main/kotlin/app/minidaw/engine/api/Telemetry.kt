package app.minidaw.engine.api

/** The peak of the left and the right side since the last read, and whether either side clipped. */
data class Peak(val left: Float, val right: Float, val clipped: Boolean) {
    companion object {
        val SILENT = Peak(0f, 0f, false)
    }
}

/** The counts of the diagnostics (architecture.md Section 5.10, item 7). */
data class TelemetryCounts(
    val underruns: Int = 0,
    val lateStreamers: Int = 0,
    val droppedCommands: Int = 0,
    val missingSamples: Int = 0
)

/**
 * The values that the engine publishes in shared memory (architecture.md Section 5.10, D-058). The UI reads them
 * once per frame without a call into the engine.
 *
 * `outputTimeNanos` is the time on the monotonic clock at which the output plays `positionTicks`. The UI computes
 * from it the position that the user hears now (FR-TR-10). The peaks of the channels stand in the order of the
 * table of channels of the current Snapshot.
 */
data class Telemetry(
    val positionTicks: Int,
    val playing: Boolean,
    val mode: TransportMode,
    val outputTimeNanos: Long,
    val channelPeaks: List<Peak>,
    val inputPeak: Peak,
    val voices: Int,
    val loadPercent: Float,
    val counts: TelemetryCounts
) {
    companion object {
        /** The values before the first block. */
        val IDLE = Telemetry(
            positionTicks = 0,
            playing = false,
            mode = TransportMode.SONG,
            outputTimeNanos = 0L,
            channelPeaks = emptyList(),
            inputPeak = Peak.SILENT,
            voices = 0,
            loadPercent = 0f,
            counts = TelemetryCounts()
        )
    }
}
