package com.future.sharednav.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.future.sharednav.actions.FutureUIActions

/**
 * מאזין ללחיצה קצרה על * - בדיוק כמו onOptionsKeyPress: שירותי הנגישות של
 * FutureUI צורכים את * ו-# ברמת המערכת (החזקה ארוכה = מרכז בקרה / מרכז התראות)
 * ולעולם לא מעבירים KEYCODE_STAR/KEYCODE_POUND לאפליקציה שבחזית, ולכן onKeyEvent
 * על המקשים האלה הוא קוד מת במכשיר אמיתי. הלחיצה הקצרה מגיעה רק כשידור.
 *
 * שימוש: onStarKeyPress { onPrevChapter() }
 */
@Composable
fun onStarKeyPress(onTrigger: () -> Unit) =
    onSystemKeyBroadcast(FutureUIActions.ACTION_STAR_SHORT_PRESS, onTrigger)

/** כמו onStarKeyPress, עבור #. */
@Composable
fun onPoundKeyPress(onTrigger: () -> Unit) =
    onSystemKeyBroadcast(FutureUIActions.ACTION_POUND_SHORT_PRESS, onTrigger)

@Composable
private fun onSystemKeyBroadcast(action: String, onTrigger: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = (context as? LifecycleOwner)?.lifecycle
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    DisposableEffect(context, action) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                // השידור גלובלי - אפליקציה ברקע (activity ב-STOPPED, ה-composition
                // עדיין חי) לא אמורה להגיב ללחיצה שנועדה לאפליקציה שבחזית.
                if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false) currentOnTrigger()
            }
        }
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
}
