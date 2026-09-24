package com.future.sfarim.ui.screens
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.theme.FutureDimens

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sfarim.data.BookChapter
import com.future.sfarim.data.LibraryBook
import com.future.sfarim.ui.components.ScreenTopBar
import com.future.sfarim.ui.components.RowIcon

import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.icons.FutureIcons

/** פריט ברשימה: פרק להיכנס אליו, או חלק בספר (למשל "אורח חיים" בערוך השולחן,
 * "תפילת שחרית" בסידור) שפותח רשימה משלו. */
sealed class ChapterEntry {
    data class Chapter(val chapter: BookChapter) : ChapterEntry()
    data class Part(val title: String, val path: List<String>) : ChapterEntry()
}

private fun keyOf(entry: ChapterEntry): String = when (entry) {
    is ChapterEntry.Chapter -> "c${entry.chapter.topIndex}"
    is ChapterEntry.Part -> "p${entry.path.joinToString("/")}"
}

/** הפריטים שמוצגים תחת חלק מסוים בספר: הפרקים ששייכים לו ישירות, ולצידם
 * החלקים שמתחתיו - כל אחד פעם אחת, במקום שבו מופיע הפרק הראשון שלו. */
fun chapterEntries(chapters: List<BookChapter>, groupPath: List<String>): List<ChapterEntry> {
    val seenParts = mutableSetOf<String>()
    return buildList {
        for (chapter in chapters) {
            if (chapter.groupPath.size < groupPath.size) continue
            if (chapter.groupPath.subList(0, groupPath.size) != groupPath) continue
            if (chapter.groupPath.size == groupPath.size) {
                add(ChapterEntry.Chapter(chapter))
            } else {
                val title = chapter.groupPath[groupPath.size]
                if (seenParts.add(title)) add(ChapterEntry.Part(title, groupPath + title))
            }
        }
    }
}

@Composable
fun BookChaptersScreen(
    book: LibraryBook,
    chapters: List<BookChapter>,
    // החלק בספר שמציגים כרגע - ריק ברמה העליונה (ר' Route.BookChapters).
    groupPath: List<String> = emptyList(),
    // ראו ReaderScreen: מבדיל בין "עוד לא נטען" ל"באמת אין תוכן".
    isLoading: Boolean = false,
    theme: FutureTheme,
    onBack: () -> Unit,
    onOpenChapter: (Int) -> Unit,
    onOpenPart: (List<String>) -> Unit = {},
) {
    var jumpBuffer by remember { mutableStateOf("") }
    val entries = remember(chapters, groupPath) { chapterEntries(chapters, groupPath) }
    // קפיצה לפי מספר עובדת על המספור של הרשימה המוצגת: בספר פשוט זה top_index
    // כמו קודם, ובחלק של ספר מורכב זה מספר הסימן/השער בתוך אותו חלק.
    val jumpTargets = remember(entries) {
        entries.filterIsInstance<ChapterEntry.Chapter>()
            .mapNotNull { e -> e.chapter.number?.let { it to e.chapter.topIndex } }
            .toMap()
    }
    // * ו-# מגיעים במכשיר רק כשידור מ-FutureUI (ר' onStarKeyPress).
    com.future.sharednav.nav.onPoundKeyPress {
        val target = jumpBuffer.toIntOrNull()
        jumpBuffer = ""
        jumpTargets[target]?.let(onOpenChapter)
    }
    com.future.sharednav.nav.onStarKeyPress { jumpBuffer = "" }
    val firstFocusRequester = remember(entries.firstOrNull()) { FocusRequester() }
    LaunchedEffect(entries.firstOrNull()) {
        if (entries.isNotEmpty()) firstFocusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    val digit = digitForKey(event.key)
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        digit != null -> {
                            jumpBuffer = (jumpBuffer + digit).takeLast(4)
                            true
                        }
                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_POUND -> {
                            val target = jumpBuffer.toIntOrNull()
                            jumpBuffer = ""
                            jumpTargets[target]?.let(onOpenChapter)
                            true
                        }
                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_STAR -> {
                            jumpBuffer = ""
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(
                    title = if (groupPath.isEmpty()) book.displayTitle else "${book.displayTitle} · ${groupPath.last()}",
                    theme = theme,
                    onBack = onBack,
                )
                if (jumpBuffer.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(theme.accentColor.copy(alpha = 0.15f), FutureShapes.lg)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("עבור אל $jumpBuffer  (# לאישור, * לניקוי)", color = theme.accentColor, fontSize = FutureTypography.summary)
                        }
                    }
                }
                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (isLoading) "טוען" else "אין תוכן זמין",
                            color = theme.textColor.copy(alpha = 0.5f),
                            fontSize = FutureTypography.bodyLarge,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(entries, key = { _, it -> keyOf(it) }) { index, entry ->
                            val focusRequester = if (index == 0) firstFocusRequester else null
                            when (entry) {
                                is ChapterEntry.Chapter -> FutureListItem(
                                    title = entry.chapter.label,
                                    theme = theme,
                                    onClick = { onOpenChapter(entry.chapter.topIndex) },
                                    focusRequester = focusRequester,
                                )
                                // חלק בספר מקבל את אותו מראה של קטגוריה במסך העיון -
                                // אותו אייקון בדיוק, כדי שיהיה ברור שהוא נפתח לרשימה.
                                is ChapterEntry.Part -> FutureListItem(
                                    title = entry.title,
                                    theme = theme,
                                    onClick = { onOpenPart(entry.path) },
                                    focusRequester = focusRequester,
                                    leading = { RowIcon(FutureIcons.AutoMirrored.MenuBook, theme) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
