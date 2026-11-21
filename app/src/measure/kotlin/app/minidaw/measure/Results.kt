package app.minidaw.measure

import android.content.Context
import android.os.Build
import app.minidaw.BuildConfig
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** The file of results of a test build (feature_specifications.md Section 6.12). */
internal object Results {
    const val FORMAT = "minidaw-results"
    const val FORMAT_VERSION = 1
    private const val INDENT = 2
    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).withZone(ZoneOffset.UTC)

    fun collect(context: Context, installVerdict: Verdict): JSONObject {
        val json = JSONObject()
        json.put("format", FORMAT)
        json.put("formatVersion", FORMAT_VERSION)
        json.put("createdAt", Instant.now().toString())
        json.put("build", build(context))
        json.put("device", device())
        json.put("results", JSONArray().put(installRow(installVerdict)))
        return json
    }

    fun text(json: JSONObject): String = json.toString(INDENT)

    fun directory(context: Context): File = File(context.cacheDir, "results")

    fun write(context: Context, json: JSONObject): File {
        val dir = directory(context)
        dir.mkdirs()
        val stamp = STAMP.format(Instant.now())
        val name = "minidaw-results-${context.packageName}-${BuildConfig.VERSION_CODE}-$stamp.json"
        val file = File(dir, name)
        file.writeText(text(json))
        return file
    }

    private fun build(context: Context): JSONObject {
        val source = context.packageManager.getInstallSourceInfo(context.packageName)
        val json = JSONObject()
        json.put("applicationId", context.packageName)
        json.put("versionName", BuildConfig.VERSION_NAME)
        json.put("versionCode", BuildConfig.VERSION_CODE)
        json.put("change", BuildConfig.CHANGE_ID)
        json.put("buildType", BuildConfig.BUILD_TYPE)
        json.put("installingPackage", source.installingPackageName ?: JSONObject.NULL)
        json.put("initiatingPackage", source.initiatingPackageName ?: JSONObject.NULL)
        return json
    }

    private fun device(): JSONObject {
        val json = JSONObject()
        json.put("manufacturer", Build.MANUFACTURER)
        json.put("model", Build.MODEL)
        json.put("device", Build.DEVICE)
        json.put("androidRelease", Build.VERSION.RELEASE)
        json.put("sdkInt", Build.VERSION.SDK_INT)
        return json
    }

    private fun installRow(verdict: Verdict): JSONObject {
        val json = JSONObject()
        json.put("id", "TK-002")
        json.put("kind", "verdict")
        json.put("title", "The test build was installed from a download")
        json.put("passRule", "The owner installed this build from the page of releases and opened it")
        json.put("verdict", verdict.word)
        return json
    }
}
