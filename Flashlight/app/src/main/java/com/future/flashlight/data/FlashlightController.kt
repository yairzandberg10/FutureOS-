package com.future.flashlight.data

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * שליטה בפנס דרך Camera2 (setTorchMode) - בלי לפתוח את המצלמה ובלי הרשאת
 * CAMERA. המצב האמיתי מגיע מ-TorchCallback, כך שהמסך תמיד מראה אם הפנס דלוק
 * (גם כשהודלק/כובה ממרכז הבקרה), ומדווח כשהפנס לא זמין כי המצלמה בשימוש.
 */
class FlashlightController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // מזהה המצלמה נמצא פעם אחת לכל התהליך (FL-10), לא בכל מופע
    private val cameraId: String? = cachedCameraId ?: resolveCameraId(cameraManager)?.also { cachedCameraId = it }

    fun hasFlash(): Boolean = cameraId != null

    fun setTorch(on: Boolean): Boolean {
        val id = cameraId ?: return false
        return try {
            cameraManager.setTorchMode(id, on)
            true
        } catch (e: CameraAccessException) {
            Log.w(TAG, "setTorchMode($on) נכשל: ${e.reason}", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "setTorchMode($on) נכשל - מצלמה לא תקינה", e)
            false
        }
    }

    /** מאזין למצב האמיתי. [onChange] מקבל (דלוק, זמין). */
    fun observe(onChange: (on: Boolean, available: Boolean) -> Unit): () -> Unit {
        val id = cameraId
        if (id == null) {
            Log.w(TAG, "אין מצלמה עם פנס - לא ניתן להאזין למצב הפנס")
            return {}
        }
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == id) onChange(enabled, true)
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                if (cameraId == id) onChange(false, false)
            }
        }
        cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        return { cameraManager.unregisterTorchCallback(callback) }
    }

    companion object {
        private const val TAG = "FlashlightController"
        @Volatile private var cachedCameraId: String? = null

        /** המצלמה האחורית עם פנס, או כל מצלמה עם פנס אם אין אחורית. */
        private fun resolveCameraId(cameraManager: CameraManager): String? = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            Log.w(TAG, "איתור מצלמת הפנס נכשל", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "איתור מצלמת הפנס נכשל - מזהה מצלמה לא תקין", e)
            null
        }
    }
}
