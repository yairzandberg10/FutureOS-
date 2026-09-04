package com.future.fitness

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.future.fitness.bluetooth.HeartRateMonitor
import com.future.fitness.data.WorkoutStore
import com.future.sharednav.theme.ThemeClient
import com.future.fitness.ui.FitnessNavHost
import com.future.sharednav.theme.FutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית, בדיוק כמו בשאר אפליקציות הסוויטה.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FitnessApp() }
    }
}

@Composable
private fun FitnessApp() {
    val context = LocalContext.current
    val store = remember { WorkoutStore(context) }
    val heartRateMonitor = remember { HeartRateMonitor(context) }

    var theme by remember {
        mutableStateOf(
            ThemeClient.getTheme(context).let {
                FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val shared = ThemeClient.getTheme(context)
                theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // חיבור אוטומטי לשעון/רצועת דופק שנשמרה מפעם קודמת - התאמה אישית שחוסכת
    // מהמשתמש לסרוק ולהתחבר מחדש בכל פתיחה, בדומה לאיך ששעונים אמיתיים
    // מתחברים אוטומטית לטלפון המשויך.
    LaunchedEffect(Unit) {
        val paired = store.getPairedDevice() ?: return@LaunchedEffect
        val hasPermission = if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
        if (hasPermission) heartRateMonitor.connect(paired.address)
    }

    DisposableEffect(Unit) {
        onDispose { heartRateMonitor.disconnect() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
        FitnessNavHost(store = store, heartRateMonitor = heartRateMonitor, theme = theme)
    }
}
