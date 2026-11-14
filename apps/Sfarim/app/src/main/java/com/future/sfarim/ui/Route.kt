package com.future.sfarim.ui

/** מסלולי הניווט - מחסנית ידנית (בלי navigation-compose, כמו שאר אפליקציות
 * הסוויטה) כדי לתמוך בעץ קטגוריות בעומק משתנה. */
sealed class Route {
    data object Home : Route()
    data class Browse(val categoryId: Long, val title: String) : Route()
    /** groupPath = החלק בתוך הספר שמציגים כרגע (ר' BookChapter.groupPath),
     * ריק ברמה העליונה של הספר. כל ירידה לחלק היא push נוסף, כך שמקש חזרה
     * מטפס חזרה במבנה הספר בדיוק כמו בעץ הקטגוריות. */
    data class BookChapters(val bookId: Long, val groupPath: List<String> = emptyList()) : Route()
    /** focusSegmentId - הקטע לפתוח עליו (תוצאת חיפוש, סימניה, "המשך קריאה",
     * מפרש). בלעדיו הקורא נפתח תמיד בראש הפרק, והמשתמש חיפש שוב את המקום. */
    data class Reader(val bookId: Long, val topIndex: Int, val focusSegmentId: Long? = null) : Route()
    data object Search : Route()
    data object Bookmarks : Route()
}

private const val SEP = '\u001F'
private const val LIST_SEP = '\u001E'

/** ייצוג מחרוזתי של מסלול לשמירה (S03). שמות מפורשים כי R8 משנה שמות מחלקות. */
internal fun Route.encodeRoute(): String = when (this) {
    Route.Home -> "Home"
    is Route.Browse -> "Browse$SEP$categoryId$SEP$title"
    is Route.BookChapters -> "BookChapters$SEP$bookId$SEP${groupPath.joinToString(LIST_SEP.toString())}"
    is Route.Reader -> "Reader$SEP$bookId$SEP$topIndex$SEP${focusSegmentId ?: ""}"
    Route.Search -> "Search"
    Route.Bookmarks -> "Bookmarks"
}

internal fun decodeRoute(encoded: String): Route? {
    val parts = encoded.split(SEP, limit = 4)
    return when (parts[0]) {
        "Home" -> Route.Home
        "Browse" -> {
            val full = encoded.split(SEP, limit = 3)
            full.getOrNull(1)?.toLongOrNull()?.let { Route.Browse(it, full.getOrElse(2) { "" }) }
        }
        "BookChapters" -> {
            val full = encoded.split(SEP, limit = 3)
            full.getOrNull(1)?.toLongOrNull()?.let { id ->
                val path = full.getOrElse(2) { "" }.takeIf { it.isNotEmpty() }?.split(LIST_SEP) ?: emptyList()
                Route.BookChapters(id, path)
            }
        }
        "Reader" -> {
            val bookId = parts.getOrNull(1)?.toLongOrNull()
            val top = parts.getOrNull(2)?.toIntOrNull()
            if (bookId != null && top != null) Route.Reader(bookId, top, parts.getOrNull(3)?.toLongOrNull()) else null
        }
        "Search" -> Route.Search
        "Bookmarks" -> Route.Bookmarks
        else -> null
    }
}
