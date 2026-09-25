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
 * מאזין ל-ACTION_OPTIONS_SHORT_PRESS - זו הדרך היחידה שבה מסך יכול לדעת
 * שנלחץ מקש Options/Menu הפיזי: FutureUI's StatusBarAccessibilityService
 * צורך את המקש הזה ברמת המערכת (ללחיצה ארוכה ל"אפליקציות אחרונות") ולעולם
 * לא מעביר Key.Menu/Key.Settings לאפליקציה שבחזית. האזנה ל-onKeyEvent
 * ל-Key.Menu בקומפוז היא קוד מת שלעולם לא יופעל על מכשיר אמיתי.
 *
 * שימוש: onOptionsKeyPress { showOptionsMenu = true }
 */
@Composable
fun onOptionsKeyPress(onTrigger: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = context.lifecycleOwner()?.lifecycle
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                // השידור גלובלי ומגיע לכל אפליקציה שה-composition שלה חי - גם
                // כשהיא ברקע. בלי הבדיקה הזו, Options בתוך אפליקציה אחת פתח
                // ברקע את תפריט האפליקציה של מסך הבית, והוא חיכה פתוח ביציאה
                // למסך הבית.
                if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false) currentOnTrigger()
            }
        }
        val filter = IntentFilter(FutureUIActions.ACTION_OPTIONS_SHORT_PRESS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
}

/** ה-Activity שמאחורי ה-Context - גם מתוך דיאלוג, שה-Context שלו עטוף. */
internal fun Context.lifecycleOwner(): LifecycleOwner? {
    var current: Context? = this
    while (current != null) {
        if (current is LifecycleOwner) return current
        current = (current as? android.content.ContextWrapper)?.baseContext
    }
    return null
}
