package com.future.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/**
 * גלי הקול בזמן תמלול: עמודות מעוגלות שנעות בגל רציף. בשקט הן "נושמות"
 * בעדינות, וכשמדברים הגובה עולה לפי עוצמת הקול ([setLevel]). הרמה מוחלקת
 * כדי שהתנועה תהיה רכה ולא קופצנית.
 *
 * קל למכשיר: ציור ישיר על Canvas בלי הקצאות בכל פריים, רק כשהתצוגה גלויה
 * ופעילה ([active]), ו-postInvalidateOnAnimation מסונכרן לרענון המסך.
 * [processing] - אחרי שחרור המקש, בזמן שהתמלול מסתיים: גל איטי ונמוך.
 */
class VoiceWaveView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val bars = 11
    private var target = 0f
    private var level = 0f
    private var phase = 0f
    private var lastFrame = 0L

    var active = false
        set(value) {
            field = value
            lastFrame = 0L
            if (value) postInvalidateOnAnimation()
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
        val speed = if (target > level) 14f else 5f
        level += (target - level) * (speed * dt).coerceAtMost(1f)
        phase += dt * (if (processing) 2.2f else 5.5f)

        val w = width.toFloat()
        val h = height.toFloat()
        val barW = w / (bars * 1.8f)
        val gap = (w - barW * bars) / (bars - 1)
        val amp = if (processing) 0.22f else 0.18f + 0.82f * level
        for (i in 0 until bars) {
            // מעטפת: העמודות האמצעיות גבוהות יותר.
            val envelope = sin(PI * (i + 0.5) / bars).toFloat()
            val wave = 0.55f + 0.45f * sin(phase + i * 0.7f)
            val barH = (h * (0.14f + 0.86f * amp * envelope * wave)).coerceIn(barW, h)
            val x = i * (barW + gap)
            rect.set(x, (h - barH) / 2f, x + barW, (h + barH) / 2f)
            paint.alpha = (150 + 105 * envelope).toInt()
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
        if (active && isShown) postInvalidateOnAnimation()
    }
}
