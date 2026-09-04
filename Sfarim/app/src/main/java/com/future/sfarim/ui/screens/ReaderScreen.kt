package com.future.sfarim.ui.screens

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.future.sfarim.data.CommentaryEntry
import com.future.sfarim.data.LibraryBook
import com.future.sfarim.data.LibrarySegment
import com.future.sfarim.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme
import com.future.sfarim.util.HebrewNumerals
import com.future.sfarim.util.stripHtmlTags
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    book: LibraryBook,
    topIndex: Int,
    segments: List<LibrarySegment>,
    hasPrevChapter: Boolean,
    hasNextChapter: Boolean,
    bookmarkedSegmentIds: Set<Long>,
    theme: FutureTheme,
    onBack: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onToggleBookmark: (LibrarySegment) -> Unit,
    onSegmentShown: (LibrarySegment) -> Unit,
    onLoadCommentaries: suspend (List<Int>) -> List<CommentaryEntry>,
    onOpenCommentary: (CommentaryEntry) -> Unit,
) {
    var fontSize by remember { mutableStateOf(18f) }
    var menuSegment by remember { mutableStateOf<LibrarySegment?>(null) }
    var showPageMenu by remember { mutableStateOf(false) }
    var commentaryPrefix by remember { mutableStateOf<List<Int>?>(null) }
    var commentaryResults by remember { mutableStateOf<List<CommentaryEntry>?>(null) }
    val listState = rememberLazyListState()
    // ה-ScrollState חייב להיות ממוקד (key) לפי הפרק הנוכחי (book.id + topIndex) -
    // אחרת מעבר לפרק הבא/קודם בטקסט שוטף (ענף ה-else למטה) היה משמר את מיקום
    // הגלילה הישן של הפרק הקודם במקום להתחיל מלמעלה.
    val scrollState = key(book.id, topIndex) { rememberScrollState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pageFocusRequester = remember { FocusRequester() }
    val firstSegmentFocusRequester = remember(book.id, topIndex) { FocusRequester() }

    LaunchedEffect(book.id, topIndex) {
        segments.firstOrNull()?.let { onSegmentShown(it) }
    }

    LaunchedEffect(book.id, topIndex, book.isVerseStyle, segments.firstOrNull()?.id) {
        if (!book.isVerseStyle) {
            pageFocusRequester.requestFocus()
        } else if (segments.isNotEmpty()) {
            firstSegmentFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(commentaryPrefix) {
        val prefix = commentaryPrefix
        commentaryResults = if (prefix != null) onLoadCommentaries(prefix) else null
    }

    val chapterLabel = remember(book, topIndex) { HebrewNumerals.chapterLabel(book.sectionNames, topIndex) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_STAR -> {
                            if (hasPrevChapter) { onPrevChapter(); true } else false
                        }
                        android.view.KeyEvent.KEYCODE_POUND -> {
                            if (hasNextChapter) { onNextChapter(); true } else false
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!book.isVerseStyle) { scope.launch { scrollState.animateScrollBy(220f) }; true } else false
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!book.isVerseStyle) { scope.launch { scrollState.animateScrollBy(-220f) }; true } else false
                        }
                        else -> false
                    }
                },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = "${book.displayTitle} · $chapterLabel", theme = theme, onBack = onBack)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Text(
                        if (hasPrevChapter) "* לפרק קודם" else "",
                        color = theme.textColor.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                    )
                    Text(
                        if (hasNextChapter) "לפרק הבא #" else "",
                        color = theme.textColor.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                    )
                }
                if (segments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין תוכן זמין בפרק זה", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
                } else if (book.isVerseStyle) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(segments, key = { _, it -> it.id }) { index, segment ->
                            SegmentRow(
                                segment = segment,
                                fontSize = fontSize,
                                isBookmarked = segment.id in bookmarkedSegmentIds,
                                theme = theme,
                                focusRequester = if (index == 0) firstSegmentFocusRequester else null,
                                onFocusedShown = { onSegmentShown(segment) },
                                onMenu = { menuSegment = segment },
                            )
                        }
                    }
                } else {
                    // טקסט שוטף (גמרא, פירושים וכל השאר) - בלי תוויות מספור לכל שורה,
                    // תפריט האופציות חל על העמוד כולו ולא על קטע ספציפי.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusable()
                            .focusRequester(pageFocusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp && (event.key == Key.Menu || event.key == Key.Settings)) {
                                    showPageMenu = true
                                    true
                                } else false
                            }
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                    ) {
                        segments.forEach { segment ->
                            Text(
                                stripHtmlTags(segment.textHe),
                                color = theme.textColor,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.6f).sp,
                            )
                        }
                    }
                }
            }

            menuSegment?.let { segment ->
                ContentOptionsMenu(
                    refDisplay = segment.refDisplay,
                    contentLabel = book.contentLabel,
                    isBookmarked = segment.id in bookmarkedSegmentIds,
                    theme = theme,
                    onDismiss = { menuSegment = null },
                    onToggleBookmark = { menuSegment = null; onToggleBookmark(segment) },
                    onShare = {
                        menuSegment = null
                        shareText(context, "${segment.refDisplay}\n\n${stripHtmlTags(segment.textHe)}")
                    },
                    onShowCommentaries = { menuSegment = null; commentaryPrefix = segment.path },
                    onIncreaseFont = { fontSize = (fontSize + 2f).coerceAtMost(32f) },
                    onDecreaseFont = { fontSize = (fontSize - 2f).coerceAtLeast(12f) },
                )
            }

            if (showPageMenu) {
                val anchor = segments.firstOrNull()
                ContentOptionsMenu(
                    refDisplay = "${book.displayTitle} · $chapterLabel",
                    contentLabel = book.contentLabel,
                    isBookmarked = anchor?.id in bookmarkedSegmentIds,
                    theme = theme,
                    onDismiss = { showPageMenu = false },
                    onToggleBookmark = { showPageMenu = false; anchor?.let(onToggleBookmark) },
                    onShare = {
                        showPageMenu = false
                        val fullText = segments.joinToString("\n\n") { stripHtmlTags(it.textHe) }
                        shareText(context, "${book.displayTitle} · $chapterLabel\n\n$fullText")
                    },
                    onShowCommentaries = { showPageMenu = false; commentaryPrefix = listOf(topIndex) },
                    onIncreaseFont = { fontSize = (fontSize + 2f).coerceAtMost(32f) },
                    onDecreaseFont = { fontSize = (fontSize - 2f).coerceAtLeast(12f) },
                )
            }

            if (commentaryPrefix != null) {
                CommentariesDialog(
                    contentLabel = book.contentLabel,
                    results = commentaryResults,
                    theme = theme,
                    onDismiss = { commentaryPrefix = null; commentaryResults = null },
                    onOpen = { entry ->
                        commentaryPrefix = null
                        commentaryResults = null
                        onOpenCommentary(entry)
                    },
                )
            }
        }
    }
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Composable
private fun SegmentRow(
    segment: LibrarySegment,
    fontSize: Float,
    isBookmarked: Boolean,
    theme: FutureTheme,
    focusRequester: FocusRequester? = null,
    onFocusedShown: () -> Unit,
    onMenu: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val cleanedText = remember(segment.id) { stripHtmlTags(segment.textHe) }
    // בורדר + scale בפוקוס, כמו בשאר האפליקציה (FocusableItem המשותף) - בלי זה
    // דווקא מסך הקריאה, המרכזי ביותר, נראה שונה מכל שאר המסכים.
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "segmentRowScale")
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (isFocused) theme.accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .then(if (isFocused) Modifier.border(width = 1.5.dp, color = theme.accentColor, shape = shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            // לחיצת OK/מרכז הייתה no-op בלי שום רמז על המסך - עכשיו פותחת את
            // אותו תפריט אפשרויות (סימניה/שיתוף/פרשנים) שמקש Menu כבר מספק,
            // כך שהיא לא "מתה" בלי סיבה.
            .clickable(interactionSource = interactionSource, indication = null, onClick = onMenu)
            .focusable(interactionSource = interactionSource)
            .onFocusChanged { if (it.isFocused) onFocusedShown() }
            .onKeyEvent { event ->
                if (isFocused && event.type == KeyEventType.KeyUp && (event.key == Key.Menu || event.key == Key.Settings)) {
                    onMenu()
                    true
                } else false
            }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isBookmarked) {
            Icon(Icons.Rounded.Bookmark, contentDescription = "מסומן", tint = theme.accentColor, modifier = Modifier.padding(top = 3.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Column {
            Text(segment.refDisplay, color = theme.textColor.copy(alpha = 0.45f), fontSize = 11.sp)
            Text(cleanedText, color = theme.textColor, fontSize = fontSize.sp, lineHeight = (fontSize * 1.5f).sp)
        }
    }
}

@Composable
private fun ContentOptionsMenu(
    refDisplay: String,
    contentLabel: String,
    isBookmarked: Boolean,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onShowCommentaries: () -> Unit,
    onIncreaseFont: () -> Unit,
    onDecreaseFont: () -> Unit,
) {
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(theme.surfaceColor).padding(vertical = 8.dp),
        ) {
            Text(
                refDisplay,
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            MenuRow(
                if (isBookmarked) "הסר $contentLabel מהסימניות" else "הוסף $contentLabel לסימניות",
                if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                theme,
                focusRequester = firstRowFocusRequester,
                onClick = onToggleBookmark,
            )
            MenuRow("שתף $contentLabel", Icons.Rounded.Share, theme, onClick = onShare)
            MenuRow("מפרשים על ה$contentLabel", Icons.AutoMirrored.Rounded.Comment, theme, onClick = onShowCommentaries)
            MenuRow("הגדל גופן", Icons.Rounded.TextIncrease, theme, onClick = onIncreaseFont)
            MenuRow("הקטן גופן", Icons.Rounded.TextDecrease, theme, onClick = onDecreaseFont)
        }
    }
}

@Composable
private fun CommentariesDialog(
    contentLabel: String,
    results: List<CommentaryEntry>?,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onOpen: (CommentaryEntry) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(vertical = 8.dp),
        ) {
            Text(
                "מפרשים על ה$contentLabel",
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            when {
                results == null -> Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = theme.accentColor)
                }
                results.isEmpty() -> Text(
                    "לא נמצאו מפרשים",
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                else -> LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                    items(results, key = { it.bookId }) { entry ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isFocused) theme.accentColor.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable(interactionSource = interactionSource, indication = null) { onOpen(entry) }
                                .focusable(interactionSource = interactionSource)
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Text(entry.bookTitle, color = theme.textColor, fontSize = 15.sp)
                            Text(entry.preview, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    label: String,
    icon: ImageVector,
    theme: FutureTheme,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "menuRowScale")
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (isFocused) theme.accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .then(if (isFocused) Modifier.border(width = 1.5.dp, color = theme.accentColor, shape = shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = theme.textColor, fontSize = 15.sp)
    }
}
