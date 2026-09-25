package com.future.music.ui.components

import com.future.sharednav.icons.FutureIcons

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.music.data.Song
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * שורת שיר - רשימת שירים, תור, תוצאות חיפוש. שורת הרשימה של המערכת
 * (FutureListItem) עם אווטאר של אייקון; השיר שמתנגן עכשיו הוא הבחירה
 * של הרשימה, ולכן רק הכותרת שלו בצבע ההדגשה.
 */
@Composable
fun SongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    FutureListItem(
        title = song.title,
        summary = song.artist,
        theme = theme,
        onClick = onClick,
        modifier = modifier,
        titleColor = if (isCurrent) theme.readableAccentColor else theme.textColor,
        focusRequester = focusRequester,
        leading = {
            FutureAvatar(
                theme = theme,
                icon = if (isCurrent && isPlaying) FutureIcons.Equalizer else FutureIcons.MusicNote,
            )
        },
        trailing = { Text(formatDuration(song.durationMs), color = theme.subtleTextColor, fontSize = type.summary) },
    )
}
