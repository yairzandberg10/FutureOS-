package com.future.tasks.data

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.future.tasks.MainActivity
import java.util.Calendar

/**
 * תזכורות למשימות: AlarmManager מדויק (גם במצב Doze), והתראה כשמגיע הזמן.
 * הכל נשמר ב-DB (Task.reminderAt), ו-TaskReminderReceiver מתזמן מחדש אחרי
 * אתחול, שינוי שעה או עדכון של האפליקציה.
 */
object TaskReminders {
    const val ACTION_REMIND = "com.future.tasks.ACTION_REMIND"
    const val EXTRA_TASK_ID = "taskId"
    private const val CHANNEL_ID = "task_reminders"

    fun schedule(context: Context, task: Task) {
        val at = task.reminderAt
        if (at == null || task.isDone || at <= System.currentTimeMillis()) {
            cancel(context, task.id)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, task.id, PendingIntent.FLAG_UPDATE_CURRENT)!!
        // בלי הרשאת "שעונים מעוררים ותזכורות" - תזכורת לא מדויקת עדיף על קריסה.
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, taskId: Int) {
        val pending = pendingIntent(context, taskId, PendingIntent.FLAG_NO_CREATE) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(context: Context, taskId: Int, flag: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            taskId,
            Intent(context, TaskReminderReceiver::class.java).setAction(ACTION_REMIND).putExtra(EXTRA_TASK_ID, taskId),
            flag or PendingIntent.FLAG_IMMUTABLE,
        )

    fun notify(context: Context, task: Task) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "תזכורות למשימות", NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val open = PendingIntent.getActivity(
            context,
            task.id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(task.title.ifBlank { "משימה" })
            .setContentText(task.notes.ifBlank { "תזכורת" })
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_BASE + task.id, notification)
    }

    /**
     * אפשרויות התזכורת בעורך. "הערב" מופיע רק לפני 20:00 - קודם "היום" נבחר
     * ל-09:00 גם בערב, השמירה נחסמה כ"המועד עבר" ו-BACK נתקע (TK1).
     */
    fun presets(now: Calendar = Calendar.getInstance()): List<Pair<String, Long>> {
        val result = mutableListOf<Pair<String, Long>>()
        result += "בעוד שעה" to now.timeInMillis + 60 * 60 * 1000L
        val evening = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        if (evening.after(now)) result += "הערב 20:00" to evening.timeInMillis
        val tomorrow = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        result += "מחר 09:00" to tomorrow.timeInMillis
        val nextWeek = (tomorrow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        result += "בעוד שבוע" to nextWeek.timeInMillis
        return result
    }

    /** "מחר 09:00", "היום 20:00", "12.10 09:00" - לתצוגה בעורך וברשימה. */
    fun label(at: Long, now: Calendar = Calendar.getInstance()): String {
        val c = Calendar.getInstance().apply { timeInMillis = at }
        val time = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        val dayDiff = ((startOfDay(c) - startOfDay(now)) / (24 * 60 * 60 * 1000L)).toInt()
        val day = when (dayDiff) {
            0 -> "היום"
            1 -> "מחר"
            else -> "%d.%d".format(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1)
        }
        return "$day $time"
    }

    private fun startOfDay(c: Calendar): Long = (c.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private const val NOTIFICATION_BASE = 5000
}
