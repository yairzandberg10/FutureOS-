package com.future.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.future.music.data.Song
import com.future.music.ui.components.ScreenTopBar
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.mutedTextColor

/**
 * הוספת שירים לפלייליסט ישירות מתוך הפלייליסט: כל השירים עם תיבת סימון,
 * OK מוסיף/מסיר, ושדה סינון למעלה.
 */
@Composable
fun PlaylistAddSongsScreen(
    playlistName: String,
    allSongs: List<Song>,
    memberIds: List<Long>,
    theme: FutureTheme,
    onToggle: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    val shown = remember(filter, allSongs) {
        val q = filter.trim()
        if (q.isEmpty()) allSongs else allSongs.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    }

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        ScreenTopBar(title = "הוספה ל$playlistName", theme = theme, onBack = onBack)
        FutureTextField(
            value = filter,
            onValueChange = { filter = it },
            theme = theme,
            placeholder = "סינון שירים",
            autoFocus = true,
            leading = {
                Icon(FutureIcons.Search, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            items(shown, key = { it.id }) { song ->
                FutureListItem(
                    title = song.title,
                    summary = song.artist,
                    theme = theme,
                    onClick = { onToggle(song.id) },
                    trailing = { FutureCheckbox(song.id in memberIds, theme) },
                )
            }
        }
    }
}
