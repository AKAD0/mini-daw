package app.minidaw.measure

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout

/**
 * The values of V-01 that only OboeTester or the settings show (requirements.md Section 3.2, items 1, 3,
 * 4, 5 and 8). The owner types each value. An empty field gives no entry.
 */
internal class Entries {
    private val fields = mutableListOf<EditText>()

    fun addTo(layout: LinearLayout) {
        for ((title, unit) in TITLES) {
            layout.text(if (unit.isEmpty()) title else "$title, in $unit")
            val field = EditText(layout.context)
            field.inputType = InputType.TYPE_CLASS_TEXT
            fields += field
            layout.addView(field)
        }
    }

    fun rows(): List<ResultRow> {
        val rows = mutableListOf<ResultRow>()
        for ((index, field) in fields.withIndex()) {
            val text = field.text.toString().trim()
            if (text.isNotEmpty()) {
                val (title, unit) = TITLES[index]
                val number = text.replace(',', '.').toDoubleOrNull()
                rows += ResultRow("V-01", "entry", title, number, unit, if (number == null) text else "")
            }
        }
        return rows
    }

    fun save(state: Bundle) {
        state.putStringArray(KEY, fields.map { it.text.toString() }.toTypedArray())
    }

    fun restore(state: Bundle?) {
        val values = state?.getStringArray(KEY) ?: return
        for ((index, value) in values.withIndex()) {
            fields.getOrNull(index)?.setText(value)
        }
    }

    private companion object {
        const val KEY = "entries"
        val TITLES = listOf(
            "One UI version (Settings, About phone)" to "",
            "Low-latency path granted for the output (OboeTester)" to "",
            "Round-trip latency with the speaker (OboeTester)" to "ms",
            "Round-trip latency with USB-C headphones (OboeTester)" to "ms",
            "Round-trip latency with Bluetooth headphones (OboeTester)" to "ms",
            "Tap-to-sound latency with the speaker (OboeTester)" to "ms",
            "Tap-to-sound latency with USB-C headphones (OboeTester)" to "ms",
            "Tap-to-sound latency with Bluetooth headphones (OboeTester)" to "ms",
            "Underruns under load, battery saver off (OboeTester)" to "count",
            "Underruns under load, battery saver on (OboeTester)" to "count",
            "Memory limit of an app on Android 17, where a screen shows it" to "MB"
        )
    }
}
