package com.future.settings.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.settings.utils.SystemInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** נורה כל 15 דקות (מתוזמן ע"י SystemInteractor.scheduleAppTimerChecks) ואוכף
 *  טיימרים לאפליקציות גם כשמסך ההגדרות סגור - אותו רעיון בדיוק כמו
 *  FocusActionReceiver עבור מצב מיקוד/שינה. */
class AppTimerCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // שאילתת שימוש ו-su לוקחות זמן - לא על ה-main thread. goAsync מחזיק את הקליטה עד finish
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SystemInteractor(context).checkAndEnforceAppTimers()
            } finally {
                pending.finish()
            }
        }
    }
}
