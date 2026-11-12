package com.future.settings.futureserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.future.settings.MainActivity
import com.future.settings.feedback.CrashStore
import com.future.settings.update.UpdatePolicy
import com.future.settings.update.UpdateRepository

/**
 * ההתראות של מנוע העדכונים ושל המשוב. בלי GMS אין push: הן נוצרות מקומית, אחרי בדיקה ברקע
 * (FutureServerSyncJob) או כשמגיע דוח קריסה. כל התראה פותחת מסך ב-Settings, ולא עושה שום פעולה בעצמה.
 */
object FutureNotifications {
    private const val CH_UPDATES = "futureos_updates"
    private const val CH_FEEDBACK = "futureos_feedback"
    private const val CH_CRASH = "futureos_crash"
    private const val ID_UPDATES = 7101
    private const val ID_FEEDBACK = 7102
    private const val ID_CRASH_BASE = 7200

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    private fun ensureChannels(context: Context) {
        val nm = manager(context) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_UPDATES, "עדכוני תוכנה", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_FEEDBACK, "תשובות למשוב", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_CRASH, "דיווח על קריסות", NotificationManager.IMPORTANCE_LOW))
    }

    private fun open(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent.setClass(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun post(context: Context, id: Int, n: Notification) {
        runCatching { manager(context)?.notify(id, n) }
    }

    fun showUpdates(context: Context, offers: List<UpdatePolicy.Offer>) {
        if (offers.isEmpty()) return
        ensureChannels(context)
        val names = offers.map { UpdateRepository.appLabel(context, it.pkg) }
        val title = if (offers.size == 1) "עדכון ל${names[0]}" else "${offers.size} עדכונים זמינים"
        val text = if (offers.size == 1) "גרסה ${offers[0].versionName}. לחץ כדי לעדכן" else names.joinToString(", ")
        post(
            context,
            ID_UPDATES,
            Notification.Builder(context, CH_UPDATES)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(open(context, ID_UPDATES, Intent(MainActivity.ACTION_OPEN_UPDATES)))
                .build(),
        )
    }

    /** תשובות מהשרת ("בטיפול", "תוקן בגרסה X"). כמה תשובות = התראה אחת. */
    fun showFeedbackReplies(context: Context, messages: List<String>) {
        if (messages.isEmpty()) return
        ensureChannels(context)
        val title = if (messages.size == 1) "תשובה למשוב שלך" else "${messages.size} תשובות למשוב שלך"
        post(
            context,
            ID_FEEDBACK,
            Notification.Builder(context, CH_FEEDBACK)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(messages.last())
                .setStyle(Notification.BigTextStyle().bigText(messages.takeLast(3).joinToString("\n")))
                .setAutoCancel(true)
                .setContentIntent(
                    open(context, ID_FEEDBACK, Intent(MainActivity.ACTION_SEND_FEEDBACK).putExtra(MainActivity.EXTRA_OPEN_HISTORY, true)),
                )
                .build(),
        )
    }

    /** אפליקציה נסגרה בגלל תקלה. התראה אחת לכל אפליקציה, שמתעדכנת אם היא נסגרת שוב. */
    fun showCrash(context: Context, crash: CrashStore.Crash) {
        ensureChannels(context)
        val label = UpdateRepository.appLabel(context, crash.pkg)
        val count = CrashStore.countFor(context, crash.pkg)
        val id = ID_CRASH_BASE + (crash.pkg.hashCode() and 0x7f)
        post(
            context,
            id,
            Notification.Builder(context, CH_CRASH)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(if (count > 1) "$label נסגרה $count פעמים" else "$label נסגרה בגלל תקלה")
                .setContentText("לחץ כדי לשלוח דיווח באג. תראה בדיוק מה נשלח לפני השליחה")
                .setAutoCancel(true)
                .setContentIntent(
                    open(
                        context,
                        id,
                        Intent(MainActivity.ACTION_SEND_FEEDBACK)
                            .putExtra(MainActivity.EXTRA_FEEDBACK_PKG, crash.pkg)
                            .putExtra(MainActivity.EXTRA_CRASH_ID, crash.id),
                    ),
                )
                .build(),
        )
    }

    fun cancelCrash(context: Context, pkg: String) {
        runCatching { manager(context)?.cancel(ID_CRASH_BASE + (pkg.hashCode() and 0x7f)) }
    }
}
