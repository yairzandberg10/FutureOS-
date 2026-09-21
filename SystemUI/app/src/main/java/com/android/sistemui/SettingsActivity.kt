package com.android.sistemui

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
import com.android.sistemui.settings.ui.SystemUISettingsScreen
import com.android.sistemui.settings.ui.RemovePinConfirmScreen
import com.android.sistemui.settings.ui.SetPinScreen
import com.android.sistemui.ui.theme.SystemUITheme

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
            SystemUITheme {
                var screen by remember { mutableStateOf(SettingsScreen.Main) }
                when (screen) {
                    SettingsScreen.Main -> SystemUISettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onSetPinClick = { screen = SettingsScreen.SetPin },
                        onRemovePinClick = { screen = SettingsScreen.RemovePin }
                    )
                    SettingsScreen.SetPin -> SetPinScreen(
                        onDone = { screen = SettingsScreen.Main },
                        onCancel = { screen = SettingsScreen.Main }
                    )
                    SettingsScreen.RemovePin -> {
                        val layoutManager = remember { com.android.sistemui.lockscreen.logic.LockScreenLayoutManager(this) }
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
