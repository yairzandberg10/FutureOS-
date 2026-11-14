package com.future.settings.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.settings.utils.SystemInteractor

/** אחרי הפעלה מחדש ה-AlarmManager מאבד את כל התזמונים, אבל ההעדפות נשארות. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val interactor = SystemInteractor(context)
        // מתזמנים מחדש רק את מה שהמשתמש הפעיל: שעות שינה ואכיפת טיימרים לאפליקציות
        if (interactor.isSleepModeEnabled()) interactor.setSleepModeEnabled(true)
        if (interactor.getAppTimers().isNotEmpty()) interactor.scheduleAppTimerChecks()
    }
}
