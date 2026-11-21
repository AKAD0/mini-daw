package app.minidaw.check.views

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import app.minidaw.check.StartTimes

/** The empty app with Views of the check V-02: one text, which shows the cold starts after the first frame. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this)
        setContentView(view)
        StartTimes.recordFirstFrame(this, KEY) { show(view) }
    }

    private fun show(view: TextView) {
        Thread {
            val results = StartTimes.results(this, listOf(KEY), StartTimes.profileState())
            runOnUiThread {
                view.text = getString(R.string.tap_to_share) + "\n" + results
                view.setOnClickListener { StartTimes.share(this, results) }
            }
        }.start()
    }

    private companion object {
        const val KEY = "the empty app with Views"
    }
}
