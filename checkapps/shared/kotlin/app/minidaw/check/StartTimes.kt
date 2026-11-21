package app.minidaw.check

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.profileinstaller.ProfileVerifier
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cold starts for the check V-02 (technology_stack.md Section 6.1). One start counts per process: the
 * time runs from the moment Android was asked to start the process to the end of the first frame.
 * Both apps for the check compile this file.
 */
object StartTimes {
    private const val PREFS = "v02"
    private const val INDENT = 2
    private const val HALF = 2
    private var recorded = false

    /** Records the first frame of this process under the key, then calls back on the main thread. */
    fun recordFirstFrame(activity: Activity, key: String, onRecorded: () -> Unit) {
        if (recorded) {
            onRecorded()
            return
        }
        recorded = true
        val decor = activity.window.decorView
        val listener = object : ViewTreeObserver.OnDrawListener {
            private var drawn = false

            override fun onDraw() {
                if (drawn) {
                    return
                }
                drawn = true
                Handler(Looper.getMainLooper()).postAtFrontOfQueue {
                    save(activity, key, SystemClock.uptimeMillis() - processStart())
                    decor.viewTreeObserver.removeOnDrawListener(this)
                    onRecorded()
                }
            }
        }
        decor.viewTreeObserver.addOnDrawListener(listener)
    }

    fun values(context: Context, key: String): List<Long> {
        val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, "") ?: ""
        return text.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun median(values: List<Long>): Long? {
        if (values.isEmpty()) {
            return null
        }
        val sorted = values.sorted()
        val middle = sorted.size / HALF
        return if (sorted.size % HALF == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / HALF
    }

    /** The state of the Baseline Profile, as androidx.profileinstaller reports it. Call it off the main thread. */
    fun profileState(): String {
        val status = ProfileVerifier.getCompilationStatusAsync().get()
        val compiled = if (status.isCompiledWithProfile) "compiled with the profile" else "not compiled"
        val enqueued = if (status.hasProfileEnqueuedForCompilation()) ", profile waits for compilation" else ""
        return "$compiled$enqueued (code ${status.profileInstallResultCode})"
    }

    /** The file of results (feature_specifications.md Section 6.12) for the keys of this app. */
    fun results(context: Context, keys: List<String>, profile: String): String {
        val rows = JSONArray()
        for (key in keys) {
            val values = values(context, key)
            val row = JSONObject()
            row.put("id", "V-02")
            row.put("kind", "measurement")
            row.put("title", "Cold start of $key: median of ${values.size} starts")
            row.put("verdict", "recorded")
            median(values)?.let { row.put("value", it) }
            row.put("unit", "ms")
            row.put("note", "all starts: ${values.joinToString(", ")}; $profile")
            rows.put(row)
        }
        val device = JSONObject()
        device.put("manufacturer", Build.MANUFACTURER)
        device.put("model", Build.MODEL)
        device.put("sdkInt", Build.VERSION.SDK_INT)
        val json = JSONObject()
        json.put("format", "minidaw-results")
        json.put("formatVersion", 1)
        json.put("build", JSONObject().put("applicationId", context.packageName))
        json.put("device", device)
        json.put("results", rows)
        return json.toString(INDENT)
    }

    fun share(activity: Activity, text: String) {
        val send = Intent(Intent.ACTION_SEND)
        send.type = "text/plain"
        send.putExtra(Intent.EXTRA_SUBJECT, "minidaw-results-${activity.packageName}.json")
        send.putExtra(Intent.EXTRA_TEXT, text)
        activity.startActivity(Intent.createChooser(send, "Share results"))
    }

    private fun save(context: Context, key: String, millis: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = values(context, key) + millis
        prefs.edit().putString(key, list.joinToString(",")).apply()
    }

    private fun processStart(): Long {
        val requested = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        return if (requested) Process.getStartRequestedUptimeMillis() else Process.getStartUptimeMillis()
    }
}
