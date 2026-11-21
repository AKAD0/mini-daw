package app.minidaw.measure

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The Samples of the test build: WAV with 48 kHz, 16 bits and one channel (feature_specifications.md
 * Section 7.2), rendered by tools/sounds/render.py.
 */
internal object AssetSamples {
    const val CLICK_HIGH = "click_high.wav"
    const val KEYS = "keys.wav"
    private const val RIFF_HEADER = 12
    private const val CHUNK_HEADER = 8
    private const val ID_LENGTH = 4
    private const val SIZE_OFFSET = 4
    private const val BYTES_PER_VALUE = 2
    private const val FULL_SCALE = 32768f

    /** The values of the Sample [asset]. */
    fun load(context: Context, asset: String): FloatArray {
        val bytes = context.assets.open(asset).use { it.readBytes() }
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
        error("$asset holds no data")
    }
}
