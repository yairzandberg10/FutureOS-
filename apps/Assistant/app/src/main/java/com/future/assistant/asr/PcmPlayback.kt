package com.future.assistant.asr

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/** משמיע PCM16 מונו גולמי דרך AudioTrack, וחוסם עד סוף ההשמעה. */
object PcmPlayback {
    /** [onLevel] - עוצמה 0..1 של החלון שמושמע עכשיו, כל ~40ms (גלי הקול במסך). */
    fun playAndWait(samples: ShortArray, rate: Int, onLevel: ((Float) -> Unit)? = null) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            maxOf(minBufferSize, samples.size * 2),
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        try {
            // ב-MODE_STATIC, מיד אחרי הבנייה המצב הוא STATE_NO_STATIC_DATA (לא
            // STATE_INITIALIZED) - המעבר ל-INITIALIZED קורה רק אחרי ה-write()
            // הראשון. לכן בודקים רק שהבנייה עצמה לא נכשלה לגמרי.
            if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED) return
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            val durationMs = (samples.size.toLong() * 1000L) / rate
            if (onLevel == null) {
                Thread.sleep(durationMs + 100)
            } else {
                val window = rate / 25
                val end = System.currentTimeMillis() + durationMs + 100
                while (System.currentTimeMillis() < end) {
                    val head = audioTrack.playbackHeadPosition.coerceIn(0, samples.size)
                    var sum = 0.0
                    val to = (head + window).coerceAtMost(samples.size)
                    for (i in head until to) { val v = samples[i] / 32768.0; sum += v * v }
                    val rms = if (to > head) kotlin.math.sqrt(sum / (to - head)).toFloat() else 0f
                    onLevel((rms * 3f).coerceIn(0f, 1f))
                    Thread.sleep(40)
                }
                onLevel(0f)
            }
            audioTrack.stop()
        } finally {
            audioTrack.release()
        }
    }
}
