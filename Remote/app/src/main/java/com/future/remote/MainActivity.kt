package com.future.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.future.remote.data.RemoteRepository
import com.future.remote.ui.AcRemoteScreen
import com.future.remote.ui.RemoteHomeScreen
import com.future.remote.ui.RemoteRoute
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.theme.FutureMaterialTheme
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
            var route by remember { mutableStateOf<RemoteRoute>(RemoteRoute.Home) }
            // עולה בכל חזרה למסך הראשי כדי שהרשימה תציג את המצב האחרון של כל שלט.
            var refreshKey by remember { mutableStateOf(0) }
            val goHome = { refreshKey++; route = RemoteRoute.Home }
            BackHandler(enabled = route != RemoteRoute.Home) { goHome() }

            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

            FutureMaterialTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                    AnimatedScreenHost(
                        targetState = route,
                        depthOf = { if (it == RemoteRoute.Home) 0 else 1 },
                    ) { current ->
                        when (current) {
                            RemoteRoute.Home -> RemoteHomeScreen(
                                theme = theme,
                                refreshKey = refreshKey,
                                onOpenRemote = { route = RemoteRoute.Ac(it) },
                            )
                            is RemoteRoute.Ac -> {
                                val context = LocalContext.current
                                val device = remember(current.deviceId) {
                                    RemoteRepository(context).loadDevices().firstOrNull { it.id == current.deviceId }
                                }
                                if (device?.acProtocol != null) {
                                    AcRemoteScreen(theme = theme, device = device, onBack = goHome)
                                } else {
                                    LaunchedEffect(Unit) { goHome() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
