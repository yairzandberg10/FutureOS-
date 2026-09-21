package com.android.sistemui.dnd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * כל אזעקות AlarmManager מתאפסות באתחול המכשיר - בלי המקבל הזה, תזמון DND
 * שהוגדר לפני כיבוי/הפעלה-מחדש פשוט מפסיק לפעול בשקט עד שמישהו נכנס
 * להגדרות ומשנה שם משהו (מה שקורא ל-reschedule כאגב).
 */
class DndBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val manager = DndScheduleManager(context.applicationContext)
        if (manager.isEnabled()) manager.reschedule()
    }
}
