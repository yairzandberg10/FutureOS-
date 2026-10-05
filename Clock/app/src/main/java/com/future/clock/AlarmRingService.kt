package com.future.clock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.CombinedVibration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import com.future.clock.logic.AlarmLogic
import com.future.clock.logic.EXTRA_ALARM_ID
import com.future.clock.logic.SNOOZE_MINUTES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Clock/AlarmRingService"
private const val CHANNEL_ID = "alarm_ring"
private const val NOTIFICATION_ID = 7001

/** אחרי כמה זמן צלצול שאיש לא ענה לו נדחה לבד (נודניק), כמו בכל שעון מעורר. */
private const val AUTO_SNOOZE_MILLIS = 10 * 60 * 1000L

/**
 * הצלצול עצמו - צליל, רטט והתראה במסך מלא - רץ כאן ולא ב-AlarmRingActivity.
 *
 * באנדרואיד 12 Receiver שהופעל מ-AlarmManager לא רשאי לפתוח Activity מהרקע
 * ("Abort background activity starts"), ולכן אזעקה עברה בשקט: לא נפתח מסך,
 * ולא היו צליל או התראה. אבל אזעקה מדויקת כן פוטרת הפעלה של Foreground
 * Service, וה-Service מציג התראה עם setFullScreenIntent - המערכת עצמה פותחת
 * את מסך הצלצול כשהמסך כבוי או נעול, ובמסך פעיל מציגה התראה צפה. הצליל
 * והרטט רצים כאן, כך שהאזעקה מצלצלת גם אם מסך הצלצול לא נפתח.
 */
class AlarmRingService : Service() {
    private var ringtone: Ringtone? = null
    private var vibratorManager: VibratorManager? = null
    private var alarmId = -1
    private val handler = Handler(Looper.getMainLooper())
    private val autoSnooze = Runnable { finishRinging(snooze = true) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            ACTION_DISMISS -> finishRinging(snooze = false)
            ACTION_SNOOZE -> finishRinging(snooze = true)
            ACTION_SILENCE -> runCatching { ringtone?.stop() }
            // הופעה מחדש אחרי שהתהליך נהרג (START_NOT_STICKY לא אמור להגיע לכאן) - לא מצלצלים שוב.
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        // אזעקה חדשה בזמן שאחרת מצלצלת מחליפה אותה - בלי שני צלצולים זה על זה.
        stopSound()
        alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val ringIntent = Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtras(intent)
        }
        startForeground(NOTIFICATION_ID, buildNotification(intent, ringIntent))
        _ringing.value = true
        startSound()
        handler.removeCallbacks(autoSnooze)
        handler.postDelayed(autoSnooze, AUTO_SNOOZE_MILLIS)
        // כשהאפליקציה בחזית (או כשהמערכת מתירה) המסך נפתח מיד; אחרת ההתראה
        // במסך מלא פותחת אותו, וזה לא נכשל בשקט כמו קודם.
        runCatching { startActivity(ringIntent) }.onFailure { Log.i(TAG, "ring screen via full-screen intent only: $it") }
    }

    private fun finishRinging(snooze: Boolean) {
        if (snooze && alarmId != -1) AlarmLogic.scheduleSnooze(this, alarmId)
        _ringing.value = false
        stopSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(intent: Intent, ringIntent: Intent): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "שעון מעורר וטיימר", NotificationManager.IMPORTANCE_HIGH).apply {
                    // הצליל והרטט מגיעים מה-Service עצמו (לולאה עד עצירה) - לא מהערוץ.
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }
        val fullScreen = PendingIntent.getActivity(
            this, NOTIFICATION_ID, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hour = intent.getIntExtra(AlarmRingActivity.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmRingActivity.EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(AlarmRingActivity.EXTRA_LABEL).orEmpty()
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifBlank { "שעון מעורר" })
            .setContentText("%02d:%02d".format(hour, minute))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(Notification.Action.Builder(null, "עצור", serviceIntent(ACTION_DISMISS, 1)).build())
        // לטיימר (בלי מזהה אזעקה) אין נודניק - כמו במסך הצלצול.
        if (alarmId != -1) {
            builder.addAction(Notification.Action.Builder(null, "נודניק · $SNOOZE_MINUTES דקות", serviceIntent(ACTION_SNOOZE, 2)).build())
        }
        return builder.build()
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode, Intent(this, AlarmRingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun startSound() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                isLooping = true
                play()
            }
        } catch (e: Exception) {
            // בלי צליל ברירת מחדל - רטט והתראה בלבד.
        }
        try {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager = manager
            manager.vibrate(CombinedVibration.createParallel(VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0)))
        } catch (e: Exception) {
            // בלי רטט - צליל והתראה בלבד.
        }
    }

    private fun stopSound() {
        handler.removeCallbacks(autoSnooze)
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibratorManager?.cancel() }
    }

    override fun onDestroy() {
        stopSound()
        _ringing.value = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.future.clock.ring.START"
        const val ACTION_DISMISS = "com.future.clock.ring.DISMISS"
        const val ACTION_SNOOZE = "com.future.clock.ring.SNOOZE"
        const val ACTION_SILENCE = "com.future.clock.ring.SILENCE"

        private val _ringing = MutableStateFlow(false)

        /** האם יש צלצול פעיל - מסך הצלצול נסגר כשזה יורד (למשל "עצור" מההתראה). */
        val ringing: StateFlow<Boolean> = _ringing.asStateFlow()

        fun start(context: Context, alarmId: Int, hour: Int, minute: Int, label: String) {
            context.startForegroundService(
                Intent(context, AlarmRingService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmRingActivity.EXTRA_HOUR, hour)
                    putExtra(AlarmRingActivity.EXTRA_MINUTE, minute)
                    putExtra(AlarmRingActivity.EXTRA_LABEL, label)
                },
            )
        }

        fun send(context: Context, action: String) {
            context.startService(Intent(context, AlarmRingService::class.java).setAction(action))
        }
    }
}
