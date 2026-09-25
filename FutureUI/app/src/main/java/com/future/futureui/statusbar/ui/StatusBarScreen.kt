package com.future.futureui.statusbar.ui

import com.future.sharednav.theme.FutureTypography
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.SystemClock
import android.telecom.TelecomManager
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.controlcenter.service.MediaControlService
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
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
 * עיצוב בהשראת שורות המצב המינימליסטיות של One UI (Galaxy): רקע כמעט שקוף,
 * צבירת אייקונים צפופה במשקל אחיד, שעון בגופן בינוני, וסוללה מצוירת כגלולה
 * ממולאת במקום האייקונים המדורגים של Material - כדי שהיא תרגיש "מכשיר אמיתי"
 * ולא כמו וידג'ט גנרי.
 */
@Composable
fun StatusBarScreen(
    modifier: Modifier = Modifier,
    controlManager: ControlManager? = null,
    layoutManager: StatusBarLayoutManager? = null,
    accentColor: Color = StatusBarAccent
) {
    val context = LocalContext.current
    val manager = controlManager ?: remember { ControlManager(context) }
    val layout = layoutManager ?: remember { StatusBarLayoutManager(context) }

    var currentTime by remember { mutableStateOf("") }
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var notificationCount by remember { mutableIntStateOf(0) }
    var isCallActive by remember { mutableStateOf(false) }
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
    val opacity = layout.getBarOpacity()

    // השעון והסוללה מתעדכנים מאירועי מערכת ולא מסקר: TIME_TICK מגיע בדיוק
    // בתחילת כל דקה (קודם השעון יכול היה לפגר עד 15 שניות אחרי החלפת הדקה),
    // ו-BATTERY_CHANGED מגיע רק כשמשהו בסוללה באמת משתנה. registerReceiver
    // מחזיר מיד את ה-BATTERY_CHANGED האחרון (sticky), אז הערך הראשון לא מחכה.
    DisposableEffect(use24Hour) {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        fun refreshTime() {
            currentTime = timeFormat.format(Date())
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
                    else -> refreshTime()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        refreshTime()
        applyBattery(context.registerReceiver(receiver, filter))
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Log.w("StatusBarScreen", "receiver already unregistered", e)
            }
        }
    }

    // שאר האינדיקטורים (DND, טיסה, Bluetooth, נתונים, התראות, שיחה) עדיין
    // בסקר של 15 שניות - אבל כל הקריאות רצות ב-thread רקע. קודם כולן רצו על
    // ה-main thread של FutureUI, שהוא גם זה שמסנן את כל לחיצות המקשים בטלפון.
    LaunchedEffect(Unit) {
        while (true) {
            manager.updateStates()

            val (notifications, inCall, apps) = withContext(Dispatchers.IO) {
                val active = if (MediaControlService.isEnabled(context)) {
                    try {
                        MediaControlService.instance?.activeNotifications ?: emptyArray()
                    } catch (e: Exception) {
                        Log.w("StatusBarScreen", "activeNotifications failed", e)
                        emptyArray()
                    }
                } else emptyArray()
                val count = active.size
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

                // dialer שומר את מצב השיחה בתהליך שלו בלבד (CallService.activeCall) - אין
                // לו ערוץ IPC החוצה, אז כאן פשוט שואלים את המערכת ישירות (כמו כל אינדיקטור
                // אחר בשורה הזו), באותו דפוס "משיכה ממקור מערכת" שכבר קיים למדיה.
                val call = try {
                    (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.isInCall == true
                } catch (e: SecurityException) {
                    Log.w("StatusBarScreen", "missing READ_PHONE_STATE, hiding call indicator", e)
                    false
                }
                Triple(count, call, apps)
            }
            notificationCount = notifications
            isCallActive = inCall
            notifApps = apps

            delay(15_000)
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
        callDuration = if (isCallActive) "%d:%02d".format(callSeconds / 60, callSeconds % 60) else null
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (barStyle) {
            StatusBarLayoutManager.STYLE_QUIET -> QuietBar(modifier, manager, status)
            StatusBarLayoutManager.STYLE_CAPSULES -> CapsulesBar(modifier, manager, status)
            StatusBarLayoutManager.STYLE_CENTERED -> CenteredBar(modifier, manager, status)
            else -> ClassicBar(modifier, manager, status, isCallActive, opacity, accentColor)
        }
    }
}

/** הסגנון הקלאסי - שורת המצב כפי שהייתה עד עכשיו, בלי שינוי. */
@Composable
private fun ClassicBar(
    modifier: Modifier,
    manager: ControlManager,
    status: BarStatus,
    isCallActive: Boolean,
    opacity: Float,
    accentColor: Color
) {
    val notificationCount = status.notificationCount
    val currentTime = status.time
    val showBluetooth = status.showBluetooth
    val showBattery = status.showBattery
    val batteryPercent = status.batteryPercent
    val isCharging = status.isCharging
    run {
        Row(
            modifier = modifier
                .fillMaxWidth()
                // One UI לא מציירת רקע כהה מלא מאחורי שורת המצב - היא כמעט שקופה מעל
                // הטפט/תוכן, עם רק ערפול קל לקריאות. שכבה שטוחה אחת (לא גרדיאנט
                // דו-שלבי) + hairline דק מאוד בצבע ההדגשה המשותף במקום כחול קבוע.
                .background(Color.Black.copy(alpha = opacity.coerceIn(0f, 0.55f)))
                .drawBottomHairline(accentColor.copy(alpha = 0.18f))
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = FutureTypography.summary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCallActive) {
                    Icon(Icons.Rounded.Call, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(13.dp))
                }
                if (manager.isDndOn) {
                    Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (manager.isAirplaneOn) {
                    Icon(Icons.Rounded.AirplanemodeActive, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (showBluetooth && manager.isBluetoothOn) {
                    Icon(
                        if (manager.isBluetoothDeviceConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (manager.isDataOn) {
                    Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (manager.isBatterySaverOn) {
                    Icon(Icons.Rounded.BatterySaver, contentDescription = null, tint = Color(0xFFFFD60A), modifier = Modifier.size(13.dp))
                }
                if (showBattery) {
                    Text(text = "$batteryPercent%", color = Color.White.copy(alpha = 0.9f), fontSize = FutureTypography.caption, fontWeight = FontWeight.Medium)
                    BatteryPill(
                        percent = batteryPercent,
                        isCharging = isCharging,
                        accentColor = accentColor,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
        }
    }
}

/** ברירת מחדל כשאין עדיין צבע הדגשה משותף זמין (למשל בתצוגה מקדימה). */
private val StatusBarAccent = Color(0xFF5AC8FA)

private fun Modifier.drawBottomHairline(color: Color): Modifier = this.then(
    Modifier.drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
)

/**
 * גלולת סוללה מצוירת בעצמנו (גוף מעוגל + פין קטן + מילוי פרופורציונלי) בסגנון
 * One UI, במקום האייקונים המדורגים (Battery1Bar/2Bar/...) של Material - אלה
 * נראים כמו וידג'ט גנרי ולא כמו סוללה אמיתית של מכשיר.
 */
@Composable
private fun BatteryPill(percent: Int, isCharging: Boolean, accentColor: Color, modifier: Modifier = Modifier) {
    val fillColor = when {
        isCharging -> accentColor
        percent <= 15 -> Color(0xFFFF453A)
        else -> Color.White
    }
    Canvas(modifier = modifier.size(width = 21.dp, height = 11.dp)) {
        val nubWidth = 1.6.dp.toPx()
        val bodyWidth = size.width - nubWidth
        val strokeWidth = 1.1.dp.toPx()
        val bodyCorner = CornerRadius(2.6.dp.toPx())

        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            size = Size(bodyWidth, size.height),
            cornerRadius = bodyCorner,
            style = Stroke(width = strokeWidth)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(bodyWidth, size.height / 2f - 2.dp.toPx()),
            size = Size(nubWidth, 4.dp.toPx()),
            cornerRadius = CornerRadius(0.8.dp.toPx())
        )

        val inset = strokeWidth + 1.2.dp.toPx()
        val fillMaxWidth = bodyWidth - inset * 2
        val fillWidth = (fillMaxWidth * (percent / 100f)).coerceIn(0f, fillMaxWidth)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(inset, inset),
                size = Size(fillWidth, size.height - inset * 2),
                cornerRadius = CornerRadius(1.2.dp.toPx())
            )
        }
    }
}

// ---------------- הסגנונות החדשים (עיצוב "FutureOS Status Bar") ----------------
// המידות בעיצוב הן בפיקסלים של המסך (640×960, צפיפות 2), כאן הן חצי מזה ב-dp.
// שורת המצב תמיד כהה, אז הצבעים באים ממערכת העיצוב הכהה.

private val Fos = FutureTheme(isDarkMode = true)
private val FosText70 = Fos.textColor.copy(alpha = 0.7f)
private val FosText60 = Fos.textColor.copy(alpha = 0.6f)

/** fos-glass - המילוי של הכמוסות. */
private val FosGlass = Color(0xFF2C2C2E)

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
    val callDuration: String?
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
    Text(text = text, color = color, fontSize = fontSize, fontWeight = weight, maxLines = 1, style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"))
}

@Composable
private fun AppGlyphs(apps: List<NotifApp>) {
    // בטקסט גדול אין מקום לארבעה - אייקון אחד ואחריו מונה
    val max = if (LocalDensity.current.fontScale >= 1.2f) 1 else MAX_APP_GLYPHS
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        apps.take(max).forEach { app ->
            val glyph = appGlyph(app.packageName)
            val icon = app.smallIcon
            when {
                glyph != null -> Icon(glyph, contentDescription = null, tint = Fos.textColor, modifier = Modifier.size(14.dp))
                icon != null -> Image(
                    bitmap = icon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Fos.textColor),
                    modifier = Modifier.size(14.dp)
                )
                else -> Icon(FutureIcons.Notifications, contentDescription = null, tint = Fos.textColor, modifier = Modifier.size(14.dp))
            }
        }
        if (apps.size > max) BarText("+${apps.size - max}", FosText70, FutureTypography.label, FontWeight.SemiBold)
    }
}

@Composable
private fun StatusGlyph(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = FosText70, modifier = Modifier.size(14.dp))
}

/** אייקוני מצב המכשיר בטקסט 70% - כדי שלא ייראו כמו התראות. */
@Composable
private fun DeviceGlyphs(manager: ControlManager, status: BarStatus) {
    if (manager.isDndOn) StatusGlyph(FutureIcons.DoNotDisturbOn)
    if (manager.isAirplaneOn) StatusGlyph(FutureIcons.AirplanemodeActive)
    if (manager.isWifiOn) StatusGlyph(FutureIcons.Wifi)
    if (status.showBluetooth && manager.isBluetoothOn) StatusGlyph(FutureIcons.Bluetooth)
    if (manager.isDataOn && !manager.isAirplaneOn) StatusGlyph(FutureIcons.SignalCellularAlt)
}

/**
 * הסוללה החדשה: גוף במסגרת טקסט 70%, מילוי בצבע הטקסט. טעינה - הצלחה וברק,
 * חלשה - סכנה, חיסכון - מסגרת בצבע האזהרה. 23×12dp, פינה 3.5dp.
 */
@Composable
private fun FosBattery(percent: Int, isCharging: Boolean, isSaver: Boolean) {
    val fill = when {
        isCharging -> Fos.successColor
        percent <= 15 -> Fos.dangerColor
        else -> Fos.textColor
    }
    val frame = if (isSaver) Fos.warningColor else FosText70
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isCharging) Icon(FutureIcons.Bolt, contentDescription = null, tint = Fos.successColor, modifier = Modifier.size(12.dp))
        // הפין תמיד מימין, גם בשורה מימין לשמאל - כמו סוללה אמיתית
        Canvas(modifier = Modifier.size(width = 24.5.dp, height = 12.dp)) {
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
            val maxW = bodyW - inset * 2
            val w = (maxW * percent / 100f).coerceIn(0f, maxW)
            if (w > 0f) {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(inset, inset),
                    size = Size(w, size.height - inset * 2),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun BatteryPercent(status: BarStatus, withSign: Boolean) {
    val low = status.batteryPercent <= 15 && !status.isCharging
    BarText(
        if (withSign) "${status.batteryPercent}%" else "${status.batteryPercent}",
        if (low) Fos.dangerColor else Fos.textColor,
        FutureTypography.label,
        FontWeight.Medium
    )
}

@Composable
private fun CallPill(duration: String, showIcon: Boolean) {
    Row(
        modifier = Modifier.height(22.dp).clip(CircleShape).background(Fos.successColor).padding(horizontal = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) Icon(FutureIcons.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
        BarText(duration, Color.Black, FutureTypography.label, FontWeight.Bold)
    }
}

/** כיוון א - שקט: שעה ואייקוני האפליקציות מימין, מצב המכשיר משמאל, רקע fos-bg אטום. */
@Composable
private fun QuietBar(modifier: Modifier, manager: ControlManager, status: BarStatus) {
    Row(
        modifier = modifier.fillMaxWidth().background(Fos.backgroundColor).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            status.callDuration?.let { CallPill(it, showIcon = true) }
            BarText(status.time, Fos.textColor, FutureTypography.body, FontWeight.SemiBold)
            if (status.apps.isNotEmpty()) AppGlyphs(status.apps)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
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
    Row(
        modifier = modifier.fillMaxWidth().background(Fos.backgroundColor).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.height(22.dp).clip(CircleShape).background(FosGlass).padding(horizontal = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarText(status.time, Fos.textColor, FutureTypography.body, FontWeight.SemiBold)
                if (status.apps.isNotEmpty()) AppGlyphs(status.apps)
            }
            status.callDuration?.let { CallPill(it, showIcon = false) }
        }
        Row(
            modifier = Modifier.height(22.dp).clip(CircleShape).background(FosGlass).padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
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
    val date = remember(status.time) {
        val locale = Locale("iw")
        SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).format(Date())
    }
    Box(modifier = modifier.fillMaxWidth().background(Fos.backgroundColor).padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                if (status.callDuration != null) {
                    Icon(FutureIcons.Call, contentDescription = null, tint = Fos.successColor, modifier = Modifier.size(14.dp))
                }
                BarText(date, FosText60, FutureTypography.label, FontWeight.Normal)
                if (status.notificationCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(FutureIcons.Notifications, contentDescription = null, tint = FosText70, modifier = Modifier.size(13.dp))
                        BarText("${status.notificationCount}", FosText70, FutureTypography.label, FontWeight.Medium)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                DeviceGlyphs(manager, status)
                if (status.showBattery) {
                    FosBattery(status.batteryPercent, status.isCharging, manager.isBatterySaverOn)
                    BatteryPercent(status, withSign = false)
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            BarText(status.time, Fos.textColor, FutureTypography.dialog, FontWeight.Bold)
        }
    }
}
