package com.future.clock
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AvTimer

import com.future.sharednav.icons.FutureIcons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.future.clock.data.ClockShortcuts
import com.future.sharednav.components.FutureBottomNav
import com.future.sharednav.components.FutureNavItem
import com.future.clock.ui.AlarmScreen
import com.future.clock.ui.ClockRoute
import com.future.clock.ui.StopwatchScreen
import com.future.clock.ui.TimerScreen
import com.future.clock.ui.WorldClockScreen
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureTheme

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
            // אין יותר לשונית "שעון" (השעה עצמה כבר בשורת המצב) - הלשונית
            // הראשונה היא המעוררים, ו-BACK ממנה יוצא מהאפליקציה.
            var route by remember { mutableStateOf(launchedClockRoute ?: ClockRoute.Alarms) }
            val goBack = { if (launchedAsShortcut) finish() else route = ClockRoute.Alarms }
            BackHandler(enabled = route != ClockRoute.Alarms || launchedAsShortcut) { goBack() }
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

            // 5 טאבים קבועים בסרגל תחתון (בדיוק כמו בחייגן/במוזיקה) במקום מסך
            // הבית הישן שהיה רשימה גוללת בלבד - ניווט בין שעונים מעוררים/עולמי/
            // עצר/טיימר לא דרש בעבר יותר מלחיצה אחת חזרה להום ואז שוב פנימה.
            val tabs = listOf(
                Triple(ClockRoute.Alarms, "מעוררים", FutureIcons.Alarm),
                Triple(ClockRoute.WorldClock, "עולמי", FutureIcons.Public),
                Triple(ClockRoute.Stopwatch, "עצר", Icons.Rounded.AvTimer),
                Triple(ClockRoute.Timer, "טיימר", FutureIcons.Timer),
            )
            val currentTabIndex = tabs.indexOfFirst { it.first == route }.coerceAtLeast(0)

            FutureAppTheme(theme) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        // חצים ימינה/שמאלה עוברים בין הטאבים מכל מקום במסך (בדיוק
                        // כמו בחייגן) - לא רק כשהסרגל התחתון עצמו ממוקד, כי אין
                        // מסך מגע ומעבר טאבים הוא פעולה תכופה שכדאי שתהיה נגישה מיד.
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val direction = when (event.key) {
                                Key.DirectionRight -> androidx.compose.ui.focus.FocusDirection.Right
                                Key.DirectionLeft -> androidx.compose.ui.focus.FocusDirection.Left
                                else -> return@onKeyEvent false
                            }
                            // קודם הפוקוס זז בתוך המסך (למשל מ"התחל" ל"איפוס" בשעון
                            // העצר) - רק כשאין לאן לזוז בכיוון הזה עוברים לשונית.
                            if (focusManager.moveFocus(direction)) return@onKeyEvent true
                            val nextIndex = if (event.key == Key.DirectionRight) currentTabIndex - 1 else currentTabIndex + 1
                            if (nextIndex !in tabs.indices) return@onKeyEvent false
                            route = tabs[nextIndex].first
                            true
                        },
                    containerColor = theme.backgroundColor,
                    bottomBar = {
                        // הסרגל המשותף (FutureBottomNav) ולא NavigationBar של
                        // Material3: פס מרחף בצורת גלולה, ותווית רק על הנבחר.
                        FutureBottomNav(
                            items = tabs.map { (_, label, icon) ->
                                FutureNavItem(label = label, icon = icon)
                            },
                            selectedIndex = currentTabIndex,
                            theme = theme,
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        color = theme.backgroundColor,
                    ) {
                        // כל הטאבים באותה רמה - המעבר ביניהם הוא fade ולא החלקה.
                        AnimatedScreenHost(targetState = route, depthOf = { 0 }) { shown ->
                            when (shown) {
                                // הלשונית הראשונה - BACK ממנה יוצא מהאפליקציה.
                                ClockRoute.Alarms -> AlarmScreen(theme = theme, onBack = { finish() })
                                ClockRoute.WorldClock -> WorldClockScreen(theme = theme, onBack = goBack)
                                ClockRoute.Stopwatch -> StopwatchScreen(theme = theme, onBack = goBack)
                                ClockRoute.Timer -> TimerScreen(theme = theme, onBack = goBack)
                            }
                        }
                    }
                }
            }
        }
    }
}
