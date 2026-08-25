package com.future.gallery.ui

import android.graphics.Bitmap
import android.util.Size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.gallery.data.Album
import com.future.gallery.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumsScreen(albums: List<Album>, theme: FutureTheme, onAlbumClick: (Album) -> Unit) {
    if (albums.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("אין אלבומים עדיין", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.focusGroup(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(albums, key = { it.bucketId }) { album ->
            AlbumCard(album, theme, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun AlbumCard(album: Album, theme: FutureTheme, onClick: () -> Unit) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var bitmap by remember(album.bucketId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(album.coverUri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(album.coverUri, Size(300, 300), null)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    val scale by animateFloatAsState(if (isFocused) 0.97f else 1f, label = "albumScale")
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .clip(shape)
            .background(theme.surfaceColor)
            .then(if (isFocused) Modifier.border(2.dp, theme.accentColor, shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                album.bucketName,
                color = theme.textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("${album.count} פריטים", color = theme.textColor.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}
