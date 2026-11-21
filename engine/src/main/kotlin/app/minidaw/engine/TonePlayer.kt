package app.minidaw.engine

/** Renders Notes with one Sample in the play mode gate, for the check V-05 (TK-010). */
object TonePlayer {
    init {
        System.loadLibrary("minidaw-engine")
    }

    /**
     * Renders [notes], given as triples of start and end in seconds and pitch, with [sample] at the
     * [rootNote] into mono audio at 48 kHz.
     */
    external fun render(notes: DoubleArray, sample: FloatArray, rootNote: Double): FloatArray
}
