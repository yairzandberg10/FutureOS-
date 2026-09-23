package com.future.futureui.statusbar.ui

import com.future.sharednav.icons.FutureIcons

import com.future.sharednav.theme.FutureTypography
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.util.Log
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.controlcenter.service.MediaControlService
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
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
    var extras by remember { mutableStateOf(StatusExtras()) }

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

            val (notifications, inCall) = withContext(Dispatchers.IO) {
                val count = if (MediaControlService.isEnabled(context)) {
                    try {
                        MediaControlService.instance?.activeNotifications?.size ?: 0
                    } catch (e: Exception) {
                        Log.w("StatusBarScreen", "activeNotifications failed", e)
                        0
                    }
                } else 0

                // dialer שומר את מצב השיחה בתהליך שלו בלבד (CallService.activeCall) - אין
                // לו ערוץ IPC החוצה, אז כאן פשוט שואלים את המערכת ישירות (כמו כל אינדיקטור
                // אחר בשורה הזו), באותו דפוס "משיכה ממקור מערכת" שכבר קיים למדיה.
                val call = try {
                    (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.isInCall == true
                } catch (e: SecurityException) {
                    Log.w("StatusBarScreen", "missing READ_PHONE_STATE, hiding call indicator", e)
                    false
                }
                count to call
            }
            notificationCount = notifications
            isCallActive = inCall
            extras = withContext(Dispatchers.IO) { readStatusExtras(context) }

            delay(10_000)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = FutureTypography.summary,
                    fontWeight = FontWeight.SemiBold
                )
                // מאילו אפליקציות יש התראות (עד שלוש), ומעבר לזה "+N" - במקום נקודה אחת.
                extras.notificationIcons.forEach { icon ->
                    androidx.compose.foundation.Image(
                        bitmap = icon,
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.9f)),
                        modifier = Modifier.size(12.dp)
                    )
                }
                val more = notificationCount - extras.notificationIcons.size
                if (extras.notificationIcons.isEmpty() && notificationCount > 0) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
                } else if (more > 0 && extras.notificationIcons.size >= 3) {
                    Text(text = "+$more", color = Color.White.copy(alpha = 0.8f), fontSize = FutureTypography.caption)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (extras.isMediaPlaying) {
                    Icon(FutureIcons.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (extras.hasAlarm) {
                    Icon(FutureIcons.Alarm, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (extras.headphones) {
                    Icon(FutureIcons.Headphones, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                when (extras.ringerMode) {
                    android.media.AudioManager.RINGER_MODE_SILENT ->
                        Icon(FutureIcons.AutoMirrored.VolumeOff, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                    android.media.AudioManager.RINGER_MODE_VIBRATE ->
                        Icon(Icons.Rounded.Vibration, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (isCallActive) {
                    Icon(FutureIcons.Call, contentDescription = null, tint = StatusBarPalette.successColor, modifier = Modifier.size(13.dp))
                }
                if (manager.isDndOn) {
                    Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (manager.isAirplaneOn) {
                    Icon(FutureIcons.AirplanemodeActive, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                if (showBluetooth && manager.isBluetoothOn) {
                    Icon(
                        if (manager.isBluetoothDeviceConnected) Icons.Rounded.BluetoothConnected else FutureIcons.Bluetooth,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (extras.wifiConnected) {
                    Icon(FutureIcons.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                }
                // עוצמת הקליטה הסלולרית בפועל (0-4 פסים), ולא רק "נתונים דלוקים".
                if (extras.signalLevel >= 0 && !manager.isAirplaneOn) {
                    SignalBars(level = extras.signalLevel, dataOn = manager.isDataOn)
                }
                if (manager.isBatterySaverOn) {
                    Icon(Icons.Rounded.BatterySaver, contentDescription = null, tint = StatusBarPalette.warningColor, modifier = Modifier.size(13.dp))
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

/** מידע נוסף לשורת המצב - נאסף ב-thread רקע בכל סבב סקר. */
private data class StatusExtras(
    val notificationIcons: List<androidx.compose.ui.graphics.ImageBitmap> = emptyList(),
    val wifiConnected: Boolean = false,
    val signalLevel: Int = -1,
    val ringerMode: Int = android.media.AudioManager.RINGER_MODE_NORMAL,
    val hasAlarm: Boolean = false,
    val headphones: Boolean = false,
    val isMediaPlaying: Boolean = false,
)

private val iconCache = HashMap<String, androidx.compose.ui.graphics.ImageBitmap>()

private fun readStatusExtras(context: Context): StatusExtras {
    val icons = try {
        val active = MediaControlService.instance?.activeNotifications.orEmpty()
        active.filter { !it.isOngoing && it.packageName != context.packageName }
            .sortedByDescending { it.postTime }
            .map { it.packageName to it.notification.smallIcon }
            .distinctBy { it.first }
            .take(3)
            .mapNotNull { (pkg, icon) ->
                iconCache[pkg] ?: runCatching {
                    icon?.loadDrawable(context)?.let { d ->
                        d.toBitmap(36, 36).asImageBitmap()
                    }
                }.getOrNull()?.also { iconCache[pkg] = it }
            }
    } catch (e: Exception) {
        Log.w("StatusBarScreen", "notification icons failed", e)
        emptyList()
    }
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
    val wifi = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
    }.getOrDefault(false)
    val signal = runCatching {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (tm.simState == TelephonyManager.SIM_STATE_READY) tm.signalStrength?.level ?: -1 else -1
    }.getOrDefault(-1)
    val alarm = runCatching {
        (context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).nextAlarmClock != null
    }.getOrDefault(false)
    val headphones = runCatching {
        audio?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)?.any {
            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        } == true
    }.getOrDefault(false)
    return StatusExtras(
        notificationIcons = icons,
        wifiConnected = wifi,
        signalLevel = signal,
        ringerMode = audio?.ringerMode ?: android.media.AudioManager.RINGER_MODE_NORMAL,
        hasAlarm = alarm,
        headphones = headphones,
        isMediaPlaying = audio?.isMusicActive == true,
    )
}

/** פסי קליטה: ארבעה פסים עולים, המלאים לפי [level]; בלי נתונים - שקופים יותר. */
@Composable
private fun SignalBars(level: Int, dataOn: Boolean) {
    val on = Color.White.copy(alpha = if (dataOn) 0.95f else 0.75f)
    val off = Color.White.copy(alpha = 0.25f)
    Canvas(modifier = Modifier.size(width = 14.dp, height = 11.dp)) {
        val gap = 1.2.dp.toPx()
        val w = (size.width - gap * 3) / 4
        for (i in 0 until 4) {
            val h = size.height * (i + 1) / 4f
            drawRoundRect(
                color = if (i < level) on else off,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(0.8.dp.toPx())
            )
        }
    }
}

/** ברירת מחדל כשאין עדיין צבע הדגשה משותף זמין - ההדגשה של ברירת המחדל במערכת (לבן). */
private val StatusBarAccent = Color.White

/**
 * שורת המצב יושבת מעל כל תוכן (טפט, מסך בהיר, מסך כהה) על רקע כהה משלה,
 * ולכן היא לוקחת את צבעי הסטטוס של הערכה *הכהה* - כמו ההתראה הצפה, שתמיד
 * כהה. קודם היו כאן שלושה hex של iOS שאינם הפלטה.
 */
private val StatusBarPalette = com.future.sharednav.theme.FutureTheme(isDarkMode = true)

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
        percent <= 15 -> StatusBarPalette.dangerColor
        else -> Color.White
    }
    Canvas(modifier = modifier.size(width = 21.dp, height = 11.dp)) {
        val nubWidth = 1.6.dp.toPx()
        val bodyWidth = size.width - nubWidth
        val strokeWidth = 1.1.dp.toPx()
        val bodyCorner = CornerRadius(2.6.dp.toPx())

        drawRoundRect(
            color = Color.White.copy(alpha = 0.7f),
            size = Size(bodyWidth, size.height),
            cornerRadius = bodyCorner,
            style = Stroke(width = strokeWidth)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.7f),
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
