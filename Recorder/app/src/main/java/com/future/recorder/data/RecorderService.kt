package com.future.recorder.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.future.recorder.MainActivity
import com.future.recorder.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.sqrt

enum class RecorderStatus { Idle, Recording, Paused }

/**
 * מצב ההקלטה הנוכחית. [accumulatedMs] הוא הזמן שהוקלט עד ההשהיה האחרונה,
 * ו-[resumedAt] הוא רגע ההמשך (uptime) - כך הממשק מחשב את הזמן בעצמו בכל
 * פריים בלי שהשירות ישדר עדכון כל עשירית שנייה.
 */
data class RecorderState(
    val status: RecorderStatus = RecorderStatus.Idle,
    val file: File? = null,
    val accumulatedMs: Long = 0L,
    val resumedAt: Long = 0L,
    /** עוצמות אחרונות (0..1) לתצוגת גלי הקול, הישנה ראשונה. */
    val levels: List<Float> = emptyList(),
    /** עולה בכל הקלטה שנשמרה - הרשימה מתרעננת לפיו. */
    val savedCount: Int = 0,
    val error: String? = null,
) {
    fun elapsedMs(now: Long = SystemClock.uptimeMillis()): Long =
        if (status == RecorderStatus.Recording) accumulatedMs + (now - resumedAt) else accumulatedMs
}

/**
 * שירות קדמי שמחזיק את ה-MediaRecorder: ההקלטה ממשיכה כשהמסך כבה או כשעוברים
 * לאפליקציה אחרת, ומופיעה התראה קבועה עם כפתור עצירה.
 */
class RecorderService : Service() {

    private var recorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val levelTick = object : Runnable {
        override fun run() {
            val r = recorder ?: return
            if (_state.value.status == RecorderStatus.Recording) {
                val amp = runCatching { r.maxAmplitude }.getOrDefault(0)
                // שורש ריבועי - כך גם דיבור שקט נראה בגרף ולא רק צעקות
                val level = sqrt((amp / 32767f).coerceIn(0f, 1f))
                val next = (_state.value.levels + level).takeLast(MAX_LEVELS)
                _state.value = _state.value.copy(levels = next)
            }
            handler.postDelayed(this, 80)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        if (recorder != null) return
        val file = Recordings.newFile(this)
        startForeground(NOTIFICATION_ID, buildNotification(paused = false), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        try {
            recorder = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setAudioChannels(1)
                setOutputFile(file.path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            recorder?.release()
            recorder = null
            file.delete()
            _state.value = RecorderState(savedCount = _state.value.savedCount, error = "לא ניתן להתחיל הקלטה - המיקרופון תפוס?")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Recorder:recording")
            .apply { acquire(6 * 60 * 60 * 1000L) }
        _state.value = RecorderState(
            status = RecorderStatus.Recording,
            file = file,
            resumedAt = SystemClock.uptimeMillis(),
            savedCount = _state.value.savedCount,
        )
        handler.post(levelTick)
    }

    private fun pause() {
        val s = _state.value
        if (s.status != RecorderStatus.Recording) return
        runCatching { recorder?.pause() }.onFailure { Log.w(TAG, "pause failed", it) }
        _state.value = s.copy(status = RecorderStatus.Paused, accumulatedMs = s.elapsedMs())
        notify(paused = true)
    }

    private fun resume() {
        val s = _state.value
        if (s.status != RecorderStatus.Paused) return
        runCatching { recorder?.resume() }.onFailure { Log.w(TAG, "resume failed", it) }
        _state.value = s.copy(status = RecorderStatus.Recording, resumedAt = SystemClock.uptimeMillis())
        notify(paused = false)
    }

    private fun stop() {
        handler.removeCallbacks(levelTick)
        val s = _state.value
        var saved = false
        recorder?.let { r ->
            // stop() זורק אם לא נאסף אף פריים (הקלטה של רגע) - אז הקובץ ריק ונמחק
            saved = runCatching { r.stop() }.isSuccess
            r.release()
        }
        recorder = null
        if (!saved) s.file?.delete()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        _state.value = RecorderState(savedCount = s.savedCount + if (saved) 1 else 0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (recorder != null) stop()
        super.onDestroy()
    }

    private fun notify(paused: Boolean) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(paused))
    }

    private fun buildNotification(paused: Boolean): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "הקלטה פעילה", NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RecorderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (paused) "ההקלטה מושהית" else "מקליט")
            .setContentText(_state.value.file?.nameWithoutExtension ?: "רשמקול")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "עצור ושמור", stop).build())
            .build()
    }

    companion object {
        private const val TAG = "RecorderService"
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 7
        private const val MAX_LEVELS = 48
        const val ACTION_START = "com.future.recorder.START"
        const val ACTION_PAUSE = "com.future.recorder.PAUSE"
        const val ACTION_RESUME = "com.future.recorder.RESUME"
        const val ACTION_STOP = "com.future.recorder.STOP"

        private val _state = MutableStateFlow(RecorderState())
        val state: StateFlow<RecorderState> = _state.asStateFlow()

        fun send(context: Context, action: String) {
            val intent = Intent(context, RecorderService::class.java).setAction(action)
            if (action == ACTION_START) context.startForegroundService(intent) else context.startService(intent)
        }

        fun clearError() {
            _state.value = _state.value.copy(error = null)
        }
    }
}
