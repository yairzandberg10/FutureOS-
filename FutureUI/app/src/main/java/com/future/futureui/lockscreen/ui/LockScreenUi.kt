package com.future.futureui.lockscreen.ui

import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.future.futureui.lockscreen.EditPanel
import com.future.futureui.lockscreen.EditTarget
import com.future.futureui.lockscreen.LockMode
import com.future.futureui.lockscreen.LockNotification
import com.future.futureui.lockscreen.LockUiState
import com.future.futureui.lockscreen.face.FaceStatus
import com.future.futureui.lockscreen.logic.LockCatalog
import com.future.futureui.lockscreen.logic.LockSettings
import com.future.futureui.lockscreen.logic.LockWallpapers
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
import com.future.sharednav.theme.FutureAccents
import com.future.sharednav.theme.FutureMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val Danger = Color(0xFFFF6B6B)
private val Surface = Color(0xFF1C1C1E)
private val Raised = Color(0xFF2C2C2E)
private val Glass = Surface.copy(alpha = 0.72f)

@Composable
fun LockScreenUi(state: LockUiState, accent: Color) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    val is24 = remember { DateFormat.is24HourFormat(context) }
    val clockColor = if (state.clockColor == 0) accent else FutureAccents.presets.getOrElse(state.clockColor - 1) { Color.White }

    // כניסה: השעון והתוכן "נופלים" למקום מ-1.1 בקפיץ layer.
    // ביטול נעילה - "דרך הזכוכית": התוכן גדל ל-1.5 ונעלם, והרקע נעלם איתו,
    // כך שמתחת מתגלה האפליקציה עצמה. קודם התוכן עלה 500dp מעל Box שחור בחלון
    // אטום, ובמשך 300ms ראו מסך שחור לפני שהאפליקציה הופיעה בבת אחת.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val density = LocalDensity.current
    val enter by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = FutureMotion.Springs.layer(),
        label = "lockEnter"
    )
    val unlock by animateFloatAsState(
        targetValue = if (state.unlocking) 1f else 0f,
        animationSpec = tween(FutureMotion.DurationSlow, easing = FutureMotion.EasingAccelerate),
        label = "lockUnlock"
    )
    val offset = with(density) { (1f - enter) * (-40).dp.toPx() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = 1f - unlock }.background(Color.Black)) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                translationY = offset
                val s = (1.1f - 0.1f * enter) + 0.5f * unlock
                scaleX = s
                scaleY = s
                this.alpha = (enter.coerceIn(0f, 1f)) * (1f - (unlock * 1.4f).coerceIn(0f, 1f))
            }) {
                when (state.mode) {
                    LockMode.PIN -> {
                        LockBackground(state, blurred = true, scrim = 0.55f)
                        PinScreen(state, accent, now, is24)
                    }
                    LockMode.EDIT -> {
                        LockBackground(state, blurred = state.background == 1, scrim = 0.35f)
                        EditScreen(state, accent, clockColor, now, is24)
                    }
                    LockMode.MAIN -> {
                        LockBackground(state, blurred = state.background == 1, scrim = if (state.focusedNotification >= 0) 0.45f else 0.3f)
                        MainScreen(state, accent, clockColor, now, is24)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockBackground(state: LockUiState, blurred: Boolean, scrim: Float) {
    val wallpaper = state.wallpaper
    if (wallpaper != null) {
        Image(
            bitmap = wallpaper,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().then(if (blurred) Modifier.blur(24.dp) else Modifier)
        )
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))
}

private val topInset get() = StatusBarLayoutManager.HEIGHT_DP.dp

// ------------------------------------------------------------------ ראשי

@Composable
private fun MainScreen(state: LockUiState, accent: Color, clockColor: Color, now: Date, is24: Boolean) {
    val expanded = state.focusedNotification >= 0 && state.notifications.isNotEmpty()
    Column(
        Modifier.fillMaxSize().padding(top = topInset + 28.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (expanded) {
            CompactClock(now, is24, clockColor, 56)
            Spacer(Modifier.height(24.dp))
            NotificationCards(state, accent)
        } else {
            LockClock(now, is24, state.clockStyle, clockColor)
            Spacer(Modifier.height(10.dp))
            WidgetsLine(state, now, accent)
            if (state.ownerMessage.isNotBlank()) {
                Text(
                    state.ownerMessage,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (!expanded) NotificationIcons(state)
        Spacer(Modifier.height(28.dp))
        Box(Modifier.fillMaxWidth()) {
            ShortcutCircle(state.leftShortcut, state.flashlightOn, Modifier.align(AbsoluteAlignment.CenterLeft))
            StatusLine(state, accent, Modifier.align(Alignment.Center))
            ShortcutCircle(state.rightShortcut, state.flashlightOn, Modifier.align(AbsoluteAlignment.CenterRight))
        }
    }
}

/** אמצע השורה התחתונה: מנעול / סריקת פנים + ההסבר. */
@Composable
private fun StatusLine(state: LockUiState, accent: Color, modifier: Modifier) {
    val open = state.authenticated || !state.hasPin
    val status = state.faceStatus
    val text = when {
        state.focusedNotification >= 0 -> "OK לפתיחת ההתראה"
        state.hasPin && state.authenticated -> "זוהית · לחץ OK"
        !state.hasPin -> "לחץ OK לפתיחה"
        status == FaceStatus.SCANNING -> "מחפש את הפנים שלך"
        status == FaceStatus.TOO_FAR -> "קרב את הטלפון"
        status == FaceStatus.TOO_DARK -> "חשוך מדי לזיהוי"
        status == FaceStatus.NOT_RECOGNIZED -> "הפנים לא זוהו"
        status == FaceStatus.UNAVAILABLE -> "המצלמה לא זמינה"
        else -> "לחץ OK או הקלד קוד"
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (status == FaceStatus.SCANNING && !open) {
            ScanningFace(accent, 34.dp)
        } else {
            Icon(
                if (open) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                null,
                tint = if (open && state.hasPin) accent else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun ScanningFace(accent: Color, size: Dp) {
    val spin = rememberInfiniteTransition(label = "faceSpin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "faceAngle")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.5.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.15f), radius = size.toPx() / 2 - stroke, style = Stroke(stroke))
            drawArc(
                accent, startAngle = angle - 90f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = androidx.compose.ui.geometry.Size(this.size.width - 2 * stroke, this.size.height - 2 * stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Icon(Icons.Rounded.Face, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

// ------------------------------------------------------------------ שעון

@Composable
fun LockClock(now: Date, is24: Boolean, style: Int, color: Color, scale: Float = 1f) {
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, d בMMMM", Locale("he")).format(now)
    fun s(v: Int) = (v * scale).sp
    AnimatedContent(targetState = style, transitionSpec = { fadeIn(tween(FutureMotion.DurationStandard)) togetherWith fadeOut(tween(FutureMotion.DurationFast)) }, label = "clockStyle") { st ->
        when (st) {
            // קלאסי: שעה גדולה ודקה, תאריך מתחת
            0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, color = color, fontSize = s(92), fontWeight = FontWeight.Light, lineHeight = s(96), letterSpacing = (-0.02).em)
                Text(date, color = Color.White, fontSize = s(16), fontWeight = FontWeight.Medium)
            }
            // דק
            1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, color = color, fontSize = s(96), fontWeight = FontWeight.ExtraLight, lineHeight = s(100))
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = s(15), fontWeight = FontWeight.Light)
            }
            // מוערם: שעות מעל דקות
            2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val parts = time.split(":")
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(parts[0].padStart(2, '0'), color = color, fontSize = s(84), fontWeight = FontWeight.Bold, lineHeight = 0.9.em)
                        Text(parts.getOrElse(1) { "" }, color = color.copy(alpha = 0.65f), fontSize = s(84), fontWeight = FontWeight.Bold, lineHeight = 0.9.em)
                    }
                }
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = s(15))
            }
            // אנלוגי
            3 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnalogClock(now, color, Modifier.size((140 * scale).dp))
                Spacer(Modifier.height(6.dp))
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = s(15))
            }
            // צד: צמוד לתחילת השורה, שעה + תאריך בשתי שורות
            else -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(time, color = color, fontSize = s(64), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(SimpleDateFormat("EEEE", Locale("he")).format(now), color = Color.White, fontSize = s(17), fontWeight = FontWeight.SemiBold)
                    Text(SimpleDateFormat("d בMMMM", Locale("he")).format(now), color = Color.White.copy(alpha = 0.8f), fontSize = s(14))
                }
            }
        }
    }
}

@Composable
private fun CompactClock(now: Date, is24: Boolean, color: Color, sizeSp: Int, showDate: Boolean = true) {
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, color = color, fontSize = sizeSp.sp, fontWeight = FontWeight.Light, lineHeight = (sizeSp * 1.05f).sp)
        if (showDate) {
            Text(SimpleDateFormat("EEEE, d בMMMM", Locale("he")).format(now), color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AnalogClock(now: Date, color: Color, modifier: Modifier) {
    val cal = Calendar.getInstance().apply { time = now }
    val minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
    val hours = (cal.get(Calendar.HOUR) + minutes / 60f)
    val seconds = cal.get(Calendar.SECOND).toFloat()
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(Color.White.copy(alpha = 0.18f), r, style = Stroke(1.5.dp.toPx()))
        for (i in 0 until 12 step 3) {
            val a = Math.toRadians(i * 30.0 - 90)
            drawLine(
                Color.White,
                Offset(c.x + r * 0.8f * cos(a).toFloat(), c.y + r * 0.8f * sin(a).toFloat()),
                Offset(c.x + r * 0.92f * cos(a).toFloat(), c.y + r * 0.92f * sin(a).toFloat()),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
            )
        }
        fun hand(angleDeg: Float, len: Float, width: Float, col: Color) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(col, c, Offset(c.x + len * cos(a).toFloat(), c.y + len * sin(a).toFloat()), strokeWidth = width, cap = StrokeCap.Round)
        }
        hand(hours * 30f, r * 0.5f, 4.dp.toPx(), Color.White)
        hand(minutes * 6f, r * 0.75f, 2.5.dp.toPx(), color)
        hand(seconds * 6f, r * 0.82f, 1.dp.toPx(), color.copy(alpha = 0.7f))
        drawCircle(color, 3.dp.toPx(), c)
    }
}

// --------------------------------------------------------------- ווידג'טים

@Composable
private fun WidgetsLine(state: LockUiState, now: Date, accent: Color) {
    val items = state.widgets.filter { it != "none" }
    if (items.isEmpty()) {
        if (state.mode == LockMode.EDIT) Text("אין ווידג'טים", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { id -> WidgetItem(id, state, now, accent) }
    }
}

@Composable
private fun WidgetItem(id: String, state: LockUiState, now: Date, accent: Color) {
    val text = when (id) {
        "battery" -> "${state.batteryPercent}%"
        "alarm" -> state.nextAlarm ?: "אין"
        "hebdate" -> hebrewDate(now)
        "media" -> state.mediaTitle ?: "לא מתנגן"
        "notifications" -> "${state.notifications.size}"
        else -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(widgetIcon(id, state.charging), null, tint = if (id == "battery" && state.charging) accent else Color.White, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp))
    }
}

private fun widgetIcon(id: String, charging: Boolean = false): ImageVector = when (id) {
    "battery" -> if (charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd
    "alarm" -> Icons.Rounded.Alarm
    "hebdate" -> Icons.Rounded.CalendarMonth
    "media" -> Icons.Rounded.MusicNote
    else -> Icons.Rounded.Notifications
}

private fun hebrewDate(now: Date): String = runCatching {
    val fmt = android.icu.text.SimpleDateFormat("d MMMM", ULocale("he_IL@calendar=hebrew"))
    fmt.format(now)
}.getOrDefault("")

// ---------------------------------------------------------------- התראות

/** מצב רגיל: רק אייקונים קטנים, כמו One UI. מטה פותח את הכרטיסים. */
@Composable
private fun NotificationIcons(state: LockUiState) {
    val items = state.notifications
    if (items.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.take(4).forEach { n ->
            Box(Modifier.size(28.dp).clip(CircleShape).background(Glass), contentAlignment = Alignment.Center) {
                if (n.icon != null) Image(n.icon, null, Modifier.size(18.dp).clip(CircleShape))
                else Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun NotificationCards(state: LockUiState, accent: Color) {
    val items = state.notifications
    // 0 הכל, 1 תוכן רק אחרי זיהוי, 2 אף פעם
    val showContent = state.notificationPrivacy == 0 || (state.notificationPrivacy == 1 && (state.authenticated || !state.hasPin))
    val focused = state.focusedNotification
    val start = (focused - 1).coerceIn(0, (items.size - 3).coerceAtLeast(0))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.drop(start).take(3).forEachIndexed { k, n ->
            NotificationCard(n, showContent, focused = start + k == focused, accent = accent)
        }
    }
}

@Composable
private fun NotificationCard(n: LockNotification, showContent: Boolean, focused: Boolean, accent: Color) {
    val time = remember(n.time) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(n.time)) }
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface.copy(alpha = if (focused) 0.92f else 0.8f))
            .then(if (focused) Modifier.border(1.5.dp, accent, shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = if (focused) 14.dp else 12.dp)
    ) {
        Row(verticalAlignment = if (focused) Alignment.Top else Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Raised), contentAlignment = Alignment.Center) {
                if (n.icon != null) Image(n.icon, null, Modifier.size(20.dp).clip(CircleShape))
                else Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.appName, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(time, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Text(
                    if (showContent) n.title else "התראה",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (focused && showContent && n.text.isNotBlank()) {
                    Text(n.text, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (focused) {
            Text(
                "OK לפתיחת ההתראה",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- קיצורים

@Composable
private fun ShortcutCircle(id: String, flashlightOn: Boolean, modifier: Modifier) {
    if (id == "none") return
    val active = id == "flashlight" && flashlightOn
    Box(
        modifier.size(44.dp).clip(CircleShape).background(if (active) Color.White else Glass),
        contentAlignment = Alignment.Center
    ) {
        Icon(shortcutIcon(id, active), null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(20.dp))
    }
}

fun shortcutIcon(id: String, active: Boolean = false): ImageVector = when (id) {
    "flashlight" -> if (active) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff
    "camera" -> Icons.Rounded.PhotoCamera
    "phone" -> Icons.Rounded.Call
    "messages" -> Icons.Rounded.Sms
    "notes" -> Icons.Rounded.EditNote
    "recorder" -> Icons.Rounded.Mic
    "calculator" -> Icons.Rounded.Calculate
    "music" -> Icons.Rounded.MusicNote
    "none" -> Icons.Rounded.Block
    else -> Icons.Rounded.Apps
}

// ------------------------------------------------------------------- קוד

@Composable
private fun PinScreen(state: LockUiState, accent: Color, now: Date, is24: Boolean) {
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    // קוד שגוי: השדה נזרק הצידה ומתנדנד עד שהוא נעצר - קפיץ עם ריסון נמוך
    // ומהירות התחלתית, במקום 3 צעדים קבועים של 50ms שנראו כמו רעד מכני.
    LaunchedEffect(state.pinErrorTick) {
        if (state.pinErrorTick > 0) {
            shake.snapTo(0f)
            shake.animateTo(
                0f,
                androidx.compose.animation.core.spring(dampingRatio = 0.22f, stiffness = 900f),
                initialVelocity = -30f,
            )
        }
    }
    val lockedOut = state.lockoutSeconds > 0
    val bad = state.pinError || lockedOut
    Column(
        Modifier.fillMaxSize().padding(top = topInset + 16.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(now), color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.Lock, null, tint = if (bad) Danger else Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(14.dp))
        Text(
            text = when {
                lockedOut -> "יותר מדי ניסיונות"
                state.pinError -> "קוד שגוי"
                else -> "הזן קוד"
            },
            color = if (state.pinError && !lockedOut) Danger else Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (lockedOut) {
            Text("נסה שוב בעוד ${formatSeconds(state.lockoutSeconds)}", color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(24.dp))
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Surface).padding(horizontal = 24.dp, vertical = 8.dp)) {
                val s = state.lockoutSeconds
                Text("%02d:%02d".format(s / 60, s % 60), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(16.dp))
            val fraction = if (state.lockoutTotal > 0) state.lockoutSeconds.toFloat() / state.lockoutTotal else 0f
            Box(Modifier.width(224.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(CircleShape).background(accent))
            }
        } else {
            Spacer(Modifier.height(32.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.graphicsLayer { translationX = shake.value * 14f }
                ) {
                    repeat(state.pinLength) { i ->
                        val filled = i < state.pinEntered
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .then(
                                    when {
                                        state.pinError -> Modifier.background(Danger)
                                        filled -> Modifier.background(accent)
                                        else -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    }
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            val reason = state.pinReason
            when {
                reason != null -> Text(reason, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center)
                state.pinError -> Text("הזן קוד שוב", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                state.faceStatus == FaceStatus.SCANNING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    ScanningFace(accent, 22.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("או הבט בטלפון", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                state.faceEnabled -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Face, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("או הבט בטלפון", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("חזור - מחיקה", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

private fun formatSeconds(s: Int): String = if (s >= 60) "${(s + 59) / 60} דקות" else "$s שניות"

// ---------------------------------------------------------------- עריכה

/** מסגרת עריכה: מלאה בצבע ההדגשה כשבמיקוד, מקווקוות כשלא. */
private fun Modifier.editFrame(focused: Boolean, accent: Color, corner: Dp): Modifier =
    if (focused) {
        val shape = RoundedCornerShape(corner)
        this.clip(shape).background(Color.White.copy(alpha = 0.1f)).border(2.dp, accent, shape)
    } else {
        this.drawWithContent {
            drawContent()
            val w = 1.5.dp.toPx()
            drawRoundRect(
                Color.White.copy(alpha = 0.6f),
                topLeft = Offset(w / 2, w / 2),
                size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(corner.toPx()),
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            )
        }
    }

@Composable
private fun EditScreen(state: LockUiState, accent: Color, clockColor: Color, now: Date, is24: Boolean) {
    val panel = state.editPanel
    val target = state.editTarget
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(top = topInset + 8.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pill("ביטול", Glass, Color.White)
                Pill("סיום", accent, Color.Black)
            }
            when (panel) {
                EditPanel.NONE -> {
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().editFrame(target == EditTarget.CLOCK, accent, 20.dp).padding(vertical = 12.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        LockClock(now, is24, state.clockStyle, clockColor, scale = 0.9f)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.editFrame(target == EditTarget.WIDGETS, accent, 22.dp).padding(horizontal = 14.dp, vertical = 7.dp)) {
                        WidgetsLine(state, now, accent)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth()) {
                        EditShortcut(state.leftShortcut, target == EditTarget.LEFT_SHORTCUT, accent, Modifier.align(AbsoluteAlignment.CenterLeft))
                        Row(
                            Modifier
                                .align(Alignment.Center)
                                .height(36.dp)
                                .then(
                                    if (target == EditTarget.WALLPAPER) Modifier.clip(CircleShape).background(Surface).border(2.dp, accent, CircleShape)
                                    else Modifier.clip(CircleShape).background(Glass)
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Image, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("רקע", color = Color.White, fontSize = 14.sp)
                        }
                        EditShortcut(state.rightShortcut, target == EditTarget.RIGHT_SHORTCUT, accent, Modifier.align(AbsoluteAlignment.CenterRight))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("OK לעריכה · תפריט לסיום · חזור לביטול", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                }
                EditPanel.CLOCK -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().editFrame(true, accent, 20.dp).padding(vertical = 8.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        LockClock(now, is24, state.clockStyle, clockColor, scale = 0.72f)
                    }
                }
                EditPanel.WIDGETS -> Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CompactClock(now, is24, clockColor, 44, showDate = false)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.editFrame(true, accent, 22.dp).padding(horizontal = 14.dp, vertical = 6.dp)) {
                        WidgetsLine(state, now, accent)
                    }
                }
                EditPanel.SHORTCUTS -> Box(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.fillMaxWidth().align(Alignment.Center)) {
                        EditShortcut(state.leftShortcut, state.shortcutSide == 0, accent, Modifier.align(AbsoluteAlignment.CenterLeft))
                        Box(Modifier.align(Alignment.Center)) { CompactClock(now, is24, clockColor, 40, showDate = false) }
                        EditShortcut(state.rightShortcut, state.shortcutSide == 1, accent, Modifier.align(AbsoluteAlignment.CenterRight))
                    }
                }
                EditPanel.WALLPAPER -> Spacer(Modifier.weight(1f))
            }
        }
        when (panel) {
            EditPanel.CLOCK -> Sheet { ClockPanel(state, accent, now, is24) }
            EditPanel.WIDGETS -> Sheet { WidgetsPanel(state, accent) }
            EditPanel.SHORTCUTS -> Sheet { ShortcutsPanel(state, accent) }
            EditPanel.WALLPAPER -> Sheet { WallpaperPanel(state, accent) }
            EditPanel.NONE -> Unit
        }
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(Modifier.height(32.dp).clip(CircleShape).background(bg).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EditShortcut(id: String, focused: Boolean, accent: Color, modifier: Modifier) {
    Box(
        modifier.size(48.dp).editFrame(focused, accent, 24.dp).then(if (focused) Modifier else Modifier.clip(CircleShape).background(Glass)),
        contentAlignment = Alignment.Center
    ) {
        Icon(shortcutIcon(id), null, tint = Color.White.copy(alpha = if (id == "none") 0.5f else 1f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Sheet(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Surface)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
        content = content
    )
}

@Composable
private fun SheetTitle(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
    }
}

@Composable
private fun SectionLabel(text: String, active: Boolean) {
    Text(text, color = Color.White.copy(alpha = if (active) 0.9f else 0.55f), fontSize = 13.sp, letterSpacing = 1.sp)
}

@Composable
private fun SheetHint(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
}

// --- שעון

@Composable
private fun ClockPanel(state: LockUiState, accent: Color, now: Date, is24: Boolean) {
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    SheetTitle("שעון")
    Spacer(Modifier.height(14.dp))
    SectionLabel("סגנון", state.panelRow == 0)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LockCatalog.clockStyles.indices.forEach { i ->
            val selected = i == state.clockStyle
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(74.dp)
                    .clip(shape)
                    .background(Raised)
                    .then(if (selected) Modifier.border(2.dp, if (state.panelRow == 0) accent else Color.White.copy(alpha = 0.4f), shape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                ClockStyleThumb(i, time, if (selected) accent else Color.White)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    SectionLabel("צבע", state.panelRow == 1)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LockCatalog.clockColors.indices.forEach { i ->
            val color = if (i == 0) accent else FutureAccents.presets.getOrElse(i - 1) { Color.White }
            val selected = i == state.clockColor
            Box(
                Modifier
                    .size(36.dp)
                    .then(if (selected) Modifier.border(2.dp, if (state.panelRow == 1) accent else Color.White.copy(alpha = 0.4f), CircleShape).padding(4.dp) else Modifier)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
    SheetHint("${LockCatalog.clockStyles[state.clockStyle]} · ${LockCatalog.clockColors[state.clockColor]} · חצים לשינוי · חזור לסיום")
}

@Composable
private fun ClockStyleThumb(style: Int, time: String, color: Color) {
    when (style) {
        0 -> Text(time, color = color, fontSize = 15.sp, fontWeight = FontWeight.Light)
        1 -> Text(time, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraLight)
        2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val parts = time.split(":")
            Text(parts[0].padStart(2, '0'), color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
            Text(parts.getOrElse(1) { "" }, color = color.copy(alpha = 0.6f), fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
        }
        3 -> AnalogClock(Date(), color, Modifier.size(36.dp))
        else -> Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.Start) {
            Text(time, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Box(Modifier.width(24.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
        }
    }
}

// --- ווידג'טים

@Composable
private fun WidgetsPanel(state: LockUiState, accent: Color) {
    val selected = state.widgets.filter { it != "none" }
    SheetTitle("ווידג'טים", "${selected.size} מתוך ${LockSettings.WIDGET_SLOTS}")
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LockCatalog.widgetChoices.forEachIndexed { i, id ->
            val focused = i == state.panelIndex
            val order = selected.indexOf(id)
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(shape)
                    .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.06f)).border(2.dp, accent, shape) else Modifier)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(widgetIcon(id), null, tint = if (order >= 0) accent else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(LockCatalog.widgetLabel(id), color = Color.White.copy(alpha = if (order >= 0) 1f else 0.8f), fontSize = 15.sp, fontWeight = if (order >= 0) FontWeight.Medium else FontWeight.Normal, modifier = Modifier.weight(1f))
                if (order >= 0) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                        Text("${order + 1}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(Modifier.size(22.dp).border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                }
            }
        }
    }
    SheetHint("OK להוספה או הסרה · חזור לסיום")
}

// --- קיצורים

@Composable
private fun ShortcutsPanel(state: LockUiState, accent: Color) {
    SheetTitle("קיצורים")
    Spacer(Modifier.height(12.dp))
    // הלשוניות לפי המיקום הפיזי של המקשים: שמאל = חזור, ימין = תפריט
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().height(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("שמאלי · חזור", "ימני · תפריט").forEachIndexed { side, label ->
                val selected = side == state.shortcutSide
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .then(if (selected) Modifier.background(Color.White.copy(alpha = 0.18f)) else Modifier)
                        .then(if (selected && state.panelRow == 0) Modifier.border(2.dp, accent, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White.copy(alpha = if (selected) 1f else 0.7f), fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    val current = if (state.shortcutSide == 0) state.leftShortcut else state.rightShortcut
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LockCatalog.shortcutChoices.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { id ->
                    val selected = id == current
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(shape)
                            .then(
                                if (selected) Modifier.background(Color.White.copy(alpha = 0.06f))
                                    .border(2.dp, if (state.panelRow == 1) accent else Color.White.copy(alpha = 0.4f), shape)
                                else Modifier
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(shortcutIcon(id), null, tint = if (selected) accent else Color.White.copy(alpha = if (id == "none") 0.6f else 1f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(LockCatalog.shortcutLabel(id), color = Color.White.copy(alpha = if (selected) 1f else 0.8f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
    SheetHint("חצים לבחירה · חזור לסיום")
}

// --- רקע

@Composable
private fun WallpaperPanel(state: LockUiState, accent: Color) {
    SheetTitle("רקע")
    Spacer(Modifier.height(12.dp))
    val categories = state.wallpaperCategories
    val chips = rememberLazyListState()
    LaunchedEffect(state.wallpaperCategory) { chips.animateScrollToItem(state.wallpaperCategory) }
    LazyRow(state = chips, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(categories) { i, name ->
            val selected = i == state.wallpaperCategory
            Box(
                Modifier
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else Color.White.copy(alpha = 0.08f))
                    .then(if (selected && state.panelRow == 0) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(name, color = if (selected) Color.Black else Color.White, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    val tiles = state.wallpaperTiles
    val grid = rememberLazyGridState()
    val focus = state.panelIndex.coerceIn(0, tiles.lastIndex)
    LaunchedEffect(focus, state.wallpaperCategory) {
        val info = grid.layoutInfo
        val visible = info.visibleItemsInfo
        val item = visible.firstOrNull { it.index == focus }
        val fullyVisible = item != null && item.offset.y >= 0 && item.offset.y + item.size.height <= info.viewportEndOffset
        if (!fullyVisible) {
            val first = visible.firstOrNull()?.index ?: 0
            grid.animateScrollToItem(if (focus < first) focus / 3 * 3 else ((focus / 3 - 1) * 3).coerceAtLeast(0))
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = grid,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(208.dp)
    ) {
        itemsIndexed(tiles, key = { _, id -> id.ifEmpty { "device" } }) { i, id ->
            WallpaperTile(state, id, focused = state.panelRow == 1 && i == focus, accent = accent)
        }
    }
    Spacer(Modifier.height(10.dp))
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .then(if (state.panelRow == 2) Modifier.border(2.dp, accent, shape) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("טשטוש", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        LockSwitch(state.background == 1, accent)
    }
    SheetHint("OK לבחירה · חזור לסיום")
}

@Composable
private fun WallpaperTile(state: LockUiState, id: String, focused: Boolean, accent: Color) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(if (focused) Modifier.border(2.dp, accent, shape).padding(3.dp) else Modifier)
            .clip(if (focused) RoundedCornerShape(11.dp) else shape)
            .background(Raised)
    ) {
        val image: ImageBitmap? = if (id.isEmpty()) {
            state.deviceWallpaper
        } else {
            val context = LocalContext.current
            val loaded by produceState(LockWallpapers.peekThumb(id)?.asImageBitmap(), id) {
                if (value == null) value = withContext(Dispatchers.IO) { LockWallpapers.thumb(context, id)?.asImageBitmap() }
            }
            loaded
        }
        if (image != null) Image(image, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (id.isEmpty()) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                Text("נוכחי", color = Color.White, fontSize = 12.sp)
            }
        }
        if (id == state.wallpaperId) {
            Box(Modifier.align(Alignment.TopStart).padding(6.dp).size(20.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }
        if (id == state.wallpaperLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, strokeCap = StrokeCap.Round, modifier = Modifier.align(Alignment.Center).size(22.dp))
        }
    }
}

@Composable
private fun LockSwitch(on: Boolean, accent: Color) {
    // מתג הדיזיין סיסטם: דלוק = מסילה בצבע ההדגשה ואגודל בצבע המסך, כדי שיעבוד גם כשההדגשה לבנה
    Box(
        Modifier.width(44.dp).height(24.dp).clip(CircleShape).background(if (on) accent else Color.White.copy(alpha = 0.18f)).padding(3.dp),
        // RTL: דלוק = האגודל בצד שמאל (הסוף), כמו ב-FutureSwitch
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(if (on) Color.Black else Color.White))
    }
}
