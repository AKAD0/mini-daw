package app.minidaw.check.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.minidaw.check.StartTimes

/** The empty app with Compose of the check V-02: one text, which shows the cold starts after the first frame. */
class MainActivity : ComponentActivity() {

    private var results by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BasicText(results, Modifier.clickable { StartTimes.share(this, results) })
        }
        StartTimes.recordFirstFrame(this, KEY) { load() }
    }

    private fun load() {
        Thread {
            val text = StartTimes.results(this, listOf(KEY), StartTimes.profileState())
            runOnUiThread { results = getString(R.string.tap_to_share) + "\n" + text }
        }.start()
    }

    private companion object {
        const val KEY = "the empty app with Compose"
    }
}
