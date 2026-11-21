package app.minidaw.measure

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.minidaw.R
import app.minidaw.keepClearOfSystemBars

/**
 * The screen "Measurements" of a test build (ui_ux_specification.md Section 15.4). It holds the row of
 * TK-002, the checks V-01 (TK-003), V-03 (TK-005), V-04 (TK-007), V-05 (TK-010) and V-07 (TK-006), and the button
 * that shares the results as a file
 * (NFR-MNT-06).
 */
class MeasurementsActivity : Activity() {

    private var verdict = Verdict.NOT_RUN
    private var facts: List<ResultRow> = emptyList()
    private var noise: ResultRow? = null
    private var audio: List<ResultRow> = emptyList()
    private var hummed: List<ResultRow> = emptyList()
    private var running = false
    private val entries = Entries()
    private lateinit var saved: SavedRows
    private lateinit var verdictView: TextView
    private lateinit var statusView: TextView
    private lateinit var resultsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restore()
        val padding = (PADDING_DP * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(padding, padding, padding, padding)
        layout.text(getString(R.string.measurements_title), TITLE_SP)

        layout.text(getString(R.string.results_kept))
        layout.text(getString(R.string.install_question))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.button(getString(R.string.mark_pass)) { mark(Verdict.PASS) }
        row.button(getString(R.string.mark_fail)) { mark(Verdict.FAIL) }
        layout.addView(row)
        verdictView = layout.text("")

        layout.text(getString(R.string.v01_title), SECTION_SP)
        layout.button(getString(R.string.v01_facts)) {
            facts = PhoneFacts.collect(this)
            refresh()
        }
        layout.button(getString(R.string.v01_noise)) { measureNoise() }
        layout.text(getString(R.string.v01_entries))
        entries.addTo(layout)
        entries.restore(savedInstanceState)

        layout.text(getString(R.string.v03_title), SECTION_SP)
        layout.button(getString(R.string.v03_streams)) { measureAudio(STREAMS) }
        layout.button(getString(R.string.v03_run_hint)) { measureAudio(RUN_WITH_HINT) }
        layout.button(getString(R.string.v03_run_plain)) { measureAudio(RUN_WITHOUT_HINT) }

        layout.text(getString(R.string.v04_title), SECTION_SP)
        layout.text(getString(R.string.v04_how))
        layout.button(getString(R.string.v04_kernels)) { measureAudio(KERNELS) }
        layout.button(getString(R.string.v04_load_mvp)) { measureAudio(LOAD_MVP) }
        layout.button(getString(R.string.v04_load_full)) { measureAudio(LOAD_VERSION_10) }

        layout.text(getString(R.string.v05_title), SECTION_SP)
        Humming(this, hummed) { rows ->
            hummed = hummed + rows
            refresh()
        }.addTo(layout)

        layout.text(getString(R.string.v07_title), SECTION_SP)
        layout.text(getString(R.string.v07_how))
        layout.button(getString(R.string.v07_speaker)) { measureAudio(LOOPBACK_SPEAKER) }
        layout.button(getString(R.string.v07_headphones)) { measureAudio(LOOPBACK_HEADPHONES) }
        statusView = layout.text("")

        layout.button(getString(R.string.share_results)) { share() }
        layout.button(getString(R.string.clear_results)) { saved.confirmClear(this) }
        layout.text(getString(R.string.shown_results))
        resultsView = layout.text("")
        resultsView.typeface = Typeface.MONOSPACE

        val scroll = ScrollView(this)
        scroll.addView(layout)
        scroll.keepClearOfSystemBars()
        setContentView(scroll)
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        entries.save(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (granted && requestCode == REQUEST_MICROPHONE) {
            measureNoise()
        }
        if (granted && requestCode >= REQUEST_AUDIO) {
            measureAudio(requestCode - REQUEST_AUDIO)
        }
    }

    private fun mark(value: Verdict) {
        verdict = value
        refresh()
    }

    private fun measureNoise() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE)
            return
        }
        val unprocessed = PhoneFacts.unprocessedInput(this)
        Thread {
            val level = NoiseLevel.measure(unprocessed)
            val source = if (unprocessed) "UNPROCESSED" else "MIC"
            val result = ResultRow("V-01", "measurement", "Level of noise of the microphone", level, "dBFS", source)
            runOnUiThread {
                noise = result
                refresh()
            }
        }.start()
    }

    private fun measureAudio(kind: Int) {
        val microphone = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val needsMicrophone = kind == STREAMS || kind == LOOPBACK_SPEAKER || kind == LOOPBACK_HEADPHONES
        if (needsMicrophone && !microphone) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO + kind)
            return
        }
        if (running) {
            return
        }
        running = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        statusView.text = getString(R.string.v03_running)
        Thread {
            val rows = rowsOf(kind)
            runOnUiThread {
                audio = audio + rows
                running = false
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                statusView.text = ""
                refresh()
            }
        }.start()
    }

    private fun rowsOf(kind: Int): List<ResultRow> = when (kind) {
        STREAMS -> AudioPath.streams()
        LOOPBACK_SPEAKER -> TakeLatency.run(this, "the speaker")
        LOOPBACK_HEADPHONES -> TakeLatency.run(this, "wired headphones")
        KERNELS -> EngineLoad.kernelsAndSnapshot(this)
        LOAD_MVP -> EngineLoad.load(this, version10 = false)
        LOAD_VERSION_10 -> EngineLoad.load(this, version10 = true)
        else -> AudioPath.run(kind == RUN_WITH_HINT)
    }

    private val rows: List<ResultRow>
        get() = facts + listOfNotNull(noise) + audio + hummed + entries.rows()

    /** Takes the verdict and the rows that the screen kept in its file (SavedRows). */
    private fun restore() {
        saved = SavedRows(this)
        val kept = saved.load()
        verdict = Verdict.entries.firstOrNull { it.name == kept.verdict } ?: Verdict.NOT_RUN
        facts = kept.groups[FACTS].orEmpty()
        noise = kept.groups[NOISE]?.firstOrNull()
        audio = kept.groups[AUDIO].orEmpty()
        hummed = kept.groups[HUMMED].orEmpty()
    }

    private fun refresh() {
        val groups = mapOf(FACTS to facts, NOISE to listOfNotNull(noise), AUDIO to audio, HUMMED to hummed)
        saved.save(verdict.name, groups)
        verdictView.text = getString(R.string.verdict_line, verdict.word)
        resultsView.text = Results.text(Results.collect(this, verdict, rows))
    }

    private fun share() {
        val file = Results.write(this, Results.collect(this, verdict, rows))
        val send = Intent(Intent.ACTION_SEND)
        send.type = "application/json"
        send.putExtra(Intent.EXTRA_STREAM, ResultsProvider.uriFor(this, file.name))
        send.putExtra(Intent.EXTRA_SUBJECT, file.name)
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, getString(R.string.share_results)))
    }

    private companion object {
        const val FACTS = "facts"
        const val NOISE = "noise"
        const val AUDIO = "audio"
        const val HUMMED = "hummed"
        const val REQUEST_MICROPHONE = 1
        const val REQUEST_AUDIO = 10
        const val STREAMS = 0
        const val RUN_WITH_HINT = 1
        const val RUN_WITHOUT_HINT = 2
        const val LOOPBACK_SPEAKER = 3
        const val LOOPBACK_HEADPHONES = 4
        const val KERNELS = 5
        const val LOAD_MVP = 6
        const val LOAD_VERSION_10 = 7
        const val PADDING_DP = 24
        const val TITLE_SP = 24f
        const val SECTION_SP = 20f
    }
}
