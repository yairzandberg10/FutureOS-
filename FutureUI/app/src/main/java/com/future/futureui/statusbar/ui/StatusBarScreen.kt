package com.future.futureui.statusbar.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.telecom.TelecomManager
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
import kotlinx.coroutines.delay
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

    val showBattery = layout.getShowBattery()
    val showBluetooth = layout.getShowBluetooth()
    val use24Hour = layout.getUse24HourClock()
    val opacity = layout.getBarOpacity()

    LaunchedEffect(Unit) {
        while (true) {
            manager.updateStates()

            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) batteryPercent = (level * 100) / scale
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val pattern = if (use24Hour) "HH:mm" else "h:mm a"
            currentTime = SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time)

            notificationCount = if (MediaControlService.isEnabled(context)) {
                MediaControlService.instance?.activeNotifications?.size ?: 0
            } else 0

            // dialer שומר את מצב השיחה בתהליך שלו בלבד (CallService.activeCall) - אין
            // לו ערוץ IPC החוצה, אז כאן פשוט שואלים את המערכת ישירות (כמו כל אינדיקטור
            // אחר בשורה הזו), באותו דפוס "משיכה ממקור מערכת" שכבר קיים למדיה.
            isCallActive = try {
                (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.isInCall == true
            } catch (e: SecurityException) {
                Log.w("StatusBarScreen", "missing READ_PHONE_STATE, hiding call indicator", e)
                false
            }

            delay(15_000)
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
                    fontSize = 13.sp,
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
                    Text(text = "$batteryPercent%", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
