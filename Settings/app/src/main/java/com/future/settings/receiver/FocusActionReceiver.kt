package com.future.settings.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.future.settings.utils.SystemInteractor

/** מקבל שידורים מ-AlarmManager עבור "מיקוד עכשיו" ו"שעות שינה" - מריץ אותם גם
 *  כשהאפליקציה סגורה, בלי צורך שהמסך של הגדרות יהיה פתוח. */
class FocusActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FOCUS_REVERT = "com.future.settings.action.FOCUS_REVERT"
        const val ACTION_SLEEP_START = "com.future.settings.action.SLEEP_START"
        const val ACTION_SLEEP_END = "com.future.settings.action.SLEEP_END"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val interactor = SystemInteractor(context)
        when (intent.action) {
            ACTION_FOCUS_REVERT, ACTION_SLEEP_END -> interactor.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
            ACTION_SLEEP_START -> interactor.setRingerMode(AudioManager.RINGER_MODE_SILENT)
        }
    }
}
