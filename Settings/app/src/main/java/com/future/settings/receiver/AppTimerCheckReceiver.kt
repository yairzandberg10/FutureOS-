package com.future.settings.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.settings.utils.SystemInteractor

/** נורה כל 15 דקות (מתוזמן ע"י SystemInteractor.scheduleAppTimerChecks) ואוכף
 *  טיימרים לאפליקציות גם כשמסך ההגדרות סגור - אותו רעיון בדיוק כמו
 *  FocusActionReceiver עבור מצב מיקוד/שינה. */
class AppTimerCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SystemInteractor(context).checkAndEnforceAppTimers()
    }
}
