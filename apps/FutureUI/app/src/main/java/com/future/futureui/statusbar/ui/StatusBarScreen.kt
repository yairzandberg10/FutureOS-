package com.future.futureui.statusbar.ui

import android.app.AlarmManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telecom.TelecomManager
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.controlcenter.service.MediaControlService
import com.future.futureui.statusbar.logic.PrivacyIndicators
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
import com.future.futureui.utils.FutureUIState
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureAccents
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * שורת המצב של FutureOS, בפריסה של אנדרואיד רגיל:
 *
 *   [שעה] [אייקוני התראות ...]                [מערכת] [Wi-Fi] [קליטה] [סוללה]
 *
 * בעברית (RTL) השורה מתהפכת, בדיוק כמו באנדרואיד. רקע שחור אטום וטקסט לבן -
 * תמיד, בלי לדגום את המסך ובלי סגנונות: שורת המערכת של אנדרואיד עדיין מצוירת
 * מתחת לשלנו (policy_control לא עובד ב-Android 12), אז רקע שקוף/צבעוני היה
 * חושף אותה. מעל השכבות של FutureUI שמכסות את כל המסך (נעילה, מרכז בקרה,
 * התראות, אחרונות, תפריט כיבוי) הרקע שקוף, כי הן מציירות את הרקע בעצמן.
 *
 * כל המצבים מגיעים מאירועי מערכת (broadcast, ContentObserver, TelephonyCallback,
 * NetworkCallback); הסקר היחיד הוא בדיקת שיחה פעילה, שאין לה אירוע נגיש.
 */
@Composable
fun StatusBarScreen(
    modifier: Modifier = Modifier,
    controlManager: ControlManager? = null,
    layoutManager: StatusBarLayoutManager? = null,
) {
    val context = LocalContext.current
    val manager = controlManager ?: remember { ControlManager(context) }
    val layout = layoutManager ?: remember { StatusBarLayoutManager(context) }

    // ההגדרות נכתבות מאפליקציית ההגדרות לאותם prefs - מאזין אחד כדי שיחולו מיד
    var prefsTick by remember { mutableIntStateOf(0) }
    DisposableEffect(layout) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> prefsTick++ }
        layout.registerListener(listener)
        onDispose { layout.unregisterListener(listener) }
    }
    @Suppress("UNUSED_VARIABLE") val tick = prefsTick
    val showPercent = layout.getShowBattery()
    val showBluetooth = layout.getShowBluetooth()
    val use24Hour = layout.getUse24HourClock()

    // הערכים ההתחלתיים מהעותק האחרון: החלון מוסר ונוסף מחדש כדי לעלות לחזית
    // (bringStatusBarToFront), וההרכבה מתחילה מאפס - בלי זה האינדיקטורים מבהבים.
    var time by remember { mutableStateOf("") }
    var battery by remember { mutableIntStateOf(BarMemory.battery) }
    var charging by remember { mutableStateOf(BarMemory.charging) }
    var ringerMode by remember { mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL) }
    var hasAlarm by remember { mutableStateOf(false) }
    var notifApps by remember { mutableStateOf(BarMemory.notifApps) }
    var cellLevel by remember { mutableIntStateOf(BarMemory.cellLevel) }
    var inService by remember { mutableStateOf(BarMemory.inService) }
    var simReady by remember { mutableStateOf(BarMemory.simReady) }
    var wifiConnected by remember { mutableStateOf(BarMemory.wifiConnected) }
    var wifiLevel by remember { mutableIntStateOf(BarMemory.wifiLevel) }
    var callActive by remember { mutableStateOf(BarMemory.callActive) }
    var callSeconds by remember { mutableIntStateOf(0) }
    val smallIconCache = remember { HashMap<String, ImageBitmap?>() }

    // ---- שעון, סוללה, צלצול, מנורה, ומצבי הרדיו (Wi-Fi/Bluetooth/טיסה) ----
    DisposableEffect(use24Hour) {
        val timeFormat = SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm", Locale.getDefault())
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        fun refreshTime() { time = timeFormat.format(Date()) }
        fun refreshRinger() {
            ringerMode = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            hasAlarm = runCatching { alarms?.nextAlarmClock != null }.getOrDefault(false)
        }
        fun refreshWifiLevel() {
            @Suppress("DEPRECATION")
            val rssi = runCatching { wifi?.connectionInfo?.rssi }.getOrNull() ?: return
            wifiLevel = runCatching { wifi?.calculateSignalLevel(rssi) ?: 0 }.getOrDefault(0)
                .let { lvl -> (lvl * 4 / ((wifi?.maxSignalLevel ?: 4).coerceAtLeast(1))).coerceIn(0, 4) }
            BarMemory.wifiLevel = wifiLevel
        }
        fun refreshSim() {
            simReady = runCatching { telephony?.simState == TelephonyManager.SIM_STATE_READY }.getOrDefault(false)
            BarMemory.simReady = simReady
        }
        fun applyBattery(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) battery = level * 100 / scale
            val st = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            BarMemory.battery = battery
            BarMemory.charging = charging
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> applyBattery(intent)
                    Intent.ACTION_TIMEZONE_CHANGED -> { timeFormat.timeZone = TimeZone.getDefault(); refreshTime() }
                    Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED -> refreshTime()
                    WifiManager.RSSI_CHANGED_ACTION -> refreshWifiLevel()
                    "android.telephony.action.SIM_CARD_STATE_CHANGED", "android.intent.action.SIM_STATE_CHANGED" -> refreshSim()
                    else -> { refreshRinger(); manager.updateStates() }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction("android.telephony.action.SIM_CARD_STATE_CHANGED")
            addAction("android.intent.action.SIM_STATE_CHANGED")
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { manager.updateStates() }
        }

        refreshTime()
        refreshRinger()
        refreshWifiLevel()
        refreshSim()
        manager.updateStates()
        applyBattery(context.registerReceiver(receiver, filter))
        runCatching {
            context.contentResolver.registerContentObserver(Settings.Global.getUriFor("mobile_data"), false, observer)
            context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), false, observer)
        }.onFailure { Log.w(TAG, "settings observers failed", it) }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { context.contentResolver.unregisterContentObserver(observer) }
        }
    }

    // ---- קליטה סלולרית: עוצמה אמיתית (0-4) ומצב שירות, מ-TelephonyCallback ----
    DisposableEffect(Unit) {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val callback = object : TelephonyCallback(),
            TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.ServiceStateListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                cellLevel = signalStrength.level.coerceIn(0, 4)
                BarMemory.cellLevel = cellLevel
            }
            override fun onServiceStateChanged(serviceState: ServiceState) {
                inService = serviceState.state == ServiceState.STATE_IN_SERVICE
                BarMemory.inService = inService
            }
        }
        val registered = runCatching {
            telephony?.registerTelephonyCallback(context.mainExecutor, callback)
            telephony != null
        }.onFailure { Log.w(TAG, "telephony callback failed", it) }.getOrDefault(false)
        onDispose { if (registered) runCatching { telephony?.unregisterTelephonyCallback(callback) } }
    }

    // ---- Wi-Fi מחובר (ולא רק דלוק) - כמו באנדרואיד, האייקון מופיע רק כשיש חיבור ----
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        fun refresh() {
            val caps = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
            // כל רשת Wi-Fi פעילה, לא רק ברירת המחדל (בזמן מעבר בין רשתות)
            val anyWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                runCatching {
                    @Suppress("DEPRECATION")
                    cm?.allNetworks?.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
                }.getOrNull() == true
            Handler(Looper.getMainLooper()).post {
                wifiConnected = anyWifi
                BarMemory.wifiConnected = anyWifi
            }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refresh()
        }
        val ok = runCatching { cm?.registerDefaultNetworkCallback(callback); cm != null }
            .onFailure { Log.w(TAG, "network callback failed", it) }.getOrDefault(false)
        refresh()
        onDispose { if (ok) runCatching { cm?.unregisterNetworkCallback(callback) } }
    }

    // ---- חיווי מצלמה/מיקרופון ----
    DisposableEffect(Unit) {
        PrivacyIndicators.start(context)
        onDispose { PrivacyIndicators.stop() }
    }

    // ---- אייקוני ההתראות (thread רקע; מתרענן בכל הוספה/הסרה) ----
    val notificationTick = FutureUIState.notificationTick.intValue
    LaunchedEffect(notificationTick) {
        val apps = withContext(Dispatchers.IO) {
            val active = if (MediaControlService.isEnabled(context)) {
                runCatching { MediaControlService.instance?.activeNotifications }.getOrNull() ?: emptyArray()
            } else emptyArray()
            active
                .filter { !it.isOngoing && it.packageName != context.packageName }
                .sortedByDescending { it.postTime }
                .distinctBy { it.packageName }
                .map { sbn ->
                    val pkg = sbn.packageName
                    val icon = if (appGlyph(pkg) != null) null else smallIconCache.getOrPut(pkg) {
                        runCatching {
                            sbn.notification.smallIcon?.loadDrawable(context)?.toBitmap(48, 48)?.asImageBitmap()
                        }.getOrNull()
                    }
                    NotifApp(pkg, icon)
                }
        }
        notifApps = apps
        BarMemory.notifApps = apps
    }

    // ---- שיחה פעילה: אין לה אירוע נגיש, נבדקת כל 2 שניות ב-thread רקע ----
    LaunchedEffect(Unit) {
        while (true) {
            val inCall = withContext(Dispatchers.IO) {
                runCatching { (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.isInCall == true }
                    .getOrDefault(false)
            }
            callActive = inCall
            BarMemory.callActive = inCall
            delay(2_000)
        }
    }
    LaunchedEffect(callActive) {
        callSeconds = 0
        if (!callActive) { BarMemory.callStart = 0L; return@LaunchedEffect }
        if (BarMemory.callStart == 0L) BarMemory.callStart = SystemClock.elapsedRealtime()
        while (true) {
            callSeconds = ((SystemClock.elapsedRealtime() - BarMemory.callStart) / 1000).toInt()
            delay(1_000)
        }
    }

    // מעל שכבות מסך-מלא של FutureUI השורה שקופה; בכל מקום אחר - שחורה אטומה.
    val overOverlay = FutureUIState.isLocked ||
        FutureUIState.controlCenterOpen.value ||
        FutureUIState.notificationCenterOpen.value ||
        FutureUIState.recentsOpen.value ||
        FutureUIState.powerMenuOpen.value
    val background = if (overOverlay) Color.Transparent else Color.Black
    val colors = remember { FutureTheme(isDarkMode = true) }

    val airplane = manager.isAirplaneOn
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection,
        // גודל קבוע: גופן מוגדל במערכת היה דוחס את השורה
        LocalDensity provides Density(density.density, fontScale = 1f),
    ) {
        Row(
            modifier = modifier.fillMaxWidth().background(background).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ---- צד ההתחלה: שעה, שיחה, התראות ----
            Row(
                modifier = Modifier.weight(1f).clipToBounds(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarText(time, 15.sp, FontWeight.Medium)
                if (callActive) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(FutureIcons.Call, null, tint = colors.successColor, modifier = Modifier.size(IconSize))
                        BarText("%d:%02d".format(callSeconds / 60, callSeconds % 60), 12.sp, FontWeight.Medium, colors.successColor)
                    }
                }
                NotificationIcons(notifApps)
            }

            // ---- צד הסוף: מצב המכשיר וסוללה ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (PrivacyIndicators.cameraInUse.value) SysIcon(FutureIcons.Videocam, FutureAccents.Green)
                if (PrivacyIndicators.micInUse.value) SysIcon(FutureIcons.Mic, FutureAccents.Orange)
                if (hasAlarm) SysIcon(FutureIcons.Alarm)
                when (ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> SysIcon(FutureIcons.VolumeOff)
                    AudioManager.RINGER_MODE_VIBRATE -> SysIcon(FutureIcons.Vibration)
                }
                if (manager.isDndOn) SysIcon(FutureIcons.DoNotDisturbOn)
                if (showBluetooth && manager.isBluetoothOn) {
                    SysIcon(FutureIcons.Bluetooth, if (manager.isBluetoothDeviceConnected) Color.White else Dim)
                }
                if (airplane) {
                    SysIcon(FutureIcons.AirplanemodeActive)
                } else {
                    if (wifiConnected) WifiIcon(wifiLevel)
                    CellIcon(
                        level = if (inService) cellLevel else 0,
                        noSim = !simReady,
                        dataActive = manager.isDataOn && inService && !wifiConnected,
                    )
                }
                Battery(battery, charging, manager.isBatterySaverOn, showPercent, colors)
            }
        }
    }
}

private const val TAG = "StatusBarScreen"
private val IconSize = 15.dp
private val Dim = Color.White.copy(alpha = 0.3f)

/** העותק האחרון של מצב השורה (בתהליך), כדי שהרכבה מחדש לא תתחיל מאפס. */
private object BarMemory {
    @Volatile var battery = 100
    @Volatile var charging = false
    @Volatile var notifApps: List<NotifApp> = emptyList()
    @Volatile var cellLevel = 0
    @Volatile var inService = true
    @Volatile var simReady = true
    @Volatile var wifiConnected = false
    @Volatile var wifiLevel = 4
    @Volatile var callActive = false
    @Volatile var callStart = 0L
}

private data class NotifApp(val packageName: String, val smallIcon: ImageBitmap?)

/** סמל בקו אחד לכל אפליקציה מוכרת. null - משתמשים באייקון ההתראה שהאפליקציה שולחת. */
internal fun appGlyph(pkg: String): ImageVector? = when (pkg) {
    "com.future.messages", "com.google.android.apps.messaging", "com.whatsapp", "org.telegram.messenger" -> FutureIcons.Chat
    "com.future.dialer", "com.android.server.telecom", "com.google.android.dialer" -> FutureIcons.CallMissed
    "com.future.calendar" -> FutureIcons.CalendarToday
    "com.future.clock" -> FutureIcons.Alarm
    "com.future.music" -> FutureIcons.MusicNote
    "com.future.assistant" -> FutureIcons.AutoAwesome
    "com.future.recorder" -> FutureIcons.Mic
    "com.future.navigation" -> FutureIcons.Navigation
    "com.future.tasks" -> FutureIcons.Checklist
    "com.future.notes" -> FutureIcons.Description
    "com.future.camera" -> FutureIcons.Camera
    "com.future.gallery" -> FutureIcons.Image
    "com.future.files" -> FutureIcons.Folder
    "com.future.translate" -> FutureIcons.Translate
    "com.future.fitness" -> FutureIcons.FitnessCenter
    "com.future.bluetooth" -> FutureIcons.Bluetooth
    "com.future.settings" -> FutureIcons.Settings
    "com.google.android.gm" -> FutureIcons.Email
    else -> null
}

@Composable
private fun BarText(text: String, size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, color: Color = Color.White) {
    Text(
        text = text,
        color = color,
        fontSize = size,
        lineHeight = size * 1.15f,
        fontWeight = weight,
        maxLines = 1,
        softWrap = false,
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}

@Composable
private fun SysIcon(icon: ImageVector, tint: Color = Color.White) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(IconSize))
}

/** כמו אנדרואיד: עד 4 אייקונים, ואם יש עוד - נקודה. */
@Composable
private fun NotificationIcons(apps: List<NotifApp>) {
    if (apps.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        apps.take(MAX_NOTIF_ICONS).forEach { app ->
            val glyph = appGlyph(app.packageName)
            val icon = app.smallIcon
            when {
                glyph != null -> SysIcon(glyph)
                icon != null -> Image(icon, null, colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(IconSize))
                else -> SysIcon(FutureIcons.Notifications)
            }
        }
        if (apps.size > MAX_NOTIF_ICONS) {
            Canvas(Modifier.size(5.dp)) { drawCircle(Color.White) }
        }
    }
}

private const val MAX_NOTIF_ICONS = 4

/** Wi-Fi: מניפה מלאה בשקיפות, והחלק המלא לפי עוצמת הקליטה (0-4). */
@Composable
private fun WifiIcon(level: Int) {
    Canvas(Modifier.size(width = 17.dp, height = IconSize)) {
        val cx = size.width / 2f
        val bottom = size.height * 0.92f
        val r = minOf(size.width / 2f / 0.75f, bottom)
        fun fan(radius: Float, color: Color) {
            drawArc(
                color = color,
                startAngle = -135f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(cx - radius, bottom - radius),
                size = Size(radius * 2, radius * 2),
            )
        }
        fan(r, Dim)
        if (level > 0) fan(r * (0.28f + 0.72f * level.coerceIn(0, 4) / 4f), Color.White)
    }
}

/**
 * קליטה סלולרית: משולש בשקיפות, והחלק המלא לפי העוצמה (0-4). בלי SIM -
 * משולש עמום עם קו אלכסוני. "▲▼" קטן כשהנתונים הסלולריים פעילים.
 */
@Composable
private fun CellIcon(level: Int, noSim: Boolean, dataActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        if (dataActive && !noSim) {
            Canvas(Modifier.size(width = 5.dp, height = 11.dp)) {
                val w = size.width
                val h = size.height
                val up = Path().apply { moveTo(w / 2, 0f); lineTo(w, h * 0.42f); lineTo(0f, h * 0.42f); close() }
                val down = Path().apply { moveTo(0f, h * 0.58f); lineTo(w, h * 0.58f); lineTo(w / 2, h); close() }
                drawPath(up, Color.White)
                drawPath(down, Color.White)
            }
        }
        Canvas(Modifier.size(IconSize)) {
            val w = size.width
            val h = size.height
            val tri = Path().apply { moveTo(w, 0f); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(tri, Dim)
            if (!noSim && level > 0) {
                clipRect(right = w * level.coerceIn(0, 4) / 4f) { drawPath(tri, Color.White) }
            }
            if (noSim) {
                drawLine(Color.White, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f), strokeWidth = 1.5.dp.toPx())
            }
        }
    }
}

/**
 * סוללה עומדת כמו באנדרואיד 12: מסגרת, פין למעלה ומילוי מלמטה. טעינה - ירוק
 * וברק, חלשה (15% ומטה) - אדום, חיסכון - מסגרת כתומה. אחוזים לפי ההגדרה.
 */
@Composable
private fun Battery(percent: Int, charging: Boolean, saver: Boolean, showPercent: Boolean, colors: FutureTheme) {
    val low = percent <= 15 && !charging
    val fill = when {
        charging -> colors.successColor
        low -> colors.dangerColor
        else -> Color.White
    }
    val frame = if (saver) colors.warningColor else Color.White
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showPercent) BarText("$percent%", 13.sp, FontWeight.Medium, if (low) colors.dangerColor else Color.White)
        // הסוללה לא מתהפכת ב-RTL
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(Modifier.size(width = 9.dp, height = 15.dp)) {
                val stroke = 1.2.dp.toPx()
                val nubH = 1.8.dp.toPx()
                val nubW = size.width * 0.45f
                drawRoundRect(
                    color = frame,
                    topLeft = Offset((size.width - nubW) / 2f, 0f),
                    size = Size(nubW, nubH + 0.5f),
                    cornerRadius = CornerRadius(0.6.dp.toPx()),
                )
                val bodyTop = nubH
                drawRoundRect(
                    color = frame,
                    topLeft = Offset(stroke / 2, bodyTop + stroke / 2),
                    size = Size(size.width - stroke, size.height - bodyTop - stroke),
                    cornerRadius = CornerRadius(1.8.dp.toPx()),
                    style = Stroke(width = stroke),
                )
                val inset = stroke + 1.dp.toPx()
                val maxH = (size.height - bodyTop - inset * 2).coerceAtLeast(0f)
                val h = (maxH * percent.coerceIn(0, 100) / 100f).coerceIn(0f, maxH)
                if (h > 0f) {
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(inset, size.height - inset - h),
                        size = Size((size.width - inset * 2).coerceAtLeast(0f), h),
                        cornerRadius = CornerRadius(0.8.dp.toPx()),
                    )
                }
                if (charging) {
                    val w = size.width
                    val top = bodyTop
                    val bh = size.height - bodyTop
                    val bolt = Path().apply {
                        moveTo(w * 0.62f, top + bh * 0.12f)
                        lineTo(w * 0.22f, top + bh * 0.56f)
                        lineTo(w * 0.48f, top + bh * 0.56f)
                        lineTo(w * 0.38f, top + bh * 0.90f)
                        lineTo(w * 0.78f, top + bh * 0.44f)
                        lineTo(w * 0.52f, top + bh * 0.44f)
                        close()
                    }
                    drawPath(bolt, Color.Black)
                }
            }
        }
    }
}
