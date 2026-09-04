package com.future.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.remote.data.DeviceCategory
import com.future.remote.data.IrTransmitter
import com.future.remote.data.RemoteDevice
import com.future.remote.data.RemoteRepository
import com.future.sharednav.theme.FutureTheme

fun iconForCategory(category: DeviceCategory): ImageVector = when (category) {
    DeviceCategory.AC -> Icons.Rounded.AcUnit
    DeviceCategory.FAN -> Icons.Rounded.Air
    DeviceCategory.AUDIO -> Icons.Rounded.Speaker
    DeviceCategory.CUSTOM -> Icons.Rounded.Tune
}

@Composable
fun RemoteHomeScreen(
    theme: FutureTheme,
    refreshKey: Int,
    onOpenDevice: (RemoteDevice) -> Unit,
    onAddDevice: () -> Unit,
    onAddAcPreset: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }
    val irTransmitter = remember { IrTransmitter(context) }
    var devices by remember { mutableStateOf(listOf<RemoteDevice>()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(refreshKey) {
        devices = repository.loadDevices()
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RemoteHeader(title = "שלט אינפרא אדום", theme = theme)

                if (!irTransmitter.isAvailable) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "המכשיר הזה לא כולל משדר אינפרא אדום, אז אי אפשר להשתמש בשלט",
                            color = theme.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(devices) { index, device ->
                            RemoteRow(
                                icon = iconForCategory(device.category),
                                label = device.name,
                                subtitle = "${device.category.label} · ${device.buttons.size} כפתורים",
                                theme = theme,
                                onClick = { onOpenDevice(device) },
                                focusRequester = if (index == 0) focusRequester else null
                            )
                        }
                        item {
                            RemoteRow(
                                icon = Icons.Rounded.AcUnit,
                                label = "שלט מוכן למזגן",
                                subtitle = "אלקטרה ומותגים תואמים - בלי להזין קודים בעצמך",
                                theme = theme,
                                onClick = onAddAcPreset
                            )
                        }
                        item {
                            RemoteRow(
                                icon = Icons.Rounded.Add,
                                label = "הוסף מכשיר חדש",
                                subtitle = "מזגן, מאוורר, מערכת שמע או מותאם אישית",
                                theme = theme,
                                onClick = onAddDevice,
                                focusRequester = if (devices.isEmpty()) focusRequester else null
                            )
                        }
                    }
                }
            }
        }
    }
}
