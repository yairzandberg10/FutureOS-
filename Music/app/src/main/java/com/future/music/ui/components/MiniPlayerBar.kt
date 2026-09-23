package com.future.music.ui.components

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor

import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.playback.PlayerUiState
import com.future.sharednav.theme.FutureTheme

/** מיני-נגן קבוע בתחתית מסכי הרשימה - מוצג כשמנגן שיר. לחיצה על השורה
 * פותחת את מסך הניגון המלא, כפתור נפרד להשהיה/המשך בלי לצאת מהרשימה. */
@Composable
fun MiniPlayerBar(playerState: PlayerUiState, theme: FutureTheme, onClick: () -> Unit, onTogglePlay: () -> Unit) {
    val song = playerState.currentSong ?: return
    val progress = if (playerState.durationMs > 0) {
        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = FutureShapes.radiusLg, topEnd = FutureShapes.radiusLg))
            .background(theme.surfaceColor)
    ) {
        FutureProgressBar(progress = progress, theme = theme, mini = true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FutureAvatar(theme = theme, icon = FutureIcons.MusicNote, size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = theme.textColor, fontSize = FutureTypography.body, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(song.artist, color = theme.mutedTextColor, fontSize = FutureTypography.summary, maxLines = 1)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(theme.readableAccentColor)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playerState.isPlaying) FutureIcons.Pause else FutureIcons.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "השהה" else "נגן",
                    tint = theme.onReadableAccentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
