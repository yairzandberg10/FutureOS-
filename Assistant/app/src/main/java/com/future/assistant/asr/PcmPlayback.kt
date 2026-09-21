package com.future.assistant.asr

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/** משמיע PCM16 מונו גולמי דרך AudioTrack, וחוסם עד סוף ההשמעה. */
object PcmPlayback {
    fun playAndWait(samples: ShortArray, rate: Int) {
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
            Thread.sleep(durationMs + 100)
            audioTrack.stop()
        } finally {
            audioTrack.release()
        }
    }
}
