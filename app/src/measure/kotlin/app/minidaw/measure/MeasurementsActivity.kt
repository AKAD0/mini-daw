package app.minidaw.measure

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.minidaw.R

/**
 * The screen "Measurements" of a test build (ui_ux_specification.md Section 15.4). This first version
 * holds the row of TK-002 and the button that shares the results as a file (NFR-MNT-06). Later tasks add
 * one button per measurement.
 */
class MeasurementsActivity : Activity() {

    private var verdict = Verdict.NOT_RUN
    private lateinit var verdictView: TextView
    private lateinit var resultsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verdict = savedInstanceState?.getString(KEY_VERDICT)?.let { Verdict.valueOf(it) } ?: Verdict.NOT_RUN
        val padding = (PADDING_DP * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(padding, padding, padding, padding)

        val title = TextView(this)
        title.text = getString(R.string.measurements_title)
        title.textSize = TITLE_SP
        layout.addView(title)

        val question = TextView(this)
        question.text = getString(R.string.install_question)
        layout.addView(question)
        layout.addView(markRow())

        verdictView = TextView(this)
        layout.addView(verdictView)

        val share = Button(this)
        share.text = getString(R.string.share_results)
        share.setOnClickListener { share() }
        layout.addView(share)

        val shown = TextView(this)
        shown.text = getString(R.string.shown_results)
        layout.addView(shown)
        resultsView = TextView(this)
        resultsView.typeface = Typeface.MONOSPACE
        layout.addView(resultsView)

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_VERDICT, verdict.name)
    }

    private fun markRow(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val pass = Button(this)
        pass.text = getString(R.string.mark_pass)
        pass.setOnClickListener { mark(Verdict.PASS) }
        row.addView(pass)
        val fail = Button(this)
        fail.text = getString(R.string.mark_fail)
        fail.setOnClickListener { mark(Verdict.FAIL) }
        row.addView(fail)
        return row
    }

    private fun mark(value: Verdict) {
        verdict = value
        refresh()
    }

    private fun refresh() {
        verdictView.text = getString(R.string.verdict_line, verdict.word)
        resultsView.text = Results.text(Results.collect(this, verdict))
    }

    private fun share() {
        val file = Results.write(this, Results.collect(this, verdict))
        val send = Intent(Intent.ACTION_SEND)
        send.type = "application/json"
        send.putExtra(Intent.EXTRA_STREAM, ResultsProvider.uriFor(this, file.name))
        send.putExtra(Intent.EXTRA_SUBJECT, file.name)
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, getString(R.string.share_results)))
    }

    private companion object {
        const val KEY_VERDICT = "verdict"
        const val PADDING_DP = 24
        const val TITLE_SP = 24f
    }
}
