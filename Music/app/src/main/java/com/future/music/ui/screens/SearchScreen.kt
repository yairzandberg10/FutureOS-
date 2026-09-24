package com.future.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.future.music.data.Song
import com.future.music.playback.PlayerUiState
import com.future.music.ui.components.MiniPlayerBar
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.components.SongListItem
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.mutedTextColor

/**
 * חיפוש רגיל - שדה טקסט (המקלדת של המערכת), ותוצאות חיות לפי שם השיר,
 * האמן או האלבום, בלי תלות באותיות גדולות/קטנות. חץ למטה עובר לתוצאות.
 */
@Composable
fun SearchScreen(
    allSongs: List<Song>,
    theme: FutureTheme,
    playerState: PlayerUiState,
    onBack: () -> Unit,
    onPlayResults: (songs: List<Song>, index: Int) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, allSongs) {
        val q = query.trim()
        if (q.isEmpty()) emptyList() else allSongs.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) || it.album.contains(q, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        ScreenTopBar(title = "חיפוש", theme = theme, onBack = onBack)

        FutureTextField(
            value = query,
            onValueChange = { query = it },
            theme = theme,
            placeholder = "שיר, אמן או אלבום",
            autoFocus = true,
            leading = {
                Icon(FutureIcons.Search, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        )

        when {
            query.isBlank() -> EmptyState(
                icon = FutureIcons.Search,
                title = "מה לנגן?",
                subtitle = "הקלידו שם של שיר, אמן או אלבום",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f),
            )
            results.isEmpty() -> EmptyState(
                icon = FutureIcons.SearchOff,
                title = "לא נמצאו שירים",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                itemsIndexed(results, key = { _, song -> song.id }) { index, song ->
                    SongListItem(
                        song = song,
                        isCurrent = playerState.currentSong?.id == song.id,
                        isPlaying = playerState.isPlaying,
                        theme = theme,
                        onClick = { onPlayResults(results, index) },
                    )
                }
            }
        }

        if (playerState.currentSong != null) {
            MiniPlayerBar(playerState = playerState, theme = theme, onClick = onOpenNowPlaying, onTogglePlay = onTogglePlay)
        }
    }
}
