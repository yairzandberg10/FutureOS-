package com.future.sfarim.ui.screens

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sfarim.data.BookSearchEntry
import com.future.sfarim.data.SegmentSearchResult
import com.future.sfarim.ui.components.FocusableItem
import com.future.sfarim.ui.components.ScreenTopBar
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import com.future.sfarim.util.stripHtmlTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    theme: FutureTheme,
    onBack: () -> Unit,
    onSearchBooks: suspend (String) -> List<BookSearchEntry>,
    onSearchSegments: suspend (String) -> List<SegmentSearchResult>,
    onOpenBook: (BookSearchEntry) -> Unit,
    onOpenSegment: (SegmentSearchResult) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var bookResults by remember { mutableStateOf<List<BookSearchEntry>>(emptyList()) }
    var segmentResults by remember { mutableStateOf<List<SegmentSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            bookResults = emptyList()
            segmentResults = emptyList()
            searching = false
            failed = false
            return@LaunchedEffect
        }
        failed = false
        searching = true
        delay(SEARCH_DEBOUNCE_MS) // debounce - לא לשלוח שאילתה על כל הקשה
        // חיפוש כותרים ראשון (טבלה של אלפי שורות - מיידי) ומוצג מיד, ורק אחריו
        // החיפוש בגוף הטקסט (אינדקס של מיליוני קטעים) - כך יש תוצאות על המסך
        // בזמן שהחיפוש הכבד עדיין רץ.
        try {
            bookResults = withContext(Dispatchers.IO) { onSearchBooks(query) }
            // אות בודדת = חיפוש קידומת שמתאים למיליוני קטעים; הוא לא מוסיף מידע
            // למשתמש ועולה סדר גודל יותר מכל שאילתה אחרת, אז הוא לא נשלח בכלל.
            segmentResults = if (query.trim().length >= MIN_FULLTEXT_QUERY_LENGTH) {
                withContext(Dispatchers.IO) { onSearchSegments(query) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // שאילתת חיפוש שנכשלת (טבלת אינדקס חסרה ב-sefaria.db, DB פגום וכו')
            // לא מפילה את האפליקציה - מוצגת הודעה במקום.
            android.util.Log.w("SearchScreen", "search failed for '$query'", e)
            bookResults = emptyList()
            segmentResults = emptyList()
            failed = true
        }
        searching = false
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = "חיפוש", theme = theme, onBack = onBack)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(theme.textColor.copy(alpha = 0.08f), FutureShapes.sm)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (query.isEmpty()) {
                        Text("הקלד טקסט לחיפוש", color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.bodyLarge)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(color = theme.textColor, fontSize = FutureTypography.bodyLarge, textDirection = androidx.compose.ui.text.style.TextDirection.Rtl),
                        cursorBrush = SolidColor(theme.accentColor),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .escapeTextFieldFocusTrap(),
                    )
                }

                val noResults = query.isNotBlank() && bookResults.isEmpty() && segmentResults.isEmpty()
                if (noResults) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            when {
                                failed -> "החיפוש נכשל"
                                searching -> "מחפש"
                                else -> "לא נמצאו תוצאות"
                            },
                            color = theme.textColor.copy(alpha = 0.5f),
                            fontSize = FutureTypography.bodyLarge,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        if (bookResults.isNotEmpty()) {
                            item {
                                Text(
                                    "ספרים",
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = FutureTypography.label,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                                )
                            }
                            items(bookResults, key = { "book${it.id}" }) { entry ->
                                FocusableItem(onClick = { onOpenBook(entry) }, theme = theme, modifier = Modifier.fillMaxWidth()) { _ ->
                                    Text(
                                        entry.displayTitle,
                                        color = theme.textColor,
                                        fontSize = FutureTypography.bodyLarge,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                    )
                                }
                            }
                        }
                        if (segmentResults.isNotEmpty()) {
                            item {
                                Text(
                                    "פסוקים",
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = FutureTypography.label,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                                )
                            }
                            items(segmentResults, key = { "seg${it.segmentId}" }) { result ->
                                FocusableItem(onClick = { onOpenSegment(result) }, theme = theme, modifier = Modifier.fillMaxWidth()) { _ ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                                        Text(
                                            "${result.bookTitle} · ${result.refDisplay}",
                                            color = theme.textColor.copy(alpha = 0.5f),
                                            fontSize = FutureTypography.caption,
                                        )
                                        Text(stripHtmlTags(result.snippet), color = theme.textColor, fontSize = FutureTypography.body, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** דיבאונס ארוך מ-200ms: במקלדת T9 כל תו דורש כמה הקשות, ושאילתת FTS על
 * אינדקס של מיליוני קטעים היא הפעולה היקרה ביותר באפליקציה. */
private const val SEARCH_DEBOUNCE_MS = 350L

/** מתחת לזה מחפשים רק בשמות הספרים, לא בגוף הטקסט (ראו LaunchedEffect למעלה). */
private const val MIN_FULLTEXT_QUERY_LENGTH = 2
