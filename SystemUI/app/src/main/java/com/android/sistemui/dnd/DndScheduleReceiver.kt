package com.android.sistemui.dnd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * מקבל שמופעל ע"י AlarmManager (ר' DndScheduleManager) בזמן ההתחלה/סיום המתוזמן
 * של DND. מפעיל את השינוי בפועל ומיד מתזמן את עצמו שוב ל-24 שעות קדימה - כל
 * אזעקת AlarmManager היא חד-פעמית מרגע שהופעלה.
 */
class DndScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val activate = intent.getBooleanExtra(DndScheduleManager.EXTRA_ACTIVATE, false)
        val isStart = intent.getBooleanExtra("is_start", true)
        val manager = DndScheduleManager(context.applicationContext)
        manager.applyState(activate)
        manager.rescheduleSingle(isStart)
    }
}
