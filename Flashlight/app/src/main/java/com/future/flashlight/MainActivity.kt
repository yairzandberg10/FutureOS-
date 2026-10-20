package com.future.flashlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.future.flashlight.ui.FlashlightScreen
import com.future.sharednav.theme.rememberFutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית (זהה לכל שאר האפליקציות בסוויטה).
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // העיצוב המשותף מתעדכן בזמן אמת (ContentObserver), בלי קריאה סינכרונית
            // ל-main thread בכל חזרה למסך (FL-05/FL-06/FL-26).
            val theme = rememberFutureTheme()
            FlashlightScreen(theme = theme)
        }
    }
}
