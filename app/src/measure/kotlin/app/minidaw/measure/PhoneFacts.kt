package app.minidaw.measure

import android.app.Activity
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.graphics.Insets
import android.media.AudioManager
import android.os.Build
import android.os.StatFs
import android.view.WindowInsets
import kotlin.math.roundToInt

/**
 * The check V-01: the facts of the phone that Android reports (requirements.md Section 3.2, items 1, 2,
 * 3, 7, 8 and 9). The owner adds the values that only OboeTester or the settings show.
 */
internal object PhoneFacts {
    private const val ID = "V-01"
    private const val MEGABYTE = 1_048_576L

    fun collect(activity: Activity): List<ResultRow> {
        val rows = mutableListOf<ResultRow>()
        device(rows)
        audio(activity, rows)
        screen(activity, rows)
        memory(activity, rows)
        microphone(activity, rows)
        return rows
    }

    fun unprocessedInput(activity: Activity): Boolean {
        val audio = activity.getSystemService(AudioManager::class.java)
        return audio.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    }

    private fun fact(title: String, value: Any?, unit: String = "", note: String = ""): ResultRow =
        ResultRow(ID, "measurement", title, value, unit, note)

    private fun device(rows: MutableList<ResultRow>) {
        rows += fact("Manufacturer and model", null, note = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        rows += fact("Android version", Build.VERSION.SDK_INT, "API level", "Android ${Build.VERSION.RELEASE}")
    }

    private fun audio(activity: Activity, rows: MutableList<ResultRow>) {
        val packages = activity.packageManager
        val lowLatency = packages.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)
        val pro = packages.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)
        rows += fact("System feature android.hardware.audio.low_latency", lowLatency)
        rows += fact("System feature android.hardware.audio.pro", pro)
        val audio = activity.getSystemService(AudioManager::class.java)
        val rate = audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        val burst = audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()
        rows += fact("Native sample rate of the output", rate, "Hz")
        rows += fact("Burst of the output", burst, "frames")
    }

    private fun screen(activity: Activity, rows: MutableList<ResultRow>) {
        val metrics = activity.windowManager.currentWindowMetrics
        val density = activity.resources.displayMetrics.density
        val bounds = metrics.bounds
        rows += fact("Width of the window in landscape", (bounds.width() / density).roundToInt(), "dp")
        rows += fact("Height of the window in landscape", (bounds.height() / density).roundToInt(), "dp")
        val cutout = metrics.windowInsets.getInsets(WindowInsets.Type.displayCutout())
        val gestures = metrics.windowInsets.getInsets(WindowInsets.Type.systemGestures())
        rows += fact("Insets of the display cutout", null, note = insets(cutout, density))
        rows += fact("Insets of the system gestures", null, note = insets(gestures, density))
        rows += fact("Density of the screen", density, "px per dp")
    }

    private fun insets(insets: Insets, density: Float): String {
        val left = (insets.left / density).roundToInt()
        val top = (insets.top / density).roundToInt()
        val right = (insets.right / density).roundToInt()
        val bottom = (insets.bottom / density).roundToInt()
        return "left $left, top $top, right $right, bottom $bottom dp"
    }

    private fun memory(activity: Activity, rows: MutableList<ResultRow>) {
        val manager = activity.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        rows += fact("Total RAM", info.totalMem / MEGABYTE, "MB")
        rows += fact("Memory class of an app (heap of the runtime)", manager.memoryClass, "MB")
        rows += fact("Large memory class of an app", manager.largeMemoryClass, "MB")
        rows += fact("Free storage", StatFs(activity.filesDir.path).availableBytes / MEGABYTE, "MB")
    }

    private fun microphone(activity: Activity, rows: MutableList<ResultRow>) {
        rows += fact("Input preset UNPROCESSED is supported", unprocessedInput(activity))
        val audio = activity.getSystemService(AudioManager::class.java)
        for (device in audio.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            val rates = device.sampleRates.joinToString(", ").ifEmpty { "any" }
            rows += fact("Input ${device.productName} (type ${device.type})", null, note = "sample rates: $rates")
        }
    }
}
