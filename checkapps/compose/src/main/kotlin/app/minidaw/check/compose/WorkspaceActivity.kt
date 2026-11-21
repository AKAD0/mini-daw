package app.minidaw.check.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.minidaw.check.StartTimes

/**
 * The prototype of the workspace for the check V-02 (technology_stack.md Section 6.1, rule 2): a global bar
 * and two panels drawn on canvases, like the Playlist and the Piano roll. Its cold start must meet
 * NFR-PERF-01 (AC-APP-01: median at most 800 ms, 9 of 10 at most 1,200 ms).
 */
class WorkspaceActivity : ComponentActivity() {

    private var results by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Workspace(results) { StartTimes.share(this, results) }
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
        const val KEY = "the prototype of the workspace with Compose"
    }
}

private const val BACKGROUND = 0xFF15171C
private const val BAR = 0xFF22252D
private const val LANE_A = 0xFF1B1E24
private const val LANE_B = 0xFF1F2229
private const val CLIP = 0xFF3D7BD9
private const val NOTE = 0xFFE0A030
private const val WHITE_KEY = 0xFFD8D8D8
private const val BLACK_KEY = 0xFF30333A
private const val TEXT = 0xFFE8E8E8
private const val BAR_DP = 48
private const val GAP_DP = 12
private const val TRACKS = 8
private const val CLIPS = 6
private const val KEYS = 24
private const val KEY_WIDTH = 0.08f
private const val NOTES = 32
private const val FILL = 0.8f
private const val CLIP_INSET = 2f
private const val NOTE_STEP = 5
private val LABELS = listOf("Play", "Stop", "Rec", "120 BPM", "4/4", "Song", "Mixer")

// The keys of one octave from C: 1 marks a black key.
private const val OCTAVE_KEYS = "010100101010"

@Composable
private fun Workspace(results: String, onShare: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(BACKGROUND))) {
        GlobalBar(onShare)
        Row(Modifier.weight(1f)) {
            Playlist(Modifier.weight(1f).fillMaxHeight())
            PianoRoll(Modifier.weight(1f).fillMaxHeight())
        }
        if (results.isNotEmpty()) {
            BasicText(results, style = TextStyle(color = Color(TEXT)))
        }
    }
}

@Composable
private fun GlobalBar(onShare: () -> Unit) {
    val style = TextStyle(color = Color(TEXT))
    val bar = Modifier.fillMaxWidth().height(BAR_DP.dp).background(Color(BAR))
    Row(bar, verticalAlignment = Alignment.CenterVertically) {
        for (label in LABELS) {
            BasicText(label, Modifier.padding(horizontal = GAP_DP.dp), style = style)
        }
        BasicText("Share results", Modifier.clickable(onClick = onShare).padding(horizontal = GAP_DP.dp), style = style)
    }
}

@Composable
private fun Playlist(modifier: Modifier) {
    Canvas(modifier) {
        val lane = size.height / TRACKS
        val step = size.width / CLIPS
        for (track in 0 until TRACKS) {
            val top = track * lane
            drawRect(Color(if (track % 2 == 0) LANE_A else LANE_B), Offset(0f, top), Size(size.width, lane))
            for (clip in 0 until CLIPS) {
                if ((clip + track) % 2 == 0) {
                    val origin = Offset(clip * step, top + CLIP_INSET)
                    drawRect(Color(CLIP), origin, Size(step * FILL, lane - 2 * CLIP_INSET))
                }
            }
        }
    }
}

@Composable
private fun PianoRoll(modifier: Modifier) {
    Canvas(modifier) {
        val row = size.height / KEYS
        val keyWidth = size.width * KEY_WIDTH
        for (key in 0 until KEYS) {
            val black = OCTAVE_KEYS[(KEYS - 1 - key) % OCTAVE_KEYS.length] == '1'
            drawRect(Color(if (black) BLACK_KEY else WHITE_KEY), Offset(0f, key * row), Size(keyWidth, row - 1f))
        }
        val step = (size.width - keyWidth) / NOTES
        for (note in 0 until NOTES) {
            val key = (note * NOTE_STEP) % KEYS
            drawRect(Color(NOTE), Offset(keyWidth + note * step, key * row), Size(step * FILL, row - 1f))
        }
    }
}
