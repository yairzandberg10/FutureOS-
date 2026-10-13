package com.future.recorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.future.recorder.ui.PlayerScreen
import com.future.recorder.ui.RecorderHomeScreen
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.systemui.StatusBarInset
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.rememberFutureTheme
import java.io.File

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית (זהה לכל שאר האפליקציות בסוויטה).
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // null = המסך הראשי; נתיב = נגן ההקלטה הזו. נשמר כמחרוזת כדי לשרוד יצירה מחדש.
            var playingPath by rememberSaveable { mutableStateOf<String?>(null) }
            val playing = playingPath?.let { File(it) }
            BackHandler(enabled = playing != null) { playingPath = null }
            val theme = rememberFutureTheme()

            FutureMaterialTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                    AnimatedScreenHost(
                        targetState = playing,
                        depthOf = { if (it == null) 0 else 1 },
                        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
                    ) { file ->
                        if (file == null) {
                            RecorderHomeScreen(theme = theme, onPlay = { playingPath = it.path })
                        } else {
                            PlayerScreen(theme = theme, file = file, onBack = { playingPath = null })
                        }
                    }
                }
            }
        }
    }
}
