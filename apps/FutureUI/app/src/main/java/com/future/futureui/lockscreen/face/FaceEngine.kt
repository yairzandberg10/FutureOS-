package com.future.futureui.lockscreen.face

import android.graphics.Bitmap
import android.graphics.PointF
import android.media.FaceDetector
import kotlin.math.max
import kotlin.math.min

/**
 * זיהוי פנים על המכשיר בלבד, בלי מודלים שמורידים מבחוץ:
 *
 * 1. איתור - android.media.FaceDetector (מובנה באנדרואיד מאז API 1) מוצא את
 *    נקודת האמצע בין העיניים ואת המרחק ביניהן.
 * 2. יישור - לפי העיניים חותכים ריבוע קבוע סביב הפנים ומקטינים ל-64x64,
 *    ואז איזון היסטוגרמה כדי שתאורה שונה לא תשנה את התוצאה.
 * 3. מאפיינים - LBP אחיד (Local Binary Patterns): לכל פיקסל, אילו מ-8 שכניו
 *    בהירים ממנו. סופרים את הדפוסים בכל אחד מ-64 תאים (8x8) - וקטור של 3776
 *    מספרים שמתאר את מרקם הפנים (עיניים, גבות, אף, פה) ולא את הבהירות.
 * 4. השוואה - מרחק Chi-square בין וקטורים; ככל שקטן יותר, דומה יותר.
 *
 * זו רמת האבטחה של "זיהוי פנים בסיסי" באנדרואיד (לא Face ID עם חיישן עומק):
 * נוח ומהיר, אבל תמונה טובה של בעל המכשיר עלולה לעבוד. לכן הקוד תמיד נדרש
 * אחרי הפעלה מחדש, 48 שעות, או 5 כישלונות (ר' LockSettings.strongAuthRequired).
 */
object FaceEngine {

    const val SIZE = 64
    private const val GRID = 8
    private const val CELL = SIZE / GRID
    const val BINS = 59
    const val DIM = GRID * GRID * BINS

    private const val DETECT_WIDTH = 240
    private const val MIN_CONFIDENCE = 0.4f

    /** פנים שנמצאו בפריים, בקואורדינטות של הפריים המלא. */
    class Face(val midX: Float, val midY: Float, val eyeDistance: Float, val frameWidth: Int)

    enum class Problem { NONE, NO_FACE, TOO_FAR, TOO_DARK, EDGE }

    class Result(val problem: Problem, val features: FloatArray? = null, val face: Face? = null)

    /** מפת אינדקס של כל 256 הקודים ל-59 סלים: 58 דפוסים "אחידים" + סל אחד לכל השאר. */
    private val uniformMap: IntArray = IntArray(256).also { map ->
        var next = 0
        for (code in 0 until 256) {
            var transitions = 0
            for (b in 0 until 8) {
                val a = (code shr b) and 1
                val c = (code shr ((b + 1) % 8)) and 1
                if (a != c) transitions++
            }
            map[code] = if (transitions <= 2) next++ else BINS - 1
        }
    }

    fun analyze(frame: GrayFrame): Result {
        val mean = meanBrightness(frame)
        if (mean < 28) return Result(Problem.TOO_DARK)
        val face = detect(frame) ?: return Result(Problem.NO_FACE)
        if (face.eyeDistance < frame.width * 0.085f) return Result(Problem.TOO_FAR, face = face)
        val patch = align(frame, face) ?: return Result(Problem.EDGE, face = face)
        equalize(patch)
        return Result(Problem.NONE, lbpHistogram(patch), face)
    }

    private fun meanBrightness(f: GrayFrame): Int {
        var sum = 0L
        var n = 0
        var i = 0
        while (i < f.pixels.size) { sum += f.pixels[i].toInt() and 0xFF; n++; i += 37 }
        return if (n == 0) 0 else (sum / n).toInt()
    }

    private fun detect(f: GrayFrame): Face? {
        // FaceDetector דורש RGB_565 ברוחב זוגי. מקטינים לרוחב ~240 - מהיר פי כמה ועדיין מדויק.
        val scale = DETECT_WIDTH.toFloat() / f.width
        val w = DETECT_WIDTH
        val h = ((f.height * scale).toInt() / 2) * 2
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val sy = min(f.height - 1, (y / scale).toInt())
            for (x in 0 until w) {
                val sx = min(f.width - 1, (x / scale).toInt())
                val g = f[sx, sy]
                px[y * w + x] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
        }
        val bmp = Bitmap.createBitmap(px, w, h, Bitmap.Config.RGB_565)
        return try {
            val found = arrayOfNulls<FaceDetector.Face>(1)
            val n = FaceDetector(w, h, 1).findFaces(bmp, found)
            val face = found[0]
            if (n < 1 || face == null || face.confidence() < MIN_CONFIDENCE) return null
            val mid = PointF()
            face.getMidPoint(mid)
            Face(mid.x / scale, mid.y / scale, face.eyesDistance() / scale, f.width)
        } catch (e: Exception) {
            null
        } finally {
            bmp.recycle()
        }
    }

    /** ריבוע של 2.2 מרחקי-עיניים, ממורכז קצת מתחת לעיניים (שיכלול את הפה). */
    private fun align(f: GrayFrame, face: Face): IntArray? {
        val side = face.eyeDistance * 2.2f
        val cx = face.midX
        val cy = face.midY + face.eyeDistance * 0.35f
        val left = cx - side / 2
        val top = cy - side / 2
        val outside = max(max(-left, -top), max(left + side - f.width, top + side - f.height))
        if (outside > side * 0.12f) return null
        val out = IntArray(SIZE * SIZE)
        val step = side / SIZE
        for (y in 0 until SIZE) {
            val fy = (top + (y + 0.5f) * step).coerceIn(0f, f.height - 1.001f)
            val y0 = fy.toInt()
            val dy = fy - y0
            for (x in 0 until SIZE) {
                val fx = (left + (x + 0.5f) * step).coerceIn(0f, f.width - 1.001f)
                val x0 = fx.toInt()
                val dx = fx - x0
                val a = f[x0, y0]; val b = f[x0 + 1, y0]
                val c = f[x0, y0 + 1]; val d = f[x0 + 1, y0 + 1]
                val top2 = a + (b - a) * dx
                val bottom2 = c + (d - c) * dx
                out[y * SIZE + x] = (top2 + (bottom2 - top2) * dy).toInt().coerceIn(0, 255)
            }
        }
        return out
    }

    private fun equalize(p: IntArray) {
        val hist = IntArray(256)
        for (v in p) hist[v]++
        var cum = 0
        val first = hist.indexOfFirst { it > 0 }
        val cdfMin = if (first >= 0) hist[first] else 0
        val lut = IntArray(256)
        val denom = max(1, p.size - cdfMin)
        for (i in 0 until 256) {
            cum += hist[i]
            lut[i] = (((cum - cdfMin).coerceAtLeast(0)) * 255L / denom).toInt()
        }
        for (i in p.indices) p[i] = lut[p[i]]
    }

    private fun lbpHistogram(p: IntArray): FloatArray {
        val out = FloatArray(DIM)
        val counts = IntArray(GRID * GRID)
        for (y in 1 until SIZE - 1) {
            for (x in 1 until SIZE - 1) {
                val c = p[y * SIZE + x]
                var code = 0
                if (p[(y - 1) * SIZE + x - 1] >= c) code = code or 1
                if (p[(y - 1) * SIZE + x] >= c) code = code or 2
                if (p[(y - 1) * SIZE + x + 1] >= c) code = code or 4
                if (p[y * SIZE + x + 1] >= c) code = code or 8
                if (p[(y + 1) * SIZE + x + 1] >= c) code = code or 16
                if (p[(y + 1) * SIZE + x] >= c) code = code or 32
                if (p[(y + 1) * SIZE + x - 1] >= c) code = code or 64
                if (p[y * SIZE + x - 1] >= c) code = code or 128
                val cell = (y / CELL) * GRID + (x / CELL)
                out[cell * BINS + uniformMap[code]] += 1f
                counts[cell]++
            }
        }
        for (cell in 0 until GRID * GRID) {
            val n = counts[cell]
            if (n == 0) continue
            val base = cell * BINS
            for (b in 0 until BINS) out[base + b] /= n
        }
        return out
    }

    /** מרחק Chi-square, מנורמל למספר התאים: 0 = זהה, 2 = שונה לגמרי. */
    fun distance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in 0 until DIM) {
            val s = a[i] + b[i]
            if (s > 0f) {
                val d = a[i] - b[i]
                sum += d * d / s
            }
        }
        return sum / (GRID * GRID)
    }

    fun bestDistance(probe: FloatArray, templates: List<FloatArray>): Float =
        templates.minOfOrNull { distance(probe, it) } ?: Float.MAX_VALUE

    /**
     * כמה רחוקות דגימות הרישום זו מזו (לכל דגימה - הקרובה אליה ביותר מבין
     * האחרות, ממוצע). זה "קנה המידה" של הפנים של המשתמש הזה, במצלמה הזו.
     */
    fun calibrate(samples: List<FloatArray>): Float {
        if (samples.size < 2) return 0.3f
        val nn = samples.indices.map { i ->
            samples.indices.filter { it != i }.minOf { distance(samples[i], samples[it]) }
        }
        return nn.average().toFloat()
    }

    /** מקדם הסף לפי הרגישות שנבחרה (0 מחמירה, 1 רגילה, 2 מקלה). */
    fun thresholdFactor(sensitivity: Int): Float = when (sensitivity) {
        0 -> 1.35f
        2 -> 1.9f
        else -> 1.6f
    }

    /** תמונת אפור קטנה ומשוקפת של הפריים, לתצוגה בזמן הרישום בלבד. */
    fun previewBitmap(f: GrayFrame, width: Int = 160): Bitmap {
        val scale = width.toFloat() / f.width
        val h = (f.height * scale).toInt()
        val px = IntArray(width * h)
        for (y in 0 until h) {
            val sy = min(f.height - 1, (y / scale).toInt())
            for (x in 0 until width) {
                val sx = min(f.width - 1, ((width - 1 - x) / scale).toInt())
                val g = f[sx, sy]
                px[y * width + x] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
        }
        return Bitmap.createBitmap(px, width, h, Bitmap.Config.ARGB_8888)
    }
}
