package com.future.music.ui.screens

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
import com.future.music.ui.components.FocusableItem
import com.future.music.ui.components.MiniPlayerBar
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.components.SongRow
import com.future.music.ui.theme.FutureTheme

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
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = title,
            theme = theme,
            onBack = onBack,
            trailingIcon = topBarTrailingIcon,
            trailingContentDescription = topBarTrailingDescription,
            onTrailingClick = onTopBarTrailingClick,
        )

        if (songs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = playerState.currentSong?.id == song.id
                    FocusableItem(
                        onClick = { onPlaySong(index) },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                    ) { isFocused ->
                        SongRow(song = song, isCurrent = isCurrent, isPlaying = playerState.isPlaying, isFocused = isFocused, theme = theme)
                    }
                }
            }
        }

        if (playerState.currentSong != null) {
            MiniPlayerBar(playerState = playerState, theme = theme, onClick = onOpenNowPlaying, onTogglePlay = onTogglePlay)
        }
    }
}
