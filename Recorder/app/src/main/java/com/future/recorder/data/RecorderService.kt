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
        // קבצים חלקיים מהפעלה שנהרגה באמצע לא נשארים על הדיסק.
        Recordings.dir(this).listFiles { f -> f.name.endsWith(PART_SUFFIX) }?.forEach { it.delete() }
        if (Recordings.dir(this).usableSpace < MIN_FREE_BYTES) {
            failStart("אין מקום פנוי להקלטה")
            return
        }
        // ההקלטה נכתבת לקובץ .part (לא נראה ברשימה) ונשמרת בשמה הסופי רק אחרי עצירה.
        val file = try {
            val partFile = File(Recordings.newFile(this).path + PART_SUFFIX)
            // startForeground נזרק כשההרשאה נשללה - בלי הטיפול הזה השירות קורס לפני ההתחלה.
            startForeground(NOTIFICATION_ID, buildNotification(paused = false), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            partFile
        } catch (e: Exception) {
            Log.e(TAG, "foreground start failed", e)
            failStart("לא ניתן להתחיל הקלטה - בדוק הרשאת מיקרופון")
            return
        }
        try {
            // prepare/start נקראים במפורש על r, ולא בתוך apply: שם start() נפתר
            // ל-RecorderService.start() עצמו, ו-recorder עדיין null - רקורסיה
            // אינסופית ו-StackOverflowError בכל ניסיון הקלטה.
            val r = MediaRecorder(this)
            recorder = r
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44_100)
            r.setAudioChannels(1)
            r.setOutputFile(file.path)
            // שיחה נכנסת, מיקרופון שנלקח או אחסון מלא עוצרים את המקליט - בלי זה ההקלטה
            // המשיכה להירשם על המסך כאילו ממשיכה.
            r.setOnErrorListener { _, _, _ -> onRecorderError("ההקלטה נעצרה - שגיאה במיקרופון או באחסון") }
            r.prepare()
            r.start()
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            recorder?.release()
            recorder = null
            file.delete()
            failStart("לא ניתן להתחיל הקלטה - המיקרופון תפוס?")
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

    private fun failStart(message: String) {
        _state.value = RecorderState(savedCount = _state.value.savedCount, error = message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** שגיאת מקליט באמצע הקלטה: שומרים מה שנאסף ומציגים הסבר במסך. */
    private fun onRecorderError(message: String) {
        if (recorder == null) return
        stop()
        _state.value = _state.value.copy(error = message)
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
            // stop() זורק אם לא נאסף אף פריים (הקלטה של רגע) - אז הקובץ ריק ונמחק.
            // הסף הוא שנייה: הקלטה ארוכה לא נמחקת בשקט גם אם העצירה נכשלה.
            saved = runCatching { r.stop() }.isSuccess
            r.release()
        }
        recorder = null
        val part = s.file
        val target = part?.let { File(it.path.removeSuffix(PART_SUFFIX)) }
        val keep = saved || s.elapsedMs() >= MIN_KEPT_MS
        if (part != null && !keep) part.delete()
        val kept = part != null && target != null && keep && part.renameTo(target)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        _state.value = RecorderState(
            savedCount = s.savedCount + if (kept) 1 else 0,
            error = if (keep && !saved) "ההקלטה לא נשמרה במלואה" else null,
        )
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
            .setContentText(_state.value.file?.let { File(it.path.removeSuffix(PART_SUFFIX)).nameWithoutExtension } ?: "רשמקול")
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
        private const val PART_SUFFIX = ".part"
        private const val MIN_KEPT_MS = 1_000L
        private const val MIN_FREE_BYTES = 5L * 1024 * 1024
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
