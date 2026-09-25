package com.future.music.ui.screens

import androidx.compose.material.icons.rounded.LibraryMusic
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.subtleTextColor

import com.future.sharednav.theme.FutureTypography
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
import com.future.music.ui.components.MiniPlayerBar

import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme

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
    onOpenEqualizer: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onTogglePlay: () -> Unit,
    // הפריט שנפתח לאחרונה מהתפריט הזה (לפי digit) - כשחוזרים "אחורה", הפוקוס
    // צריך לשוב אליו בדיוק, לא תמיד לפריט הראשון.
    lastOpenedItemId: String? = null,
) {
    val type = rememberFutureType()
    val items = listOf(
        HomeItem("1", Icons.Rounded.LibraryMusic, "כל השירים", "כל המוזיקה שבטלפון", onOpenAllSongs),
        HomeItem("2", FutureIcons.Person, "אמנים", "לפי זמר/זמרת", onOpenArtists),
        HomeItem("3", FutureIcons.Album, "אלבומים", "לפי אלבום", onOpenAlbums),
        HomeItem("4", FutureIcons.AutoMirrored.QueueMusic, "פלייליסטים", "הרשימות שלי", onOpenPlaylists),
        HomeItem("5", FutureIcons.Favorite, "מועדפים", "השירים שאהבת", onOpenFavorites),
        HomeItem("6", FutureIcons.Search, "חיפוש", "שיר, אמן או אלבום", onOpenSearch),
        HomeItem("7", FutureIcons.Equalizer, "אקולייזר", "אקולייזר מלא ואפקטים", onOpenEqualizer),
        HomeItem("8", FutureIcons.Headphones, "התקני שמע", "Bluetooth, אוזניות ורמקולים", onOpenDevices),
        HomeItem("9", FutureIcons.AutoMirrored.QueueMusic, "תור הניגון", "מה מתנגן הבא", onOpenQueue),
    )

    // פוקוס אוטומטי ומלא על הפריט הראשון בתפריט מיד כשהוא נפתח - בלי צורך
    // בלחיצה כלשהי קודם (למשל OK) כדי שהפוקוס הראשון יופיע.
    val itemFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(Unit) {
        val target = items.firstOrNull { it.digit == lastOpenedItemId } ?: items.firstOrNull()
        target?.let { itemFocusRequesters.getOrPut(it.digit) { FocusRequester() }.requestFocus() }
    }

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
            fontSize = FutureTypography.headline,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            itemsIndexed(items) { _, item ->
                FutureListItem(
                    title = item.label,
                    summary = item.subtitle,
                    theme = theme,
                    onClick = item.onClick,
                    focusRequester = itemFocusRequesters.getOrPut(item.digit) { FocusRequester() },
                    leading = { FutureAvatar(theme = theme, icon = item.icon) },
                    trailing = { Text(item.digit, color = theme.subtleTextColor, fontSize = type.body, fontWeight = FontWeight.Bold) },
                )
            }
        }

        if (playerState.currentSong != null) {
            MiniPlayerBar(playerState = playerState, theme = theme, onClick = onOpenNowPlaying, onTogglePlay = onTogglePlay)
        }
    }
}
