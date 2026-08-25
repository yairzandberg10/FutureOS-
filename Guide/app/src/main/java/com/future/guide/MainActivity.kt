package com.future.guide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.future.guide.data.findGuideApp
import com.future.guide.theme.ThemeClient
import com.future.guide.ui.GuideDetailScreen
import com.future.guide.ui.GuideHomeScreen
import com.future.guide.ui.GuideRoute
import com.future.guide.ui.theme.FutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var route by remember { mutableStateOf<GuideRoute>(GuideRoute.Home) }
            BackHandler(enabled = route != GuideRoute.Home) { route = GuideRoute.Home }

            var theme by remember {
                mutableStateOf(
                    ThemeClient.getTheme(this@MainActivity).let {
                        FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                    }
                )
            }

            // מרענן את העיצוב בכל חזרה למסך (למשל אחרי שינוי מצב כהה/בהיר או
            // צבע הדגשה באפליקציית ההגדרות) בלי לבנות מחדש את כל ה-Activity.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        val shared = ThemeClient.getTheme(this@MainActivity)
                        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                when (val currentRoute = route) {
                    GuideRoute.Home -> GuideHomeScreen(theme = theme, onOpen = { route = GuideRoute.Detail(it) })
                    is GuideRoute.Detail -> {
                        val app = findGuideApp(currentRoute.appId)
                        if (app != null) {
                            GuideDetailScreen(app = app, theme = theme, onBack = { route = GuideRoute.Home })
                        } else {
                            route = GuideRoute.Home
                        }
                    }
                }
            }
        }
    }
}
