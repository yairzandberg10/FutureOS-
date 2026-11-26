package com.android.sistemui.lockscreen.settings

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.android.sistemui.ui.theme.FutureUITheme

/**
 * רישום פנים למסך הנעילה. כל שאר ההגדרות באפליקציית ההגדרות (מסך הנעילה), שפותחת
 * את המסך הזה ב-ACTION_LOCK_SETTINGS עם האסימון שקיבלה מ-LockRemote אחרי הזנת
 * הקוד. בלי אסימון תקף, או בלי קוד מוגדר (הוא הגיבוי לפנים), המסך נסגר מיד.
 */
class LockSettingsActivity : ComponentActivity() {
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // קוד ונתוני פנים - לא בצילומי מסך ולא בכרטיס של האחרונות
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val settings = com.android.sistemui.lockscreen.logic.LockSettings(this)
        if (!settings.pin.hasPin() || !LockRemote.isValid(intent.getStringExtra(LockRemote.EXTRA_TOKEN))) {
            finish()
            return
        }
        setContent {
            FutureUITheme {
                LockEnrollScreen(modifier = Modifier.fillMaxSize(), onExit = { finish() })
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // יציאה מהמסך מבטלת את הרישום; המצלמה לא נשארת פתוחה ברקע. יציאה שלא
        // דרך סיום הרישום (בית, כיבוי מסך) מבטלת גם את הכניסה בקוד בהגדרות -
        // אחרת ההגדרות היו נפתחות בלי קוד אחרי חזרה.
        if (!isChangingConfigurations) {
            if (!isFinishing) LockRemote.end()
            finish()
        }
    }
}
