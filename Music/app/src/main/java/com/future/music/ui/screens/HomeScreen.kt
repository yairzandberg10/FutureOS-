package com.future.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.playback.PlayerUiState
import com.future.music.ui.components.FocusableItem
import com.future.music.ui.components.MiniPlayerBar
import com.future.music.ui.digitForKey
import com.future.music.ui.theme.FutureTheme

private data class HomeItem(val digit: String, val icon: ImageVector, val label: String, val subtitle: String, val onClick: () -> Unit)

@Composable
fun HomeScreen(
    theme: FutureTheme,
    playerState: PlayerUiState,
    onOpenAllSongs: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    val items = listOf(
        HomeItem("1", Icons.Rounded.LibraryMusic, "כל השירים", "כל המוזיקה שבטלפון", onOpenAllSongs),
        HomeItem("2", Icons.Rounded.Person, "אמנים", "לפי זמר/זמרת", onOpenArtists),
        HomeItem("3", Icons.Rounded.Album, "אלבומים", "לפי אלבום", onOpenAlbums),
        HomeItem("4", Icons.AutoMirrored.Rounded.QueueMusic, "פלייליסטים", "הרשימות שלי", onOpenPlaylists),
        HomeItem("5", Icons.Rounded.Favorite, "מועדפים", "השירים שאהבת", onOpenFavorites),
        HomeItem("6", Icons.Rounded.Search, "חיפוש", "חיפוש T9 מהיר", onOpenSearch),
    )

    // פוקוס אוטומטי ומלא על הפריט הראשון בתפריט מיד כשהוא נפתח - בלי צורך
    // בלחיצה כלשהי קודם (למשל OK) כדי שהפוקוס הראשון יופיע.
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = digitForKey(event.key) ?: return@onKeyEvent false
                val match = items.firstOrNull { it.digit == digit } ?: return@onKeyEvent false
                match.onClick()
                true
            }
    ) {
        Text(
            "מוזיקה",
            color = theme.textColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(items) { index, item ->
                FocusableItem(
                    onClick = item.onClick,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = if (index == 0) firstItemFocusRequester else null,
                ) { isFocused ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(theme.accentColor.copy(alpha = if (isFocused) 0.35f else 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.label, color = theme.textColor, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text(item.subtitle, color = theme.textColor.copy(alpha = 0.55f), fontSize = 12.sp)
                        }
                        Text(item.digit, color = theme.textColor.copy(alpha = 0.35f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (playerState.currentSong != null) {
            MiniPlayerBar(playerState = playerState, theme = theme, onClick = onOpenNowPlaying, onTogglePlay = onTogglePlay)
        }
    }
}
