package com.future.sharednav.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable

/**
 * צבע ההדגשה של ווידג'ט בסגנון iOS: הכותרת של הווידג'ט נצבעת בגוון של
 * האפליקציה (כמו האדום של היומן או הכתום של השעון ב-iOS). הגוון נגזר מהאייקון
 * עצמו - ממוצע הפיקסלים הרוויים - ומובהר כך שיהיה קריא על המשטח הכהה #1C1C1E.
 * אייקון בלי צבע (אפור/שחור-לבן) מקבל את הכחול של המערכת.
 */
object WidgetTint {
    /** הכחול של iOS במצב כהה - לאייקון בלי גוון. */
    const val DEFAULT: Int = 0xFF0A84FF.toInt()

    fun fromDrawable(drawable: Drawable?): Int {
        if (drawable == null) return DEFAULT
        return runCatching {
            val bmp = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, 24, 24)
            drawable.draw(canvas)
            fromBitmap(bmp).also { bmp.recycle() }
        }.getOrDefault(DEFAULT)
    }

    fun fromBitmap(bitmap: Bitmap): Int {
        val small = if (bitmap.width > 32 || bitmap.height > 32) Bitmap.createScaledBitmap(bitmap, 24, 24, true) else bitmap
        val hsv = FloatArray(3)
        var r = 0.0; var g = 0.0; var b = 0.0; var weight = 0.0
        for (y in 0 until small.height) for (x in 0 until small.width) {
            val p = small.getPixel(x, y)
            if (Color.alpha(p) < 200) continue
            Color.colorToHSV(p, hsv)
            if (hsv[1] < 0.3f || hsv[2] < 0.3f) continue
            val w = (hsv[1] * hsv[2]).toDouble()
            r += Color.red(p) * w; g += Color.green(p) * w; b += Color.blue(p) * w; weight += w
        }
        if (small !== bitmap) small.recycle()
        if (weight <= 0.0) return DEFAULT
        Color.colorToHSV(Color.rgb((r / weight).toInt(), (g / weight).toInt(), (b / weight).toInt()), hsv)
        hsv[1] = hsv[1].coerceIn(0.55f, 0.85f)
        hsv[2] = hsv[2].coerceAtLeast(0.92f)
        return Color.HSVToColor(hsv)
    }
}
