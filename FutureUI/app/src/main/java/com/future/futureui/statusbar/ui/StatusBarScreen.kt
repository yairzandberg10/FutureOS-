package com.future.futureui.statusbar.ui

import com.future.sharednav.theme.FutureTypography
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
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.controlcenter.service.MediaControlService
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
import com.future.futureui.utils.FutureUIState
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureTheme
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * שורת המצב הקבועה (Status Bar) - מוצגת תמיד בראש המסך, לא רק לפי דרישה.
 *
 * בסגנון One UI: שורה אטומה שמתחלפת בין לבן (טקסט ואייקונים שחורים) לשחור (לבנים)
 * לפי מה שמוצג מתחתיה ([lightTone] - השירות דוגם אותו מצילום המסך), עם אייקונים
 * גדולים וצפופים וסוללה מצוירת. כל האינדיקטורים מתעדכנים מאירועי מערכת ולא מסקר,
 * והגופן קבוע (בלי הגדלת גופן של המערכת) כדי שהשורה לא תידחס ותתעוות.
 */
@Composable
fun StatusBarScreen(
    modifier: Modifier = Modifier,
    controlManager: ControlManager? = null,
    layoutManager: StatusBarLayoutManager? = null,
    accentColor: Color = StatusBarAccent,
    lightTone: Boolean = false
) {
    val context = LocalContext.current
    val manager = controlManager ?: remember { ControlManager(context) }
    val layout = layoutManager ?: remember { StatusBarLayoutManager(context) }

    var currentTime by remember { mutableStateOf("") }
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var notificationCount by remember { mutableIntStateOf(0) }
    var isCallActive by remember { mutableStateOf(false) }
    var ringerMode by remember { mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL) }
    var hasAlarm by remember { mutableStateOf(false) }
    // האפליקציות שיש להן התראה, החדשה ביותר ראשונה, כל אפליקציה פעם אחת
    var notifApps by remember { mutableStateOf<List<NotifApp>>(emptyList()) }
    var callSeconds by remember { mutableIntStateOf(0) }
    // אייקון ההתראה הקטן של אפליקציה שאין לה סמל במיפוי - נטען פעם אחת לכל חבילה
    val smallIconCache = remember { HashMap<String, ImageBitmap?>() }

    // ההגדרות נכתבות מאפליקציית ההגדרות דרך SystemUiSettingsProvider לאותם prefs,
    // באותו תהליך - אז מאזין אחד מספיק כדי שהחלפת סגנון תחול מיד ולא בדקה הבאה.
    var prefsTick by remember { mutableIntStateOf(0) }
    DisposableEffect(layout) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> prefsTick++ }
        layout.registerListener(listener)
        onDispose { layout.unregisterListener(listener) }
    }
    @Suppress("UNUSED_VARIABLE") val tick = prefsTick

    val barStyle = layout.getBarStyle()
    val showBattery = layout.getShowBattery()
    val showBluetooth = layout.getShowBluetooth()
    val use24Hour = layout.getUse24HourClock()

    // כל מה שמשתנה במערכת מגיע כאירוע (broadcast / ContentObserver) ומרענן את המצב
    // מיד - קודם רק השעון והסוללה היו כאלה, וכל השאר חיכה עד 15 שניות לסקר.
    // registerReceiver מחזיר מיד את ה-BATTERY_CHANGED האחרון (sticky), אז הסוללה
    // מוצגת כבר בפריים הראשון.
    DisposableEffect(use24Hour) {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        fun refreshTime() {
            currentTime = timeFormat.format(Date())
        }
        fun refreshRingerAndAlarm() {
            ringerMode = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            hasAlarm = try { alarms?.nextAlarmClock != null } catch (e: Exception) { false }
        }
        fun applyBattery(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) batteryPercent = (level * 100) / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> applyBattery(intent)
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        timeFormat.timeZone = TimeZone.getDefault()
                        refreshTime()
                    }
                    Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED -> refreshTime()
                    else -> {
                        refreshRingerAndAlarm()
                        manager.updateStates()
                    }
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
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(LocationManager.MODE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        // נתונים סלולריים ומצב טיסה משתנים גם דרך ההגדרות עצמן, בלי שידור אמין.
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                manager.updateStates()
            }
        }
        refreshTime()
        refreshRingerAndAlarm()
        applyBattery(context.registerReceiver(receiver, filter))
        try {
            context.contentResolver.registerContentObserver(Settings.Global.getUriFor("mobile_data"), false, observer)
            context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), false, observer)
        } catch (e: Exception) {
            Log.w("StatusBarScreen", "settings observers failed", e)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Log.w("StatusBarScreen", "receiver already unregistered", e)
            }
            runCatching { context.contentResolver.unregisterContentObserver(observer) }
        }
    }

    // ההתראות מתעדכנות בכל הוספה/הסרה של התראה (notificationTick, ר' MediaControlService),
    // והקריאות רצות ב-thread רקע: קודם כולן רצו על ה-main thread של FutureUI, שהוא גם זה
    // שמסנן את כל לחיצות המקשים בטלפון.
    val notificationTick = FutureUIState.notificationTick.intValue
    LaunchedEffect(notificationTick) {
        val (count, apps) = withContext(Dispatchers.IO) {
            val active = if (MediaControlService.isEnabled(context)) {
                try {
                    MediaControlService.instance?.activeNotifications ?: emptyArray()
                } catch (e: Exception) {
                    Log.w("StatusBarScreen", "activeNotifications failed", e)
                    emptyArray()
                }
            } else emptyArray()
            // התראות מתמשכות (נגן פעיל, שירותי רקע) הן מצב ולא משהו שמחכה למשתמש
            val apps = active
                .filter { !it.isOngoing && it.packageName != context.packageName }
                .sortedByDescending { it.postTime }
                .distinctBy { it.packageName }
                .map { sbn ->
                    val pkg = sbn.packageName
                    val icon = if (appGlyph(pkg) != null) null else {
                        if (!smallIconCache.containsKey(pkg)) {
                            smallIconCache[pkg] = try {
                                sbn.notification.smallIcon?.loadDrawable(context)?.toBitmap(48, 48)?.asImageBitmap()
                            } catch (e: Exception) {
                                Log.w("StatusBarScreen", "small icon failed for $pkg", e)
                                null
                            }
                        }
                        smallIconCache[pkg]
                    }
                    NotifApp(pkg, icon)
                }
            active.size to apps
        }
        notificationCount = count
        notifApps = apps
    }

    // שיחה פעילה אין לה broadcast נגיש (dialer שומר את מצב השיחה בתהליך שלו בלבד), אז היא
    // נבדקת בסקר קצר של 2 שניות ב-thread רקע; יחד איתו רשת ביטחון לרענון שאר המצבים
    // למקרה שאירוע מערכת הוחמץ.
    LaunchedEffect(Unit) {
        var round = 0
        while (true) {
            val call = withContext(Dispatchers.IO) {
                try {
                    (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.isInCall == true
                } catch (e: SecurityException) {
                    Log.w("StatusBarScreen", "missing READ_PHONE_STATE, hiding call indicator", e)
                    false
                }
            }
            isCallActive = call
            if (round++ % 3 == 0) manager.updateStates()
            delay(2_000)
        }
    }

    // משך השיחה נספר מהרגע שהשורה זיהתה אותה - אין ערוץ מה-dialer לזמן ההתחלה המדויק
    LaunchedEffect(isCallActive) {
        callSeconds = 0
        if (isCallActive) {
            val start = SystemClock.elapsedRealtime()
            while (true) {
                callSeconds = ((SystemClock.elapsedRealtime() - start) / 1000).toInt()
                delay(1_000)
            }
        }
    }

    val status = BarStatus(
        time = currentTime,
        batteryPercent = batteryPercent,
        isCharging = isCharging,
        showBattery = showBattery,
        showBluetooth = showBluetooth,
        notificationCount = notificationCount,
        apps = notifApps,
        callDuration = if (isCallActive) "%d:%02d".format(callSeconds / 60, callSeconds % 60) else null,
        ringerMode = ringerMode,
        hasAlarm = hasAlarm
    )

    // המעבר בין לבן לשחור מונפש (300ms), כך שהוא נראה ולא נקרא כהבהוב.
    val colors = remember(lightTone) { FutureTheme(isDarkMode = !lightTone) }
    val bg by animateColorAsState(if (lightTone) Color.White else Color.Black, tween(300), label = "barBg")
    val ink by animateColorAsState(if (lightTone) Color(0xFF111113) else Color.White, tween(300), label = "barInk")
    val glass by animateColorAsState(if (lightTone) Color(0xFFE5E5EA) else Color(0xFF2C2C2E), tween(300), label = "barGlass")
    val palette = BarPalette(
        background = bg,
        text = ink,
        glass = glass,
        success = colors.successColor,
        danger = colors.dangerColor,
        warning = colors.warningColor
    )

    // גודל הטקסט של שורת המצב קבוע: גופן מוגדל במערכת היה דוחס את השעון והאחוזים
    // לתוך 28dp ומעוות אותם.
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(density.density, fontScale = 1f),
        LocalBar provides palette
    ) {
        when (barStyle) {
            StatusBarLayoutManager.STYLE_QUIET -> QuietBar(modifier, manager, status)
            StatusBarLayoutManager.STYLE_CAPSULES -> CapsulesBar(modifier, manager, status)
            StatusBarLayoutManager.STYLE_CENTERED -> CenteredBar(modifier, manager, status)
            else -> ClassicBar(modifier, manager, status, accentColor)
        }
    }
}

/** ברירת מחדל כשאין עדיין צבע הדגשה משותף זמין (למשל בתצוגה מקדימה). */
private val StatusBarAccent = Color(0xFF5AC8FA)

/** הצבעים של שורת המצב - מתחלפים (מונפשים) בין לבן לשחור. */
@Immutable
private class BarPalette(
    val background: Color,
    val text: Color,
    val glass: Color,
    val success: Color,
    val danger: Color,
    val warning: Color
) {
    val text70: Color = text.copy(alpha = 0.7f)
    val text60: Color = text.copy(alpha = 0.6f)
}

private val LocalBar = compositionLocalOf {
    val dark = FutureTheme(isDarkMode = true)
    BarPalette(Color.Black, Color.White, Color(0xFF2C2C2E), dark.successColor, dark.dangerColor, dark.warningColor)
}

// ---------------- המרכיבים המשותפים לכל הסגנונות ----------------
// האייקונים 16dp (היו 13-14) והרווח ביניהם 3dp (היה 6-7): בשורה בגובה 28dp זה ההבדל
// בין סמלים שנראים וסמלים שנבלעים. כל המידות הן dp ולכן לא מושפעות מגודל הגופן.

private val GlyphSize = 16.dp
private val GlyphGap = 3.dp

/** כמה אייקוני אפליקציות מוצגים לפני שהם הופכים למונה +N. */
private const val MAX_APP_GLYPHS = 4

/** אפליקציה עם התראה: סמל Future Glyphs לפי שם החבילה, או אייקון ההתראה של האפליקציה עצמה. */
private data class NotifApp(val packageName: String, val smallIcon: ImageBitmap?)

private data class BarStatus(
    val time: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val showBattery: Boolean,
    val showBluetooth: Boolean,
    val notificationCount: Int,
    val apps: List<NotifApp>,
    val callDuration: String?,
    val ringerMode: Int,
    val hasAlarm: Boolean
)

/** סמל פשוט בקו אחד לכל אפליקציה מוכרת. null - משתמשים באייקון ההתראה שהאפליקציה שולחת. */
private fun appGlyph(pkg: String): ImageVector? = when (pkg) {
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
private fun BarText(text: String, color: Color, fontSize: TextUnit, weight: FontWeight) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * 1.15f,
        fontWeight = weight,
        maxLines = 1,
        softWrap = false,
        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
    )
}

/** אייקוני האפליקציות עם ההתראות; נחתכים (ולא דוחסים את השעון) כשחסר מקום. */
@Composable
private fun AppGlyphs(apps: List<NotifApp>) {
    val bar = LocalBar.current
    Row(
        modifier = Modifier.clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(GlyphGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.take(MAX_APP_GLYPHS).forEach { app ->
            val glyph = appGlyph(app.packageName)
            val icon = app.smallIcon
            when {
                glyph != null -> Icon(glyph, contentDescription = null, tint = bar.text, modifier = Modifier.size(GlyphSize))
                icon != null -> Image(
                    bitmap = icon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(bar.text),
                    modifier = Modifier.size(GlyphSize)
                )
                else -> Icon(FutureIcons.Notifications, contentDescription = null, tint = bar.text, modifier = Modifier.size(GlyphSize))
            }
        }
        if (apps.size > MAX_APP_GLYPHS) BarText("+${apps.size - MAX_APP_GLYPHS}", bar.text70, FutureTypography.label, FontWeight.SemiBold)
    }
}

@Composable
private fun StatusGlyph(icon: ImageVector, tint: Color = LocalBar.current.text) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(GlyphSize))
}

/** כל אינדיקטורי מצב המכשיר: מנורה, צלצול, DND, טיסה, פנס, מיקום, Bluetooth, Wi-Fi, נתונים, חיסכון. */
@Composable
private fun DeviceGlyphs(manager: ControlManager, status: BarStatus) {
    val bar = LocalBar.current
    if (status.hasAlarm) StatusGlyph(FutureIcons.Alarm)
    when (status.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> StatusGlyph(FutureIcons.VolumeOff)
        AudioManager.RINGER_MODE_VIBRATE -> StatusGlyph(FutureIcons.Vibration)
    }
    if (manager.isDndOn) StatusGlyph(FutureIcons.DoNotDisturbOn)
    if (manager.isAirplaneOn) StatusGlyph(FutureIcons.AirplanemodeActive)
    if (manager.isFlashlightOn) StatusGlyph(FutureIcons.FlashlightOn, bar.warning)
    if (manager.isLocationOn) StatusGlyph(FutureIcons.LocationOn)
    if (status.showBluetooth && manager.isBluetoothOn) {
        StatusGlyph(FutureIcons.Bluetooth, if (manager.isBluetoothDeviceConnected) bar.text else bar.text70)
    }
    if (manager.isWifiOn && !manager.isAirplaneOn) StatusGlyph(FutureIcons.Wifi)
    if (manager.isDataOn && !manager.isAirplaneOn) StatusGlyph(FutureIcons.SignalCellularAlt)
    if (manager.isBatterySaverOn) StatusGlyph(FutureIcons.BatterySaver, bar.warning)
}

/**
 * הסוללה: גוף במסגרת טקסט 70%, מילוי בצבע הטקסט. טעינה - הצלחה וברק,
 * חלשה - סכנה, חיסכון - מסגרת בצבע האזהרה. 25×12dp, פינה 3.5dp.
 */
@Composable
private fun FosBattery(percent: Int, isCharging: Boolean, isSaver: Boolean) {
    val bar = LocalBar.current
    val fill = when {
        isCharging -> bar.success
        percent <= 15 -> bar.danger
        else -> bar.text
    }
    val frame = if (isSaver) bar.warning else bar.text70
    Row(horizontalArrangement = Arrangement.spacedBy(GlyphGap), verticalAlignment = Alignment.CenterVertically) {
        if (isCharging) Icon(FutureIcons.Bolt, contentDescription = null, tint = bar.success, modifier = Modifier.size(13.dp))
        // הפין תמיד מימין, גם בשורה מימין לשמאל - כמו סוללה אמיתית
        Canvas(modifier = Modifier.size(width = 25.dp, height = 12.dp)) {
            val stroke = 1.dp.toPx()
            val nubW = 1.5.dp.toPx()
            val bodyW = size.width - nubW
            drawRoundRect(
                color = frame,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(bodyW - stroke, size.height - stroke),
                cornerRadius = CornerRadius(3.5.dp.toPx()),
                style = Stroke(width = stroke)
            )
            drawRoundRect(
                color = frame,
                topLeft = Offset(bodyW, size.height / 2f - 2.5.dp.toPx()),
                size = Size(nubW, 5.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
            val inset = stroke + 1.5.dp.toPx()
            // coerceAtLeast: כשהאייקון צר מה-inset (למשל באמצע אנימציה) הרוחב יוצא שלילי,
            // ו-coerceIn(0f, שלילי) הפיל את FutureUI בשלב הציור ("maximum -13.0").
            val maxW = (bodyW - inset * 2).coerceAtLeast(0f)
            val w = (maxW * percent / 100f).coerceIn(0f, maxW)
            if (w > 0f) {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(inset, inset),
                    size = Size(w, (size.height - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun BatteryPercent(status: BarStatus, withSign: Boolean) {
    val bar = LocalBar.current
    val low = status.batteryPercent <= 15 && !status.isCharging
    BarText(
        if (withSign) "${status.batteryPercent}%" else "${status.batteryPercent}",
        if (low) bar.danger else bar.text,
        FutureTypography.label,
        FontWeight.Medium
    )
}

@Composable
private fun CallPill(duration: String, showIcon: Boolean) {
    val bar = LocalBar.current
    Row(
        modifier = Modifier.height(22.dp).clip(CircleShape).background(bar.success).padding(horizontal = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) Icon(FutureIcons.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
        BarText(duration, Color.Black, FutureTypography.label, FontWeight.Bold)
    }
}

/**
 * הצד של השעון וההתראות: השעון קבוע, ואייקוני ההתראות מקבלים את המקום שנשאר ונחתכים
 * אם הוא קטן - לעולם לא השעון ולא אינדיקטורי המערכת שבצד השני.
 */
@Composable
private fun RowScope.LeadingGroup(spacing: Int, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** הסגנון הקלאסי: שורה אטומה לבנה/שחורה, שעון ואייקוני התראות מימין, מצב המכשיר משמאל. */
@Composable
private fun ClassicBar(modifier: Modifier, manager: ControlManager, status: BarStatus, accentColor: Color) {
    val bar = LocalBar.current
    Row(
        modifier = modifier.fillMaxWidth().background(bar.background).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingGroup(spacing = 6) {
            status.callDuration?.let { CallPill(it, showIcon = true) }
            BarText(status.time, bar.text, FutureTypography.body, FontWeight.SemiBold)
            if (status.apps.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f, fill = false).clipToBounds()) { AppGlyphs(status.apps) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GlyphGap), verticalAlignment = Alignment.CenterVertically) {
            DeviceGlyphs(manager, status)
            if (status.showBattery) {
                BarText("${status.batteryPercent}%", if (status.batteryPercent <= 15 && !status.isCharging) bar.danger else bar.text, FutureTypography.label, FontWeight.Medium)
                BatteryPill(status.batteryPercent, status.isCharging, accentColor)
            }
        }
    }
}

/**
 * גלולת סוללה מצוירת בעצמנו (גוף מעוגל + פין קטן + מילוי פרופורציונלי) בסגנון
 * One UI, במקום האייקונים המדורגים (Battery1Bar/2Bar/...) של Material - אלה
 * נראים כמו וידג'ט גנרי ולא כמו סוללה אמיתית של מכשיר.
 */
@Composable
private fun BatteryPill(percent: Int, isCharging: Boolean, accentColor: Color) {
    val bar = LocalBar.current
    val fillColor = when {
        isCharging -> bar.success
        percent <= 15 -> bar.danger
        else -> bar.text
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isCharging) Icon(FutureIcons.Bolt, contentDescription = null, tint = bar.success, modifier = Modifier.size(13.dp))
        Canvas(modifier = Modifier.size(width = 25.dp, height = 12.dp)) {
            val nubWidth = 1.8.dp.toPx()
            val bodyWidth = size.width - nubWidth
            val strokeWidth = 1.2.dp.toPx()
            val body = bar.text.copy(alpha = 0.85f)

            drawRoundRect(
                color = body,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(bodyWidth - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
            drawRoundRect(
                color = body,
                topLeft = Offset(bodyWidth, size.height / 2f - 2.2.dp.toPx()),
                size = Size(nubWidth, 4.4.dp.toPx()),
                cornerRadius = CornerRadius(0.9.dp.toPx())
            )

            val inset = strokeWidth + 1.3.dp.toPx()
            // coerceAtLeast: כשהאייקון צר מה-inset (למשל באמצע אנימציה) הרוחב יוצא שלילי,
            // ו-coerceIn(0f, שלילי) הפיל את FutureUI בשלב הציור ("maximum -13.0").
            val fillMaxWidth = (bodyWidth - inset * 2).coerceAtLeast(0f)
            val fillWidth = (fillMaxWidth * (percent / 100f)).coerceIn(0f, fillMaxWidth)
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(inset, inset),
                    size = Size(fillWidth, (size.height - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }
        }
    }
}

/** כיוון א - שקט: שעה ואייקוני האפליקציות מימין, מצב המכשיר משמאל. */
@Composable
private fun QuietBar(modifier: Modifier, manager: ControlManager, status: BarStatus) {
    val bar = LocalBar.current
    Row(
        modifier = modifier.fillMaxWidth().background(bar.background).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingGroup(spacing = 8) {
            status.callDuration?.let { CallPill(it, showIcon = true) }
            BarText(status.time, bar.text, FutureTypography.body, FontWeight.SemiBold)
            if (status.apps.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f, fill = false).clipToBounds()) { AppGlyphs(status.apps) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GlyphGap), verticalAlignment = Alignment.CenterVertically) {
            DeviceGlyphs(manager, status)
            if (status.showBattery) {
                BatteryPercent(status, withSign = true)
                FosBattery(status.batteryPercent, status.isCharging, manager.isBatterySaverOn)
            }
        }
    }
}

/** כיוון ב - כמוסות: שעה ואייקוני אפליקציות בכמוסה אחת, מצב המכשיר בשנייה. */
@Composable
private fun CapsulesBar(modifier: Modifier, manager: ControlManager, status: BarStatus) {
    val bar = LocalBar.current
    Row(
        modifier = modifier.fillMaxWidth().background(bar.background).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingGroup(spacing = 4) {
            Row(
                modifier = Modifier.weight(1f, fill = false).height(22.dp).clip(CircleShape).background(bar.glass).padding(horizontal = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarText(status.time, bar.text, FutureTypography.body, FontWeight.SemiBold)
                if (status.apps.isNotEmpty()) {
                    Box(modifier = Modifier.weight(1f, fill = false).clipToBounds()) { AppGlyphs(status.apps) }
                }
            }
            status.callDuration?.let { CallPill(it, showIcon = false) }
        }
        Row(
            modifier = Modifier.height(22.dp).clip(CircleShape).background(bar.glass).padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(GlyphGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceGlyphs(manager, status)
            if (status.showBattery) {
                BatteryPercent(status, withSign = true)
                FosBattery(status.batteryPercent, status.isCharging, manager.isBatterySaverOn)
            }
        }
    }
}

/** כיוון ג - שעון במרכז: תאריך והתראות מימין, השעה באמצע, מצב המכשיר משמאל. */
@Composable
private fun CenteredBar(modifier: Modifier, manager: ControlManager, status: BarStatus) {
    val bar = LocalBar.current
    val date = remember(status.time) {
        val locale = Locale("iw")
        SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).format(Date())
    }
    Box(modifier = modifier.fillMaxWidth().background(bar.background).padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (status.callDuration != null) {
                    Icon(FutureIcons.Call, contentDescription = null, tint = bar.success, modifier = Modifier.size(GlyphSize))
                }
                BarText(date, bar.text60, FutureTypography.label, FontWeight.Normal)
                if (status.notificationCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(FutureIcons.Notifications, contentDescription = null, tint = bar.text70, modifier = Modifier.size(GlyphSize))
                        BarText("${status.notificationCount}", bar.text70, FutureTypography.label, FontWeight.Medium)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GlyphGap), verticalAlignment = Alignment.CenterVertically) {
                DeviceGlyphs(manager, status)
                if (status.showBattery) {
                    FosBattery(status.batteryPercent, status.isCharging, manager.isBatterySaverOn)
                    BatteryPercent(status, withSign = false)
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            BarText(status.time, bar.text, FutureTypography.dialog, FontWeight.Bold)
        }
    }
}
