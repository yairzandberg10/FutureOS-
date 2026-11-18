package com.future.bluetooth

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.future.sharednav.i18n.tr
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.future.bluetooth.data.BluetoothController
import com.future.bluetooth.data.TransferManager
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
        if (savedInstanceState == null) handleShare(intent)

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
            // תוצאת דיאלוג הנראות: resultCode הוא מספר השניות שאושרו (0 = בוטל).
            val discoverableLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { controller.onDiscoverableResult(it.resultCode) }
            fun launchDiscoverable() {
                try {
                    discoverableLauncher.launch(controller.requestDiscoverableIntent(DISCOVERABLE_SECONDS))
                } catch (e: ActivityNotFoundException) {
                    openSystemBluetoothSettings()
                } catch (e: SecurityException) {
                    openSystemBluetoothSettings()
                }
            }
            // ההרשאה מבוקשת רק כשלוחצים על "גלוי" - היא באותה קבוצה ("מכשירים בקרבת
            // מקום") כמו SCAN/CONNECT, ולכן בדרך כלל מאושרת בלי דיאלוג.
            val advertiseLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) launchDiscoverable() else openSystemBluetoothSettings() }
            val pickLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val data = result.data
                val uris = if (result.resultCode == android.app.Activity.RESULT_OK && data != null) urisOf(data) else emptyList()
                if (uris.isEmpty()) TransferManager.setPickTarget(null, null)
                else TransferManager.onFilesChosen(this@MainActivity, uris)
            }

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

            // NEW-04: בקשת ההרשאות רק פעם אחת - יצירה מחדש של הפעילות לא שולחת אותה שוב.
            var permissionAsked by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (!hasPermission && !permissionAsked) {
                    permissionAsked = true
                    permissionLauncher.launch(requiredPermissions)
                }
            }

            val actions = remember {
                BluetoothActions(
                    requestPermission = { permissionLauncher.launch(requiredPermissions) },
                    requestEnable = { enableLauncher.launch(controller.requestEnableIntent()) },
                    openSystemSettings = { openSystemBluetoothSettings() },
                    openAppSettings = { openAppDetails() },
                    pickFiles = {
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                            .setType("*/*")
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        // אפליקציית הקבצים של המערכת עובדת עם המקשים; בלעדיה - כל בורר.
                        val files = Intent(intent).setPackage(FILES_PACKAGE)
                        val target = if (files.resolveActivity(packageManager) != null) files else intent
                        try {
                            pickLauncher.launch(target)
                            true
                        } catch (e: ActivityNotFoundException) {
                            false
                        }
                    },
                    requestDiscoverable = {
                        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                            launchDiscoverable()
                        } else {
                            advertiseLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
                        }
                    },
                    openFile = { openFile(it) },
                    shareFile = { shareFile(it) },
                    grantFilesAccess = { openAllFilesAccess() },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    /**
     * "שתף > בלוטוס" מאפליקציה אחרת: הקבצים ממתינים, ונפתח מסך בחירת
     * המכשיר. רק בהפעלה הראשונה של ה-Intent, לא אחרי יצירה מחדש של הפעילות.
     */
    private fun handleShare(intent: Intent?) {
        intent ?: return
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return
        val uris = urisOf(intent)
        if (uris.isEmpty()) return
        TransferManager.setPickTarget(null, null)
        TransferManager.onFilesChosen(this, uris)
    }

    /** הקבצים מ-Intent של שיתוף או של בורר: EXTRA_STREAM, ClipData או data. */
    @Suppress("DEPRECATION")
    private fun urisOf(intent: Intent): List<android.net.Uri> {
        val out = ArrayList<android.net.Uri>()
        runCatching { intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let { out.addAll(it) } }
        runCatching { intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let { out.add(it) } }
        intent.clipData?.let { clip -> for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { out.add(it) } }
        intent.data?.let { out.add(it) }
        return out.distinct()
    }

    /** פתיחת קובץ שהתקבל באפליקציה שמתאימה לסוג שלו. null כשנפתח. */
    private fun openFile(file: File): String? {
        return try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
            null
        } catch (e: ActivityNotFoundException) {
            tr("אין אפליקציה שפותחת את הקובץ")
        } catch (e: IllegalArgumentException) {
            tr("לא ניתן לפתוח את הקובץ")
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
            val send = Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData = android.content.ClipData.newRawUri(file.name, uri)
            com.future.sharednav.share.FutureShare.open(this, send, tr("שיתוף קובץ"))
        } catch (e: Exception) {
            // אין חלון שיתוף - אין מה לעשות.
        }
    }

    /** "גישה לכל הקבצים" - בלעדיה קבצים שאינם מדיה שהתקבלו לא נראים. */
    private fun openAllFilesAccess() {
        try {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, android.net.Uri.fromParts("package", packageName, null))
            )
        } catch (e: ActivityNotFoundException) {
            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
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

    /** false כשאין מסך הגדרות בלוטות' במכשיר - אז ההודעה למשתמש צריכה להיות אחרת. */
    private fun openSystemBluetoothSettings(): Boolean = try {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private companion object {
        const val FILES_PACKAGE = "com.future.files"
        /** כמה זמן הטלפון גלוי אחרי אישור (כמו במסך הבלוטות' של אנדרואיד). */
        const val DISCOVERABLE_SECONDS = 120
    }
}
