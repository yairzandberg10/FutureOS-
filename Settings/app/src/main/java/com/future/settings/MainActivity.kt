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
                        SettingsApp(viewModel)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(theme.backgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = theme.primaryColor, strokeWidth = 3.dp)
                        }
                    }
                }
            }
        }
    }
}
