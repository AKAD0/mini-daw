package app.minidaw.measure

import android.content.Context
import app.minidaw.engine.AudioPathProbe
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The check V-07 (TK-006, D-040): the offset of a Take by the timestamps of the streams and by the
 * calibration by loopback. The speaker or the wired headphones play the Sample "Click high" 8 times.
 */
internal object TakeLatency {
    private const val ID = "V-07"
    private const val ASSET = "click_high.wav"
    private const val RIFF_HEADER = 12
    private const val CHUNK_HEADER = 8
    private const val ID_LENGTH = 4
    private const val SIZE_OFFSET = 4
    private const val BYTES_PER_VALUE = 2
    private const val FULL_SCALE = 32768f

    /** Plays the click and records it; takes about 12 s. Needs the microphone. */
    fun run(context: Context, route: String): List<ResultRow> {
        val json = AudioPathProbe.probeTakeLatency(click(context))
        return AudioPath.rowsOf(ID, "Loopback with $route", json) { if (it.endsWith("Ms")) "ms" else "" }
    }

    /** The values of "Click high": WAV with 48 kHz, 16 bits and one channel (feature_specifications.md 7.2). */
    private fun click(context: Context): FloatArray {
        val bytes = context.assets.open(ASSET).use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = RIFF_HEADER
        while (offset + CHUNK_HEADER <= bytes.size) {
            val name = String(bytes, offset, ID_LENGTH, Charsets.US_ASCII)
            val size = buffer.getInt(offset + SIZE_OFFSET)
            val start = offset + CHUNK_HEADER
            if (name == "data") {
                return FloatArray(size / BYTES_PER_VALUE) { buffer.getShort(start + BYTES_PER_VALUE * it) / FULL_SCALE }
            }
            offset = start + size + (size and 1)
        }
        error("$ASSET holds no data")
    }
}
