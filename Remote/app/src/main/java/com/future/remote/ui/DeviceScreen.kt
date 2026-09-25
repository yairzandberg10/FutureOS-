package com.future.remote.ui

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.subtleTextColor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.remote.data.IrTransmitter
import com.future.remote.data.RemoteButton
import com.future.remote.data.RemoteRepository
import com.future.sharednav.theme.FutureTheme

@Composable
fun DeviceScreen(theme: FutureTheme, deviceId: String, refreshKey: Int, onBack: () -> Unit, onAddButton: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RemoteRepository(context) }
    val irTransmitter = remember { IrTransmitter(context) }
    var device by remember { mutableStateOf(repository.loadDevices().firstOrNull { it.id == deviceId }) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(refreshKey) {
        device = repository.loadDevices().firstOrNull { it.id == deviceId }
    }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val currentDevice = device
    // מזגן מחברה מוכרת - שלט בצורת שלט, לא רשימת כפתורים.
    if (currentDevice?.acProtocol != null) {
        AcRemoteScreen(theme = theme, device = currentDevice, onBack = onBack)
        return
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RemoteHeader(
                    title = currentDevice?.name ?: "מכשיר",
                    theme = theme,
                    onBack = onBack,
                    trailing = {
                        RemoteIconButton(FutureIcons.Delete, "מחק מכשיר", theme = theme, tint = theme.dangerColor) {
                            repository.deleteDevice(deviceId)
                            onBack()
                        }
                    }
                )

                if (currentDevice == null) {
                    return@Column
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(currentDevice.buttons) { index, button ->
                        RemoteRow(
                            icon = FutureIcons.RadioButtonChecked,
                            label = button.label,
                            subtitle = "",
                            theme = theme,
                            onClick = {
                                val transmission = button.toTransmission()
                                val sent = transmission != null && irTransmitter.transmit(transmission.first, transmission.second)
                                Toast.makeText(context, if (sent) "נשלח" else "השליחה נכשלה", Toast.LENGTH_SHORT).show()
                            },
                            trailing = {
                                RemoteIconButton(FutureIcons.Delete, "מחק כפתור", theme = theme, tint = theme.textColor.copy(alpha = 0.4f)) {
                                    repository.deleteButton(deviceId, button.id)
                                    device = repository.loadDevices().firstOrNull { it.id == deviceId }
                                }
                            },
                            focusRequester = if (index == 0) focusRequester else null
                        )
                    }
                    item {
                        RemoteRow(
                            icon = FutureIcons.Add,
                            label = "הוסף כפתור",
                            subtitle = "קוד NEC (כתובת+פקודה) או תבנית גולמית",
                            theme = theme,
                            onClick = onAddButton,
                            focusRequester = if (currentDevice.buttons.isEmpty()) focusRequester else null
                        )
                    }
                }
            }
        }
    }
}
