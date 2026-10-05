package com.future.camera.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * שמירת תמונה שצולמה ל-MediaStore (DCIM/Camera) - ללא צורך בהרשאת
 * אחסון מפורשת כי אנחנו רק כותבים לפריט שאנחנו עצמנו יצרנו (Scoped Storage,
 * minSdk 31 תמיד תחת המדיניות הזו).
 */
object PhotoStorage {
    // עיבוד התמונה (פענוח, סיבוב, פילטר, JPEG) - מחוץ ל-thread הראשי.
    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor()

    fun capture(
        context: Context,
        imageCapture: ImageCapture,
        onSaved: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val name = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, CAMERA_DIR)
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let(onSaved)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    /**
     * צילום לזיכרון, עיבוד (סיבוב לפי החיישן, חיתוך ריבועי, פילטר) ושמירה
     * כ-JPEG. איטי מעט מהשמירה הישירה, ולכן משמש רק כשיש מה לעבד.
     */
    fun captureProcessed(
        context: Context,
        imageCapture: ImageCapture,
        square: Boolean,
        matrix: android.graphics.ColorMatrix?,
        onSaved: (Uri) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val main = ContextCompat.getMainExecutor(context)
        imageCapture.takePicture(worker, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                try {
                    val rotation = image.imageInfo.rotationDegrees
                    var bmp = image.toBitmap()
                    image.close()
                    if (rotation != 0) {
                        val m = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                        bmp = android.graphics.Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                    }
                    if (square) {
                        val side = minOf(bmp.width, bmp.height)
                        bmp = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
                    }
                    if (matrix != null) bmp = com.future.camera.data.CameraFilter.apply(bmp, matrix)
                    val uri = saveJpeg(context, bmp)
                    main.execute { if (uri != null) onSaved(uri) else onError(IllegalStateException("save failed")) }
                } catch (e: Exception) {
                    runCatching { image.close() }
                    main.execute { onError(e) }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                main.execute { onError(exception) }
            }
        })
    }

    private fun saveJpeg(context: Context, bmp: android.graphics.Bitmap): Uri? {
        val name = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, CAMERA_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }
}
