package app.minidaw.measure

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10

/** Item 9 of requirements.md Section 3.2: the level of noise of the microphone in a quiet room. */
internal object NoiseLevel {
    private const val RATE = 48_000
    private const val SKIP_SECONDS = 0.5
    private const val SECONDS = 2.0
    private const val BUFFER_SECONDS = 4
    private const val DECIBELS = 10.0
    private const val SILENCE_DB = -140.0

    /** Records 2.5 s and gives the level of the last 2 s in dBFS. It needs the permission RECORD_AUDIO. */
    fun measure(unprocessed: Boolean): Double {
        val source = if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_FLOAT
        val size = maxOf(AudioRecord.getMinBufferSize(RATE, channel, encoding), RATE * BUFFER_SECONDS)
        val record = AudioRecord(source, RATE, channel, encoding, size)
        val total = (RATE * (SKIP_SECONDS + SECONDS)).toInt()
        val samples = FloatArray(total)
        record.startRecording()
        var read = 0
        var reading = true
        while (reading && read < total) {
            val count = record.read(samples, read, total - read, AudioRecord.READ_BLOCKING)
            reading = count > 0
            read += maxOf(count, 0)
        }
        record.stop()
        record.release()
        val first = (RATE * SKIP_SECONDS).toInt()
        var sum = 0.0
        for (i in first until read) {
            sum += samples[i] * samples[i]
        }
        val count = read - first
        return if (count > 0 && sum > 0.0) DECIBELS * log10(sum / count) else SILENCE_DB
    }
}
