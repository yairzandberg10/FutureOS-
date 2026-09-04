package com.future.clock

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
import com.future.clock.data.ClockShortcuts
import com.future.sharednav.theme.ThemeClient
import com.future.clock.ui.ClockHomeScreen
import com.future.clock.ui.ClockRoute
import com.future.clock.ui.StopwatchScreen
import com.future.clock.ui.TimerScreen
import com.future.clock.ui.AlarmScreen
import com.future.clock.ui.WorldClockScreen
import com.future.sharednav.theme.FutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // אם ההפעלה הגיעה דרך activity-alias של שעון עצר/טיימר (קיצור דרך
        // שהתווסף למסך הבית, ראו ClockShortcuts) - נכנסים ישר לאותו מסך במקום
        // לרשימה הראשית, וה"חוויה" היא של אפליקציה עצמאית ולא של מסך בתוך "שעון".
        val launchedClockRoute = ClockShortcuts.ROUTE_BY_ALIAS[intent.component?.className]
        val launchedAsShortcut = launchedClockRoute != null

        setContent {
            var route by remember { mutableStateOf(launchedClockRoute ?: ClockRoute.Home) }
            val goBack = { if (launchedAsShortcut) finish() else route = ClockRoute.Home }
            BackHandler(enabled = route != ClockRoute.Home || launchedAsShortcut) { goBack() }

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
                when (route) {
                    ClockRoute.Home -> ClockHomeScreen(theme = theme, onOpen = { route = it })
                    ClockRoute.Alarms -> AlarmScreen(theme = theme, onBack = goBack)
                    ClockRoute.WorldClock -> WorldClockScreen(theme = theme, onBack = goBack)
                    ClockRoute.Stopwatch -> StopwatchScreen(theme = theme, onBack = goBack)
                    ClockRoute.Timer -> TimerScreen(theme = theme, onBack = goBack)
                }
            }
        }
    }
}
