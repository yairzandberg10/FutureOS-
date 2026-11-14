package com.future.sfarim.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.future.sfarim.data.LibraryRepository
import com.future.sfarim.ui.screens.BookChaptersScreen
import com.future.sfarim.ui.screens.ChapterEntry
import com.future.sfarim.ui.screens.chapterEntries
import com.future.sfarim.ui.screens.BookmarksScreen
import com.future.sfarim.ui.screens.BrowseScreen
import com.future.sfarim.ui.screens.HomeScreen
import com.future.sfarim.ui.screens.ReaderScreen
import com.future.sfarim.ui.screens.SearchScreen
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibraryNavHost(repository: LibraryRepository, theme: FutureTheme) {
    // S03: המחסנית שורדת סיבוב מסך ושחזור תהליך
    val backStack = rememberSaveable(saver = backStackSaver) { mutableStateListOf<Route>(Route.Home) }
    val scope = rememberCoroutineScope()
    val current = backStack.last()
    // גרסת נתונים - מוגברת אחרי כל כתיבה (סימניה/התקדמות) כדי לאלץ רענון של
    // שאילתות שנשענות על מפתח שלא בהכרח משתנה (למשל אותו bookId+topIndex).
    var dataVersion by remember { mutableIntStateOf(0) }

    // מפתח הפריט (ראו BrowseScreen.keyOf) שנפתח לאחרונה מכל מסך "עיון בקטגוריה"
    // (categoryId -> מפתח) - כך שכשחוזרים "אחורה" מכל רמת קטגוריה, הפוקוס
    // חוזר בדיוק לפריט שממנו יצאנו, לא תמיד לראשון ברשימה.
    var lastBrowseSelection by remember { mutableStateOf<Map<Long?, String>>(emptyMap()) }

    // כמו lastBrowseSelection: הפריט שנפתח לאחרונה בכל מסך פרקים (לפי ספר וחלק)
    var lastChapterSelection by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }

    fun push(route: Route) = backStack.add(route)
    fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

    /** פותח ספר: אם יש רק "פרק" אחד (או אפס, למשל טקסט קצר בלי חלוקה) קופצים
     * ישר לקורא, אחרת מציגים את רשימת הפרקים - ובספר שכולו חלק אחד ("שולחן
     * ערוך" שיש בו רק אורח חיים למשל) יורדים ישר לתוכו, בלי מסך שיש בו פריט
     * בודד שהמשתמש חייב ללחוץ עליו. */
    // S26: לחיצה כפולה בזמן טעינה לא דוחפת שני מסלולים
    var opening by remember { mutableStateOf(false) }

    fun openBook(bookId: Long) {
        if (opening) return
        opening = true
        scope.launch {
            try {
                val chapters = withContext(Dispatchers.IO) { repository.getChapters(bookId) }
                if (chapters.size <= 1) {
                    push(Route.Reader(bookId, chapters.firstOrNull()?.topIndex ?: 1))
                } else {
                    var groupPath = emptyList<String>()
                    while (true) {
                        val only = chapterEntries(chapters, groupPath).singleOrNull()
                        if (only is ChapterEntry.Part) groupPath = only.path else break
                    }
                    push(Route.BookChapters(bookId, groupPath))
                }
            } finally {
                opening = false
            }
        }
    }

    fun openSegment(bookId: Long, segmentId: Long) {
        if (opening) return
        opening = true
        scope.launch {
            try {
                val segment = withContext(Dispatchers.IO) { repository.getSegment(segmentId) } ?: return@launch
                push(Route.Reader(bookId, segment.topIndex, segmentId))
            } finally {
                opening = false
            }
        }
    }

    // כל push/pop מחליק בכיוון הניווט; מעבר פרק (החלפת ראש המחסנית) הוא fade.
    com.future.sharednav.components.AnimatedBackStackHost(backStack) { route ->
    when (route) {
        is Route.Home -> {
            val categories = rememberIoState(Unit) { repository.getChildCategories(null) } ?: emptyList()
            val progress = rememberIoState(Unit) { repository.getLatestReadingProgress() }
            HomeScreen(
                categories = categories,
                continueReading = progress,
                theme = theme,
                onOpenCategory = { push(Route.Browse(it.id, it.nameHe?.takeIf { n -> n.isNotBlank() } ?: it.nameEn)) },
                onOpenSearch = { push(Route.Search) },
                onOpenBookmarks = { push(Route.Bookmarks) },
                onContinueReading = { openSegment(it.bookId, it.segmentId) },
            )
        }

        is Route.Browse -> {
            val childCategoriesState = rememberIoState(route.categoryId) { repository.getChildCategories(route.categoryId) }
            val booksState = rememberIoState(route.categoryId) { repository.getBooksInCategory(route.categoryId) }
            BrowseScreen(
                title = route.title,
                childCategories = childCategoriesState ?: emptyList(),
                books = booksState ?: emptyList(),
                isLoading = childCategoriesState == null || booksState == null,
                theme = theme,
                onBack = ::pop,
                onOpenCategory = {
                    lastBrowseSelection = lastBrowseSelection + (route.categoryId to "c${it.id}")
                    push(Route.Browse(it.id, it.nameHe?.takeIf { n -> n.isNotBlank() } ?: it.nameEn))
                },
                onOpenBook = {
                    lastBrowseSelection = lastBrowseSelection + (route.categoryId to "b${it.id}")
                    openBook(it.id)
                },
                lastSelectedKey = lastBrowseSelection[route.categoryId],
            )
        }

        is Route.BookChapters -> {
            val book = rememberIoState(route.bookId) { repository.getBook(route.bookId) }
            val chaptersState = rememberIoState(route.bookId) { repository.getChapters(route.bookId) }
            if (book != null) {
                BookChaptersScreen(
                    book = book,
                    chapters = chaptersState ?: emptyList(),
                    groupPath = route.groupPath,
                    isLoading = chaptersState == null,
                    theme = theme,
                    onBack = ::pop,
                    lastSelectedKey = lastChapterSelection["${route.bookId}/${route.groupPath.joinToString("/")}"],
                    onSelected = { key -> lastChapterSelection = lastChapterSelection + ("${route.bookId}/${route.groupPath.joinToString("/")}" to key) },
                    onOpenChapter = { topIndex -> push(Route.Reader(route.bookId, topIndex)) },
                    onOpenPart = { path -> push(Route.BookChapters(route.bookId, path)) },
                )
            }
        }

        is Route.Reader -> {
            val book = rememberIoState(route.bookId) { repository.getBook(route.bookId) }
            val segmentsState = rememberIoState(route.bookId to route.topIndex) {
                repository.getSegments(route.bookId, route.topIndex)
            }
            val segments = segmentsState ?: emptyList()
            val allChapters = rememberIoState(route.bookId) { repository.getChapters(route.bookId) } ?: emptyList()
            val allChapterIndices = remember(allChapters) { allChapters.map { c -> c.topIndex } }
            val bookmarkedIds = rememberIoState(Triple(route.bookId, route.topIndex, dataVersion), keepPrevious = true) {
                repository.getBookmarkedSegmentIds(route.bookId)
            } ?: emptySet()

            if (book != null) {
                val currentPos = allChapterIndices.indexOf(route.topIndex)
                ReaderScreen(
                    book = book,
                    topIndex = route.topIndex,
                    // בספר מורכב אי אפשר לגזור את שם הפרק מ-section_names (ר'
                    // BookChapter) - הוא מגיע מרשימת הפרקים, ועד שהיא נטענת
                    // הכותרת מציגה רק את שם הספר.
                    chapterLabel = allChapters.firstOrNull { it.topIndex == route.topIndex }?.fullLabel,
                    segments = segments,
                    focusSegmentId = route.focusSegmentId,
                    isLoading = segmentsState == null,
                    hasPrevChapter = currentPos > 0,
                    hasNextChapter = currentPos in 0 until allChapterIndices.lastIndex,
                    bookmarkedSegmentIds = bookmarkedIds,
                    theme = theme,
                    onBack = ::pop,
                    onPrevChapter = {
                        if (currentPos > 0) {
                            backStack[backStack.lastIndex] = Route.Reader(route.bookId, allChapterIndices[currentPos - 1])
                        }
                    },
                    onNextChapter = {
                        if (currentPos in 0 until allChapterIndices.lastIndex) {
                            backStack[backStack.lastIndex] = Route.Reader(route.bookId, allChapterIndices[currentPos + 1])
                        }
                    },
                    onToggleBookmark = { segment ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (segment.id in bookmarkedIds) {
                                    repository.removeBookmarkBySegment(segment.id)
                                } else {
                                    repository.addBookmark(segment.bookId, segment.id, null, System.currentTimeMillis())
                                }
                            }
                            dataVersion++
                        }
                    },
                    onSegmentShown = { segment ->
                        scope.launch(Dispatchers.IO) {
                            repository.setReadingProgress(segment.bookId, segment.id, System.currentTimeMillis())
                        }
                    },
                    onLoadCommentaries = { prefix ->
                        // S09: כשל בשאילתה מוצג כ"לא נמצאו מפרשים" ולא כספינר לנצח
                        try {
                            withContext(Dispatchers.IO) { repository.getCommentaries(book, prefix) }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.w("LibraryNavHost", "commentaries failed", e)
                            emptyList()
                        }
                    },
                    onOpenCommentary = { entry -> push(Route.Reader(entry.bookId, entry.topIndex, entry.segmentId)) },
                )
            }
        }

        is Route.Search -> {
            SearchScreen(
                theme = theme,
                onBack = ::pop,
                onSearchBooks = { query -> repository.searchBooks(query) },
                onSearchSegments = { query -> repository.searchSegments(query) },
                onOpenBook = { openBook(it.id) },
                onOpenSegment = { push(Route.Reader(it.bookId, it.topIndex, it.segmentId)) },
            )
        }

        is Route.Bookmarks -> {
            // S07: אחרי מחיקה הרשימה הקודמת נשארת עד שהחדשה נטענת - בלי הבהוב ובלי איבוד פוקוס
            val bookmarks = rememberIoState(dataVersion, keepPrevious = true) { repository.getBookmarks() } ?: emptyList()
            BookmarksScreen(
                bookmarks = bookmarks,
                theme = theme,
                onBack = ::pop,
                onOpenBookmark = { openSegment(it.bookId, it.segmentId) },
                onDeleteBookmark = { bookmark ->
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.removeBookmark(bookmark.id) }
                        dataVersion++
                    }
                },
            )
        }
    }
    }
}

/** מחסנית הניווט נשמרת כרשימת מחרוזות (מסלול אחד בכל שורה) - שורדת סיבוב ושחזור. */
private val backStackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map { it.encodeRoute() } },
    restore = { encoded ->
        val routes = encoded.mapNotNull { decodeRoute(it) }
        mutableStateListOf<Route>().apply { addAll(routes.ifEmpty { listOf(Route.Home) }) }
    },
)
