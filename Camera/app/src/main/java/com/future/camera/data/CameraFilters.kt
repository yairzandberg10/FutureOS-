package com.future.camera.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * פילטרים כמטריצת צבע: אותה מטריצה מוחלת על התצוגה החיה (Paint של שכבת
 * ה-PreviewView) ועל התמונה שנשמרת, כך שמה שרואים הוא מה שמקבלים.
 */
enum class CameraFilter(val label: String) {
    NONE("ללא"),
    VIVID("חי"),
    WARM("חם"),
    COOL("קר"),
    FADE("דהוי"),
    SEPIA("ספיה"),
    MONO("שחור לבן"),
    NOIR("נואר");

    fun matrix(): ColorMatrix? = when (this) {
        NONE -> null
        VIVID -> ColorMatrix().apply { setSaturation(1.45f) }.also { it.postConcat(contrast(1.1f)) }
        WARM -> ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, 12f,
            0f, 1.02f, 0f, 0f, 4f,
            0f, 0f, 0.88f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f,
        ))
        COOL -> ColorMatrix(floatArrayOf(
            0.9f, 0f, 0f, 0f, -6f,
            0f, 1.0f, 0f, 0f, 2f,
            0f, 0f, 1.12f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f,
        ))
        FADE -> ColorMatrix().apply { setSaturation(0.7f) }.also {
            it.postConcat(ColorMatrix(floatArrayOf(
                0.85f, 0f, 0f, 0f, 30f,
                0f, 0.85f, 0f, 0f, 30f,
                0f, 0f, 0.85f, 0f, 34f,
                0f, 0f, 0f, 1f, 0f,
            )))
        }
        SEPIA -> ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ))
        MONO -> ColorMatrix().apply { setSaturation(0f) }
        NOIR -> ColorMatrix().apply { setSaturation(0f) }.also { it.postConcat(contrast(1.45f)) }
    }

    fun paint(): Paint? = matrix()?.let { Paint().apply { colorFilter = ColorMatrixColorFilter(it) } }

    companion object {
        /** מסמך: שחור-לבן בניגודיות גבוהה, כדי שטקסט על נייר ייקרא. */
        val DOCUMENT: ColorMatrix get() = ColorMatrix().apply { setSaturation(0f) }.also { it.postConcat(contrast(1.8f)) }

        fun contrast(c: Float): ColorMatrix {
            val t = 128f * (1f - c)
            return ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ))
        }

        fun apply(src: Bitmap, matrix: ColorMatrix): Bitmap {
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
            return out
        }
    }
}
