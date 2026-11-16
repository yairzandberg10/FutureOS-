package com.future.systemui.statusbar

import android.media.AudioManager
import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.future.sharednav.i18n.Tr
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureAccents
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.delay

private val GlyphSize = 16.dp
private val GlyphGap = 3.dp

/** App icons shown before they turn into a "+N" counter. */
private const val MAX_APP_ICONS = 3

/**
 * Optional device glyphs (alarm, ringer, DND, Bluetooth...) shown at most. The bar is 320dp wide:
 * without a cap a busy state pushed the clock out of the bar. Privacy, network and battery are
 * never dropped - only the least important of these.
 */
private const val MAX_STATUS_GLYPHS = 5

/**
 * The status bar itself. Its tone follows the app under it: APPEARANCE_LIGHT_STATUS_BARS (reported
 * by the framework in onSystemBarAttributesChanged) gives the light theme background with dark
 * ink, otherwise black with white ink - no screenshots, no guessing. FutureOS apps declare that
 * appearance from the shared theme (SharedKeypadNav StatusBarInset) and paint the same theme
 * background under the bar, so the bar reads as part of the app.
 *
 * Opaque rather than transparent: apps that are not edge-to-edge (the launcher, Notes...) get the
 * Material decor's grey status bar colour under it, which a transparent bar would show.
 */
@Composable
internal fun StatusBarContent(monitor: DeviceStatusMonitor, privacy: PrivacyMonitor) {
    val light by StatusBarState.lightBackground.collectAsState()
    val lowProfile by StatusBarState.lowProfile.collectAsState()
    val apps by StatusBarState.notificationApps.collectAsState()
    val count by StatusBarState.notificationCount.collectAsState()
    val icons by StatusBarState.icons.collectAsState()
    val clockHidden by StatusBarState.clockHidden.collectAsState()
    val notifHidden by StatusBarState.notificationIconsHidden.collectAsState()
    val sysHidden by StatusBarState.systemInfoHidden.collectAsState()

    val theme = remember(light) { FutureTheme(isDarkMode = !light) }
    val ink by animateColorAsState(theme.textColor, FutureMotion.standard<Color>(), label = "barInk")
    val background by animateColorAsState(theme.backgroundColor, FutureMotion.standard<Color>(), label = "barBackground")
    val palette = BarPalette(ink = ink, success = theme.successColor, danger = theme.dangerColor, warning = theme.warningColor)
    val density = LocalDensity.current
    // Low profile (an app asking for a quiet bar, e.g. a video): dimmed, notification icons hidden.
    val contentAlpha = if (lowProfile) 0.5f else 1f

    CompositionLocalProvider(
        LocalLayoutDirection provides Tr.layoutDirection,
        // Fixed font scale so a large system font never squeezes the bar.
        LocalDensity provides Density(density.density, fontScale = 1f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().background(background).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Start side: call, clock, notification icons. The clock never shrinks; the icons get
            // whatever room is left and are clipped first.
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight().alpha(contentAlpha),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (monitor.callStartedAt != 0L) CallPill(monitor.callStartedAt)
                if (!clockHidden) BarText(monitor.time, palette.ink, FutureTypography.body, FontWeight.SemiBold)
                if (!notifHidden && !lowProfile && apps.isNotEmpty()) {
                    Box(modifier = Modifier.weight(1f, fill = false).clipToBounds()) { AppIcons(apps, count, palette) }
                }
            }
            if (!sysHidden) {
                Row(horizontalArrangement = Arrangement.spacedBy(GlyphGap), verticalAlignment = Alignment.CenterVertically) {
                    // Privacy is never dimmed, not even in low profile.
                    PrivacyGlyphs(privacy, palette)
                    Row(
                        modifier = Modifier.alpha(contentAlpha),
                        horizontalArrangement = Arrangement.spacedBy(GlyphGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusGlyphs(monitor, palette, icons)
                        NetworkGlyphs(monitor, palette)
                        Battery(monitor, palette)
                    }
                }
            }
        }
    }
}

private class BarPalette(val ink: Color, val success: Color, val danger: Color, val warning: Color) {
    val dim: Color get() = ink.copy(alpha = 0.3f)
    val soft: Color get() = ink.copy(alpha = 0.7f)
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
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}

@Composable
private fun Glyph(icon: ImageVector, tint: Color, description: String?) {
    Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(GlyphSize))
}

// ---------------------------------------------------------------- start side

/** Green pill with the running call time, counted from the moment the call went off-hook. Same bright green in both tones, so black text always reads. */
@Composable
private fun CallPill(startedAt: Long) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000L - (now - startedAt) % 1_000L)
        }
    }
    val seconds = ((now - startedAt) / 1000L).coerceAtLeast(0L)
    val label = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
    Row(
        modifier = Modifier.height(22.dp).clip(CircleShape).background(FutureAccents.Green).padding(horizontal = 9.dp)
            .semantics { contentDescription = tr("שיחה פעילה") },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(FutureIcons.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
        BarText(label, Color.Black, FutureTypography.label, FontWeight.Bold)
    }
}

/** The apps that have notifications, each with its own small icon (like stock Android). */
@Composable
private fun AppIcons(apps: List<NotificationAppIcon>, count: Int, palette: BarPalette) {
    Row(
        modifier = Modifier.semantics { contentDescription = tr("התראות: {0}", count) },
        horizontalArrangement = Arrangement.spacedBy(GlyphGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        apps.take(MAX_APP_ICONS).forEach { app ->
            val bmp = app.bitmap
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = null, colorFilter = ColorFilter.tint(palette.ink), modifier = Modifier.size(GlyphSize))
            } else {
                Glyph(FutureIcons.Notifications, palette.ink, null)
            }
        }
        if (apps.size > MAX_APP_ICONS) BarText("+${apps.size - MAX_APP_ICONS}", palette.soft, FutureTypography.label, FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- end side

/** Camera green, microphone orange (the Android/iOS colours, recognised at a glance). Never capped. */
@Composable
private fun PrivacyGlyphs(privacy: PrivacyMonitor, palette: BarPalette) {
    if (privacy.camera) Glyph(FutureIcons.Videocam, FutureAccents.Green, tr("המצלמה פעילה"))
    if (privacy.microphone) Glyph(FutureIcons.Mic, FutureAccents.Orange, tr("המיקרופון פעיל"))
    if (privacy.location) Glyph(FutureIcons.LocationOn, palette.ink, tr("המיקום בשימוש"))
}

/** One optional glyph: a Future glyph or a framework slot bitmap. */
private class StatusItem(
    val icon: ImageVector? = null,
    val bitmap: ImageBitmap? = null,
    val tint: Color,
    val description: String?,
)

/** Airplane, ringer, DND, alarm, Bluetooth, headset and framework slot icons, most important first, capped. */
@Composable
private fun StatusGlyphs(monitor: DeviceStatusMonitor, palette: BarPalette, slots: List<StatusBarSlotIcon>) {
    val items = ArrayList<StatusItem>()
    if (monitor.airplane) items += StatusItem(FutureIcons.AirplanemodeActive, tint = palette.ink, description = tr("מצב טיסה"))
    when (monitor.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> items += StatusItem(FutureIcons.VolumeOff, tint = palette.ink, description = tr("שקט"))
        AudioManager.RINGER_MODE_VIBRATE -> items += StatusItem(FutureIcons.Vibration, tint = palette.ink, description = tr("רטט"))
    }
    if (monitor.dnd) items += StatusItem(FutureIcons.DoNotDisturbOn, tint = palette.ink, description = tr("נא לא להפריע"))
    if (monitor.alarmSet) items += StatusItem(FutureIcons.Alarm, tint = palette.ink, description = tr("שעון מעורר"))
    if (monitor.bluetoothOn) {
        val connected = monitor.bluetoothConnected
        items += StatusItem(
            FutureIcons.Bluetooth,
            tint = if (connected) palette.ink else palette.soft,
            description = if (connected) tr("Bluetooth מחובר") else tr("Bluetooth פועל"),
        )
    }
    if (monitor.wiredHeadset) items += StatusItem(FutureIcons.Headphones, tint = palette.ink, description = tr("אוזניות"))
    for (slot in slots) {
        val bmp = slot.bitmap ?: continue
        items += StatusItem(bitmap = bmp, tint = palette.ink, description = slot.description?.toString())
    }
    for (item in items.take(MAX_STATUS_GLYPHS)) {
        val icon = item.icon
        val bmp = item.bitmap
        when {
            icon != null -> Glyph(icon, item.tint, item.description)
            bmp != null -> Image(
                bitmap = bmp,
                contentDescription = item.description,
                modifier = Modifier.size(GlyphSize),
                colorFilter = ColorFilter.tint(item.tint),
            )
        }
    }
}

/** Wi-Fi (only while connected, with its level) and the cellular bars with the network type. */
@Composable
private fun NetworkGlyphs(monitor: DeviceStatusMonitor, palette: BarPalette) {
    if (monitor.wifiConnected) WifiLevel(monitor.wifiLevel, palette)
    if (monitor.airplane) return
    if (!monitor.simPresent) {
        Glyph(FutureIcons.SimCard, palette.soft, tr("אין כרטיס SIM"))
        return
    }
    // The type is the data connection's: shown only while mobile data is really up, and not
    // while Wi-Fi carries the traffic (same as stock).
    if (monitor.cellularData && !monitor.wifiConnected && monitor.cellType.isNotEmpty()) {
        BarText(monitor.cellType, palette.ink, FutureTypography.label, FontWeight.SemiBold)
    }
    SignalBars(if (monitor.cellInService) monitor.cellLevel else 0, palette, monitor.cellInService)
}

/**
 * The Wi-Fi glyph with its arcs lit by level: the whole glyph dimmed, then the lit part drawn on
 * top, clipped by a circle around the point the arcs share (12, 19.6 on the 24 grid).
 */
@Composable
private fun WifiLevel(level: Int, palette: BarPalette) {
    val lit = when (level) { 0, 1 -> 1; 2 -> 2; else -> 3 }
    val radius = when (lit) { 1 -> 5f; 2 -> 10.6f; else -> 24f }
    Box(modifier = Modifier.semantics { contentDescription = tr("Wi-Fi: {0} מתוך {1}", level, DeviceStatusMonitor.MAX_LEVEL) }) {
        Icon(FutureIcons.Wifi, contentDescription = null, tint = palette.dim, modifier = Modifier.size(GlyphSize))
        Icon(
            FutureIcons.Wifi,
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier.size(GlyphSize).drawWithContent {
                val unit = size.width / 24f
                val clip = Path().apply { addOval(Rect(Offset(12f * unit, 19.6f * unit), radius * unit)) }
                clipPath(clip) { this@drawWithContent.drawContent() }
            },
        )
    }
}

/** The SignalCellularAlt glyph's four bars, drawn so each one lights by level. */
@Composable
private fun SignalBars(level: Int, palette: BarPalette, inService: Boolean) {
    val ink = palette.ink
    val dim = palette.dim
    val description = if (inService) tr("קליטה: {0} מתוך {1}", level, DeviceStatusMonitor.MAX_LEVEL) else tr("אין שירות")
    Canvas(modifier = Modifier.size(GlyphSize).semantics { contentDescription = description }) {
        val unit = size.width / 24f
        val stroke = 1.6f * unit
        BAR_X.forEachIndexed { i, x ->
            drawLine(
                color = if (inService && i < level) ink else dim,
                start = Offset(x * unit, 19f * unit),
                end = Offset(x * unit, BAR_TOP[i] * unit),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

private val BAR_X = floatArrayOf(5.4f, 10f, 14.6f, 19.2f)
private val BAR_TOP = floatArrayOf(15f, 11.4f, 8f, 4.6f)

@Composable
private fun Battery(monitor: DeviceStatusMonitor, palette: BarPalette) {
    val percent = monitor.batteryPercent
    val low = percent <= 15 && !monitor.charging
    BarText("$percent%", if (low) palette.danger else palette.ink, FutureTypography.label, FontWeight.Medium)
    BatteryPill(percent, monitor.charging, monitor.batterySaver, palette)
}

/** Hand-drawn battery: rounded body, small nub, proportional fill. Battery saver = warning-coloured frame. */
@Composable
private fun BatteryPill(percent: Int, isCharging: Boolean, saver: Boolean, palette: BarPalette) {
    val fillColor = when {
        isCharging -> palette.success
        percent <= 15 -> palette.danger
        else -> palette.ink
    }
    val bodyColor = if (saver) palette.warning else palette.ink.copy(alpha = 0.85f)
    val description = when {
        isCharging -> tr("סוללה {0}%, בטעינה", percent)
        saver -> tr("סוללה {0}%, חיסכון בסוללה", percent)
        else -> tr("סוללה {0}%", percent)
    }
    Row(
        modifier = Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCharging) Icon(FutureIcons.Bolt, contentDescription = null, tint = palette.success, modifier = Modifier.size(13.dp))
        Canvas(modifier = Modifier.size(width = 25.dp, height = 12.dp)) {
            val nubWidth = 1.8.dp.toPx()
            val bodyWidth = size.width - nubWidth
            val strokeWidth = 1.2.dp.toPx()
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(bodyWidth - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = strokeWidth),
            )
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(bodyWidth, size.height / 2f - 2.2.dp.toPx()),
                size = Size(nubWidth, 4.4.dp.toPx()),
                cornerRadius = CornerRadius(0.9.dp.toPx()),
            )
            val inset = strokeWidth + 1.3.dp.toPx()
            val fillMaxWidth = (bodyWidth - inset * 2).coerceAtLeast(0f)
            val fillWidth = (fillMaxWidth * (percent / 100f)).coerceIn(0f, fillMaxWidth)
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(inset, inset),
                    size = Size(fillWidth, (size.height - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
            }
        }
    }
}
