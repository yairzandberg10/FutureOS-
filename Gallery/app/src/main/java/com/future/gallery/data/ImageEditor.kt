package com.future.gallery.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

enum class PhotoFilter(val label: String) {
    NONE("רגיל"),
    VIVID("חי"),
    GRAYSCALE("שחור-לבן"),
    NOIR("נואר"),
    SEPIA("ספיה"),
    VINTAGE("וינטג'"),
    FADE("דהוי"),
    COOL("קריר"),
    WARM("חמים"),
    POP("פופ"),
    INVERT("היפוך"),
}

enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("מקורי", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

/** מדבקות "אפקט" שמונחות על הפנים שבתמונה (אוזניים, משקפיים, כתר...). */
enum class StickerKind(val label: String) {
    CAT_EARS("אוזני חתול"),
    BUNNY_EARS("אוזני ארנב"),
    BEAR_EARS("אוזני דובי"),
    GLASSES("משקפיים"),
    CROWN("כתר"),
    HEARTS("לבבות"),
}

/**
 * מדבקה אחת. המיקום והגודל יחסיים (0..1) לתמונה הסופית אחרי סיבוב וחיתוך,
 * כך שהם נשמרים גם כשהתצוגה המקדימה קטנה והשמירה ברזולוציה מלאה.
 * [cx]/[cy] - מרכז הפנים (בין העיניים), [size] - המרחק בין העיניים כחלק מהרוחב.
 */
data class Sticker(val kind: StickerKind, val cx: Float, val cy: Float, val size: Float)

/** מצב עריכה מלא - כל שינוי נשמר כאן ומוחל מחדש על התמונה המקורית, כך
 * שאפשר לבטל/לשנות פרמטרים בלי לאבד איכות מעריכות קודמות. */
data class EditState(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,
    val filter: PhotoFilter = PhotoFilter.NONE,
    val cropAspect: CropAspect = CropAspect.FREE,
    // מרכז חלון החיתוך, יחסי (0..1) לתוך התמונה אחרי סיבוב/היפוך - מאפשר "פאן" עם החיצים בלי מסך מגע
    val cropCenterX: Float = 0.5f,
    val cropCenterY: Float = 0.5f,
    val cropZoom: Float = 1f,
    val stickers: List<Sticker> = emptyList(),
) {
    val hasEdits: Boolean
        get() = this != EditState()
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
        if (state.warmth != 0f) {
            val w = state.warmth * 0.3f
            cm.postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, w,
                0f, 1f, 0f, 0f, w * 0.2f,
                0f, 0f, 1f, 0f, -w,
                0f, 0f, 0f, 1f, 0f,
            )))
        }

        val filterMatrix = when (state.filter) {
            PhotoFilter.NONE -> null
            PhotoFilter.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            PhotoFilter.NOIR -> ColorMatrix().apply {
                setSaturation(0f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.5f, 0f, 0f, 0f, -60f,
                    0f, 1.5f, 0f, 0f, -60f,
                    0f, 0f, 1.5f, 0f, -60f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            PhotoFilter.VIVID -> ColorMatrix().apply {
                setSaturation(1.45f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.1f, 0f, 0f, 0f, -10f,
                    0f, 1.1f, 0f, 0f, -10f,
                    0f, 0f, 1.1f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            PhotoFilter.SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.VINTAGE -> ColorMatrix().apply {
                setSaturation(0.6f)
                postConcat(ColorMatrix(floatArrayOf(
                    0.9f, 0.1f, 0f, 0f, 22f,
                    0.05f, 0.85f, 0.05f, 0f, 12f,
                    0f, 0.1f, 0.75f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            PhotoFilter.FADE -> ColorMatrix(floatArrayOf(
                0.8f, 0f, 0f, 0f, 38f,
                0f, 0.8f, 0f, 0f, 38f,
                0f, 0f, 0.8f, 0f, 44f,
                0f, 0f, 0f, 1f, 0f,
            ))
            PhotoFilter.POP -> ColorMatrix().apply {
                setSaturation(1.8f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.25f, 0f, 0f, 0f, -30f,
                    0f, 1.25f, 0f, 0f, -30f,
                    0f, 0f, 1.25f, 0f, -30f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
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

    /** מפיק את התמונה הסופית: סיבוב/היפוך, חיתוך, צבע, וינייטה, ואז המדבקות. */
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
        if (state.vignette > 0f) drawVignette(canvas, result.width, result.height, state.vignette / 100f)
        state.stickers.forEach { drawSticker(canvas, it, result.width, result.height) }
        return result
    }

    private fun drawVignette(canvas: Canvas, w: Int, h: Int, strength: Float) {
        val radius = kotlin.math.hypot(w / 2f, h / 2f)
        val shader = RadialGradient(
            w / 2f, h / 2f, radius,
            intArrayOf(0x00000000, 0x00000000, ((strength * 215).toInt() shl 24)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { this.shader = shader })
    }

    /**
     * מאתר פנים בתמונה בעזרת android.media.FaceDetector (מובנה, עובד בלי רשת
     * ובלי ספריות) ומחזיר מדבקה לכל פנים. בלי פנים - מדבקה אחת במרכז-למעלה.
     */
    fun placeStickers(preview: Bitmap, kind: StickerKind): Pair<List<Sticker>, Boolean> {
        val faces = runCatching {
            // FaceDetector דורש RGB_565 ורוחב זוגי; מקטינים כדי שיהיה מהיר.
            val scale = (480f / maxOf(preview.width, preview.height)).coerceAtMost(1f)
            var w = (preview.width * scale).toInt().coerceAtLeast(2)
            if (w % 2 == 1) w -= 1
            val h = (preview.height * scale).toInt().coerceAtLeast(2)
            val small = Bitmap.createScaledBitmap(preview, w, h, true).copy(Bitmap.Config.RGB_565, false)
            val found = arrayOfNulls<android.media.FaceDetector.Face>(5)
            val count = android.media.FaceDetector(w, h, found.size).findFaces(small, found)
            found.take(count).filterNotNull().filter { it.confidence() >= 0.3f }.map { face ->
                val mid = android.graphics.PointF()
                face.getMidPoint(mid)
                Sticker(kind, mid.x / w, mid.y / h, face.eyesDistance() / w)
            }
        }.getOrDefault(emptyList())
        return if (faces.isNotEmpty()) faces to true
        else listOf(Sticker(kind, 0.5f, 0.42f, 0.16f)) to false
    }

    private fun drawSticker(canvas: Canvas, s: Sticker, w: Int, h: Int) {
        val cx = s.cx * w
        val cy = s.cy * h
        val d = s.size * w // המרחק בין העיניים
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        when (s.kind) {
            StickerKind.CAT_EARS -> {
                val top = cy - d * 2.6f
                val base = cy - d * 1.35f
                for (side in listOf(-1f, 1f)) {
                    val x = cx + side * d * 0.95f
                    val outer = Path().apply {
                        moveTo(x - d * 0.55f, base); lineTo(x + side * d * 0.15f, top); lineTo(x + d * 0.55f, base); close()
                    }
                    p.style = Paint.Style.FILL; p.color = 0xFF2B2B2E.toInt(); canvas.drawPath(outer, p)
                    val inner = Path().apply {
                        moveTo(x - d * 0.3f, base - d * 0.08f); lineTo(x + side * d * 0.12f, top + d * 0.4f); lineTo(x + d * 0.3f, base - d * 0.08f); close()
                    }
                    p.color = 0xFFF4A6B8.toInt(); canvas.drawPath(inner, p)
                }
            }
            StickerKind.BUNNY_EARS -> {
                for (side in listOf(-1f, 1f)) {
                    canvas.save()
                    val x = cx + side * d * 0.6f
                    val bottom = cy - d * 1.3f
                    canvas.rotate(side * 12f, x, bottom)
                    p.style = Paint.Style.FILL
                    p.color = 0xFFF7F7F7.toInt()
                    canvas.drawOval(RectF(x - d * 0.33f, bottom - d * 2.2f, x + d * 0.33f, bottom + d * 0.1f), p)
                    p.color = 0xFFF4A6B8.toInt()
                    canvas.drawOval(RectF(x - d * 0.16f, bottom - d * 1.95f, x + d * 0.16f, bottom - d * 0.15f), p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = d * 0.04f; p.color = 0x33000000
                    canvas.drawOval(RectF(x - d * 0.33f, bottom - d * 2.2f, x + d * 0.33f, bottom + d * 0.1f), p)
                    canvas.restore()
                }
            }
            StickerKind.BEAR_EARS -> {
                p.style = Paint.Style.FILL
                for (side in listOf(-1f, 1f)) {
                    val x = cx + side * d * 1.05f
                    val y = cy - d * 1.6f
                    p.color = 0xFF7A4B2A.toInt(); canvas.drawCircle(x, y, d * 0.5f, p)
                    p.color = 0xFFC99466.toInt(); canvas.drawCircle(x, y, d * 0.27f, p)
                }
            }
            StickerKind.GLASSES -> {
                p.style = Paint.Style.FILL; p.color = 0x55101418
                val r = d * 0.42f
                canvas.drawCircle(cx - d * 0.5f, cy, r, p)
                canvas.drawCircle(cx + d * 0.5f, cy, r, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = d * 0.09f; p.color = 0xFF111111.toInt()
                canvas.drawCircle(cx - d * 0.5f, cy, r, p)
                canvas.drawCircle(cx + d * 0.5f, cy, r, p)
                canvas.drawLine(cx - d * 0.1f, cy - d * 0.05f, cx + d * 0.1f, cy - d * 0.05f, p)
                canvas.drawLine(cx - d * 0.92f, cy - d * 0.1f, cx - d * 1.25f, cy - d * 0.2f, p)
                canvas.drawLine(cx + d * 0.92f, cy - d * 0.1f, cx + d * 1.25f, cy - d * 0.2f, p)
            }
            StickerKind.CROWN -> {
                val base = cy - d * 1.45f
                val top = base - d * 1.0f
                val half = d * 1.0f
                val crown = Path().apply {
                    moveTo(cx - half, base); lineTo(cx - half, top + d * 0.2f); lineTo(cx - half * 0.5f, top + d * 0.55f)
                    lineTo(cx, top); lineTo(cx + half * 0.5f, top + d * 0.55f); lineTo(cx + half, top + d * 0.2f)
                    lineTo(cx + half, base); close()
                }
                p.style = Paint.Style.FILL; p.color = 0xFFF2C230.toInt(); canvas.drawPath(crown, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = d * 0.05f; p.color = 0xFFB8860B.toInt(); canvas.drawPath(crown, p)
                p.style = Paint.Style.FILL
                p.color = 0xFFE0314B.toInt(); canvas.drawCircle(cx, base - d * 0.3f, d * 0.12f, p)
                p.color = 0xFF2F7DE1.toInt()
                canvas.drawCircle(cx - half * 0.55f, base - d * 0.25f, d * 0.09f, p)
                canvas.drawCircle(cx + half * 0.55f, base - d * 0.25f, d * 0.09f, p)
            }
            StickerKind.HEARTS -> {
                p.style = Paint.Style.FILL; p.color = 0xFFE8436A.toInt()
                val arc = listOf(-1.3f to -1.5f, -0.55f to -2.0f, 0.25f to -2.15f, 1.0f to -1.85f, 1.5f to -1.25f)
                arc.forEachIndexed { i, (dx, dy) ->
                    drawHeart(canvas, cx + dx * d, cy + dy * d, d * (if (i % 2 == 0) 0.32f else 0.24f), p)
                }
            }
        }
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, r: Float, p: Paint) {
        val path = Path().apply {
            moveTo(x, y + r * 0.9f)
            cubicTo(x - r * 1.6f, y - r * 0.2f, x - r * 0.7f, y - r * 1.4f, x, y - r * 0.45f)
            cubicTo(x + r * 0.7f, y - r * 1.4f, x + r * 1.6f, y - r * 0.2f, x, y + r * 0.9f)
            close()
        }
        canvas.drawPath(path, p)
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
