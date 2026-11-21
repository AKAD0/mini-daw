package app.minidaw.core.model

/** The time base of architecture.md Section 4.3 and feature_specifications.md Section 1.1 (D-060). */
object TimeBase {
    const val TICKS_PER_QUARTER = 960

    /** Ticks of a whole note: a bar of 4/4. */
    const val TICKS_PER_WHOLE = 4 * TICKS_PER_QUARTER

    /** A step of the Channel Rack: one 1/16 note in every time signature. */
    const val STEP = 240

    const val FRAMES_PER_SECOND = 48_000

    /** Frames per Tick are this number divided by the stored tempo in thousandths of a BPM. */
    const val FRAMES_PER_TICK_TIMES_TEMPO = 3_000_000L

    const val MIN_TEMPO_MILLI_BPM = 30_000
    const val MAX_TEMPO_MILLI_BPM = 300_000

    fun barTicks(signature: TimeSignature): Int = signature.numerator * TICKS_PER_WHOLE / signature.denominator

    /** The length on the timeline of an Audio clip of `frames` frames: rounded up (Section 2.2.2). */
    fun audioLengthTicks(frames: Int, tempoMilliBpm: Int): Int {
        val scaled = frames.toLong() * tempoMilliBpm
        return ((scaled + FRAMES_PER_TICK_TIMES_TEMPO - 1) / FRAMES_PER_TICK_TIMES_TEMPO).toInt()
    }

    /** The number of frames of `ticks` Ticks, rounded to the nearest frame (the trim of Section 2.2.2). */
    fun framesOfTicks(ticks: Int, tempoMilliBpm: Int): Int {
        val exact = ticks.toLong() * FRAMES_PER_TICK_TIMES_TEMPO
        return ((2 * exact + tempoMilliBpm) / (2L * tempoMilliBpm)).toInt()
    }
}
