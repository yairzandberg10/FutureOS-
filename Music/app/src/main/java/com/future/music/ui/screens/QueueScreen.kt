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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.playback.PlayerUiState
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.components.SongListItem
import com.future.sharednav.theme.FutureTheme

@Composable
fun QueueScreen(playerState: PlayerUiState, theme: FutureTheme, onBack: () -> Unit, onPlayAt: (Int) -> Unit) {
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "תור הניגון", theme = theme, onBack = onBack)

        if (playerState.queue.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("התור ריק", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                itemsIndexed(playerState.queue, key = { index, song -> "$index-${song.id}" }) { index, song ->
                    val isCurrent = index == playerState.currentIndex
                    SongListItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = playerState.isPlaying,
                        theme = theme,
                        onClick = { onPlayAt(index) },
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                    )
                }
            }
        }
    }
}
