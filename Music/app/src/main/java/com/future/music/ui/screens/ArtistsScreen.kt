package com.future.music.ui.screens

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.FutureDimens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.music.data.ArtistGroup
import com.future.music.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

@Composable
fun ArtistsScreen(artists: List<ArtistGroup>, theme: FutureTheme, onBack: () -> Unit, onOpenArtist: (String) -> Unit) {
    val type = rememberFutureType()
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "אמנים", theme = theme, onBack = onBack)
        if (artists.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("לא נמצאו אמנים", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                itemsIndexed(artists, key = { _, artist -> artist.name }) { index, artist ->
                    FutureListItem(
                        title = artist.name,
                        theme = theme,
                        onClick = { onOpenArtist(artist.name) },
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                        leading = { FutureAvatar(theme = theme, icon = FutureIcons.Person) },
                        trailing = { Text("${artist.songCount} שירים", color = theme.subtleTextColor, fontSize = type.summary) },
                    )
                }
            }
        }
    }
}
