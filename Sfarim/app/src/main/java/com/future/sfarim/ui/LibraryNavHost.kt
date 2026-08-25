package com.future.sfarim.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.future.sfarim.data.LibraryRepository
import com.future.sfarim.ui.screens.BookChaptersScreen
import com.future.sfarim.ui.screens.BookmarksScreen
import com.future.sfarim.ui.screens.BrowseScreen
import com.future.sfarim.ui.screens.HomeScreen
import com.future.sfarim.ui.screens.ReaderScreen
import com.future.sfarim.ui.screens.SearchScreen
import com.future.sfarim.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibraryNavHost(repository: LibraryRepository, theme: FutureTheme) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    val scope = rememberCoroutineScope()
    val current = backStack.last()
    // גרסת נתונים - מוגברת אחרי כל כתיבה (סימניה/התקדמות) כדי לאלץ רענון של
    // שאילתות שנשענות על מפתח שלא בהכרח משתנה (למשל אותו bookId+topIndex).
    var dataVersion by remember { mutableIntStateOf(0) }

    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }

    fun push(route: Route) = backStack.add(route)
    fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

    /** פותח ספר: אם יש רק "פרק" אחד (או אפס, למשל טקסט קצר בלי חלוקה) קופצים
     * ישר לקורא, אחרת מציגים את רשימת הפרקים. */
    fun openBook(bookId: Long) {
        scope.launch {
            val chapters = withContext(Dispatchers.IO) { repository.getChapters(bookId) }
            if (chapters.size <= 1) {
                push(Route.Reader(bookId, chapters.firstOrNull()?.topIndex ?: 1))
            } else {
                push(Route.BookChapters(bookId))
            }
        }
    }

    fun openSegment(bookId: Long, segmentId: Long) {
        scope.launch {
            val segment = withContext(Dispatchers.IO) { repository.getSegment(segmentId) } ?: return@launch
            push(Route.Reader(bookId, segment.topIndex))
        }
    }

    when (val route = current) {
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
            val childCategories = rememberIoState(route.categoryId) { repository.getChildCategories(route.categoryId) } ?: emptyList()
            val books = rememberIoState(route.categoryId) { repository.getBooksInCategory(route.categoryId) } ?: emptyList()
            BrowseScreen(
                title = route.title,
                childCategories = childCategories,
                books = books,
                theme = theme,
                onBack = ::pop,
                onOpenCategory = { push(Route.Browse(it.id, it.nameHe?.takeIf { n -> n.isNotBlank() } ?: it.nameEn)) },
                onOpenBook = { openBook(it.id) },
            )
        }

        is Route.BookChapters -> {
            val book = rememberIoState(route.bookId) { repository.getBook(route.bookId) }
            val chapters = rememberIoState(route.bookId) { repository.getChapters(route.bookId) } ?: emptyList()
            if (book != null) {
                BookChaptersScreen(
                    book = book,
                    chapters = chapters,
                    theme = theme,
                    onBack = ::pop,
                    onOpenChapter = { topIndex -> push(Route.Reader(route.bookId, topIndex)) },
                )
            }
        }

        is Route.Reader -> {
            val book = rememberIoState(route.bookId) { repository.getBook(route.bookId) }
            val segments = rememberIoState(route.bookId to route.topIndex) {
                repository.getSegments(route.bookId, route.topIndex)
            } ?: emptyList()
            val allChapterIndices = rememberIoState(route.bookId) {
                repository.getChapters(route.bookId).map { c -> c.topIndex }
            } ?: emptyList()
            val bookmarkedIds = rememberIoState(Triple(route.bookId, route.topIndex, dataVersion)) {
                repository.getBookmarkedSegmentIds(route.bookId)
            } ?: emptySet()

            if (book != null) {
                val currentPos = allChapterIndices.indexOf(route.topIndex)
                ReaderScreen(
                    book = book,
                    topIndex = route.topIndex,
                    segments = segments,
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
                        withContext(Dispatchers.IO) { repository.getCommentaries(book, prefix) }
                    },
                    onOpenCommentary = { entry -> push(Route.Reader(entry.bookId, entry.topIndex)) },
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
                onOpenSegment = { push(Route.Reader(it.bookId, it.topIndex)) },
            )
        }

        is Route.Bookmarks -> {
            val bookmarks = rememberIoState(dataVersion) { repository.getBookmarks() } ?: emptyList()
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
