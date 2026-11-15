package com.future.systemui.statusbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.future.sharednav.i18n.Tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography

private val GlyphSize = 16.dp
private val GlyphGap = 3.dp

/** The status bar itself: dark bar, clock + notification count on the start side, device glyphs on the end side. */
@Composable
internal fun StatusBarContent(monitor: DeviceStatusMonitor) {
    val theme = remember { FutureTheme(isDarkMode = true) }
    val ink = theme.textColor
    val count by StatusBarState.notificationCount.collectAsState()
    val icons by StatusBarState.icons.collectAsState()
    val clockHidden by StatusBarState.clockHidden.collectAsState()
    val notifHidden by StatusBarState.notificationIconsHidden.collectAsState()
    val sysHidden by StatusBarState.systemInfoHidden.collectAsState()
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalLayoutDirection provides Tr.layoutDirection,
        // Fixed font scale so a large system font never squeezes the bar.
        LocalDensity provides Density(density.density, fontScale = 1f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!clockHidden) BarText(monitor.time, ink, FutureTypography.body, FontWeight.SemiBold)
                if (!notifHidden && count > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph(FutureIcons.Notifications, ink)
                        BarText(count.toString(), ink.copy(alpha = 0.7f), FutureTypography.label, FontWeight.SemiBold)
                    }
                }
            }
            if (!sysHidden) {
                Row(horizontalArrangement = Arrangement.spacedBy(GlyphGap), verticalAlignment = Alignment.CenterVertically) {
                    for (icon in icons) {
                        val bmp = icon.bitmap ?: continue
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.size(GlyphSize),
                            colorFilter = ColorFilter.tint(ink),
                        )
                    }
                    if (monitor.bluetoothOn) Glyph(FutureIcons.Bluetooth, ink.copy(alpha = 0.7f))
                    if (monitor.wifiOn) Glyph(FutureIcons.Wifi, ink)
                    if (monitor.cellular) Glyph(FutureIcons.SignalCellularAlt, ink)
                    val low = monitor.batteryPercent <= 15 && !monitor.charging
                    BarText(
                        "${monitor.batteryPercent}%",
                        if (low) theme.dangerColor else ink,
                        FutureTypography.label,
                        FontWeight.Medium,
                    )
                    BatteryPill(monitor.batteryPercent, monitor.charging, theme)
                }
            }
        }
    }
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
private fun Glyph(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(GlyphSize))
}

/** Same hand-drawn battery as FutureUI's bar: rounded body, small nub, proportional fill. */
@Composable
private fun BatteryPill(percent: Int, isCharging: Boolean, theme: FutureTheme) {
    val fillColor = when {
        isCharging -> theme.successColor
        percent <= 15 -> theme.dangerColor
        else -> theme.textColor
    }
    val bodyColor = theme.textColor.copy(alpha = 0.85f)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isCharging) Icon(FutureIcons.Bolt, contentDescription = null, tint = theme.successColor, modifier = Modifier.size(13.dp))
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
