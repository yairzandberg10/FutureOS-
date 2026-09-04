package com.future.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.playback.MusicPlaybackService
import com.future.music.ui.components.FocusableItem
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.digitForKey
import com.future.sharednav.theme.FutureTheme

private data class EqPreset(val id: Int, val digit: String, val icon: ImageVector, val label: String, val subtitle: String)

/** אקולייזר אמיתי - הפריסטים נשלחים כ-custom session command ל-Equalizer
 * שרץ בתוך MusicPlaybackService (קשור ל-audioSessionId האמיתי של הנגן). */
@Composable
fun SoundScreen(theme: FutureTheme, onBack: () -> Unit, onSelectPreset: (Int) -> Unit) {
    // מאתחלים לפי הפריסט שבאמת פעיל כרגע ב-service, לא תמיד ל"רגיל" - אחרת
    // המסך היה מציג "רגיל" מודגש גם כשפריסט אחר כבר פעיל בפועל.
    var selected by remember { mutableIntStateOf(MusicPlaybackService.currentEqPreset.value) }

    val presets = listOf(
        EqPreset(MusicPlaybackService.EQ_PRESET_NORMAL, "1", Icons.Rounded.MusicNote, "רגיל", "בלי עיבוד"),
        EqPreset(MusicPlaybackService.EQ_PRESET_BASS, "2", Icons.Rounded.GraphicEq, "הגברת באסים", "מדגיש תדרים נמוכים"),
        EqPreset(MusicPlaybackService.EQ_PRESET_TREBLE, "3", Icons.Rounded.Equalizer, "הגברת טרבל", "מדגיש תדרים גבוהים"),
        EqPreset(MusicPlaybackService.EQ_PRESET_VOCAL, "4", Icons.Rounded.RecordVoiceOver, "קול", "מדגיש טווח קולי"),
    )

    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = digitForKey(event.key) ?: return@onKeyEvent false
                val match = presets.firstOrNull { it.digit == digit } ?: return@onKeyEvent false
                selected = match.id
                onSelectPreset(match.id)
                true
            }
    ) {
        ScreenTopBar(title = "סאונד", theme = theme, onBack = onBack)
        Text(
            "בחרו פריסט אקולייזר - הבחירה חלה מיד על הניגון",
            color = theme.textColor.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            presets.forEachIndexed { index, preset ->
                val isSelected = selected == preset.id
                FocusableItem(
                    onClick = {
                        selected = preset.id
                        onSelectPreset(preset.id)
                    },
                    theme = theme,
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = if (index == 0) firstItemFocusRequester else null,
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            preset.icon,
                            contentDescription = null,
                            tint = if (isSelected) theme.accentColor else theme.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.label, color = theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(preset.subtitle, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                        Text(preset.digit, color = theme.textColor.copy(alpha = 0.35f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
