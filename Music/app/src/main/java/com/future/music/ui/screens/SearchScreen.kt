package com.future.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.data.Song
import com.future.music.playback.PlayerUiState
import com.future.music.ui.components.FocusableItem
import com.future.music.ui.components.MiniPlayerBar
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.components.SongRow
import com.future.music.ui.digitForKey
import com.future.music.ui.theme.FutureTheme
import com.future.music.util.T9Search

/** חיפוש T9: מקשי הספרות הפיזיים בונים רצף, ומתאמים חי מול כותרת/אמן של
 * כל שיר (בלי מקלדת מסך - עקבי עם אילוץ "בלי מסך מגע" של כל הסוויטה). */
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
    var query by remember { mutableStateOf("") }
    val results = remember(query, allSongs) {
        if (query.isEmpty()) emptyList() else allSongs.filter {
            T9Search.matchesAnyWord(it.title, query) || T9Search.matchesAnyWord(it.artist, query)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val firstResultFocusRequester = remember { FocusRequester() }
    LaunchedEffect(results) {
        if (results.isNotEmpty()) firstResultFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = digitForKey(event.key)
                if (digit != null) {
                    query += digit
                    return@onKeyEvent true
                }
                if (event.key == Key.Backspace || event.key == Key.Delete) {
                    if (query.isNotEmpty()) query = query.dropLast(1)
                    return@onKeyEvent true
                }
                false
            }
    ) {
        ScreenTopBar(title = "חיפוש", theme = theme, onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.textColor.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (query.isEmpty()) "הקלידו ספרות לחיפוש (T9)..." else query,
                color = if (query.isEmpty()) theme.textColor.copy(alpha = 0.4f) else theme.textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                Text("${results.size} תוצאות", color = theme.textColor.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }

        if (query.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("לחצו על מקשי הספרות כדי לחפש", color = theme.textColor.copy(alpha = 0.4f), fontSize = 13.sp)
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("לא נמצאו שירים", color = theme.textColor.copy(alpha = 0.4f), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(results, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = playerState.currentSong?.id == song.id
                    FocusableItem(
                        onClick = { onPlayResults(results, index) },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = if (index == 0) firstResultFocusRequester else null,
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
