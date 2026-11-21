package app.minidaw.measure

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import app.minidaw.R
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the verdict and the rows of the screen "Measurements" in a file, so that they survive when
 * Android recreates the screen or ends the process. The button "Clear the results" empties the file.
 */
internal class SavedRows(context: Context) {
    class State(val verdict: String, val groups: Map<String, List<ResultRow>>)

    private val file = File(context.filesDir, FILE_NAME)

    fun load(): State {
        val text = if (file.exists()) file.readText() else ""
        val json = runCatching { JSONObject(text) }.getOrNull() ?: JSONObject()
        val groups = mutableMapOf<String, List<ResultRow>>()
        val stored = json.optJSONObject(GROUPS) ?: JSONObject()
        for (key in stored.keys()) {
            groups[key] = rowsOf(stored.getJSONArray(key))
        }
        return State(json.optString(VERDICT), groups)
    }

    fun save(verdict: String, groups: Map<String, List<ResultRow>>) {
        val stored = JSONObject()
        for ((key, rows) in groups) {
            val array = JSONArray()
            for (row in rows) {
                array.put(jsonOf(row))
            }
            stored.put(key, array)
        }
        val json = JSONObject()
        json.put(VERDICT, verdict)
        json.put(GROUPS, stored)
        file.writeText(json.toString())
    }

    /** Asks first, then deletes the file and opens the screen anew, without results and typed values. */
    fun confirmClear(activity: Activity) {
        val dialog = AlertDialog.Builder(activity)
        dialog.setMessage(R.string.clear_question)
        dialog.setPositiveButton(R.string.clear_yes) { _, _ ->
            file.delete()
            activity.finish()
            activity.startActivity(activity.intent)
        }
        dialog.setNegativeButton(android.R.string.cancel, null)
        dialog.show()
    }

    private fun jsonOf(row: ResultRow): JSONObject {
        val json = JSONObject()
        json.put("id", row.id)
        json.put("kind", row.kind)
        json.put("title", row.title)
        json.put("value", row.value ?: JSONObject.NULL)
        json.put("unit", row.unit)
        json.put("note", row.note)
        return json
    }

    private fun rowsOf(array: JSONArray): List<ResultRow> {
        val rows = ArrayList<ResultRow>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val value = if (item.isNull("value")) null else item.get("value")
            rows += ResultRow(
                item.getString("id"),
                item.getString("kind"),
                item.getString("title"),
                value,
                item.optString("unit"),
                item.optString("note")
            )
        }
        return rows
    }

    private companion object {
        const val FILE_NAME = "measurements.json"
        const val VERDICT = "verdict"
        const val GROUPS = "groups"
    }
}
