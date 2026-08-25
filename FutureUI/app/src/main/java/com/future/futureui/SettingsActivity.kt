package com.future.futureui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.future.futureui.settings.ui.FutureUISettingsScreen
import com.future.futureui.settings.ui.RemovePinConfirmScreen
import com.future.futureui.settings.ui.SetPinScreen
import com.future.futureui.ui.theme.FutureUITheme

private enum class SettingsScreen { Main, SetPin, RemovePin }

class SettingsActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FutureUITheme {
                var screen by remember { mutableStateOf(SettingsScreen.Main) }
                when (screen) {
                    SettingsScreen.Main -> FutureUISettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onSetPinClick = { screen = SettingsScreen.SetPin },
                        onRemovePinClick = { screen = SettingsScreen.RemovePin }
                    )
                    SettingsScreen.SetPin -> SetPinScreen(
                        onDone = { screen = SettingsScreen.Main },
                        onCancel = { screen = SettingsScreen.Main }
                    )
                    SettingsScreen.RemovePin -> {
                        val layoutManager = remember { com.future.futureui.lockscreen.logic.LockScreenLayoutManager(this) }
                        RemovePinConfirmScreen(
                            onConfirm = {
                                layoutManager.clearPin()
                                screen = SettingsScreen.Main
                            },
                            onCancel = { screen = SettingsScreen.Main }
                        )
                    }
                }
            }
        }
    }
}
