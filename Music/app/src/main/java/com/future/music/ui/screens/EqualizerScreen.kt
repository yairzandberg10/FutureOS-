package com.future.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.music.playback.AudioFx
import com.future.music.ui.components.FxSlider
import com.future.music.ui.components.ScreenTopBar
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme

/**
 * אקולייזר מלא - כל הפסים של המכשיר, פריסטים ואפקטים (באס, 3D, הגברה).
 * נפתח מהתפריט הראשי ולא רק ממסך הניגון. ההגדרות חלות מיד, ונשמרות להתקן
 * הפלט הנוכחי (רמקול / אוזניות מסוימות).
 */
@Composable
fun EqualizerScreen(theme: FutureTheme, onBack: () -> Unit) {
    val settings by AudioFx.settings.collectAsState()
    val deviceName by AudioFx.deviceName.collectAsState()
    var showPresets by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    val range = AudioFx.levelRange
    val span = (range.last - range.first).toFloat().coerceAtLeast(1f)
    val levels = if (settings.bandLevels.size == AudioFx.bands.size) settings.bandLevels else List(AudioFx.bands.size) { 0 }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "אקולייזר", theme = theme, onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            item {
                FutureSettingItem(
                    title = "אקולייזר ואפקטים",
                    summary = "פרופיל: $deviceName",
                    theme = theme,
                    icon = FutureIcons.Equalizer,
                    showChevron = false,
                    focusRequester = first,
                    onClick = { AudioFx.update { it.copy(enabled = !it.enabled) } },
                    trailing = { FutureSwitch(settings.enabled, theme) },
                )
            }
            item {
                FutureSettingItem(
                    title = "פריסט",
                    summary = settings.presetName,
                    theme = theme,
                    icon = FutureIcons.GraphicEq,
                    onClick = { showPresets = true },
                )
            }
            if (!AudioFx.hasEqualizer) {
                item {
                    FutureSettingItem(title = "האקולייזר עוד לא מוכן", summary = "נגנו שיר כדי להפעיל אותו", theme = theme, onClick = null)
                }
            }
            if (AudioFx.bands.isNotEmpty()) {
                item { FutureSectionHeader("תדרים", theme, inset = false) }
                items(AudioFx.bands, key = { it.index }) { band ->
                    val level = levels.getOrElse(band.index) { 0 }
                    FxSlider(
                        label = formatHz(band.centerHz),
                        valueText = "%+.1f dB".format(level / 100f),
                        value = (level - range.first) / span,
                        step = 100f / span,
                        theme = theme,
                        onValueChange = { AudioFx.setBand(band.index, (range.first + it * span).toInt()) },
                    )
                }
            }
            item { FutureSectionHeader("אפקטים", theme, inset = false) }
            item {
                FxSlider(
                    label = "הגברת באס",
                    valueText = "${settings.bass / 10}%",
                    value = settings.bass / 1000f,
                    theme = theme,
                    onValueChange = { v -> AudioFx.update { it.copy(bass = (v * 1000).toInt()) } },
                )
            }
            item {
                FxSlider(
                    label = "סאונד 3D",
                    valueText = "${settings.virtualizer / 10}%",
                    value = settings.virtualizer / 1000f,
                    theme = theme,
                    onValueChange = { v -> AudioFx.update { it.copy(virtualizer = (v * 1000).toInt()) } },
                )
            }
            item {
                FxSlider(
                    label = "הגברת עוצמה",
                    valueText = "+%.1f dB".format(settings.loudness / 100f),
                    value = settings.loudness / 1500f,
                    theme = theme,
                    onValueChange = { v -> AudioFx.update { it.copy(loudness = (v * 1500).toInt()) } },
                )
            }
            item {
                FutureSettingItem(
                    title = "איפוס",
                    summary = "כל הפסים והאפקטים חוזרים לאפס",
                    theme = theme,
                    icon = FutureIcons.RestartAlt,
                    showChevron = false,
                    onClick = { AudioFx.reset() },
                )
            }
        }
    }

    if (showPresets) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { showPresets = false }, header = "פריסט") {
            AudioFx.Effect.entries.forEach { effect ->
                FutureMenuRow(effect.label, FutureIcons.GraphicEq, theme, {
                    showPresets = false
                    AudioFx.useEffect(effect)
                })
            }
            AudioFx.systemPresets().forEach { name ->
                FutureMenuRow(name, FutureIcons.MusicNote, theme, {
                    showPresets = false
                    AudioFx.usePreset(name)
                })
            }
        }
    }
}

private fun formatHz(hz: Int): String = if (hz >= 1000) "%.1f kHz".format(hz / 1000f).replace(".0 ", " ") else "$hz Hz"
