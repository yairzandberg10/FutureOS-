package com.future.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.sin

/**
 * גלי הקול בזמן תמלול: כל עמודה היא עוצמת הקול האמיתית ברגע מסוים
 * ([setLevel], מ-onRmsChanged), והעמודות זזות מצד לצד כמו היסטוגרמה חיה -
 * כשמדברים הן עולות לפי הקול, בשקט הן נשארות נמוכות. אין תנועה מלאכותית
 * שלא קשורה לקול.
 *
 * קל למכשיר: ציור ישיר על Canvas בלי הקצאות בכל פריים, רק כשהתצוגה גלויה
 * ופעילה ([active]), ו-postInvalidateOnAnimation מסונכרן לרענון המסך.
 * [processing] - אחרי שחרור המקש, בזמן שהתמלול מסתיים: גל איטי ונמוך.
 */
class VoiceWaveView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val bars = 11
    // היסטוריית העוצמה - [0] הדגימה הישנה ביותר, [bars-1] העדכנית.
    private val history = FloatArray(bars)
    private var target = 0f
    private var level = 0f
    private var phase = 0f
    private var sinceShift = 0f
    private var lastFrame = 0L

    var active = false
        set(value) {
            field = value
            lastFrame = 0L
            if (value) {
                history.fill(0f)
                target = 0f
                level = 0f
                postInvalidateOnAnimation()
            }
        }

    var processing = false

    var color: Int
        get() = paint.color
        set(value) { paint.color = value; invalidate() }

    /** עוצמת הקול 0..1. */
    fun setLevel(value: Float) {
        target = value.coerceIn(0f, 1f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 0.016f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrame = now
        // החלקה: עולה מהר, יורדת לאט - כמו מד עוצמה אמיתי.
        val speed = if (target > level) 18f else 7f
        level += (target - level) * (speed * dt).coerceAtMost(1f)
        // דגימה חדשה נכנסת מהקצה כל ~70ms והשאר זזות פנימה.
        sinceShift += dt
        if (sinceShift >= SHIFT_SECONDS) {
            sinceShift = 0f
            System.arraycopy(history, 1, history, 0, bars - 1)
            history[bars - 1] = level
        }
        phase += dt * 2.2f

        val w = width.toFloat()
        val h = height.toFloat()
        val barW = w / (bars * 1.8f)
        val gap = (w - barW * bars) / (bars - 1)
        for (i in 0 until bars) {
            val amp = if (processing) {
                0.18f + 0.12f * (0.5f + 0.5f * sin(phase + i * 0.6f))
            } else {
                // העמודה העדכנית עוקבת אחרי הקול באופן רציף, לא רק בהזזה.
                if (i == bars - 1) level else history[i]
            }
            val barH = (h * (0.12f + 0.88f * amp)).coerceIn(barW, h)
            val x = i * (barW + gap)
            rect.set(x, (h - barH) / 2f, x + barW, (h + barH) / 2f)
            // העמודות הישנות דוהות מעט - הכיוון של הזמן נראה לעין.
            paint.alpha = (110 + 145 * (i + 1) / bars)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
        if (active && isShown) postInvalidateOnAnimation()
    }

    private companion object {
        const val SHIFT_SECONDS = 0.07f
    }
}
