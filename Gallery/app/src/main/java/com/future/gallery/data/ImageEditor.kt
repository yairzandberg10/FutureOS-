package com.future.gallery.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

enum class PhotoFilter(val label: String) {
    NONE("רגיל"),
    GRAYSCALE("שחור-לבן"),
    SEPIA("ספיה"),
    INVERT("היפוך"),
    COOL("קריר"),
    WARM("חמים")
}

enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("מקורי", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

/** מצב עריכה מלא - כל שינוי נשמר כאן ומוחל מחדש על התמונה המקורית, כך
 * שאפשר לבטל/לשנות פרמטרים בלי לאבד איכות מעריכות קודמות. */
data class EditState(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val filter: PhotoFilter = PhotoFilter.NONE,
    val cropAspect: CropAspect = CropAspect.FREE,
    // מרכז חלון החיתוך, יחסי (0..1) לתוך התמונה אחרי סיבוב/היפוך - מאפשר "פאן" עם החיצים בלי מסך מגע
    val cropCenterX: Float = 0.5f,
    val cropCenterY: Float = 0.5f,
    val cropZoom: Float = 1f
) {
    val hasEdits: Boolean
        get() = rotationDegrees != 0 || flipHorizontal || flipVertical || brightness != 0f ||
            contrast != 0f || saturation != 0f || filter != PhotoFilter.NONE || cropAspect != CropAspect.FREE || cropZoom != 1f
}

object ImageEditor {

    private fun buildColorMatrix(state: EditState): ColorMatrix {
        val cm = ColorMatrix()
        cm.postConcat(ColorMatrix().apply { setSaturation((1f + state.saturation / 100f).coerceAtLeast(0f)) })

        val contrastScale = 1f + state.contrast / 100f
        val translate = (1f - contrastScale) * 128f + state.brightness * 1.28f
        cm.postConcat(
            ColorMatrix(
                floatArrayOf(
                    contrastScale, 0f, 0f, 0f, translate,
                    0f, contrastScale, 0f, 0f, translate,
                    0f, 0f, contrastScale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        val filterMatrix = when (state.filter) {
            PhotoFilter.NONE -> null
            PhotoFilter.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            PhotoFilter.SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.INVERT -> ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.COOL -> ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, -6f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.WARM -> ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 14f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        if (filterMatrix != null) cm.postConcat(filterMatrix)
        return cm
    }

    /** מסובב/הופך את התמונה בלבד (בלי חיתוך) - משמש לתצוגה מקדימה של גיאומטריה. */
    fun applyRotationAndFlip(source: Bitmap, state: EditState): Bitmap {
        val matrix = Matrix()
        if (state.rotationDegrees != 0) matrix.postRotate(state.rotationDegrees.toFloat())
        val sx = if (state.flipHorizontal) -1f else 1f
        val sy = if (state.flipVertical) -1f else 1f
        if (sx != 1f || sy != 1f) matrix.postScale(sx, sy)
        return if (matrix.isIdentity) source else Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropRect(rotated: Bitmap, state: EditState): RectF {
        val aspect = state.cropAspect.ratio
        val fullW = rotated.width.toFloat()
        val fullH = rotated.height.toFloat()

        var cropW: Float
        var cropH: Float
        if (aspect == null) {
            cropW = fullW
            cropH = fullH
        } else if (fullW / fullH > aspect) {
            cropH = fullH
            cropW = fullH * aspect
        } else {
            cropW = fullW
            cropH = fullW / aspect
        }
        cropW /= state.cropZoom
        cropH /= state.cropZoom

        val centerX = state.cropCenterX * fullW
        val centerY = state.cropCenterY * fullH
        var left = centerX - cropW / 2f
        var top = centerY - cropH / 2f
        left = left.coerceIn(0f, (fullW - cropW).coerceAtLeast(0f))
        top = top.coerceIn(0f, (fullH - cropH).coerceAtLeast(0f))
        return RectF(left, top, (left + cropW).coerceAtMost(fullW), (top + cropH).coerceAtMost(fullH))
    }

    /** מפיק את התמונה הסופית: סיבוב/היפוך, חיתוך, ואז מסננים/בהירות/ניגודיות/רוויה. */
    fun renderFinal(source: Bitmap, state: EditState): Bitmap {
        val rotated = applyRotationAndFlip(source, state)
        val rect = cropRect(rotated, state)
        val cropped = if (rect.width() < rotated.width - 0.5f || rect.height() < rotated.height - 0.5f) {
            Bitmap.createBitmap(
                rotated,
                rect.left.toInt(),
                rect.top.toInt(),
                rect.width().toInt().coerceAtLeast(1),
                rect.height().toInt().coerceAtLeast(1)
            )
        } else rotated

        val result = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(buildColorMatrix(state)) }
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        return result
    }

    /** שומר את התוצאה כתמונה חדשה ב-MediaStore, בלי לגעת במקור. */
    fun saveAsNewImage(context: Context, bitmap: Bitmap, baseName: String): Uri? {
        val displayName = "${baseName}_edited_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (Build.VERSION.SDK_INT >= 29) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                context.contentResolver.update(uri, done, null, null)
            }
            uri
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            null
        }
    }
}
