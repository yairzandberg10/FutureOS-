package com.future.futureui.lockscreen.settings

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.future.futureui.ui.theme.FutureUITheme

/**
 * הגדרות מסך הנעילה והאבטחה. אם מוגדר קוד - צריך להזין אותו לפני שרואים
 * משהו כאן (אחרת כל מי שמחזיק את הטלפון פתוח יכול לכבות את הנעילה).
 * נפתח מ"התאמה אישית" של FutureUI, או מאפליקציית ההגדרות ב-ACTION_LOCK_SETTINGS.
 */
class LockSettingsActivity : ComponentActivity() {
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // קוד ונתוני פנים - לא בצילומי מסך ולא בכרטיס של האחרונות
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            FutureUITheme {
                LockSettingsScreen(modifier = Modifier.fillMaxSize(), onExit = { finish() })
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // יציאה מהמסך = צריך שוב קוד בפעם הבאה
        if (!isChangingConfigurations) finish()
    }
}
