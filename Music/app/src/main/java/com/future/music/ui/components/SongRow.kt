package com.future.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.data.Song
import com.future.sharednav.theme.FutureTheme

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** תוכן שורת שיר משותף - בשימוש ברשימת שירים גנרית ובתוצאות חיפוש. */
@Composable
fun SongRow(song: Song, isCurrent: Boolean, isPlaying: Boolean, isFocused: Boolean, theme: FutureTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(theme.accentColor.copy(alpha = if (isCurrent) 0.35f else if (isFocused) 0.22f else 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isCurrent && isPlaying) Icons.Rounded.Equalizer else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = theme.accentColor,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) theme.accentColor else theme.textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(song.artist, color = theme.textColor.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(formatDuration(song.durationMs), color = theme.textColor.copy(alpha = 0.4f), fontSize = 12.sp)
    }
}
