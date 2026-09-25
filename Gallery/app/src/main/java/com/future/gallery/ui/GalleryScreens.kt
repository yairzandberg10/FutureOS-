package com.future.gallery.ui
import com.future.sharednav.systemui.StatusBarInset

import android.app.RecoverableSecurityException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.zIndex
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.future.gallery.data.Album
import com.future.gallery.data.MediaItem
import com.future.gallery.data.SortOption
import com.future.gallery.data.sortedBy
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

/** רמות הזום: מ-1x (כל התמונה) עד 8x; OK מדלג בין 1/2/4, 1 ו-3 בצעדים רציפים. */
private const val ZOOM_MAX = 8f
private const val ZOOM_STEP = 1.25f
private val ZOOM_PRESETS = listOf(1f, 2f, 4f)

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
    var tab by rememberSaveable { mutableStateOf(GalleryTab.ALL) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DATE_NEWEST) }
    var showMenu by remember { mutableStateOf(false) }
    val sortedItems = remember(items, sortOption) { items.sortedBy(sortOption) }

    com.future.sharednav.nav.onOptionsKeyPress { showMenu = !showMenu }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp + StatusBarInset.TITLE_GAP_DP.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("גלריה", fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold, color = theme.textColor, modifier = Modifier.weight(1f))
                    if (hasPermission) {
                        Text(
                            if (tab == GalleryTab.ALL) "${items.size} תמונות" else "${albums.size} אלבומים",
                            color = theme.mutedTextColor,
                            fontSize = FutureTypography.caption,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FutureChip("הכל", theme, selected = tab == GalleryTab.ALL, onClick = { tab = GalleryTab.ALL })
                    FutureChip("אלבומים", theme, selected = tab == GalleryTab.ALBUMS, onClick = { tab = GalleryTab.ALBUMS })
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    !hasPermission -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("כדי להציג תמונות צריך לאשר הרשאה", color = theme.mutedTextColor, fontSize = FutureTypography.bodyLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            FutureButton("אשר הרשאה", theme, onRequestPermission)
                        }
                    }
                    tab == GalleryTab.ALBUMS -> AlbumsScreen(albums, theme, onAlbumClick, lastSelectedAlbumId = lastSelectedAlbumId)
                    sortedItems.isEmpty() -> {
                        com.future.sharednav.components.EmptyState(
                            icon = FutureIcons.Image,
                            title = "אין תמונות במכשיר",
                            textColor = theme.textColor,
                        )
                    }
                    else -> MediaGrid(sortedItems, theme, onItemClick = { item -> onItemClick(sortedItems, item) }, lastSelectedId = lastSelectedItemId)
                }
            }

            if (showMenu) {
                FutureOptionsMenu(theme = theme, onDismissRequest = { showMenu = false }, header = "גלריה") {
                    FutureMenuRow(
                        if (tab == GalleryTab.ALL) "הצג אלבומים" else "הצג את כל התמונות",
                        if (tab == GalleryTab.ALL) FutureIcons.Folder else FutureIcons.Image,
                        theme,
                        onClick = {
                            tab = if (tab == GalleryTab.ALL) GalleryTab.ALBUMS else GalleryTab.ALL
                            showMenu = false
                        },
                    )
                    if (tab == GalleryTab.ALL && hasPermission) {
                        SortOption.entries.forEach { option ->
                            FutureMenuRow(
                                label = SORT_LABELS.getValue(option),
                                icon = null,
                                theme = theme,
                                onClick = { sortOption = option; showMenu = false },
                                trailing = if (option == sortOption) {
                                    { Icon(FutureIcons.Check, contentDescription = null, tint = theme.readableAccentColor, modifier = Modifier.size(FutureDimens.iconMenuRow)) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** תוכן אלבום. בלי כפתור חזרה על המסך - מקש BACK חוזר (MainActivity). */
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
                    modifier = Modifier.fillMaxWidth().padding(top = StatusBarInset.TITLE_GAP_DP.dp).padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(albumName, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold, color = theme.textColor, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${items.size} תמונות", color = theme.mutedTextColor, fontSize = FutureTypography.caption)
                }
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("האלבום ריק", color = theme.mutedTextColor, fontSize = FutureTypography.bodyLarge)
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
    // הפריט שנפתח לאחרונה - כשחוזרים מהצפייה הפוקוס חוזר אליו בדיוק.
    lastSelectedId: Long? = null,
) {
    val gridState = rememberLazyGridState()
    val thumbnailFocusRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    val ids = remember(items) { items.map { it.id } }
    LaunchedEffect(ids) {
        val index = items.indexOfFirst { it.id == lastSelectedId }.takeIf { it >= 0 } ?: 0
        val target = items.getOrNull(index) ?: return@LaunchedEffect
        // פריט רחוק ברשת עוד לא מורכב - קודם גוללים אליו, ורק אז מבקשים פוקוס.
        // קודם הבקשה נפלה בשקט והפוקוס "קפץ" למקום אקראי.
        if (index > 0) gridState.scrollToItem(index)
        withFrameNanos { }
        runCatching { thumbnailFocusRequesters.getOrPut(target.id) { FocusRequester() }.requestFocus() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.focusGroup(),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

/** תמונה ממוזערת: פוקוס = מתרוממת מעט ומסגרת בהדגשה (כמו כל פריט במערכת). */
@Composable
private fun MediaThumbnail(item: MediaItem, onClick: () -> Unit, theme: FutureTheme, focusRequester: FocusRequester? = null) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(item.uri, Size(220, 220), null)
            } catch (e: Exception) {
                null
            }
        }
    }
    val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, FutureMotion.fast(), label = "thumbFade")
    val shape = FutureShapes.sm

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .zIndex(if (isFocused) 1f else 0f)
            .focusMotion(interactionSource, focusedScale = 1.06f)
            .clip(shape)
            .background(theme.textColor.copy(alpha = 0.08f))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
                contentScale = ContentScale.Crop
            )
        }
        if (isFocused) {
            Box(modifier = Modifier.matchParentSize().border(FutureDimens.focusBorderControl, theme.readableAccentColor, shape))
        }
    }
}

/**
 * צפייה בתמונה - כותרת (שם, תאריך, מיקום ברשימה), התמונה, וסרגל מקשים רכים
 * (SoftKeyBar) שאומר תמיד מה עושים OK, Options וחזור. קודם המסך היה ריק
 * מכל הסבר חוץ מרמז שנעלם אחרי שתי שניות, ולא היה ברור איך עוברים תמונה
 * או יוצאים מזום.
 * - ימינה/שמאלה: התמונה הקודמת/הבאה (רק כשלא בזום) - חיצים בצדי התמונה
 *   מראים לאן אפשר לעבור.
 * - OK: דילוג בין 1x/2x/4x; 3 מגדיל ו-1 מקטין בצעדים (החזקה = רציף), עד 8x.
 * - בזום: החצים (או 2/4/6/8) מזיזים בתוך התמונה ולא עוברים לתמונה אחרת;
 *   0 או BACK מחזירים לתמונה המלאה.
 * - 5: מסך מלא - מסתיר את הכותרת והסרגל, ו-5 שוב מחזיר.
 * - Options: עריכה, שיתוף, פרטים, רקע, מחיקה.
 */
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
    val scope = rememberCoroutineScope()
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var imageSize by remember(item.id) { mutableStateOf<android.util.Size?>(null) }
    var loadFailed by remember(item.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var zoomTarget by remember(item.id) { mutableFloatStateOf(1f) }
    var panX by remember(item.id) { mutableFloatStateOf(0f) }
    var panY by remember(item.id) { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var direction by remember { mutableIntStateOf(1) }
    var zoomBadgeTick by remember { mutableIntStateOf(0) }
    var showZoomBadge by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(true) }
    var immersive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val zoom by animateFloatAsState(zoomTarget, FutureMotion.standard(), label = "mediaZoom")
    val animatedPanX by animateFloatAsState(panX, FutureMotion.fast(), label = "mediaPanX")
    val animatedPanY by animateFloatAsState(panY, FutureMotion.fast(), label = "mediaPanY")

    val currentIndex = remember(item.id, items) { items.indexOfFirst { it.id == item.id } }

    com.future.sharednav.nav.onOptionsKeyPress { showMenu = !showMenu }
    BackHandler(enabled = zoomTarget > 1f) { zoomTarget = 1f }

    LaunchedEffect(item.id) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(Unit) { delay(2600); showHint = false }
    LaunchedEffect(zoomBadgeTick) {
        if (zoomBadgeTick == 0) return@LaunchedEffect
        showZoomBadge = true
        delay(1400)
        showZoomBadge = false
    }

    // התמונה מוצגת ב-Fit: הגבולות של הפאן נגזרים מהגודל המוצג בפועל, לא מהמכל.
    val (renderedW, renderedH) = remember(bitmap, boxSize) {
        val bmp = bitmap
        if (bmp == null || boxSize.width == 0 || boxSize.height == 0) {
            boxSize.width.toFloat() to boxSize.height.toFloat()
        } else {
            val bitmapAspect = bmp.width.toFloat() / bmp.height.toFloat()
            val boxAspect = boxSize.width.toFloat() / boxSize.height.toFloat()
            if (bitmapAspect > boxAspect) boxSize.width.toFloat() to (boxSize.width.toFloat() / bitmapAspect)
            else (boxSize.height.toFloat() * bitmapAspect) to boxSize.height.toFloat()
        }
    }

    fun clampPan() {
        val maxX = renderedW * (zoomTarget - 1f) / 2f
        val maxY = renderedH * (zoomTarget - 1f) / 2f
        panX = panX.coerceIn(-maxX, maxX)
        panY = panY.coerceIn(-maxY, maxY)
    }

    fun setZoom(z: Float) {
        zoomTarget = z.coerceIn(1f, ZOOM_MAX)
        if (zoomTarget <= 1.001f) { zoomTarget = 1f; panX = 0f; panY = 0f } else clampPan()
        zoomBadgeTick++
    }

    LaunchedEffect(item.id) {
        bitmap = null
        loadFailed = false
        bitmap = withContext(Dispatchers.IO) {
            try {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, item.uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    imageSize = info.size
                    // רזולוציה גבוהה יותר מבעבר - בזום 8x צריך פרטים.
                    val maxDim = 2560
                    val w = info.size.width
                    val h = info.size.height
                    val scale = maxDim.toFloat() / maxOf(w, h)
                    if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                }
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }
        if (bitmap == null) loadFailed = true
    }

    fun goTo(delta: Int) {
        if (currentIndex < 0 || zoomTarget > 1f) return
        val nextIndex = currentIndex + delta
        if (nextIndex in items.indices) {
            direction = delta
            onNavigate(items[nextIndex])
        }
    }

    fun share() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            clipData = android.content.ClipData.newRawUri(item.displayName, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        com.future.sharednav.share.FutureShare.open(context, send, "שיתוף תמונה")
    }

    fun setAsWallpaper() {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        android.app.WallpaperManager.getInstance(context).setStream(input)
                    }
                    true
                }.getOrDefault(false)
            }
            android.widget.Toast.makeText(context, if (ok) "הוגדר כרקע" else "לא ניתן להגדיר כרקע", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val zoomed = zoomTarget > 1f
                    val panStep = 60f * zoomTarget
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (bitmap != null && event.nativeKeyEvent.repeatCount == 0) {
                                setZoom(ZOOM_PRESETS.firstOrNull { it > zoomTarget + 0.01f } ?: 1f)
                            }
                            true
                        }
                        Key.Three, Key.VolumeUp -> { if (bitmap != null) setZoom(zoomTarget * ZOOM_STEP); true }
                        Key.One, Key.VolumeDown -> { if (bitmap != null) setZoom(zoomTarget / ZOOM_STEP); true }
                        Key.Zero -> { setZoom(1f); true }
                        Key.Five -> { if (event.nativeKeyEvent.repeatCount == 0) immersive = !immersive; true }
                        // בזום נשארים בתוך התמונה - גם בקצה, החץ לא מחליף תמונה.
                        Key.DirectionRight, Key.Six -> {
                            if (zoomed) { panX -= panStep; clampPan() } else if (event.key == Key.DirectionRight) goTo(-1)
                            true
                        }
                        Key.DirectionLeft, Key.Four -> {
                            if (zoomed) { panX += panStep; clampPan() } else if (event.key == Key.DirectionLeft) goTo(1)
                            true
                        }
                        Key.DirectionUp, Key.Two -> { if (zoomed) { panY += panStep; clampPan() }; true }
                        Key.DirectionDown, Key.Eight -> { if (zoomed) { panY -= panStep; clampPan() }; true }
                        else -> false
                    }
                }
        ) {
            AnimatedVisibility(visible = !immersive, enter = fadeIn(), exit = fadeOut()) {
                ViewerHeader(item = item, position = if (items.size > 1 && currentIndex >= 0) "${currentIndex + 1} / ${items.size}" else null, theme = theme)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(androidx.compose.ui.graphics.RectangleShape)
                    .onSizeChanged { boxSize = it }
            ) {
                AnimatedContent(
                    targetState = item.id,
                    transitionSpec = {
                        // RTL: "הבאה" נכנסת משמאל.
                        val sign = if (direction > 0) -1 else 1
                        (slideInHorizontally(FutureMotion.enter()) { sign * it / 5 } + fadeIn(FutureMotion.enter()) + scaleIn(FutureMotion.enter(), initialScale = 0.96f))
                            .togetherWith(slideOutHorizontally(FutureMotion.exit()) { -sign * it / 5 } + fadeOut(FutureMotion.exit()))
                    },
                    label = "photoSwap",
                    modifier = Modifier.fillMaxSize(),
                ) { id ->
                    val bmp = if (id == item.id) bitmap else null
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when {
                            id == item.id && loadFailed -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                            ) {
                                Icon(FutureIcons.Image, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconEmptyState))
                                Text("לא ניתן לטעון את התמונה", color = theme.mutedTextColor, fontSize = FutureTypography.body)
                            }
                            bmp != null -> Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoom; scaleY = zoom
                                    translationX = animatedPanX; translationY = animatedPanY
                                },
                                contentScale = ContentScale.Fit
                            )
                            id == item.id -> com.future.sharednav.components.FutureSpinner(theme = theme)
                        }
                    }
                }

                // חיצים בצדי התמונה: יש תמונה קודמת/הבאה (RTL - הבאה משמאל).
                if (zoomTarget <= 1f && currentIndex >= 0) {
                    if (currentIndex > 0) ViewerEdgeArrow(FutureIcons.ChevronRight, theme, Modifier.align(Alignment.CenterStart))
                    if (currentIndex < items.size - 1) ViewerEdgeArrow(FutureIcons.ChevronLeft, theme, Modifier.align(Alignment.CenterEnd))
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showZoomBadge || zoomTarget > 1f,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = FutureDimens.spacingSm),
                ) {
                    ViewerBadge("${"%.1f".format(zoomTarget)}x", theme)
                }

                // מפת מיקום קטנה בזום - איפה בתוך התמונה נמצאים
                if (zoomTarget > 1f && renderedW > 0f && renderedH > 0f) {
                    ZoomMiniMap(
                        zoom = zoomTarget, panX = panX, panY = panY, renderedW = renderedW, renderedH = renderedH, theme = theme,
                        modifier = Modifier.align(Alignment.BottomStart).padding(FutureDimens.spacingMd),
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHint && bitmap != null && !immersive,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = FutureDimens.spacingMd),
                ) {
                    ViewerBadge(if (items.size > 1) "‹ › תמונה אחרת · 5 מסך מלא" else "5 מסך מלא", theme)
                }
            }

            AnimatedVisibility(visible = !immersive, enter = fadeIn(), exit = fadeOut()) {
                com.future.sharednav.components.FutureSoftKeyBar(
                    theme = theme,
                    left = "תפריט",
                    center = when {
                        bitmap == null -> null
                        zoomTarget >= ZOOM_PRESETS.last() -> "הקטן"
                        else -> "הגדל"
                    },
                    right = if (zoomTarget > 1f) "תמונה מלאה" else "חזור",
                )
            }
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

        if (showMenu) {
            FutureOptionsMenu(theme = theme, onDismissRequest = { showMenu = false }, header = item.displayName.ifBlank { "תמונה" }) {
                if (bitmap != null) FutureMenuRow("עריכה ואפקטים", FutureIcons.Edit, theme, onClick = { showMenu = false; onEdit() })
                FutureMenuRow("שיתוף", FutureIcons.Share, theme, onClick = { showMenu = false; share() })
                if (zoomTarget > 1f) FutureMenuRow("חזרה לתמונה המלאה", FutureIcons.RestartAlt, theme, onClick = { showMenu = false; setZoom(1f) })
                FutureMenuRow("פרטים", FutureIcons.Info, theme, onClick = { showMenu = false; showInfo = true })
                FutureMenuRow("הגדר כרקע", FutureIcons.Palette, theme, onClick = { showMenu = false; setAsWallpaper() })
                FutureMenuRow("מחיקה", FutureIcons.Delete, theme, destructive = true, onClick = { showMenu = false; showDeleteConfirm = true })
            }
        }

        if (showInfo) {
            FutureOptionsMenu(theme = theme, onDismissRequest = { showInfo = false }, header = "פרטים") {
                val date = remember(item.dateAdded) {
                    java.text.SimpleDateFormat("d.M.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.dateAdded * 1000))
                }
                InfoRow("שם", item.displayName, theme) { showInfo = false }
                InfoRow("תאריך", date, theme) { showInfo = false }
                imageSize?.let { InfoRow("רזולוציה", "${it.width}×${it.height}", theme) { showInfo = false } }
                InfoRow("גודל", android.text.format.Formatter.formatShortFileSize(context, item.size), theme) { showInfo = false }
                if (item.bucketName.isNotBlank()) InfoRow("אלבום", item.bucketName, theme) { showInfo = false }
            }
        }

        if (showDeleteConfirm) {
            ConfirmDialog(
                message = "למחוק את התמונה?",
                theme = theme,
                onCancel = { showDeleteConfirm = false },
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
                        android.widget.Toast.makeText(context, "לא ניתן למחוק", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, theme: FutureTheme, onClick: () -> Unit) {
    FutureMenuRow(
        label = label,
        icon = null,
        theme = theme,
        onClick = onClick,
        trailing = { Text(value, color = theme.mutedTextColor, fontSize = FutureTypography.summary, maxLines = 1) },
    )
}

/** כותרת הצופה (TopBar של הדיזיין סיסטם): שם הקובץ, תאריך ואלבום, והמיקום ברשימה. */
@Composable
private fun ViewerHeader(item: MediaItem, position: String?, theme: FutureTheme) {
    val subtitle = remember(item.id) {
        val date = java.text.SimpleDateFormat("d.M.yyyy · HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.dateAdded * 1000))
        listOf(date, item.bucketName).filter { it.isNotBlank() }.joinToString(" · ")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayName.ifBlank { "תמונה" },
                color = theme.textColor,
                fontSize = FutureTypography.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(subtitle, color = theme.mutedTextColor, fontSize = FutureTypography.summary, maxLines = 1)
        }
        if (position != null) ViewerBadge(position, theme)
    }
}

/** חץ קטן בצד התמונה - אפשר לעבור לתמונה בכיוון הזה. */
@Composable
private fun ViewerEdgeArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, theme: FutureTheme, modifier: Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = FutureDimens.spacingSm)
            .size(FutureDimens.rowHeightTopBarButton)
            .clip(FutureShapes.pill)
            .background(theme.surfaceColor.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(FutureDimens.iconMenuRow))
    }
}

@Composable
private fun ViewerBadge(text: String, theme: FutureTheme) {
    Text(
        text,
        color = theme.textColor,
        fontSize = FutureTypography.caption,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(FutureShapes.pill)
            .background(theme.surfaceColor.copy(alpha = 0.86f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** מפה קטנה: מסגרת התמונה, והמלבן הממוקד = החלק שרואים עכשיו. */
@Composable
private fun ZoomMiniMap(zoom: Float, panX: Float, panY: Float, renderedW: Float, renderedH: Float, theme: FutureTheme, modifier: Modifier = Modifier) {
    val w = 64f
    val h = w * renderedH / renderedW
    val accent = theme.readableAccentColor
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(w.dp, h.coerceIn(24f, 96f).dp)
            .clip(FutureShapes.xs)
            .background(theme.surfaceColor.copy(alpha = 0.8f))
    ) {
        val viewW = size.width / zoom
        val viewH = size.height / zoom
        // פאן חיובי = התמונה זזה ימינה/למטה, כלומר רואים את החלק השמאלי/העליון.
        val cx = size.width / 2f - panX / renderedW * size.width / zoom
        val cy = size.height / 2f - panY / renderedH * size.height / zoom
        drawRect(
            color = accent,
            topLeft = androidx.compose.ui.geometry.Offset(cx - viewW / 2f, cy - viewH / 2f),
            size = androidx.compose.ui.geometry.Size(viewW, viewH),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
        )
    }
}
