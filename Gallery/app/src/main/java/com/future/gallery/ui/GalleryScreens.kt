package com.future.gallery.ui
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.app.RecoverableSecurityException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.gallery.data.Album
import com.future.gallery.data.MediaItem
import com.future.gallery.data.SortOption
import com.future.gallery.data.sortedBy
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class GalleryTab { ALL, ALBUMS }

private val SORT_LABELS = mapOf(
    SortOption.DATE_NEWEST to "תאריך - החדש קודם",
    SortOption.DATE_OLDEST to "תאריך - הישן קודם",
    SortOption.NAME_AZ to "שם - א' עד ת'",
    SortOption.NAME_ZA to "שם - ת' עד א'",
    SortOption.SIZE_LARGEST to "גודל - הגדול קודם",
    SortOption.SIZE_SMALLEST to "גודל - הקטן קודם"
)

@Composable
fun GalleryHomeScreen(
    items: List<MediaItem>,
    albums: List<Album>,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onItemClick: (List<MediaItem>, MediaItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    theme: FutureTheme,
    lastSelectedItemId: Long? = null,
    lastSelectedAlbumId: String? = null,
) {
    var tab by remember { mutableStateOf(GalleryTab.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    val sortedItems = remember(items, sortOption) { items.sortedBy(sortOption) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("גלריה", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.textColor, modifier = Modifier.weight(1f))
                    if (tab == GalleryTab.ALL && hasPermission) {
                        GalleryIconButton(Icons.Rounded.Sort, "מיון", theme) { showSortMenu = true }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GalleryTabChip("הכל", tab == GalleryTab.ALL, theme) { tab = GalleryTab.ALL }
                    GalleryTabChip("אלבומים", tab == GalleryTab.ALBUMS, theme) { tab = GalleryTab.ALBUMS }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    !hasPermission -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("כדי להציג תמונות צריך לאשר הרשאה", color = theme.textColor.copy(alpha = 0.7f), fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            FocusableTextButton("אשר הרשאה", onRequestPermission, theme)
                        }
                    }
                    tab == GalleryTab.ALBUMS -> AlbumsScreen(albums, theme, onAlbumClick, lastSelectedAlbumId = lastSelectedAlbumId)
                    sortedItems.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("אין תמונות או סרטונים במכשיר", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                        }
                    }
                    else -> MediaGrid(sortedItems, theme, onItemClick = { item -> onItemClick(sortedItems, item) }, lastSelectedId = lastSelectedItemId)
                }
            }

            if (showSortMenu) {
                SortMenu(
                    current = sortOption,
                    theme = theme,
                    onDismiss = { showSortMenu = false },
                    onSelect = { sortOption = it; showSortMenu = false }
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    albumName: String,
    items: List<MediaItem>,
    theme: FutureTheme,
    onBack: () -> Unit,
    onItemClick: (List<MediaItem>, MediaItem) -> Unit,
    lastSelectedItemId: Long? = null,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GalleryIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזור", theme, onBack)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(albumName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                }
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("האלבום ריק", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
                } else {
                    MediaGrid(items, theme, onItemClick = { item -> onItemClick(items, item) }, lastSelectedId = lastSelectedItemId)
                }
            }
        }
    }
}

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    theme: FutureTheme,
    onItemClick: (MediaItem) -> Unit,
    // הפריט שנפתח לאחרונה מהרשת הזו - כשחוזרים "אחורה" מהצפייה, הפוקוס צריך
    // לשוב אליו בדיוק, לא תמיד לפריט הראשון ברשת.
    lastSelectedId: Long? = null,
) {
    val gridState = rememberLazyGridState()
    val thumbnailFocusRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    // בלי זה, אין שום פריט ממוקד כשנכנסים לרשת הזו (או חוזרים אליה) - dead
    // end ב-D-pad בלי מסך מגע.
    LaunchedEffect(items.map { it.id }) {
        val target = items.firstOrNull { it.id == lastSelectedId } ?: items.firstOrNull()
        target?.let { thumbnailFocusRequesters.getOrPut(it.id) { FocusRequester() }.requestFocus() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        // focusGroup() נותן ל-Compose גבול/סדר חיפוש פוקוס מפורש לרשת הזו - בלי זה,
        // חיפוש הפוקוס הדו-ממדי הדיפולטיבי (במיוחד בשילוב עם RTL שכל המסך עטוף בו)
        // לפעמים לא מוצא את הפריט "הבא ההגיוני" בין שורות ונשאר תקוע במקום, למרות
        // שיש עוד תוכן לגלול אליו. זה ה-API הרשמי המומלץ ב-Compose בדיוק למקרה הזה
        // (רשימות/רשתות מנווטות ב-D-pad).
        modifier = Modifier.focusGroup(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(items, key = { it.id }) { item ->
            MediaThumbnail(
                item,
                onClick = { onItemClick(item) },
                theme = theme,
                focusRequester = thumbnailFocusRequesters.getOrPut(item.id) { FocusRequester() },
            )
        }
    }
}

@Composable
private fun GalleryTabChip(label: String, isSelected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        when {
            isSelected -> theme.accentColor
            isFocused -> theme.textColor.copy(alpha = 0.18f)
            else -> theme.textColor.copy(alpha = 0.06f)
        },
        label = "tabChipBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (isSelected) Color.Black else theme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GalleryIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, theme: FutureTheme, onClick: () -> Unit) {
    // עטיפה דקה סביב TopBarIconButton המשותף (מודול SharedKeypadNav) - חתימת
    // הקריאה נשארת זהה כדי שקריאות קיימות ב-Gallery לא ישתנו.
    com.future.sharednav.components.TopBarIconButton(icon, contentDescription, theme.textColor, theme.accentColor, onClick)
}

@Composable
private fun SortMenu(current: SortOption, theme: FutureTheme, onDismiss: () -> Unit, onSelect: (SortOption) -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(vertical = 8.dp)
        ) {
            Text("מיין לפי", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            SortOption.entries.forEach { option ->
                SortRow(SORT_LABELS.getValue(option), isSelected = option == current, theme = theme) { onSelect(option) }
            }
        }
    }
}

@Composable
private fun SortRow(label: String, isSelected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.12f) else Color.Transparent, label = "sortRowBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (isSelected) theme.accentColor else theme.textColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun FocusableTextButton(text: String, onClick: () -> Unit, theme: FutureTheme) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor else theme.accentColor.copy(alpha = 0.7f), label = "btnBg")
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MediaThumbnail(item: MediaItem, onClick: () -> Unit, theme: FutureTheme, focusRequester: FocusRequester? = null) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(item.uri, Size(200, 200), null)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val scale by animateFloatAsState(if (isFocused) 0.94f else 1f, label = "thumbScale")
    val shape = if (isFocused) RoundedCornerShape(10.dp) else RoundedCornerShape(4.dp)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(theme.textColor.copy(alpha = 0.08f))
            .then(if (isFocused) Modifier.border(width = 3.dp, color = theme.accentColor, shape = shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (item.isVideo) {
            Icon(
                Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(28.dp)
            )
        }
    }
}

@Composable
fun MediaViewerScreen(
    item: MediaItem,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onNavigate: (MediaItem) -> Unit = {},
    onDeleted: () -> Unit = {},
    onEdit: () -> Unit = {},
    theme: FutureTheme
) {
    val context = LocalContext.current
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(item.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var zoomTarget by remember(item.id) { mutableStateOf(1f) }
    var panX by remember(item.id) { mutableStateOf(0f) }
    var panY by remember(item.id) { mutableStateOf(0f) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var videoView by remember(item.id) { mutableStateOf<android.widget.VideoView?>(null) }
    var isVideoPlaying by remember(item.id) { mutableStateOf(true) }
    var videoLoadFailed by remember(item.id) { mutableStateOf(false) }

    // אנימציה חלקה במקום קפיצה מיידית של הזום - לחיצה על כפתור הזום הייתה
    // "מטלפרת" את התמונה בין הרמות בלי שום מעבר, מה שהרגיש שבור/מקוטע.
    val zoom by androidx.compose.animation.core.animateFloatAsState(
        targetValue = zoomTarget,
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "mediaZoom"
    )
    val animatedPanX by androidx.compose.animation.core.animateFloatAsState(
        targetValue = panX,
        animationSpec = androidx.compose.animation.core.tween(160),
        label = "mediaPanX"
    )
    val animatedPanY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = panY,
        animationSpec = androidx.compose.animation.core.tween(160),
        label = "mediaPanY"
    )

    val currentIndex = remember(item.id, items) { items.indexOfFirst { it.id == item.id } }

    LaunchedEffect(item.id) { focusRequester.requestFocus() }

    // מאפסים את הפאן בכל שינוי רמת זום כדי שלא יישאר תזוזה "תקועה" מחוץ לתמונה
    LaunchedEffect(zoomTarget) { panX = 0f; panY = 0f }

    // הגבלת הפאן חייבת להתבסס על הגודל שבו התמונה בפועל מוצגת (ContentScale.Fit
    // עלול "להקטין" רק ציר אחד ולהשאיר פסי ריווח בציר השני - letterbox), לא על
    // גודל המכל כולו. אחרת קליפ הגבול מרשה לפאן את התמונה עד מחוץ למסך לגמרי
    // בציר המצומצם.
    val (renderedW, renderedH) = remember(bitmap, boxSize) {
        val bmp = bitmap
        if (bmp == null || boxSize.width == 0 || boxSize.height == 0) {
            boxSize.width.toFloat() to boxSize.height.toFloat()
        } else {
            val bitmapAspect = bmp.width.toFloat() / bmp.height.toFloat()
            val boxAspect = boxSize.width.toFloat() / boxSize.height.toFloat()
            if (bitmapAspect > boxAspect) {
                boxSize.width.toFloat() to (boxSize.width.toFloat() / bitmapAspect)
            } else {
                (boxSize.height.toFloat() * bitmapAspect) to boxSize.height.toFloat()
            }
        }
    }

    LaunchedEffect(item.id, item.isVideo) {
        if (item.isVideo) return@LaunchedEffect
        bitmap = null
        loadFailed = false
        bitmap = withContext(Dispatchers.IO) {
            try {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, item.uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDim = 1920
                    val w = info.size.width
                    val h = info.size.height
                    val scale = maxDim.toFloat() / maxOf(w, h)
                    if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                }
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap == null) loadFailed = true
    }

    fun goTo(delta: Int) {
        if (currentIndex < 0) return
        val nextIndex = currentIndex + delta
        if (nextIndex in items.indices) onNavigate(items[nextIndex])
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { boxSize = it }
                    .focusRequester(focusRequester)
                    .focusable().bringIntoViewOnFocus()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        if (item.isVideo) {
                            // בקרת וידאו: שמאל/ימין תמיד מכוונים הרצה אחורה/קדימה בציר
                            // הזמן של הסרטון (המוסכמה האוניברסלית של נגני מדיה/שלטים -
                            // לא תלוית כיוון RTL כמו ניווט בין פריטים), ולמעלה/למטה
                            // עוברים לפריט הקודם/הבא כי אין זום להזיז בו במסך וידאו.
                            return@onKeyEvent when (event.key) {
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    videoView?.let { vv ->
                                        if (vv.isPlaying) { vv.pause(); isVideoPlaying = false } else { vv.start(); isVideoPlaying = true }
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    videoView?.let { vv -> vv.seekTo((vv.currentPosition + 10_000).coerceAtMost(vv.duration)) }
                                    true
                                }
                                Key.DirectionLeft -> {
                                    videoView?.let { vv -> vv.seekTo((vv.currentPosition - 10_000).coerceAtLeast(0)) }
                                    true
                                }
                                Key.DirectionUp -> { goTo(-1); true }
                                Key.DirectionDown -> { goTo(1); true }
                                else -> false
                            }
                        }
                        val panStep = 40f * zoomTarget
                        if (zoomTarget > 1f) {
                            val maxPanX = (renderedW * (zoomTarget - 1f) / 2f)
                            val maxPanY = (renderedH * (zoomTarget - 1f) / 2f)
                            when (event.key) {
                                // כשכבר הגענו לקצה הפאן בכיוון הזה, החץ עדיין לא "מת" -
                                // עובר לתמונה הבאה/קודמת במקום, כדי שדפדוף לא יתקע
                                // כשמזוגמים פנימה (בלי דרך אחרת לצאת חוץ מלאפס זום).
                                Key.DirectionRight -> {
                                    val next = (panX - panStep).coerceIn(-maxPanX, maxPanX)
                                    if (next == panX && panX <= -maxPanX + 0.5f) goTo(-1) else panX = next
                                    true
                                }
                                Key.DirectionLeft -> {
                                    val next = (panX + panStep).coerceIn(-maxPanX, maxPanX)
                                    if (next == panX && panX >= maxPanX - 0.5f) goTo(1) else panX = next
                                    true
                                }
                                Key.DirectionUp -> { panY = (panY + panStep).coerceIn(-maxPanY, maxPanY); true }
                                Key.DirectionDown -> { panY = (panY - panStep).coerceIn(-maxPanY, maxPanY); true }
                                else -> false
                            }
                        } else {
                            when (event.key) {
                                Key.DirectionRight -> { goTo(-1); true }
                                Key.DirectionLeft -> { goTo(1); true }
                                else -> false
                            }
                        }
                    }
            ) {
                when {
                    item.isVideo -> {
                        if (videoLoadFailed) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.VideocamOff, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("לא ניתן לנגן את הסרטון", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            }
                        } else {
                            androidx.compose.ui.viewinterop.AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setOnErrorListener { _, _, _ -> videoLoadFailed = true; true }
                                        setOnPreparedListener { player ->
                                            player.isLooping = false
                                            start()
                                        }
                                        setOnCompletionListener { isVideoPlaying = false }
                                        setVideoURI(item.uri)
                                        videoView = this
                                    }
                                },
                                onRelease = { it.stopPlayback(); videoView = null },
                            )
                            if (!isVideoPlaying) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(56.dp))
                                }
                            }
                        }
                    }
                    loadFailed -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("לא ניתן לטעון את התמונה", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    }
                    else -> {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoom; scaleY = zoom
                                    translationX = animatedPanX; translationY = animatedPanY
                                },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onBack() }
                        .padding(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "חזור", tint = Color.White)
                }
            }

            MediaViewerBottomBar(
                theme = theme,
                canZoom = !item.isVideo && !loadFailed && bitmap != null,
                isZoomed = zoomTarget > 1f,
                showEdit = !item.isVideo && !loadFailed && bitmap != null,
                onZoom = { zoomTarget = if (zoomTarget >= 3f) 1f else zoomTarget + 1f },
                onShare = {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (item.isVideo) "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, item.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {}
                },
                onEdit = onEdit,
                onDelete = { showDeleteConfirm = true }
            )
        }

        val deleteIntentLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                onDeleted()
            } else {
                android.widget.Toast.makeText(context, "המחיקה בוטלה", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        if (showDeleteConfirm) {
            DeleteConfirmDialog(
                onCancel = { showDeleteConfirm = false },
                theme = theme,
                onConfirm = {
                    showDeleteConfirm = false
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                        onDeleted()
                    } catch (securityException: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
                            try {
                                val intentSender = securityException.userAction.actionIntent.intentSender
                                deleteIntentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "לא ניתן למחוק - צריך אישור נוסף", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "לא ניתן למחוק - צריך אישור נוסף", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "לא ניתן למחוק - צריך אישור נוסף", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun MediaViewerBottomBar(
    theme: FutureTheme,
    canZoom: Boolean,
    isZoomed: Boolean,
    showEdit: Boolean,
    onZoom: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MediaViewerBarButton(Icons.Rounded.ZoomIn, "זום", theme, enabled = canZoom, isActive = isZoomed, onClick = onZoom)
        MediaViewerBarButton(Icons.Rounded.Share, "שתף", theme, onClick = onShare)
        if (showEdit) {
            MediaViewerBarButton(Icons.Rounded.Edit, "ערוך", theme, onClick = onEdit)
        }
        MediaViewerBarButton(Icons.Rounded.Delete, "מחק", theme, isDestructive = true, onClick = onDelete)
    }
}

@Composable
private fun MediaViewerBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    theme: FutureTheme,
    enabled: Boolean = true,
    isActive: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val tint = when {
        !enabled -> Color.White.copy(alpha = 0.25f)
        isDestructive -> Color(0xFFFF6B6B)
        isActive || isFocused -> theme.accentColor
        else -> Color.White
    }
    val bgColor by animateColorAsState(if (isFocused && enabled) theme.accentColor.copy(alpha = 0.2f) else Color.Transparent, label = "viewerBarBtnBg")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .focusable(interactionSource = interactionSource, enabled = enabled).bringIntoViewOnFocus()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 10.sp)
    }
}

@Composable
private fun DeleteConfirmDialog(onCancel: () -> Unit, onConfirm: () -> Unit, theme: FutureTheme) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("למחוק את הפריט הזה?", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableTextButton("ביטול", onCancel, theme)
                FocusableDestructiveButton("מחק", onConfirm)
            }
        }
    }
}

@Composable
private fun FocusableDestructiveButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) Color(0xFFFF6B6B) else Color(0xFFFF6B6B).copy(alpha = 0.7f), label = "delBtnBg")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
