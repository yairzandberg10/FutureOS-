package com.android.sistemui.dnd

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

/**
 * תזמון אוטומטי ל"נא לא להפריע" - תוספת ל-DND הידני שכבר קיים בכפתור ב-Control
 * Center (ControlManager.toggleDnd). לא תלוי ב-ControlManager או בשירות ה-overlay
 * כלשהו כי הוא צריך לפעול גם כשהמסך כבוי ואף חלון overlay לא חי - AlarmManager +
 * BroadcastReceiver עצמאי (DndScheduleReceiver) הם המנגנון הנכון לזה באנדרואיד,
 * לא polling מתמשך שהיה מכלה סוללה.
 *
 * כל הפעלה של המקבל מתזמנת את עצמה מחדש ל-24 שעות קדימה (ולא alarm חוזר
 * מובנה) כדי שהזמן שנקבע יישאר יציב מול שינויי שעון קיץ/חורף.
 */
class DndScheduleManager(private val context: Context) {

    companion object {
        private const val TAG = "DndScheduleManager"
        private const val PREFS = "dnd_schedule_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_START_MINUTES = "start_minutes"
        private const val KEY_END_MINUTES = "end_minutes"

        private const val DEFAULT_START_MINUTES = 22 * 60 // 22:00
        private const val DEFAULT_END_MINUTES = 7 * 60     // 07:00

        const val EXTRA_ACTIVATE = "activate"
        private const val REQUEST_CODE_START = 4001
        private const val REQUEST_CODE_END = 4002
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)
    fun getStartMinutes(): Int = prefs.getInt(KEY_START_MINUTES, DEFAULT_START_MINUTES)
    fun getEndMinutes(): Int = prefs.getInt(KEY_END_MINUTES, DEFAULT_END_MINUTES)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        if (value) reschedule() else cancelAll()
    }

    fun setStartMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_START_MINUTES, minutes.mod(24 * 60)).apply()
        if (isEnabled()) reschedule()
    }

    fun setEndMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_END_MINUTES, minutes.mod(24 * 60)).apply()
        if (isEnabled()) reschedule()
    }

    /** מבטל ומגדיר מחדש את שני האזעקות (התחלה/סיום) להתרחשות הבאה שלהן - קרוי גם אחרי
     *  איתחול (BootReceiver) כי כל אזעקות AlarmManager מתאפסות ב-reboot. */
    fun reschedule() {
        cancelAll()
        if (!isEnabled()) return
        scheduleNext(startOfDay = true, minutesOfDay = getStartMinutes(), activate = true)
        scheduleNext(startOfDay = false, minutesOfDay = getEndMinutes(), activate = false)
    }

    /** קרוי מ-DndScheduleReceiver אחרי שהוא כבר טיפל בהפעלה/כיבוי, כדי לקבוע את
     *  ההתרחשות הבאה של אותה אזעקה בדיוק (24 שעות מהיום שעבר, לא מ"עכשיו"). */
    fun rescheduleSingle(isStart: Boolean) {
        if (!isEnabled()) return
        scheduleNext(startOfDay = isStart, minutesOfDay = if (isStart) getStartMinutes() else getEndMinutes(), activate = isStart)
    }

    private fun scheduleNext(startOfDay: Boolean, minutesOfDay: Int, activate: Boolean) {
        val manager = alarmManager ?: return
        val trigger = nextOccurrence(minutesOfDay)
        val intent = Intent(context, DndScheduleReceiver::class.java).apply {
            putExtra(EXTRA_ACTIVATE, activate)
            putExtra("is_start", startOfDay)
        }
        val requestCode = if (startOfDay) REQUEST_CODE_START else REQUEST_CODE_END
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to schedule DND alarm", t)
        }
    }

    private fun cancelAll() {
        val manager = alarmManager ?: return
        listOf(REQUEST_CODE_START to true, REQUEST_CODE_END to false).forEach { (code, isStart) ->
            val intent = Intent(context, DndScheduleReceiver::class.java).apply { putExtra("is_start", isStart) }
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try { manager.cancel(pendingIntent) } catch (t: Throwable) {}
        }
    }

    /** מחשב את זמן ה-epoch millis הבא של "שעה:דקה נתונים" - היום אם עוד לא עבר, אחרת מחר. */
    private fun nextOccurrence(minutesOfDay: Int): Long {
        val now = Calendar.getInstance()
        val candidate = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.timeInMillis <= now.timeInMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }

    /** מפעיל/מכבה DND ישירות - אותה בדיקת הרשאה כמו ב-ControlManager.toggleDnd, בלי תלות
     *  ב-ControlManager עצמו (זה חי רק כשמרכז הבקרה פתוח). */
    fun applyState(activate: Boolean) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (!nm.isNotificationPolicyAccessGranted) {
                Log.w(TAG, "No notification policy access - skipping scheduled DND change")
                return
            }
            nm.setInterruptionFilter(
                if (activate) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to apply scheduled DND state", t)
        }
    }
}
