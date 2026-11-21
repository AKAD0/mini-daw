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

/**
 * The screen "Measurements" of a test build (ui_ux_specification.md Section 15.4). It holds the row of
 * TK-002, the checks V-01 (TK-003) and V-03 (TK-005), and the button that shares the results as a file
 * (NFR-MNT-06).
 */
class MeasurementsActivity : Activity() {

    private var verdict = Verdict.NOT_RUN
    private var facts: List<ResultRow> = emptyList()
    private var noise: ResultRow? = null
    private var audio: List<ResultRow> = emptyList()
    private var running = false
    private val entries = Entries()
    private lateinit var verdictView: TextView
    private lateinit var statusView: TextView
    private lateinit var resultsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verdict = savedInstanceState?.getString(KEY_VERDICT)?.let { Verdict.valueOf(it) } ?: Verdict.NOT_RUN
        val padding = (PADDING_DP * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(padding, padding, padding, padding)
        layout.text(getString(R.string.measurements_title), TITLE_SP)

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
        statusView = layout.text("")

        layout.button(getString(R.string.share_results)) { share() }
        layout.text(getString(R.string.shown_results))
        resultsView = layout.text("")
        resultsView.typeface = Typeface.MONOSPACE

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_VERDICT, verdict.name)
        entries.save(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (granted && requestCode == REQUEST_MICROPHONE) {
            measureNoise()
        }
        if (granted && requestCode == REQUEST_STREAMS) {
            measureAudio(STREAMS)
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
        if (kind == STREAMS && !microphone) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_STREAMS)
            return
        }
        if (running) {
            return
        }
        running = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        statusView.text = getString(R.string.v03_running)
        Thread {
            val rows = if (kind == STREAMS) AudioPath.streams() else AudioPath.run(kind == RUN_WITH_HINT)
            runOnUiThread {
                audio = audio + rows
                running = false
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                statusView.text = ""
                refresh()
            }
        }.start()
    }

    private fun rows(): List<ResultRow> = facts + listOfNotNull(noise) + audio + entries.rows()

    private fun refresh() {
        verdictView.text = getString(R.string.verdict_line, verdict.word)
        resultsView.text = Results.text(Results.collect(this, verdict, rows()))
    }

    private fun share() {
        val file = Results.write(this, Results.collect(this, verdict, rows()))
        val send = Intent(Intent.ACTION_SEND)
        send.type = "application/json"
        send.putExtra(Intent.EXTRA_STREAM, ResultsProvider.uriFor(this, file.name))
        send.putExtra(Intent.EXTRA_SUBJECT, file.name)
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, getString(R.string.share_results)))
    }

    private companion object {
        const val KEY_VERDICT = "verdict"
        const val REQUEST_MICROPHONE = 1
        const val REQUEST_STREAMS = 2
        const val STREAMS = 0
        const val RUN_WITH_HINT = 1
        const val RUN_WITHOUT_HINT = 2
        const val PADDING_DP = 24
        const val TITLE_SP = 24f
        const val SECTION_SP = 20f
    }
}
