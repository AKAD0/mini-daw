package app.minidaw.check.humming

import android.content.Context
import java.io.File

/**
 * Both pitch candidates of the check V-05 (TK-010, technology_stack.md Section 8.2) with the same
 * code for Notes. Each call returns one JSON object with the times and the Notes.
 */
object HumProbe {
    const val SWIFTF0 = 1
    const val PYIN = 2
    private const val MODEL_ASSET = "swiftf0/swiftf0-0.3.0.onnx"

    init {
        System.loadLibrary("minidaw-humming")
    }

    /** Converts mono [audio] at [rate] with one [candidate]; the model is needed for SwiftF0 only. */
    external fun convert(audio: FloatArray, rate: Int, candidate: Int, model: String): String

    /** The model file of SwiftF0, copied once from the assets of :analysis into the private files. */
    fun modelPath(context: Context): String {
        val file = File(context.filesDir, MODEL_ASSET.substringAfter('/'))
        if (!file.exists()) {
            context.assets.open(MODEL_ASSET).use { input -> file.outputStream().use { input.copyTo(it) } }
        }
        return file.absolutePath
    }
}
