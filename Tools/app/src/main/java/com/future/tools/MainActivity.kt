package com.future.tools

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
import com.future.tools.data.ToolShortcuts
import com.future.sharednav.theme.ThemeClient
import com.future.tools.ui.AngleRulerScreen
import com.future.tools.ui.CoinDiceScreen
import com.future.tools.ui.CompassScreen
import com.future.tools.ui.FlashlightScreen
import com.future.tools.ui.LevelScreen
import com.future.tools.ui.LuxMeterScreen
import com.future.tools.ui.NoiseMeterScreen
import com.future.tools.ui.PasswordGeneratorScreen
import com.future.tools.ui.PomodoroScreen
import com.future.tools.ui.QrScannerScreen
import com.future.tools.ui.QuickFinanceCalculatorScreen
import com.future.tools.ui.QuickNotesScreen
import com.future.tools.ui.RandomNumberScreen
import com.future.tools.ui.RandomPickerScreen
import com.future.tools.ui.TextScannerScreen
import com.future.tools.ui.TimeZoneConverterScreen
import com.future.tools.ui.TipSplitCalculatorScreen
import com.future.tools.ui.ToolRoute
import com.future.tools.ui.ToolsHomeScreen
import com.future.tools.ui.UnitConverterScreen
import com.future.tools.ui.VoiceTranscribeScreen
import com.future.sharednav.theme.FutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // אם ההפעלה הגיעה דרך activity-alias של כלי ספציפי (קיצור דרך שהתווסף
        // למסך הבית, ראו ToolShortcuts) - נכנסים ישר לאותו מסך במקום לרשימה
        // הראשית, וה"חוויה" היא של אפליקציה עצמאית ולא של מסך בתוך "כלים".
        val launchedToolRoute = ToolShortcuts.ROUTE_BY_ALIAS[intent.component?.className]
        val launchedAsShortcut = launchedToolRoute != null

        setContent {
            var route by remember { mutableStateOf(launchedToolRoute ?: ToolRoute.Home) }
            // נשמר גם אחרי route חוזר ל-Home - כדי שהפוקוס יחזור בדיוק לכלי
            // שממנו נכנסנו, לא תמיד לשורה הראשונה במסך הבית.
            var lastOpenedTool by remember { mutableStateOf<ToolRoute?>(null) }
            val goBack = { if (launchedAsShortcut) finish() else route = ToolRoute.Home }
            BackHandler(enabled = route != ToolRoute.Home || launchedAsShortcut) { goBack() }

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
                    ToolRoute.Home -> ToolsHomeScreen(theme = theme, onOpen = { lastOpenedTool = it; route = it }, lastOpenedRoute = lastOpenedTool)
                    ToolRoute.Flashlight -> FlashlightScreen(theme = theme, onBack = goBack)
                    ToolRoute.UnitConverter -> UnitConverterScreen(theme = theme, onBack = goBack)
                    ToolRoute.Compass -> CompassScreen(theme = theme, onBack = goBack)
                    ToolRoute.Level -> LevelScreen(theme = theme, onBack = goBack)
                    ToolRoute.NoiseMeter -> NoiseMeterScreen(theme = theme, onBack = goBack)
                    ToolRoute.LuxMeter -> LuxMeterScreen(theme = theme, onBack = goBack)
                    ToolRoute.AngleRuler -> AngleRulerScreen(theme = theme, onBack = goBack)
                    ToolRoute.TipSplitCalculator -> TipSplitCalculatorScreen(theme = theme, onBack = goBack)
                    ToolRoute.QuickFinanceCalculator -> QuickFinanceCalculatorScreen(theme = theme, onBack = goBack)
                    ToolRoute.TimeZoneConverter -> TimeZoneConverterScreen(theme = theme, onBack = goBack)
                    ToolRoute.QrScanner -> QrScannerScreen(theme = theme, onBack = goBack)
                    ToolRoute.Pomodoro -> PomodoroScreen(theme = theme, onBack = goBack)
                    ToolRoute.PasswordGenerator -> PasswordGeneratorScreen(theme = theme, onBack = goBack)
                    ToolRoute.QuickNotes -> QuickNotesScreen(theme = theme, onBack = goBack)
                    ToolRoute.CoinDice -> CoinDiceScreen(theme = theme, onBack = goBack)
                    ToolRoute.RandomPicker -> RandomPickerScreen(theme = theme, onBack = goBack)
                    ToolRoute.RandomNumber -> RandomNumberScreen(theme = theme, onBack = goBack)
                    ToolRoute.TextScanner -> TextScannerScreen(theme = theme, onBack = goBack)
                    ToolRoute.VoiceTranscribe -> VoiceTranscribeScreen(theme = theme, onBack = goBack)
                }
            }
        }
    }
}
