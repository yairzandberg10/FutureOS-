package com.future.assistant.data

import com.future.assistant.asr.PcmPlayback
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * ביטבוקס מסונתז מקומית - בלי קבצי שמע: "בום" (B, סוויפ סינוס נמוך עם
 * קליק שפתיים), "קה" (K, פרץ רעש עם טון), "טס" (t, רעש גבוה וקצר) ו"פף"
 * (סנר שפתיים לסיום תיבה). ארבע תיבות של 16 צעדים, בערך 10 שניות.
 */
object Beatbox {
    private const val RATE = 22050
    private const val BPM = 96

    /** ניגון חוסם - יש לקרוא מ-thread ברקע. [onLevel] לגלי הקול במסך. */
    fun play(onLevel: ((Float) -> Unit)? = null) {
        PcmPlayback.playAndWait(render(), RATE, onLevel)
    }

    private fun render(): ShortArray {
        val step = RATE * 60 / BPM / 4 // שש-עשרית
        val bars = 4
        val out = FloatArray(step * 16 * bars + RATE / 2)
        val rnd = Random(7)
        for (bar in 0 until bars) {
            val last = bar == bars - 1
            for (s in 0 until 16) {
                val at = (bar * 16 + s) * step
                when {
                    s == 0 || s == 10 || (s == 7 && bar % 2 == 1) -> kick(out, at)
                    s == 4 || (s == 12 && !last) -> snare(out, at, rnd)
                    last && s >= 12 -> if (s % 2 == 0) pf(out, at, rnd) else kick(out, at)
                }
                if (s % 2 == 0 && !(last && s >= 12)) hat(out, at, rnd, if (s % 4 == 2) 1f else 0.6f)
                if (s % 2 == 1 && bar % 2 == 1 && s > 12) hat(out, at, rnd, 0.45f)
            }
        }
        // קליפינג רך ונרמול לשיא.
        var peak = 0.01f
        for (i in out.indices) { out[i] = kotlin.math.tanh(out[i] * 1.4f); peak = maxOf(peak, kotlin.math.abs(out[i])) }
        val gain = 30000f / peak
        return ShortArray(out.size) { (out[it] * gain).toInt().coerceIn(-32768, 32767).toShort() }
    }

    /** "בום": סינוס שיורד מ-130 ל-45Hz, דעיכה של ~150ms, עם קליק שפתיים בהתחלה. */
    private fun kick(buf: FloatArray, at: Int) {
        val len = RATE * 18 / 100
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toFloat() / RATE
            val f = 45 + 85 * exp(-t * 28.0)
            phase += 2 * PI * f / RATE
            val env = exp(-t * 16.0).toFloat()
            val click = if (i < RATE / 400) 0.5f * (1 - i / (RATE / 400f)) else 0f
            add(buf, at + i, (sin(phase).toFloat() * env + click) * 1.0f)
        }
    }

    /** "קה": רעש עם טון 210Hz, דעיכה מהירה. */
    private fun snare(buf: FloatArray, at: Int, rnd: Random) {
        val len = RATE * 14 / 100
        var lp = 0f
        for (i in 0 until len) {
            val t = i.toFloat() / RATE
            val n = rnd.nextFloat() * 2 - 1
            lp += 0.35f * (n - lp) // רעש קצת עמום - נשמע כמו "קה" מהפה ולא כמו מצילה
            val tone = sin(2 * PI * 210 * t).toFloat() * exp(-t * 40.0).toFloat()
            add(buf, at + i, (lp * 0.9f + tone * 0.35f) * exp(-t * 22.0).toFloat())
        }
    }

    /** "פף": סנר שפתיים - רעש בהתקפה רכה. */
    private fun pf(buf: FloatArray, at: Int, rnd: Random) {
        val len = RATE * 12 / 100
        var lp = 0f
        for (i in 0 until len) {
            val t = i.toFloat() / RATE
            val n = rnd.nextFloat() * 2 - 1
            lp += 0.2f * (n - lp)
            val env = (1 - exp(-t * 300.0)).toFloat() * exp(-t * 26.0).toFloat()
            add(buf, at + i, lp * 1.1f * env)
        }
    }

    /** "טס": רעש גבוה (הפרש בין דגימות), 35ms. */
    private fun hat(buf: FloatArray, at: Int, rnd: Random, vol: Float) {
        val len = RATE * 35 / 1000
        var prev = 0f
        for (i in 0 until len) {
            val t = i.toFloat() / RATE
            val n = rnd.nextFloat() * 2 - 1
            val hp = n - prev
            prev = n
            add(buf, at + i, hp * 0.22f * vol * exp(-t * 90.0).toFloat())
        }
    }

    private fun add(buf: FloatArray, i: Int, v: Float) {
        if (i in buf.indices) buf[i] += v
    }
}
