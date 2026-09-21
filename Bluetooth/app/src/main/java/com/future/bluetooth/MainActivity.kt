package com.future.bluetooth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.future.bluetooth.data.BluetoothController
import com.future.bluetooth.ui.BluetoothScreen
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.ThemeClient

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית (זהה לכל שאר האפליקציות בסוויטה).
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )

    private fun hasRequiredPermissions(): Boolean =
        requiredPermissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var theme by remember {
                mutableStateOf(
                    ThemeClient.getTheme(this@MainActivity).let {
                        FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                    }
                )
            }
            var hasPermission by remember { mutableStateOf(hasRequiredPermissions()) }
            val controller = remember { BluetoothController(this@MainActivity) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                hasPermission = result.values.all { it }
                if (hasPermission) controller.refreshState()
            }
            val enableLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { controller.refreshState() }

            // מרענן את העיצוב, ההרשאות ומצב הבלוטוס בכל חזרה למסך - כל השלושה
            // יכולים להשתנות בזמן שהאפליקציה ברקע (הגדרות עיצוב, הרשאות,
            // כיבוי/הפעלת בלוטוס משורת המצב).
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        val shared = ThemeClient.getTheme(this@MainActivity)
                        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                        hasPermission = hasRequiredPermissions()
                        if (hasPermission) controller.refreshState()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            DisposableEffect(Unit) {
                controller.register()
                onDispose { controller.unregister() }
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) controller.refreshState()
            }

            BluetoothScreen(
                theme = theme,
                isSupported = controller.isSupported(),
                hasPermission = hasPermission,
                isEnabled = controller.isEnabled,
                isScanning = controller.isScanning,
                pairedDevices = controller.pairedDevices,
                discoveredDevices = controller.discoveredDevices,
                onRequestPermission = { permissionLauncher.launch(requiredPermissions) },
                onEnableBluetooth = { enableLauncher.launch(controller.requestEnableIntent()) },
                onScan = { controller.startDiscovery() },
                onStopScan = { controller.stopDiscovery() },
                onPair = { address -> controller.pair(address) },
                onUnpair = { address -> controller.unpair(address) },
            )
        }
    }
}
