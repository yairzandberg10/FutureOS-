package com.future.remote.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.remote.data.AcFan
import com.future.remote.data.AcMode
import com.future.remote.data.AcProtocols
import com.future.remote.data.AcState
import com.future.remote.data.IrTransmitter
import com.future.remote.data.RemoteDevice
import com.future.remote.data.RemoteRepository
import com.future.sharednav.components.FutureActionCell
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.subtleTextColor

/**
 * שלט מזגן בצורת שלט, לא רשימת כפתורים: למעלה צג כמו של שלט אמיתי
 * (טמפרטורה גדולה, מצב, מאוורר, תנודה), ומתחתיו מקשי השלט. כל לחיצה
 * שולחת את כל המצב למזגן, כמו שלט אמיתי.
 *
 * מקשים: 5 הפעלה/כיבוי, 2/8 טמפרטורה, 1 מצב, 3 מאוורר, 7 תנודה. Options -
 * דגם שלט אחר (אם המזגן לא מגיב), ומחיקת המכשיר.
 */
@Composable
fun AcRemoteScreen(theme: FutureTheme, device: RemoteDevice, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }
    val ir = remember { IrTransmitter(context) }
    val company = device.acCompany
    var protocol by remember { mutableStateOf(device.acProtocol!!) }
    var state by remember { mutableStateOf(device.acState ?: AcState()) }
    var showMenu by remember { mutableStateOf(false) }
    onOptionsKeyPress { showMenu = true }

    fun send(next: AcState) {
        state = next
        repository.updateDevice(device.id) { it.copy(acState = next, acProtocol = protocol) }
        val ok = ir.isAvailable && ir.transmit(AcProtocols.CARRIER_HZ, AcProtocols.encode(protocol, next))
        if (ok) buzz(context) else Toast.makeText(context, "אין משדר אינפרא-אדום", Toast.LENGTH_SHORT).show()
    }

    fun power() = send(state.copy(power = !state.power))
    fun temp(delta: Int) = send(state.copy(power = true, temp = (state.temp + delta).coerceIn(AcProtocols.MIN_TEMP, AcProtocols.MAX_TEMP)))
    fun mode() = send(state.copy(power = true, mode = AcMode.entries[(state.mode.ordinal + 1) % AcMode.entries.size]))
    fun fan() = send(state.copy(power = true, fan = AcFan.entries[(state.fan.ordinal + 1) % AcFan.entries.size]))
    fun swing() = send(state.copy(power = true, swing = !state.swing))

    val powerFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { powerFocus.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (digitForKey(e.key)) {
                        "5" -> { power(); true }
                        "2" -> { temp(+1); true }
                        "8" -> { temp(-1); true }
                        "1" -> { mode(); true }
                        "3" -> { fan(); true }
                        "7" -> { swing(); true }
                        else -> false
                    }
                },
        ) {
            RemoteHeader(title = device.name, theme = theme, onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FutureDimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AcDisplay(theme, state, subtitle = listOfNotNull(company?.label, protocol.label).joinToString(" · "))
                Spacer(Modifier.height(FutureDimens.spacingLg))

                // מקש ההפעלה - רחב, באדום כשהמזגן דלוק (כמו בשלט).
                FutureActionCell(
                    icon = Icons.Rounded.PowerSettingsNew,
                    label = if (state.power) "כיבוי (5)" else "הפעלה (5)",
                    theme = theme,
                    onClick = ::power,
                    active = state.power,
                    iconColor = if (state.power) theme.dangerColor else null,
                    height = 72.dp,
                    focusRequester = powerFocus,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(FutureDimens.spacingSm))
                Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                    FutureActionCell(Icons.Rounded.KeyboardArrowUp, "חם יותר (2)", theme, { temp(+1) }, height = 72.dp, modifier = Modifier.weight(1f))
                    FutureActionCell(Icons.Rounded.KeyboardArrowDown, "קר יותר (8)", theme, { temp(-1) }, height = 72.dp, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(FutureDimens.spacingSm))
                Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                    FutureActionCell(modeIcon(state.mode), "מצב (1)", theme, ::mode, height = 72.dp, modifier = Modifier.weight(1f))
                    FutureActionCell(Icons.Rounded.Air, "מאוורר (3)", theme, ::fan, height = 72.dp, modifier = Modifier.weight(1f))
                    FutureActionCell(Icons.Rounded.SwapVert, "תנודה (7)", theme, ::swing, active = state.swing, height = 72.dp, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(FutureDimens.spacingMd))
                Text(
                    "כוונו את הטלפון אל המזגן. לא מגיב? Options ← דגם שלט אחר",
                    color = theme.subtleTextColor,
                    fontSize = FutureTypography.caption,
                )
                Spacer(Modifier.height(FutureDimens.spacingMd))
            }
        }
    }

    if (showMenu) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { showMenu = false }, header = device.name) {
            company?.protocols?.filter { it != protocol }?.forEach { other ->
                FutureMenuRow("דגם שלט: ${other.label}", FutureIcons.SwapHoriz, theme, {
                    showMenu = false
                    protocol = other
                    repository.updateDevice(device.id) { it.copy(acProtocol = other) }
                    Toast.makeText(context, "עכשיו: ${other.label}", Toast.LENGTH_SHORT).show()
                })
            }
            FutureMenuRow("מחיקת השלט", FutureIcons.Delete, theme, {
                showMenu = false
                repository.deleteDevice(device.id)
                onBack()
            }, destructive = true)
        }
    }
}

/** צג השלט - משטח כהה-בהיר עם טמפרטורה גדולה ושורת סטטוס. */
@Composable
private fun AcDisplay(theme: FutureTheme, state: AcState, subtitle: String) {
    val accent = theme.readableAccentColor
    val lit by animateColorAsState(if (state.power) accent else theme.subtleTextColor, FutureMotion.focusColorSpec, label = "acLit")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FutureShapes.xl)
            .background(theme.surfaceColor)
            .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(subtitle, color = theme.subtleTextColor, fontSize = FutureTypography.caption, maxLines = 1)
        Spacer(Modifier.height(FutureDimens.spacingSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(modeIcon(state.mode), contentDescription = null, tint = lit, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(FutureDimens.spacingMd))
            Text(
                if (state.power) "${state.temp}°" else "כבוי",
                color = if (state.power) theme.textColor else theme.subtleTextColor,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().alpha(if (state.power) 1f else 0.45f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.mode.label, color = theme.textColor, fontSize = FutureTypography.body)
            FanBars(state.fan, lit, theme)
            Text(if (state.swing) "תנודה" else "קבוע", color = theme.textColor, fontSize = FutureTypography.body)
        }
    }
}

/** עוצמת המאוורר כעמודות כמו בצג שלט; "אוטו'" כטקסט. */
@Composable
private fun FanBars(fan: AcFan, color: Color, theme: FutureTheme) {
    if (fan == AcFan.AUTO) {
        Text("מאוורר אוטו'", color = theme.textColor, fontSize = FutureTypography.body)
        return
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Rounded.Air, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(18.dp))
        for (level in 1..3) {
            Box(
                Modifier
                    .width(6.dp)
                    .height((6 + level * 5).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (level <= fan.ordinal) color else theme.subtleTextColor.copy(alpha = 0.3f)),
            )
        }
    }
}

private fun modeIcon(mode: AcMode): ImageVector = when (mode) {
    AcMode.COOL -> Icons.Rounded.AcUnit
    AcMode.HEAT -> Icons.Rounded.WbSunny
    AcMode.FAN -> Icons.Rounded.Air
    AcMode.DRY -> Icons.Rounded.WaterDrop
    AcMode.AUTO -> Icons.Rounded.AutoMode
}

private fun buzz(context: Context) {
    runCatching {
        context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
