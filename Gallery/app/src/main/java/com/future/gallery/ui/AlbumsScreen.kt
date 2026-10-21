package com.future.gallery.ui
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.icons.FutureIcons

import android.graphics.Bitmap
import android.util.Size
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.gallery.data.Album
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    theme: FutureTheme,
    onAlbumClick: (Album) -> Unit,
    // האלבום שנפתח לאחרונה - כשחוזרים "אחורה" מתצוגת האלבום, הפוקוס צריך
    // לשוב אליו בדיוק, לא תמיד לאלבום הראשון ברשת.
    lastSelectedAlbumId: String? = null,
) {
    if (albums.isEmpty()) {
        com.future.sharednav.components.EmptyState(
            icon = Icons.Rounded.PhotoAlbum,
            title = "אין אלבומים עדיין",
            textColor = theme.textColor,
        )
        return
    }
    val cardFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val gridState = rememberLazyGridState()
    // GAL-N06: שמות שחוזרים על עצמם (למשל שני אלבומי Camera) מקבלים נתיב להבחנה
    val duplicateNames = remember(albums) {
        albums.groupingBy { it.bucketName }.eachCount().filterValues { it > 1 }.keys
    }
    LaunchedEffect(albums.map { it.bucketId }) {
        val target = albums.firstOrNull { it.bucketId == lastSelectedAlbumId } ?: albums.firstOrNull()
        if (target != null) {
            // גוללים ליעד ומחכים לפריים שבו הכרטיס מורכב, אחרת אין FocusRequester מחובר
            gridState.scrollToItem(albums.indexOf(target))
            withFrameNanos { }
            runCatching { cardFocusRequesters.getOrPut(target.bucketId) { FocusRequester() }.requestFocus() }
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier.focusGroup(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(albums, key = { it.bucketId }) { album ->
            AlbumCard(
                album,
                theme,
                onClick = { onAlbumClick(album) },
                focusRequester = cardFocusRequesters.getOrPut(album.bucketId) { FocusRequester() },
                showPath = album.bucketName in duplicateNames,
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    theme: FutureTheme,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    showPath: Boolean = false,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val cacheKey = album.coverUri.toString()
    // ממוזערת השער מהמטמון המשותף של הגלריה (GAL-18)
    var bitmap by remember(album.bucketId) { mutableStateOf<Bitmap?>(ThumbCache.get(cacheKey)) }

    LaunchedEffect(album.coverUri) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.loadThumbnail(album.coverUri, Size(300, 300), null)
                } catch (e: Exception) {
                    null
                }
            }
            if (loaded != null) ThumbCache.put(cacheKey, loaded)
            bitmap = loaded
        }
    }

    val shape = FutureShapes.sm
    val titleText = if (showPath && album.path.isNotBlank()) {
        "${album.bucketName} · ${album.path.trimEnd('/')}"
    } else {
        album.bucketName
    }

    Column(
        modifier = Modifier
            .focusMotion(interactionSource)
            .clip(shape)
            .background(theme.surfaceColor)
            .then(if (isFocused) Modifier.border(FutureDimens.focusBorderItem, theme.readableAccentColor, shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            // GAL-08: clickable כבר מספק יעד פוקוס - בלי focusable נוסף.
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .bringIntoViewOnFocus()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(theme.textColor.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            val cover = bitmap
            if (cover != null) {
                Image(
                    bitmap = cover.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // GAL-N07: שער שלא נטען מוצג כאייקון תמונה ולא כמלבן ריק
                Icon(
                    FutureIcons.Image,
                    contentDescription = null,
                    tint = theme.mutedTextColor,
                    modifier = Modifier.size(FutureDimens.iconEmptyState),
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                titleText,
                color = theme.textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = FutureTypography.summary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("${album.count} פריטים", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
        }
    }
}
