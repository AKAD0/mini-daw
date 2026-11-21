package app.minidaw

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * First screen until the shell of the workspace exists (TK-031). It names the build, so the owner can see
 * which version runs (development_roadmap.md Section 2.2, rule 6). A test build adds the way to the
 * screen "Measurements".
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (PADDING_DP * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(padding, padding, padding, padding)

        val title = TextView(this)
        title.text = getString(R.string.app_name)
        title.textSize = TITLE_SP
        layout.addView(title)

        val version = BuildConfig.VERSION_NAME
        val build = TextView(this)
        build.text = getString(R.string.build_line, version, BuildConfig.VERSION_CODE, BuildConfig.CHANGE_ID)
        layout.addView(build)

        if (BuildConfig.TEST_BUILD) {
            val open = Button(this)
            open.text = getString(R.string.open_measurements)
            open.setOnClickListener {
                startActivity(Intent().setClassName(this, MEASUREMENTS))
            }
            layout.addView(open)
        }
        setContentView(layout)
    }

    private companion object {
        const val PADDING_DP = 24
        const val TITLE_SP = 24f
        const val MEASUREMENTS = "app.minidaw.measure.MeasurementsActivity"
    }
}
