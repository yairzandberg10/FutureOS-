package com.future.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.settings.ui.SettingsApp
import com.future.settings.ui.theme.SettingsTheme
import com.future.settings.utils.SystemInteractor
import com.future.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // הענקת ההרשאות המיוחדות (WRITE_SETTINGS וכו') דרך root בכל עלייה של
        // האפליקציה - בלי זה, שינוי בהירות/עוצמת שמע נכשל בשקט במכשירים חדשים.
        lifecycleScope.launch(Dispatchers.IO) {
            SystemInteractor(applicationContext).initSystemPermissions()
        }

        // סנכרון רקע מול שרת FutureOS (משוב, תשובות, בדיקת עדכונים). לא עושה כלום בלי כתובת שרת
        com.future.settings.futureserver.FutureServerSyncJob.schedule(applicationContext)

        // נפתח מ"שלח משוב" של אפליקציה אחרת, ממרכז הבקרה או מהתראה: מתחילים ישר במסך המשוב.
        // ה-extras לא אמינים (ה-Activity חשופה): החבילה נבדקת מול תבנית, ו-crashId רק בוחר דוח
        // שכבר שמור אצלנו ב-CrashStore. שום דבר לא נשלח בלי תצוגה מקדימה ולחיצה על "שלח".
        val openFeedback = intent?.action == ACTION_SEND_FEEDBACK
        val openUpdates = intent?.action == ACTION_OPEN_UPDATES
        // התראת אבטחה מ-FutureUI (ThreatWatcher) פותחת ישר את מרכז האבטחה. בלי extras - רק ניווט
        val openSecurity = intent?.action == ACTION_OPEN_SECURITY
        val feedbackPkg = intent?.getStringExtra(EXTRA_FEEDBACK_PKG)?.takeIf { openFeedback && PKG_RE.matches(it) }
        val crashId = intent?.getStringExtra(EXTRA_CRASH_ID)?.takeIf { openFeedback && CRASH_ID_RE.matches(it) }
        val openHistory = openFeedback && intent?.getBooleanExtra(EXTRA_OPEN_HISTORY, false) == true

        setContent {
            val viewModel: SettingsViewModel = viewModel()
            val theme by viewModel.themeConfig
            val isReady by viewModel.isReady

            SettingsTheme(
                darkTheme = theme.isDarkMode,
                primaryColor = theme.primaryColor,
                backgroundColor = theme.backgroundColor
            ) {
                // Immediate background to avoid any flicker
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = theme.backgroundColor
                ) {
                    // System bar management
                    val isLight = theme.backgroundColor.luminance() > 0.5f
                    LaunchedEffect(theme.backgroundColor, theme.isDarkMode) {
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = isLight
                            isAppearanceLightNavigationBars = isLight
                        }
                    }

                    if (isReady) {
                        SettingsApp(
                            viewModel,
                            startRoute = when {
                                openFeedback -> com.future.settings.ui.Screen.Feedback.route
                                openUpdates -> com.future.settings.ui.Screen.Updates.route
                                openSecurity -> com.future.settings.ui.Screen.Security.route
                                else -> com.future.settings.ui.Screen.Main.route
                            },
                            feedbackPkg = feedbackPkg,
                            crashId = crashId,
                            openFeedbackHistory = openHistory,
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(theme.backgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            com.future.sharednav.components.FutureSpinner(theme = theme.futureTheme)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_SEND_FEEDBACK = "com.future.settings.action.SEND_FEEDBACK"
        const val EXTRA_FEEDBACK_PKG = "com.future.settings.extra.FEEDBACK_PKG"
        const val EXTRA_CRASH_ID = "com.future.settings.extra.CRASH_ID"
        const val EXTRA_OPEN_HISTORY = "com.future.settings.extra.OPEN_HISTORY"
        /** פנימי: התראת "יש עדכונים" פותחת ישר את מסך העדכונים (Intent מפורש, בלי intent-filter). */
        const val ACTION_OPEN_UPDATES = "com.future.settings.action.OPEN_UPDATES"
        /** התראת אבטחה של FutureUI. חייב להיות זהה ל-ThreatWatcher.ACTION_OPEN_SECURITY. */
        const val ACTION_OPEN_SECURITY = "com.future.settings.action.OPEN_SECURITY"
        private val PKG_RE = Regex("""^com\.future\.[a-z0-9_.]{1,64}$""")
        private val CRASH_ID_RE = Regex("""^[0-9a-f-]{36}$""")
    }
}
