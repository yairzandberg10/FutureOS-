package com.future.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.future.calculator.ui.CalculatorScreen
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.rememberFutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים - גם כשהשינוי
            // נעשה מהקונטרול סנטר שנפתח מעל המחשבון, בלי לצאת ממנו.
            val theme = rememberFutureTheme()

            BackHandler { finish() }

            FutureAppTheme(theme) {
                CalculatorScreen(theme = theme, onBack = { finish() })
            }
        }
    }
}
