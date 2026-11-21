package app.minidaw

import android.view.View
import android.view.WindowInsets

/**
 * Keeps the content clear of the system bars, the display cutout and the keyboard. Android 15 and later
 * draw an app with target SDK 35 or higher from edge to edge, so the app pads its content itself.
 * [padding] in pixels is added on each side.
 */
internal fun View.keepClearOfSystemBars(padding: Int = 0) {
    setOnApplyWindowInsetsListener { view, insets ->
        val types = WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime()
        val clear = insets.getInsets(types)
        view.setPadding(padding + clear.left, padding + clear.top, padding + clear.right, padding + clear.bottom)
        insets
    }
    requestApplyInsets()
}
