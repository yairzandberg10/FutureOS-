package com.future.music.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.future.music.playback.AudioFx
import com.future.music.playback.BluetoothAudio
import com.future.music.ui.components.FxSlider
import com.future.music.ui.components.ScreenTopBar
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.readableAccentColor
import kotlinx.coroutines.delay

/**
 * התקני שמע - Bluetooth ישירות מהמוזיקה (הפעלה, חיבור לאוזניות/בוקסה),
 * ומתחת הגדרות להתקן המחובר, בסגנון אפליקציות של יצרני אוזניות: אפקט
 * סאונד, אקולייזר מלא, תקרת עוצמה והמשך ניגון אוטומטי בחיבור. כל ההגדרות
 * נשמרות להתקן הזה בלבד.
 */
@Composable
fun AudioDevicesScreen(theme: FutureTheme, onBack: () -> Unit, onOpenEqualizer: () -> Unit) {
    val context = LocalContext.current
    val bt = remember { BluetoothAudio(context) }
    var version by remember { mutableIntStateOf(0) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) }

    DisposableEffect(hasPermission) {
        if (hasPermission) bt.start { version++ }
        onDispose { bt.stop() }
    }
    // סוללה וחיבור לא תמיד מגיעים כשידור - רענון עדין בזמן שהמסך פתוח.
    LaunchedEffect(hasPermission) {
        while (hasPermission) { delay(3000); version++ }
    }

    val enabled = remember(version, hasPermission) { hasPermission && bt.isEnabled }
    val devices = remember(version, hasPermission) { if (hasPermission && enabled) bt.devices() else emptyList() }
    val settings by AudioFx.settings.collectAsState()
    val deviceName by AudioFx.deviceName.collectAsState()
    val currentDevice by AudioFx.device.collectAsState()
    var showEffects by remember { mutableStateOf(false) }

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "התקני שמע", theme = theme, onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            item {
                FutureSettingItem(
                    title = "Bluetooth",
                    summary = when {
                        !hasPermission -> "צריך הרשאת Bluetooth"
                        enabled -> "מופעל"
                        else -> "כבוי"
                    },
                    theme = theme,
                    icon = if (enabled) FutureIcons.Bluetooth else FutureIcons.BluetoothDisabled,
                    showChevron = false,
                    focusRequester = first,
                    onClick = {
                        if (!hasPermission) {
                            permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else if (!bt.setEnabled(!enabled)) {
                            Toast.makeText(context, "לא ניתן לשנות את מצב ה-Bluetooth", Toast.LENGTH_SHORT).show()
                        }
                        version++
                    },
                    trailing = { FutureSwitch(enabled, theme) },
                )
            }

            if (enabled) {
                item { FutureSectionHeader("אוזניות ורמקולים", theme, inset = false) }
                if (devices.isEmpty()) {
                    item {
                        FutureSettingItem(
                            title = "אין התקנים מצומדים",
                            summary = "צמדו אוזניות או בוקסה באפליקציית Bluetooth",
                            theme = theme,
                            onClick = null,
                        )
                    }
                }
                items(devices, key = { it.address }) { device ->
                    FutureListItem(
                        title = device.name,
                        summary = buildString {
                            append(if (device.connected) "מחובר · OK לניתוק" else "OK לחיבור")
                            device.battery?.let { append(" · סוללה $it%") }
                        },
                        theme = theme,
                        onClick = {
                            val ok = if (device.connected) bt.disconnect(device.address) else bt.connect(device.address)
                            if (!ok) Toast.makeText(context, "הפעולה נכשלה", Toast.LENGTH_SHORT).show()
                            version++
                        },
                        leading = {
                            FutureAvatar(
                                theme = theme,
                                icon = if (device.isHeadphones) FutureIcons.Headphones else FutureIcons.Speaker,
                                contentColor = if (device.connected) theme.readableAccentColor else null,
                            )
                        },
                    )
                }
            }

            item { FutureSectionHeader("הגדרות ל$deviceName", theme, inset = false) }
            item {
                FutureSettingItem(
                    title = "אפקט סאונד",
                    summary = settings.presetName,
                    theme = theme,
                    icon = FutureIcons.GraphicEq,
                    onClick = { showEffects = true },
                )
            }
            item {
                FutureSettingItem(
                    title = "אקולייזר מלא",
                    summary = "כל התדרים, באס, 3D והגברה",
                    theme = theme,
                    icon = FutureIcons.Equalizer,
                    onClick = onOpenEqualizer,
                )
            }
            item {
                FxSlider(
                    label = "תקרת עוצמה",
                    valueText = if (settings.volumeLimit >= 100) "ללא" else "${settings.volumeLimit}%",
                    value = settings.volumeLimit / 100f,
                    theme = theme,
                    onValueChange = { v -> AudioFx.update { it.copy(volumeLimit = (v * 100).toInt().coerceIn(10, 100)) } },
                )
            }
            if (currentDevice != AudioFx.SPEAKER) {
                item {
                    FutureSettingItem(
                        title = "המשך ניגון בחיבור",
                        summary = "הניגון ממשיך כשההתקן הזה מתחבר",
                        theme = theme,
                        icon = FutureIcons.PlayArrow,
                        showChevron = false,
                        onClick = { AudioFx.update { it.copy(autoResume = !it.autoResume) } },
                        trailing = { FutureSwitch(settings.autoResume, theme) },
                    )
                }
            }
        }
    }

    if (showEffects) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { showEffects = false }, header = "אפקט סאונד") {
            AudioFx.Effect.entries.forEach { effect ->
                FutureMenuRow(effect.label, FutureIcons.GraphicEq, theme, {
                    showEffects = false
                    AudioFx.useEffect(effect)
                })
            }
        }
    }
}
