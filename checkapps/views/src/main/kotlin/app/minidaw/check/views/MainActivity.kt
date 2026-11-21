package app.minidaw.check.views

import android.app.Activity
import android.os.Bundle
import android.view.WindowInsets
import android.widget.TextView
import app.minidaw.check.StartTimes

/** The empty app with Views of the check V-02: one text, which shows the cold starts after the first frame. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this)
        // Android 15 and later draw from edge to edge: the text keeps clear of the bars and the cutout.
        view.setOnApplyWindowInsetsListener { v, insets ->
            val clear = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(clear.left, clear.top, clear.right, clear.bottom)
            insets
        }
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
