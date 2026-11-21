package app.minidaw.measure

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Small helpers that build the screen "Measurements" without a toolkit of its own. */
internal fun LinearLayout.text(value: String, size: Float = 0f): TextView {
    val view = TextView(context)
    view.text = value
    if (size > 0f) {
        view.textSize = size
    }
    addView(view)
    return view
}

internal fun LinearLayout.button(label: String, onClick: () -> Unit): Button {
    val view = Button(context)
    view.text = label
    view.setOnClickListener { onClick() }
    addView(view)
    return view
}
