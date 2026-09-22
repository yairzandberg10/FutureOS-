package com.future.bluetooth.ui
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureButton

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.bluetooth.data.BluetoothDeviceInfo
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme

@Composable
fun BluetoothScreen(
    theme: FutureTheme,
    isSupported: Boolean,
    hasPermission: Boolean,
    isEnabled: Boolean,
    isScanning: Boolean,
    pairedDevices: List<BluetoothDeviceInfo>,
    discoveredDevices: List<BluetoothDeviceInfo>,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onPair: (String) -> Unit,
    onUnpair: (String) -> Unit,
) {
    var unpairTarget by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }
    val canScan = isSupported && hasPermission && isEnabled

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "בלוטוס",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        trailingIcon = if (canScan) {
            if (isScanning) Icons.AutoMirrored.Rounded.BluetoothSearching else Icons.Rounded.Refresh
        } else null,
        trailingContentDescription = "סרוק מכשירים",
        onTrailingClick = if (canScan) {
            { if (isScanning) onStopScan() else onScan() }
        } else null,
    ) {
        when {
            !isSupported -> EmptyState(
                icon = Icons.Rounded.BluetoothDisabled,
                title = "אין תמיכה בבלוטוס במכשיר הזה",
                textColor = theme.textColor,
            )
            !hasPermission -> ActionPrompt(
                icon = Icons.Rounded.Bluetooth,
                title = "כדי להשתמש בבלוטוס צריך לאשר הרשאות",
                buttonLabel = "אשר הרשאה",
                theme = theme,
                onClick = onRequestPermission,
            )
            !isEnabled -> ActionPrompt(
                icon = Icons.Rounded.BluetoothDisabled,
                title = "הבלוטוס כבוי",
                subtitle = "הפעילו אותו כדי לראות ולזווג מכשירים",
                buttonLabel = "הפעל בלוטוס",
                theme = theme,
                onClick = onEnableBluetooth,
            )
            else -> BluetoothDeviceList(
                paired = pairedDevices,
                discovered = discoveredDevices,
                isScanning = isScanning,
                theme = theme,
                onDeviceClick = { device -> if (device.isBonded) unpairTarget = device else onPair(device.address) },
            )
        }

        unpairTarget?.let { device ->
            ConfirmDialog(
                message = "לבטל את הזיווג עם \"${device.name}\"?",
                surfaceColor = theme.surfaceColor,
                textColor = theme.textColor,
                dangerColor = theme.dangerColor,
                onCancel = { unpairTarget = null },
                onConfirm = { unpairTarget = null; onUnpair(device.address) },
            )
        }
    }
}

@Composable
private fun BluetoothDeviceList(
    paired: List<BluetoothDeviceInfo>,
    discovered: List<BluetoothDeviceInfo>,
    isScanning: Boolean,
    theme: FutureTheme,
    onDeviceClick: (BluetoothDeviceInfo) -> Unit,
) {
    // כמו בכל שאר מסכי הרשימה בסוויטה (ר' notes/Files) - פוקוס D-pad התחלתי
    // על הפריט הממשי הראשון, אחרת המסך עלול להיפתח בלי שום דבר ממוקד.
    val rowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val firstDeviceAddress = remember(paired, discovered) { (paired + discovered).firstOrNull()?.address }
    LaunchedEffect(firstDeviceAddress) {
        firstDeviceAddress?.let { rowFocusRequesters.getOrPut(it) { FocusRequester() }.requestFocus() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
    ) {
        item { SectionHeader("מכשירים מזווגים", theme) }
        if (paired.isEmpty()) {
            item { MutedHint("אין מכשירים מזווגים", theme) }
        } else {
            items(paired, key = { "paired:${it.address}" }) { device ->
                DeviceRow(device, theme, rowFocusRequesters.getOrPut(device.address) { FocusRequester() }) { onDeviceClick(device) }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { SectionHeader("מכשירים זמינים", theme) }
        if (discovered.isEmpty()) {
            item { MutedHint(if (isScanning) "מחפש מכשירים..." else "לא נמצאו מכשירים - נווטו ל\"סרוק\" ולחצו OK", theme) }
        } else {
            items(discovered, key = { "discovered:${it.address}" }) { device ->
                DeviceRow(device, theme, rowFocusRequesters.getOrPut(device.address) { FocusRequester() }) { onDeviceClick(device) }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: BluetoothDeviceInfo, theme: FutureTheme, focusRequester: FocusRequester, onClick: () -> Unit) {
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = FutureDimens.cardCornerRadius,
        contentPadding = 12.dp,
        focusRequester = focusRequester,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (device.isBonded) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = theme.accentColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, color = theme.textColor, fontSize = FutureTypography.bodyLarge, maxLines = 1)
                Text(device.address, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
            }
            if (device.isBonded) {
                Text("מזווג", color = theme.successColor, fontSize = FutureTypography.label, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** כותרת קטע - FutureSectionHeader של הדיזיין סיסטם (13sp, 55%, ריווח 1sp). */
@Composable
private fun SectionHeader(text: String, theme: FutureTheme) {
    FutureSectionHeader(text, theme, inset = false)
}

@Composable
private fun MutedHint(text: String, theme: FutureTheme) {
    Text(text, color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.summary, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun ActionPrompt(
    icon: ImageVector,
    title: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    buttonLabel: String,
    subtitle: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
        Text(
            title,
            color = theme.textColor.copy(alpha = 0.7f),
            fontSize = FutureTypography.title,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                color = theme.textColor.copy(alpha = 0.4f),
                fontSize = FutureTypography.body,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        FutureButton(buttonLabel, theme, onClick, focusRequester = focusRequester)
    }
}
