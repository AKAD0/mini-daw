package app.minidaw.measure

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.ceil

/**
 * The picture of the check V-05: the Notes of both candidates in two lanes, time to the right and
 * pitch upward. A line marks each C. The owner compares the Notes with the melody that was hummed.
 */
internal class NotesView(context: Context) : View(context) {
    /** The Notes of a candidate as triples of start, end and pitch. */
    class Lane(val name: String, val notes: DoubleArray, val color: Int)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lanes: List<Lane> = emptyList()
    private var seconds = 1.0

    fun show(lanes: List<Lane>, seconds: Double) {
        this.lanes = lanes
        this.seconds = maxOf(seconds, 1.0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(BACKGROUND)
        if (lanes.isEmpty()) {
            return
        }
        val pitches = lanes.flatMap { lane -> lane.notes.indices.filter { it % FIELDS == 2 }.map { lane.notes[it] } }
        val low = (pitches.minOrNull() ?: DEFAULT_LOW) - MARGIN
        val high = (pitches.maxOrNull() ?: DEFAULT_HIGH) + MARGIN
        val laneHeight = height.toFloat() / lanes.size
        for ((index, lane) in lanes.withIndex()) {
            val top = index * laneHeight
            val y = { pitch: Double -> (top + laneHeight * (1.0 - (pitch - low) / (high - low))).toFloat() }
            paint.color = GRID
            var c = ceil(low / OCTAVE) * OCTAVE
            while (c <= high) {
                canvas.drawLine(0f, y(c), width.toFloat(), y(c), paint)
                c += OCTAVE
            }
            paint.color = lane.color
            val rowHeight = maxOf(laneHeight / (high - low).toFloat(), MIN_ROW)
            for (n in 0 until lane.notes.size / FIELDS) {
                val left = (lane.notes[FIELDS * n] / seconds * width).toFloat()
                val right = (lane.notes[FIELDS * n + 1] / seconds * width).toFloat()
                val centre = y(lane.notes[FIELDS * n + 2])
                val half = rowHeight / 2
                canvas.drawRect(left, centre - half, maxOf(right, left + MIN_WIDTH), centre + half, paint)
            }
            paint.color = TEXT
            paint.textSize = LABEL_SIZE
            canvas.drawText(lane.name, LABEL_INSET, top + LABEL_SIZE + LABEL_INSET, paint)
        }
    }

    private companion object {
        const val BACKGROUND = 0xFF15171C.toInt()
        const val GRID = 0xFF33363F.toInt()
        const val TEXT = Color.WHITE
        const val DEFAULT_LOW = 48.0
        const val DEFAULT_HIGH = 72.0
        const val MARGIN = 2.0
        const val OCTAVE = 12.0
        const val MIN_ROW = 4f
        const val MIN_WIDTH = 2f
        const val FIELDS = 3
        const val LABEL_SIZE = 32f
        const val LABEL_INSET = 8f
    }
}
