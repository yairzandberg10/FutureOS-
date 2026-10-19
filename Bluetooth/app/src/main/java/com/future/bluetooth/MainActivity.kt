package com.future.bluetooth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import com.future.bluetooth.data.BluetoothController
import com.future.bluetooth.ui.BluetoothActions
import com.future.bluetooth.ui.BluetoothApp
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.rememberFutureTheme
import java.io.File

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
            // הערכה מתעדכנת חי (ContentObserver) - גם כשמצב כהה/בהיר משתנה
            // ממרכז הבקרה שנפתח מעל האפליקציה.
            val theme = rememberFutureTheme()
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

            // ההרשאות ומצב הבלוטות' יכולים להשתנות בזמן שהאפליקציה ברקע
            // (הגדרות, שורת המצב) - מרעננים בכל חזרה למסך.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                // BT-17: הרסיבר חי רק כשהאפליקציה גלויה. ברקע הדלקת בלוטות' ממרכז הבקרה
                // לא מתחילה סריקה בלי מסך, והסריקה נעצרת כשיוצאים.
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_START -> controller.register()
                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                            hasPermission = hasRequiredPermissions()
                            if (hasPermission) controller.refreshState()
                        }
                        androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                            controller.stopDiscovery()
                            controller.unregister()
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(Unit) {
                if (!hasPermission) permissionLauncher.launch(requiredPermissions)
            }

            val actions = remember {
                BluetoothActions(
                    requestPermission = { permissionLauncher.launch(requiredPermissions) },
                    requestEnable = { enableLauncher.launch(controller.requestEnableIntent()) },
                    openSystemSettings = { openSystemBluetoothSettings() },
                    openReceivedFiles = { openReceivedFiles() },
                    openAppSettings = { openAppDetails() },
                )
            }

            FutureAppTheme(theme) {
                BluetoothApp(
                    controller = controller,
                    theme = theme,
                    hasPermission = hasPermission,
                    actions = actions,
                )
            }
        }
    }

    /** מסך ההגדרות של האפליקציה, שם מאשרים הרשאה שנדחתה לצמיתות. */
    private fun openAppDetails() {
        try {
            startActivity(
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", packageName, null),
                )
            )
        } catch (e: Exception) {
            // אין מסך הגדרות - אין לאן לפנות.
        }
    }

    private fun openSystemBluetoothSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (e: Exception) {
            // אין מסך הגדרות בלוטות' במכשיר - אין לאן לפנות.
        }
    }

    /**
     * קבצים שהתקבלו בבלוטות' נשמרים ב-Download/Bluetooth (או bluetooth בשורש
     * האחסון בחלק מהמכשירים). אם התיקייה קיימת נפתחת אפליקציית הקבצים; אם לא -
     * false, ואין מה להראות.
     */
    /** null כשאפליקציית הקבצים נפתחה, אחרת ההודעה שמסבירה למה לא. */
    private fun openReceivedFiles(): String? {
        val root = Environment.getExternalStorageDirectory()
        val folders = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bluetooth"),
            File(root, "bluetooth"),
        )
        // BT-10: אפליקציית קבצים חסרה היא לא "אין קבצים" - כל מצב מקבל הודעה משלו.
        val launch = packageManager.getLaunchIntentForPackage(FILES_PACKAGE) ?: return "אפליקציית הקבצים לא מותקנת"
        if (folders.none { it.isDirectory }) return "אין קבצים שהתקבלו"
        startActivity(launch)
        return null
    }

    private companion object {
        const val FILES_PACKAGE = "com.future.files"
    }
}
