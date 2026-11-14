package com.android.sistemui.statusbar.logic

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateOf

/**
 * חיווי מצלמה ומיקרופון בשורת המצב (סעיפים 375/376 בצ'קליסט).
 *
 * שורת המערכת של אנדרואיד 12 מציגה נקודה ירוקה/כתומה, אבל השורה של FutureUI
 * מצוירת מעליה ומסתירה אותה - אז החיווי עובר לכאן. שני המקורות לא דורשים
 * הרשאה ומגיעים כאירועים (בלי סקר):
 *  - מצלמה: CameraManager.AvailabilityCallback - מצלמה "לא זמינה" = אפליקציה כלשהי פתחה אותה.
 *  - מיקרופון: AudioManager.AudioRecordingCallback - כל הקלטה פעילה במכשיר
 *    (בלי זהות האפליקציה, שאנדרואיד מסתיר מאפליקציה לא מערכתית).
 *
 * גם זיהוי הפנים של מסך הנעילה פותח את המצלמה, אז הנקודה הירוקה מופיעה בזמן
 * הסריקה - זה נכון ומכוון: החיווי מראה כל שימוש במצלמה, גם שלנו.
 */
object PrivacyIndicators {
    private const val TAG = "PrivacyIndicators"

    val cameraInUse = mutableStateOf(false)
    val micInUse = mutableStateOf(false)

    private val main = Handler(Looper.getMainLooper())
    private val busyCameras = HashSet<String>()
    private var users = 0
    private var cameraManager: CameraManager? = null
    private var audioManager: AudioManager? = null

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            busyCameras.remove(cameraId)
            cameraInUse.value = busyCameras.isNotEmpty()
        }

        override fun onCameraUnavailable(cameraId: String) {
            busyCameras.add(cameraId)
            cameraInUse.value = true
        }
    }

    private val audioCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            micInUse.value = !configs.isNullOrEmpty()
        }
    }

    /** כל שורת מצב שמוצגת קוראת ל-start, ול-stop כשהיא יורדת (ספירת משתמשים). */
    fun start(context: Context) {
        if (users++ > 0) return
        val app = context.applicationContext
        runCatching {
            cameraManager = (app.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.also {
                it.registerAvailabilityCallback(cameraCallback, main)
            }
        }.onFailure { Log.w(TAG, "camera availability callback failed", it) }
        runCatching {
            audioManager = (app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.also {
                it.registerAudioRecordingCallback(audioCallback, main)
                micInUse.value = it.activeRecordingConfigurations.isNotEmpty()
            }
        }.onFailure { Log.w(TAG, "audio recording callback failed", it) }
    }

    fun stop() {
        if (users == 0 || --users > 0) return
        runCatching { cameraManager?.unregisterAvailabilityCallback(cameraCallback) }
        runCatching { audioManager?.unregisterAudioRecordingCallback(audioCallback) }
        cameraManager = null
        audioManager = null
        busyCameras.clear()
        cameraInUse.value = false
        micInUse.value = false
    }
}
