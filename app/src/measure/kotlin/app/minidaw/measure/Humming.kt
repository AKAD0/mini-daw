package app.minidaw.measure

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import app.minidaw.check.humming.HumProbe
import app.minidaw.engine.TonePlayer
import java.io.File
import java.util.zip.ZipFile
import org.json.JSONArray
import org.json.JSONObject

/**
 * The check V-05 (TK-010, technology_stack.md Section 8.2): the owner hums a melody; both pitch
 * candidates convert it with the same code for Notes. The screen shows both results as a picture and
 * plays them with the default tone "Keys". The owner counts the wrong and the missing Notes of each.
 */
internal class Humming(private val activity: Activity, private val onRows: (List<ResultRow>) -> Unit) {
    private class Result(
        val name: String,
        val notes: DoubleArray,
        val octaveShift: Int,
        val loadMs: Double,
        val convertMs: Double
    )

    private class Counts(val wrong: EditText, val missing: EditText)

    @Volatile private var recording = false
    private var audio = FloatArray(0)
    private var results: List<Result> = emptyList()
    private var melody = 1
    private var sizesKept = false
    private var track: AudioTrack? = null
    private val counts = mutableMapOf<String, Counts>()
    private lateinit var status: TextView
    private lateinit var picture: NotesView

    fun addTo(layout: LinearLayout) {
        layout.text(activity.getString(R.string.v05_how))
        layout.button(activity.getString(R.string.v05_record)) { record() }
        layout.button(activity.getString(R.string.v05_stop)) { recording = false }
        status = layout.text("")
        picture = NotesView(activity)
        layout.addView(picture, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, PICTURE_HEIGHT))
        layout.button(activity.getString(R.string.v05_play_recording)) { play(audio) }
        for (name in NAMES) {
            layout.button(activity.getString(R.string.v05_play, name)) { playNotes(name) }
        }
        for (name in NAMES) {
            layout.text(activity.getString(R.string.v05_wrong, name))
            val wrong = numberField(layout)
            layout.text(activity.getString(R.string.v05_missing, name))
            counts[name] = Counts(wrong, numberField(layout))
        }
        layout.button(activity.getString(R.string.v05_keep)) { keep() }
    }

    private fun numberField(layout: LinearLayout): EditText {
        val field = EditText(activity)
        field.inputType = InputType.TYPE_CLASS_NUMBER
        layout.addView(field)
        return field
    }

    private fun record() {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_PERMISSION)
            return
        }
        if (recording) {
            return
        }
        recording = true
        status.text = activity.getString(R.string.v05_recording, melody)
        val unprocessed = PhoneFacts.unprocessedInput(activity)
        val source = if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC
        Thread {
            val taken = take(source)
            activity.runOnUiThread { status.text = activity.getString(R.string.v05_converting) }
            val converted = convert(taken)
            activity.runOnUiThread {
                audio = taken
                results = converted
                val lanes = converted.map {
                    NotesView.Lane(it.name, it.notes, if (it.name == PYIN) PYIN_COLOR else SWIFTF0_COLOR)
                }
                picture.show(lanes, taken.size / RATE.toDouble())
                status.text = converted.joinToString("\n") { summary(it) }
            }
        }.start()
    }

    /** Records until the owner taps "Stop", at most 2 min (requirements.md Section 8). */
    private fun take(source: Int): FloatArray {
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_FLOAT
        val size = maxOf(AudioRecord.getMinBufferSize(RATE, channel, encoding), RATE)
        val record = AudioRecord(source, RATE, channel, encoding, size)
        val samples = FloatArray(RATE * MAX_SECONDS)
        var read = 0
        record.startRecording()
        while (recording && read < samples.size) {
            val count = record.read(samples, read, minOf(CHUNK, samples.size - read), AudioRecord.READ_BLOCKING)
            if (count <= 0) {
                break
            }
            read += count
        }
        record.stop()
        record.release()
        recording = false
        return samples.copyOf(read)
    }

    private fun convert(samples: FloatArray): List<Result> {
        val model = HumProbe.modelPath(activity)
        return listOf(HumProbe.PYIN to PYIN, HumProbe.SWIFTF0 to SWIFTF0).map { (candidate, name) ->
            val json = JSONObject(HumProbe.convert(samples, RATE, candidate, model))
            val list = json.optJSONArray("notes") ?: JSONArray()
            val notes = DoubleArray(list.length() * FIELDS)
            for (i in 0 until list.length()) {
                val note = list.getJSONArray(i)
                for (k in 0 until FIELDS) {
                    notes[FIELDS * i + k] = note.getDouble(k)
                }
            }
            Result(
                name = name,
                notes = notes,
                octaveShift = json.optInt("octaveShift"),
                loadMs = json.optDouble("loadMs", -1.0),
                convertMs = json.optDouble("convertMs", -1.0)
            )
        }
    }

    private fun playNotes(name: String) {
        val result = results.firstOrNull { it.name == name } ?: return
        val shifted = result.notes.copyOf()
        for (n in 0 until shifted.size / FIELDS) {
            shifted[FIELDS * n + 2] += result.octaveShift
        }
        Thread {
            val keys = AssetSamples.load(activity, AssetSamples.KEYS)
            play(TonePlayer.render(shifted, keys, KEYS_ROOT))
        }.start()
    }

    private fun play(samples: FloatArray) {
        if (samples.isEmpty()) {
            return
        }
        track?.release()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        val player = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = player
        Thread {
            player.play()
            player.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            player.stop()
        }.start()
    }

    private fun keep() {
        if (results.isEmpty()) {
            return
        }
        val rows = mutableListOf<ResultRow>()
        if (!sizesKept) {
            rows += sizes()
            sizesKept = true
        }
        val prefix = "Melody $melody"
        rows += ResultRow(ID, "measurement", "$prefix: length", audio.size / RATE.toDouble(), "s")
        for (result in results) {
            val notes = result.notes.size / FIELDS
            val field = counts.getValue(result.name)
            val wrong = field.wrong.text.toString().toIntOrNull()
            val missing = field.missing.text.toString().toIntOrNull()
            val title = "$prefix, ${result.name}"
            rows += ResultRow(ID, "measurement", "$title: Notes", notes, "count")
            rows += ResultRow(ID, "measurement", "$title: time of the conversion", result.convertMs, "ms")
            rows += ResultRow(ID, "measurement", "$title: time to load the candidate", result.loadMs, "ms")
            rows += ResultRow(ID, "entry", "$title: wrong or extra Notes", wrong, "count")
            rows += ResultRow(ID, "entry", "$title: missing Notes", missing, "count")
            if (wrong != null && missing != null) {
                rows += ResultRow(ID, "measurement", "$title: F-measure of Notes", fMeasure(notes, wrong, missing))
            }
            field.wrong.setText("")
            field.missing.setText("")
        }
        melody += 1
        status.text = activity.getString(R.string.v05_kept, melody - 1)
        onRows(rows)
    }

    /** The size of ONNX Runtime in the test build against NFR-RES-05 (technology_stack.md Section 8.2). */
    private fun sizes(): List<ResultRow> {
        val apk = File(activity.applicationInfo.sourceDir)
        val runtime = ZipFile(apk).use { zip -> zip.getEntry(RUNTIME_ENTRY)?.size }
        return listOf(
            ResultRow(ID, "measurement", "Size of the test build", apk.length(), "bytes"),
            ResultRow(ID, "measurement", "Size of libonnxruntime.so in the test build", runtime, "bytes")
        )
    }

    private fun summary(result: Result): String =
        "${result.name}: ${result.notes.size / FIELDS} Notes in ${result.convertMs.toInt()} ms"

    companion object {
        private const val ID = "V-05"
        private const val REQUEST_PERMISSION = 3
        private const val RATE = 48_000
        private const val MAX_SECONDS = 120
        private const val CHUNK = 4800
        private const val FIELDS = 3
        private const val KEYS_ROOT = 60.0
        private const val PICTURE_HEIGHT = 600
        private const val PYIN = "pYIN"
        private const val SWIFTF0 = "SwiftF0"
        private const val PYIN_COLOR = 0xFFE0A030.toInt()
        private const val SWIFTF0_COLOR = 0xFF3D7BD9.toInt()
        private const val RUNTIME_ENTRY = "lib/arm64-v8a/libonnxruntime.so"
        private val NAMES = listOf(PYIN, SWIFTF0)

        /** The F-measure of Notes from the counts of the owner: the correct Notes against found and true Notes. */
        fun fMeasure(found: Int, wrong: Int, missing: Int): Double {
            val correct = maxOf(found - wrong, 0)
            val precision = if (found > 0) correct.toDouble() / found else 0.0
            val truth = correct + missing
            val recall = if (truth > 0) correct.toDouble() / truth else 0.0
            return if (precision + recall > 0.0) 2 * precision * recall / (precision + recall) else 0.0
        }
    }
}
