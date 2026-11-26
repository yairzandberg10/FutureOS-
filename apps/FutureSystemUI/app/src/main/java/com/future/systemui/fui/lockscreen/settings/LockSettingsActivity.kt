package com.future.systemui.fui.lockscreen.settings

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.future.systemui.fui.ui.theme.FutureUITheme

/**
 * רישום פנים למסך הנעילה של SystemUI. נפתח עם האסימון שהגדרות קיבלו מ-FutureUI אחרי הזנת הקוד;
 * האסימון נבדק מול FutureUI (ברקע), ובלי אסימון תקף או בלי קוד מוגדר המסך נסגר.
 * חשוב להצהיר ב-manifest עם android:exported="true" ו-android:permission של FutureUI
 * (com.future.futureui.permission.SYSTEM_SETTINGS) - בלי זה כל אפליקציה יכולה לפתוח אותו.
 */
class LockSettingsActivity : ComponentActivity() {
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // קוד ונתוני פנים - לא בצילומי מסך ולא בכרטיס של האחרונות
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val token = intent.getStringExtra(LockRemote.EXTRA_TOKEN)
        val ctx = applicationContext
        Thread {
            val ok = LockRemote.isValid(ctx, token)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (!ok) { finish(); return@runOnUiThread }
                setContent {
                    FutureUITheme {
                        LockEnrollScreen(modifier = Modifier.fillMaxSize(), onExit = { finish() })
                    }
                }
            }
        }.start()
    }

    override fun onStop() {
        super.onStop()
        // יציאה מהמסך מבטלת את הרישום; המצלמה לא נשארת פתוחה ברקע
        if (!isChangingConfigurations) finish()
    }
}
