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
import com.future.music.ui.components.FocusableItem
import com.future.music.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

@Composable
fun AlbumsScreen(albums: List<AlbumGroup>, theme: FutureTheme, onBack: () -> Unit, onOpenAlbum: (Long, String) -> Unit) {
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "אלבומים", theme = theme, onBack = onBack)
        if (albums.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("לא נמצאו אלבומים", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(albums, key = { _, album -> album.albumId }) { index, album ->
                    FocusableItem(
                        onClick = { onOpenAlbum(album.albumId, album.name) },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(theme.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Album, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(album.name, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(album.artist, color = theme.textColor.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1)
                            }
                            Text("${album.songCount}", color = theme.textColor.copy(alpha = 0.4f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
