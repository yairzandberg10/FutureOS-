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
import com.future.remote.ui.AcPresetsScreen
import com.future.remote.ui.AddButtonScreen
import com.future.remote.ui.AddDeviceScreen
import com.future.remote.ui.DeviceScreen
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
            // עולה בכל שינוי שנשמר ל-Repository כדי שהמסכים ירעננו את הרשימה
            // בלי צורך במנגנון תצפית מלא (ViewModel/Flow) לאפליקציה כה קטנה.
            var refreshKey by remember { mutableStateOf(0) }
            val goBack = { route = RemoteRoute.Home }
            BackHandler(enabled = route != RemoteRoute.Home) {
                route = when (val current = route) {
                    is RemoteRoute.AddButton -> RemoteRoute.Device(current.deviceId)
                    else -> RemoteRoute.Home
                }
            }

            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

            FutureMaterialTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                    // בית 0 -> מכשיר/הוספה 1 -> הוספת כפתור 2.
                    AnimatedScreenHost(
                        targetState = route,
                        depthOf = {
                            when (it) {
                                RemoteRoute.Home -> 0
                                is RemoteRoute.AddButton -> 2
                                else -> 1
                            }
                        },
                    ) { current ->
                        when (current) {
                            RemoteRoute.Home -> RemoteHomeScreen(
                                theme = theme,
                                refreshKey = refreshKey,
                                onOpenDevice = { route = RemoteRoute.Device(it.id) },
                                onAddDevice = { route = RemoteRoute.AddDevice },
                                onAddAcPreset = { route = RemoteRoute.AcPresets }
                            )
                            RemoteRoute.AddDevice -> AddDeviceScreen(
                                theme = theme,
                                onBack = goBack,
                                onSaved = { refreshKey++; route = RemoteRoute.Home }
                            )
                            RemoteRoute.AcPresets -> AcPresetsScreen(
                                theme = theme,
                                onBack = goBack,
                                onDeviceCreated = { deviceId -> refreshKey++; route = RemoteRoute.Device(deviceId) }
                            )
                            is RemoteRoute.Device -> DeviceScreen(
                                theme = theme,
                                deviceId = current.deviceId,
                                refreshKey = refreshKey,
                                onBack = goBack,
                                onAddButton = { route = RemoteRoute.AddButton(current.deviceId) }
                            )
                            is RemoteRoute.AddButton -> AddButtonScreen(
                                theme = theme,
                                deviceId = current.deviceId,
                                onBack = { route = RemoteRoute.Device(current.deviceId) },
                                onSaved = { refreshKey++; route = RemoteRoute.Device(current.deviceId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
