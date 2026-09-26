package com.future.futureui.lockscreen.ui

import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.future.futureui.lockscreen.EditSector
import com.future.futureui.lockscreen.LockMode
import com.future.futureui.lockscreen.LockNotification
import com.future.futureui.lockscreen.LockUiState
import com.future.futureui.lockscreen.face.FaceStatus
import com.future.futureui.lockscreen.logic.LockCatalog
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
import com.future.sharednav.theme.FutureAccents
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val Danger = Color(0xFFFF6B6B)

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
    val editing = state.mode == LockMode.EDIT

    // כניסה: עולה מלמטה. פתיחה: ממשיך למעלה ודועך.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val density = LocalDensity.current
    val offset by animateFloatAsState(
        targetValue = when {
            state.unlocking -> with(density) { (-500).dp.toPx() }
            entered -> 0f
            else -> with(density) { 60.dp.toPx() }
        },
        animationSpec = tween(FutureMotion.DurationSlow, easing = if (state.unlocking) FutureMotion.EasingAccelerate else FutureMotion.EasingDecelerate),
        label = "lockOffset"
    )
    val alpha by animateFloatAsState(if (state.unlocking) 0f else 1f, tween(FutureMotion.DurationSlow), label = "lockAlpha")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.fillMaxSize().graphicsLayer { translationY = offset; this.alpha = alpha }) {
                LockBackground(state, accent, blurred = editing || state.mode == LockMode.PIN)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = (StatusBarLayoutManager.HEIGHT_DP + 10).dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LockIndicator(state, accent)
                    Spacer(Modifier.height(6.dp))
                    EditFrame(editing && state.editSector in setOf(EditSector.CLOCK_STYLE, EditSector.CLOCK_COLOR)) {
                        LockClock(now, is24, state.clockStyle, clockColor)
                    }
                    Spacer(Modifier.height(10.dp))
                    EditFrame(editing && state.editSector in setOf(EditSector.WIDGET_1, EditSector.WIDGET_2, EditSector.WIDGET_3)) {
                        WidgetsRow(state, now, accent)
                    }
                    if (state.ownerMessage.isNotBlank()) {
                        Text(
                            state.ownerMessage,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = FutureTypography.summary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (!editing) {
                        NotificationList(state, accent)
                        Spacer(Modifier.height(12.dp))
                    }
                    BottomBar(state, accent, editing)
                }

                PinOverlay(state, accent)
                if (editing) EditPanel(state, accent, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun LockBackground(state: LockUiState, accent: Color, blurred: Boolean) {
    val wallpaper = state.wallpaper
    val blurRadius = if (blurred || state.background == 1) 24.dp else 0.dp
    when {
        state.background == 2 || (wallpaper == null && state.background < 2) ->
            Box(Modifier.fillMaxSize().background(Color.Black))
        state.background == 3 ->
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color.Black))))
        else -> Image(
            bitmap = wallpaper!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(blurRadius)
        )
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (blurred) 0.5f else 0.22f)))
}

/** מנעול סגור / פתוח + מצב זיהוי הפנים, כמו בראש מסך הנעילה של אייפון. */
@Composable
private fun LockIndicator(state: LockUiState, accent: Color) {
    val open = state.authenticated || !state.hasPin
    val status = state.faceStatus
    val scanning = status == FaceStatus.SCANNING
    val pulse by animateFloatAsState(
        targetValue = if (scanning) 1.12f else 1f,
        animationSpec = if (scanning) repeatable(20, tween(500), RepeatMode.Reverse) else tween(FutureMotion.DurationStandard),
        label = "lockPulse"
    )
    val shake by animateFloatAsState(
        targetValue = if (status == FaceStatus.NOT_RECOGNIZED) 1f else 0f,
        animationSpec = if (status == FaceStatus.NOT_RECOGNIZED) repeatable(3, tween(60), RepeatMode.Reverse) else tween(0),
        label = "lockShake"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = when {
                open -> Icons.Rounded.LockOpen
                scanning -> Icons.Rounded.Face
                else -> Icons.Rounded.Lock
            },
            contentDescription = null,
            tint = if (open && state.hasPin) accent else Color.White,
            modifier = Modifier.size(22.dp).scale(pulse).graphicsLayer { translationX = shake * 10f }
        )
        val text = when {
            state.hasPin && state.authenticated -> "זוהית"
            status == FaceStatus.SCANNING -> "מחפש את הפנים שלך"
            status == FaceStatus.TOO_FAR -> "קרב את הטלפון"
            status == FaceStatus.TOO_DARK -> "חשוך מדי לזיהוי"
            status == FaceStatus.NOT_RECOGNIZED -> "הפנים לא זוהו"
            status == FaceStatus.UNAVAILABLE -> "המצלמה לא זמינה"
            else -> null
        }
        if (text != null) {
            Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = FutureTypography.caption, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun EditFrame(focused: Boolean, content: @Composable () -> Unit) {
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, FutureMotion.standard(), label = "editScale")
    Box(
        Modifier
            .scale(scale)
            .then(if (focused) Modifier.border(2.dp, Color.White, FutureShapes.xl) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { content() }
}

// ------------------------------------------------------------------ שעון

@Composable
fun LockClock(now: Date, is24: Boolean, style: Int, color: Color) {
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, d בMMMM", Locale("he")).format(now)
    AnimatedContent(targetState = style, transitionSpec = { fadeIn(tween(FutureMotion.DurationStandard)) togetherWith fadeOut(tween(FutureMotion.DurationFast)) }, label = "clockStyle") { s ->
        when (s) {
            // קלאסי (אייפון): תאריך קטן מעל שעה גדולה ועבה
            0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = FutureTypography.title, fontWeight = FontWeight.SemiBold)
                Text(time, color = color, fontSize = 84.sp, fontWeight = FontWeight.Bold, lineHeight = 90.sp)
            }
            // דק
            1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, color = color, fontSize = 92.sp, fontWeight = FontWeight.ExtraLight, lineHeight = 96.sp)
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Light)
            }
            // מוערם (One UI): שעות מעל דקות
            2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val parts = time.split(":")
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(parts[0].padStart(2, '0'), color = color, fontSize = 86.sp, fontWeight = FontWeight.Bold, lineHeight = 0.85.em)
                        Text(parts.getOrElse(1) { "" }, color = color.copy(alpha = 0.8f), fontSize = 86.sp, fontWeight = FontWeight.Bold, lineHeight = 0.85.em)
                    }
                }
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = FutureTypography.body)
            }
            // אנלוגי
            3 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnalogClock(now, color, Modifier.size(150.dp))
                Spacer(Modifier.height(6.dp))
                Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = FutureTypography.body)
            }
            // צד (One UI): צמוד לימין, שעה + תאריך בשתי שורות
            else -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(time, color = color, fontSize = 64.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(SimpleDateFormat("EEEE", Locale("he")).format(now), color = Color.White, fontSize = FutureTypography.title, fontWeight = FontWeight.SemiBold)
                    Text(SimpleDateFormat("d בMMMM", Locale("he")).format(now), color = Color.White.copy(alpha = 0.8f), fontSize = FutureTypography.body)
                }
            }
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
        drawCircle(Color.White.copy(alpha = 0.12f), r)
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val inner = if (i % 3 == 0) r * 0.78f else r * 0.86f
            drawLine(
                Color.White.copy(alpha = if (i % 3 == 0) 0.9f else 0.5f),
                Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                Offset(c.x + r * 0.93f * cos(a).toFloat(), c.y + r * 0.93f * sin(a).toFloat()),
                strokeWidth = if (i % 3 == 0) 4f else 2f, cap = StrokeCap.Round
            )
        }
        fun hand(angleDeg: Float, len: Float, width: Float, col: Color) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(col, c, Offset(c.x + len * cos(a).toFloat(), c.y + len * sin(a).toFloat()), strokeWidth = width, cap = StrokeCap.Round)
        }
        hand(hours * 30f, r * 0.5f, 9f, Color.White)
        hand(minutes * 6f, r * 0.75f, 6f, color)
        hand(seconds * 6f, r * 0.82f, 2f, color.copy(alpha = 0.7f))
        drawCircle(color, 6f, c)
    }
}

// --------------------------------------------------------------- ווידג'טים

@Composable
private fun WidgetsRow(state: LockUiState, now: Date, accent: Color) {
    val items = state.widgets.filter { it != "none" }
    if (items.isEmpty()) {
        if (state.mode == LockMode.EDIT) Text("אין ווידג'טים", color = Color.White.copy(alpha = 0.6f), fontSize = FutureTypography.label)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { id -> WidgetChip(id, state, now, accent) }
    }
}

@Composable
private fun WidgetChip(id: String, state: LockUiState, now: Date, accent: Color) {
    val (icon, text) = when (id) {
        "battery" -> (if (state.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd) to "${state.batteryPercent}%"
        "alarm" -> Icons.Rounded.Alarm to (state.nextAlarm ?: "אין")
        "hebdate" -> Icons.Rounded.CalendarMonth to hebrewDate(now)
        "media" -> Icons.Rounded.MusicNote to (state.mediaTitle ?: "לא מתנגן")
        "notifications" -> Icons.Rounded.Notifications to "${state.notifications.size}"
        else -> return
    }
    Row(
        Modifier
            .clip(FutureShapes.chip)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (id == "battery" && state.charging) accent else Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, fontSize = FutureTypography.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp))
    }
}

private fun hebrewDate(now: Date): String = runCatching {
    val fmt = android.icu.text.SimpleDateFormat("d MMMM", ULocale("he_IL@calendar=hebrew"))
    fmt.format(now)
}.getOrDefault("")

// ---------------------------------------------------------------- התראות

@Composable
private fun NotificationList(state: LockUiState, accent: Color) {
    val items = state.notifications
    if (items.isEmpty()) return
    // 0 הכל, 1 תוכן רק אחרי זיהוי, 2 אף פעם
    val showContent = state.notificationPrivacy == 0 || (state.notificationPrivacy == 1 && (state.authenticated || !state.hasPin))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, n -> NotificationCard(n, showContent, focused = i == state.focusedNotification, accent = accent) }
    }
}

@Composable
private fun NotificationCard(n: LockNotification, showContent: Boolean, focused: Boolean, accent: Color) {
    val time = remember(n.time) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(n.time)) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FutureShapes.lg)
            .background(if (focused) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.14f))
            .then(if (focused) Modifier.border(2.dp, accent, FutureShapes.lg) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (n.icon != null) {
            Image(n.icon, null, Modifier.size(28.dp).clip(CircleShape))
        } else {
            Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.appName, color = Color.White.copy(alpha = 0.75f), fontSize = FutureTypography.caption, modifier = Modifier.weight(1f), maxLines = 1)
                Text(time, color = Color.White.copy(alpha = 0.6f), fontSize = FutureTypography.caption)
            }
            if (showContent) {
                Text(n.title, color = Color.White, fontSize = FutureTypography.body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (n.text.isNotBlank()) {
                    Text(n.text, color = Color.White.copy(alpha = 0.85f), fontSize = FutureTypography.summary, maxLines = if (focused) 3 else 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text("התראה", color = Color.White, fontSize = FutureTypography.body, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------- קיצורים ותחתית

@Composable
private fun BottomBar(state: LockUiState, accent: Color, editing: Boolean) {
    if (editing) { Spacer(Modifier.height(250.dp)); return }
    Box(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        ShortcutCircle(state.leftShortcut, state.flashlightOn, "חזור", Modifier.align(AbsoluteAlignment.CenterLeft), accent)
        Text(
            text = when {
                state.focusedNotification >= 0 -> "OK לפתיחת ההתראה"
                !state.hasPin || state.authenticated -> "לחץ OK לפתיחה"
                else -> "לחץ OK או הקלד קוד"
            },
            color = Color.White.copy(alpha = 0.7f),
            fontSize = FutureTypography.caption,
            modifier = Modifier.align(Alignment.Center)
        )
        ShortcutCircle(state.rightShortcut, state.flashlightOn, "תפריט", Modifier.align(AbsoluteAlignment.CenterRight), accent)
    }
}

@Composable
private fun ShortcutCircle(id: String, flashlightOn: Boolean, keyHint: String, modifier: Modifier, accent: Color) {
    if (id == "none") return
    val active = id == "flashlight" && flashlightOn
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(shortcutIcon(id, active), null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(24.dp))
        }
        Text(keyHint, color = Color.White.copy(alpha = 0.55f), fontSize = FutureTypography.badge, modifier = Modifier.padding(top = 2.dp))
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
    else -> Icons.Rounded.Apps
}

// ------------------------------------------------------------------- קוד

@Composable
private fun PinOverlay(state: LockUiState, accent: Color) {
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(state.pinErrorTick) {
        if (state.pinErrorTick > 0) {
            repeat(3) {
                shake.animateTo(1f, tween(50)); shake.animateTo(-1f, tween(50))
            }
            shake.animateTo(0f, tween(50))
        }
    }
    AnimatedVisibility(
        visible = state.mode == LockMode.PIN,
        enter = fadeIn(tween(FutureMotion.DurationStandard)),
        exit = fadeOut(tween(FutureMotion.DurationFast)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(if (state.faceStatus == FaceStatus.SCANNING) Icons.Rounded.Face else Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    state.lockoutSeconds > 0 -> "יותר מדי ניסיונות"
                    state.pinError -> "קוד שגוי"
                    else -> "הזן קוד"
                },
                color = if (state.pinError || state.lockoutSeconds > 0) Danger else Color.White,
                fontSize = FutureTypography.screenTitle,
                fontWeight = FontWeight.SemiBold
            )
            val sub = when {
                state.lockoutSeconds > 0 -> "נסה שוב בעוד ${formatSeconds(state.lockoutSeconds)}"
                state.pinReason != null -> state.pinReason
                state.faceStatus == FaceStatus.SCANNING -> "או הבט בטלפון"
                else -> null
            }
            if (sub != null) {
                Text(sub!!, color = Color.White.copy(alpha = 0.75f), fontSize = FutureTypography.summary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.graphicsLayer { translationX = shake.value * 14f }
            ) {
                repeat(state.pinLength) { i ->
                    val filled = i < state.pinEntered
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (filled) Color.White else Color.Transparent)
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("חזור - מחיקה", color = Color.White.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
        }
    }
}

private fun formatSeconds(s: Int): String = if (s >= 60) "${(s + 59) / 60} דקות" else "$s שניות"

// ---------------------------------------------------------------- עריכה

@Composable
private fun EditPanel(state: LockUiState, accent: Color, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(FutureShapes.dialog)
            .background(Color(0xFF1C1C1E).copy(alpha = 0.94f))
            .padding(vertical = 8.dp)
    ) {
        Text(
            "עריכת מסך הנעילה",
            color = Color.White,
            fontSize = FutureTypography.title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        EditSector.values().forEach { s ->
            val focused = s == state.editSector
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(FutureShapes.md)
                    .background(if (focused) accent.copy(alpha = 0.25f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.label, color = Color.White, fontSize = FutureTypography.body, modifier = Modifier.weight(1f))
                if (focused) Text("‹ ", color = accent, fontSize = FutureTypography.body)
                Text(editValue(state, s), color = if (focused) accent else Color.White.copy(alpha = 0.7f), fontSize = FutureTypography.body, fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal)
                if (focused) Text(" ›", color = accent, fontSize = FutureTypography.body)
            }
        }
        Text(
            "חצים לשינוי · חזור לסיום",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = FutureTypography.caption,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

private fun editValue(state: LockUiState, s: EditSector): String = when (s) {
    EditSector.CLOCK_STYLE -> LockCatalog.clockStyles[state.clockStyle.coerceIn(0, LockCatalog.clockStyles.lastIndex)]
    EditSector.CLOCK_COLOR -> LockCatalog.clockColors[state.clockColor.coerceIn(0, LockCatalog.clockColors.lastIndex)]
    EditSector.BACKGROUND -> LockCatalog.backgrounds[state.background.coerceIn(0, LockCatalog.backgrounds.lastIndex)]
    EditSector.WIDGET_1 -> LockCatalog.widgetLabel(state.widgets[0])
    EditSector.WIDGET_2 -> LockCatalog.widgetLabel(state.widgets[1])
    EditSector.WIDGET_3 -> LockCatalog.widgetLabel(state.widgets[2])
    EditSector.LEFT_SHORTCUT -> LockCatalog.shortcutLabel(state.leftShortcut)
    EditSector.RIGHT_SHORTCUT -> LockCatalog.shortcutLabel(state.rightShortcut)
}
