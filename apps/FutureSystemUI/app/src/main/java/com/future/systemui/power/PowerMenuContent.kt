package com.future.systemui.power

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.future.sharednav.i18n.tr
import com.future.sharednav.theme.FutureAccents
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.focusFillMenuColor
import com.future.sharednav.theme.mutedTextColor

/** The rows of the power menu, in D-pad order. */
internal enum class PowerAction { POWER_OFF, RESTART, AIRPLANE, CANCEL }

/** Observable state of the power menu; the key handler writes it, the composable draws it. */
internal class PowerMenuState(initialFocus: Int, airplaneOn: Boolean) {
    var focus by mutableStateOf(initialFocus)
    var airplaneOn by mutableStateOf(airplaneOn)
}

/** Power menu card, same look as FutureUI's PowerMenuScreen, driven by the D-pad. */
@Composable
internal fun PowerMenuContent(state: PowerMenuState) {
    val theme = LocalFutureTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(FutureShapes.xxl)
                .background(theme.elevatedSurfaceColor)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            PowerAction.values().forEachIndexed { index, action ->
                PowerRow(
                    action = action,
                    focused = index == state.focus,
                    airplaneOn = state.airplaneOn,
                    theme = theme,
                )
            }
        }
    }
}

/** Full-screen black screen shown while the device powers off or restarts. */
@Composable
internal fun ShutdownContent(text: String) {
    val theme = LocalFutureTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = theme.textColor,
            fontSize = FutureTypography.title,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PowerRow(
    action: PowerAction,
    focused: Boolean,
    airplaneOn: Boolean,
    theme: FutureTheme,
) {
    val icon: ImageVector = when (action) {
        PowerAction.POWER_OFF -> Icons.Rounded.PowerSettingsNew
        PowerAction.RESTART -> Icons.Rounded.RestartAlt
        PowerAction.AIRPLANE -> Icons.Rounded.AirplanemodeActive
        PowerAction.CANCEL -> Icons.Rounded.Close
    }
    val label: String = when (action) {
        PowerAction.POWER_OFF -> tr("כיבוי")
        PowerAction.RESTART -> tr("הפעלה מחדש")
        PowerAction.AIRPLANE -> tr("מצב טיסה")
        PowerAction.CANCEL -> tr("ביטול")
    }
    val tint: Color = when (action) {
        PowerAction.POWER_OFF -> theme.dangerColor
        else -> theme.textColor
    }
    val circle: Color = when (action) {
        PowerAction.POWER_OFF -> theme.dangerColor.copy(alpha = 0.18f)
        PowerAction.RESTART -> theme.successColor.copy(alpha = 0.2f)
        PowerAction.AIRPLANE -> FutureAccents.Cyan.copy(alpha = if (airplaneOn) 0.45f else 0.18f)
        PowerAction.CANCEL -> theme.textColor.copy(alpha = 0.12f)
    }
    val status: String? = if (action == PowerAction.AIRPLANE) {
        if (airplaneOn) tr("פועל") else tr("כבוי")
    } else {
        null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(FutureShapes.lg)
            .background(if (focused) theme.focusFillMenuColor else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = tint,
            fontSize = FutureTypography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (status != null) {
            Text(
                text = status,
                color = if (airplaneOn) theme.textColor else theme.mutedTextColor,
                fontSize = FutureTypography.summary,
                fontWeight = if (airplaneOn) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
