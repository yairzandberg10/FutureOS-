package com.future.sharednav.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.future.sharednav.actions.FutureUIActions

/**
 * רישום למקשי Options / * / # ש-FutureUI משדר (FutureUIActions.ACTION_*_SHORT_PRESS).
 *
 * מקבלים את השידור רק משני שולחים:
 *  - אפליקציות FutureOS (הרשאת החתימה PERMISSION_SYSTEM) - FutureUI עצמו;
 *  - adb shell (`am broadcast` רץ כ-shell, שמחזיק את android.permission.DUMP),
 *    כדי שבדיקות ממשיכות לעבוד.
 * אפליקציה רגילה לא מחזיקה אף אחת מהשתיים, אז היא לא יכולה יותר "ללחוץ" על
 * מקשים בתוך אפליקציות אחרות או במקלדת.
 */
object KeyPressBroadcasts {
    private const val SHELL_PERMISSION = "android.permission.DUMP"

    fun register(context: Context, receiver: BroadcastReceiver, filter: IntentFilter) {
        ContextCompat.registerReceiver(
            context, receiver, filter, FutureUIActions.PERMISSION_SYSTEM, null, ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(filter), SHELL_PERMISSION, null, ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregister(context: Context, receiver: BroadcastReceiver) {
        runCatching { context.unregisterReceiver(receiver) }
    }
}
