package com.future.music.ui.screens
import com.future.sharednav.theme.FutureDimens

import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.data.Song
import com.future.music.playback.PlayerUiState
import com.future.music.ui.components.MiniPlayerBar
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.components.SongListItem
import com.future.sharednav.theme.FutureTheme

/** מסך רשימת שירים גנרי - משמש לכל השירים / שירי אמן / שירי אלבום / שירי
 * פלייליסט / מועדפים. OK על שורה מנגן את הרשימה הזו החל מהשיר הזה (הרשימה
 * כולה הופכת לתור הניגון). */
@Composable
fun SongListScreen(
    title: String,
    songs: List<Song>,
    theme: FutureTheme,
    playerState: PlayerUiState,
    onBack: () -> Unit,
    onPlaySong: (index: Int) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onTogglePlay: () -> Unit,
    emptyMessage: String = "אין כאן שירים",
    topBarTrailingIcon: ImageVector? = null,
    topBarTrailingDescription: String? = null,
    onTopBarTrailingClick: (() -> Unit)? = null,
    // שורת פעולה בראש הרשימה (למשל "הוספת שירים" בפלייליסט) - מקבלת פוקוס ראשונה.
    headerActionLabel: String? = null,
    onHeaderAction: (() -> Unit)? = null,
) {
    // פוקוס-בפתיחה חוזר לשיר שמתנגן כרגע (אם הוא ברשימה הזו) ולא תמיד לשיר
    // הראשון - כדי שחזרה ממסך "מתנגן עכשיו" לא תאבד את המיקום ברשימה. אם
    // השיר המתנגן גלל מחוץ לתחום הנראה של ה-LazyColumn, השורה שלו לא
    // קיימת בקומפוזיציה עדיין ו-requestFocus זורק - במקרה כזה נופלים חזרה
    // לשיר הראשון (תמיד קיים, כי הוא באזור הנראה הראשוני).
    val rowFocusRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    val initialFocusSongId = remember(songs, playerState.currentSong?.id) {
        songs.firstOrNull { it.id == playerState.currentSong?.id }?.id ?: songs.firstOrNull()?.id
    }
    LaunchedEffect(Unit) {
        val focusedTarget = initialFocusSongId?.let {
            try {
                rowFocusRequesters.getOrPut(it) { FocusRequester() }.requestFocus()
                true
            } catch (e: IllegalStateException) {
                false
            }
        } ?: false
        if (!focusedTarget) {
            songs.firstOrNull()?.let {
                try {
                    rowFocusRequesters.getOrPut(it.id) { FocusRequester() }.requestFocus()
                } catch (e: IllegalStateException) {
                    // הרשימה ריקה מתוכן ממורכב כרגע - אין מה לעשות פוקוס עליו.
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = title,
            theme = theme,
            onBack = onBack,
            trailingIcon = topBarTrailingIcon,
            trailingContentDescription = topBarTrailingDescription,
            onTrailingClick = onTopBarTrailingClick,
        )

        if (headerActionLabel != null && onHeaderAction != null) {
            val headerFocus = remember { FocusRequester() }
            LaunchedEffect(songs.isEmpty()) { if (songs.isEmpty()) runCatching { headerFocus.requestFocus() } }
            com.future.sharednav.components.FutureListItem(
                title = headerActionLabel,
                theme = theme,
                onClick = onHeaderAction,
                focusRequester = headerFocus,
                leading = { com.future.sharednav.components.FutureAvatar(theme = theme, icon = com.future.sharednav.icons.FutureIcons.Add) },
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
            )
        }

        if (songs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = playerState.currentSong?.id == song.id
                    SongListItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = playerState.isPlaying,
                        theme = theme,
                        onClick = { onPlaySong(index) },
                        focusRequester = rowFocusRequesters.getOrPut(song.id) { FocusRequester() },
                    )
                }
            }
        }

        if (playerState.currentSong != null) {
            MiniPlayerBar(playerState = playerState, theme = theme, onClick = onOpenNowPlaying, onTogglePlay = onTogglePlay)
        }
    }
}
