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
    val seconds = totalSeconds % 60
    // M-17: מעל שעה מציגים h:mm:ss ולא "125:30"
    if (totalSeconds >= 3600) return "%d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds / 60) % 60, seconds)
    return "%d:%02d".format(totalSeconds / 60, seconds)
}

/** M-15: ריבוי עברי - "שיר אחד", "שני שירים", "5 שירים". */
fun songCountText(n: Int): String = when (n) {
    1 -> "שיר אחד"
    2 -> "שני שירים"
    else -> "$n שירים"
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
