package com.future.music.ui.screens
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.FutureDimens

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
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
import com.future.music.data.AlbumGroup
import com.future.music.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

@Composable
fun AlbumsScreen(albums: List<AlbumGroup>, theme: FutureTheme, onBack: () -> Unit, onOpenAlbum: (Long, String) -> Unit) {
    val type = rememberFutureType()
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstItemFocusRequester.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "אלבומים", theme = theme, onBack = onBack)
        if (albums.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("לא נמצאו אלבומים", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                itemsIndexed(albums, key = { _, album -> album.albumId }) { index, album ->
                    FutureListItem(
                        title = album.name,
                        summary = album.artist,
                        theme = theme,
                        onClick = { onOpenAlbum(album.albumId, album.name) },
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                        leading = { FutureAvatar(theme = theme, icon = Icons.Rounded.Album) },
                        trailing = { Text("${album.songCount}", color = theme.subtleTextColor, fontSize = type.summary) },
                    )
                }
            }
        }
    }
}
