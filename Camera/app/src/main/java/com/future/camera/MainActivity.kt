package com.future.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.future.sharednav.theme.ThemeClient
import com.future.camera.ui.CameraScreen
import com.future.sharednav.theme.FutureTheme

/** ערכת הנושא המשותפת (FutureUI) ממופה ל-FutureTheme - במקום אחד (CAM-23). */
private fun sharedFutureTheme(context: android.content.Context): FutureTheme {
    val shared = ThemeClient.getTheme(context)
    return FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
}

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var theme by remember { mutableStateOf(sharedFutureTheme(this@MainActivity)) }
            val scope = rememberCoroutineScope()

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        // CAM-09: קריאת ערכת הנושא מחוץ ל-main thread בכל חזרה לאפליקציה
                        scope.launch {
                            theme = withContext(Dispatchers.IO) { sharedFutureTheme(this@MainActivity) }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                CameraScreen(theme = theme, onExit = { finish() })
            }
        }
    }
}
