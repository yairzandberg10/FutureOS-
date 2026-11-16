package com.future.systemui.statusbar

import android.app.AppOpsManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Camera / microphone / location in use right now.
 *
 * With this app as THE SystemUI, nobody else draws Android 12's green privacy dot - stock SystemUI's
 * PrivacyItemController is gone - so the bar must. The source is the same one stock uses: AppOps
 * "active" ops (WATCH_APPOPS, granted to the platform signature). If that is refused, camera and
 * microphone fall back to CameraManager / AudioManager callbacks (no permission, no package names).
 */
internal class PrivacyMonitor(private val context: Context) {
    var camera by mutableStateOf(false)
        private set
    var microphone by mutableStateOf(false)
        private set
    var location by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val active = HashSet<String>() // "op|uid|package"
    private var opsListener: AppOpsManager.OnOpActiveChangedListener? = null
    private var cameraCallback: CameraManager.AvailabilityCallback? = null
    private var audioCallback: AudioManager.AudioRecordingCallback? = null

    fun start() {
        if (!startAppOps()) startFallback()
    }

    fun stop() {
        val ops = context.getSystemService(AppOpsManager::class.java)
        runCatching { opsListener?.let { ops?.stopWatchingActive(it) } }
        runCatching { cameraCallback?.let { context.getSystemService(CameraManager::class.java)?.unregisterAvailabilityCallback(it) } }
        runCatching { audioCallback?.let { context.getSystemService(AudioManager::class.java)?.unregisterAudioRecordingCallback(it) } }
        opsListener = null
        cameraCallback = null
        audioCallback = null
    }

    private fun startAppOps(): Boolean = try {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: throw IllegalStateException("no AppOpsManager")
        val listener = AppOpsManager.OnOpActiveChangedListener { op, uid, pkg, isActive ->
            val key = "$op|$uid|$pkg"
            main.post {
                if (isActive) active.add(key) else active.remove(key)
                publish()
            }
        }
        ops.startWatchingActive(WATCHED_OPS, context.mainExecutor, listener)
        opsListener = listener
        true
    } catch (t: Throwable) {
        Log.w(TAG, "AppOps active watch refused, using camera/audio callbacks", t)
        false
    }

    private fun publish() {
        camera = active.any { it.startsWith("$OP_CAMERA|") }
        microphone = active.any { it.startsWith("$OP_RECORD_AUDIO|") || it.startsWith("$OP_PHONE_CALL_MIC|") }
        location = active.any { it.startsWith("$OP_HIGH_POWER_LOCATION|") }
    }

    private fun startFallback() {
        val busy = HashSet<String>()
        runCatching {
            val cb = object : CameraManager.AvailabilityCallback() {
                override fun onCameraAvailable(cameraId: String) { busy.remove(cameraId); camera = busy.isNotEmpty() }
                override fun onCameraUnavailable(cameraId: String) { busy.add(cameraId); camera = true }
            }
            context.getSystemService(CameraManager::class.java)?.registerAvailabilityCallback(cb, main)
            cameraCallback = cb
        }.onFailure { Log.w(TAG, "camera availability callback failed", it) }
        runCatching {
            val am = context.getSystemService(AudioManager::class.java) ?: return@runCatching
            val cb = object : AudioManager.AudioRecordingCallback() {
                override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                    microphone = !configs.isNullOrEmpty()
                }
            }
            am.registerAudioRecordingCallback(cb, main)
            audioCallback = cb
            microphone = am.activeRecordingConfigurations.isNotEmpty()
        }.onFailure { Log.w(TAG, "audio recording callback failed", it) }
    }

    private companion object {
        const val TAG = "FutureSystemUI"
        const val OP_CAMERA = "android:camera"
        const val OP_RECORD_AUDIO = "android:record_audio"
        const val OP_PHONE_CALL_MIC = "android:phone_call_microphone"
        const val OP_HIGH_POWER_LOCATION = "android:monitor_location_high_power"
        val WATCHED_OPS = arrayOf(OP_CAMERA, OP_RECORD_AUDIO, OP_PHONE_CALL_MIC, OP_HIGH_POWER_LOCATION)
    }
}
