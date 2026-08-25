package com.future.tools.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/** שליטה אמיתית בפנס המכשיר דרך Camera2, בלי לפתוח את המצלמה. */
class FlashlightController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val cameraId: String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
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
}
