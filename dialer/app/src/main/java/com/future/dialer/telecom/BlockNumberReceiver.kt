package com.future.dialer.telecom

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.BlockedNumberContract
import android.util.Log

/**
 * חסימת מספר מאפליקציית אנשי הקשר. רשימת החסומים של המערכת נכתבת רק על ידי
 * אפליקציית ברירת המחדל לשיחות - החייגן - ולכן אנשי קשר מבקשים ממנו. מוגן
 * בהרשאת החתימה של FutureOS, כך שרק אפליקציות הסוויטה יכולות לשלוח אותו.
 */
class BlockNumberReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SET_BLOCKED) return
        val number = intent.getStringExtra(EXTRA_NUMBER)?.takeIf { it.isNotBlank() } ?: return
        val blocked = intent.getBooleanExtra(EXTRA_BLOCKED, true)
        try {
            if (blocked) {
                val values = ContentValues().apply { put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number) }
                context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
            } else {
                BlockedNumberContract.unblock(context, number)
            }
        } catch (e: Exception) {
            // לא אפליקציית ברירת המחדל לשיחות - איש הקשר עדיין מסומן SEND_TO_VOICEMAIL.
            Log.w("BlockNumberReceiver", "Could not update the blocked-numbers list", e)
        }
    }

    companion object {
        const val ACTION_SET_BLOCKED = "com.future.dialer.ACTION_SET_BLOCKED"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_BLOCKED = "blocked"
    }
}
