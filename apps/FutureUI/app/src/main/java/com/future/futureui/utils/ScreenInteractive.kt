package com.future.futureui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * האם המסך דולק עכשיו. החלונות של FutureUI (שורת המצב, מסך הנעילה) נשארים מורכבים גם
 * כשהמסך כבוי, ולכן לולאות רענון בתוכם ממשיכות לרוץ בכל פעם שאפליקציה אחרת מעירה את
 * המעבד ברקע. לולאה שעוטפת את עצמה ב-`if (interactive)` עוצרת כשהמסך נכבה וחוזרת
 * מיד כשהוא נדלק - בלי שום עבודה ברקע בין לבין.
 */
@Composable
fun rememberScreenInteractive(): State<Boolean> {
    val context = LocalContext.current
    val state = remember {
        mutableStateOf(context.getSystemService(PowerManager::class.java)?.isInteractive != false)
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> state.value = true
                    Intent.ACTION_SCREEN_OFF -> state.value = false
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(receiver, filter)
        // מצב שהשתנה בין ה-remember לרישום
        state.value = context.getSystemService(PowerManager::class.java)?.isInteractive != false
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state
}
