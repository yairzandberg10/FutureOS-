package com.future.camera.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.text.SimpleDateFormat
import java.util.Locale

/** מקליט וידאו ל-MediaStore (Movies/Camera) - אותה תבנית בדיוק כמו PhotoStorage
 * (Scoped Storage, בלי הרשאת אחסון מפורשת כי אנחנו כותבים לפריט שיצרנו בעצמנו). */
object VideoStorage {
    fun startRecording(
        context: Context,
        recorder: Recorder,
        onFinished: (Uri?) -> Unit,
        onError: () -> Unit,
    ): Recording? {
        val name = "VID_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Camera")
        }
        val outputOptions = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        val hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PermissionChecker.PERMISSION_GRANTED

        return try {
            var pendingRecording = recorder.prepareRecording(context, outputOptions)
            if (hasAudioPermission) pendingRecording = pendingRecording.withAudioEnabled()
            pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (event.hasError()) onError() else onFinished(event.outputResults.outputUri)
                }
            }
        } catch (e: Exception) {
            onError()
            null
        }
    }
}
