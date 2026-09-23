package com.future.flashlight.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/**
 * שליטה בפנס דרך Camera2 (setTorchMode) - בלי לפתוח את המצלמה ובלי הרשאת
 * CAMERA. המצב האמיתי מגיע מ-TorchCallback, כך שהמסך תמיד מראה אם הפנס דלוק
 * (גם כשהודלק/כובה ממרכז הבקרה), ומדווח כשהפנס לא זמין כי המצלמה בשימוש.
 */
class FlashlightController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val cameraId: String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        null
    }

    fun hasFlash(): Boolean = cameraId != null

    fun setTorch(on: Boolean): Boolean {
        val id = cameraId ?: return false
        return try {
            cameraManager.setTorchMode(id, on)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** מאזין למצב האמיתי. [onChange] מקבל (דלוק, זמין). */
    fun observe(onChange: (on: Boolean, available: Boolean) -> Unit): () -> Unit {
        val id = cameraId ?: return {}
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
}
