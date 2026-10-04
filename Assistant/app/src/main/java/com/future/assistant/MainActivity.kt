package com.future.assistant

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.future.assistant.asr.LocalSpeechEngine
import com.future.assistant.ui.AssistantScreen
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.rememberFutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    // מקש "חזור" חייב לסגור את העוזר - בלי override מפורש כאן, מסך העוזר
    // הקולי (activity ייעודי, לא route בתוך אפליקציה אחרת) עלול להישאר פתוח.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // טעינת מודל זיהוי הדיבור מתחילה לפני שהחלון בכלל מצויר, במקביל לפתיחה.
        LocalSpeechEngine.preloadAsync(this)
        enableEdgeToEdge()

        setContent {
            val theme = rememberFutureTheme()
            FutureMaterialTheme(theme) {
                AssistantScreen(theme = theme, onExit = { finish() })
            }
        }
    }
}
