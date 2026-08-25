package com.future.sfarim.ui

/** מסלולי הניווט - מחסנית ידנית (בלי navigation-compose, כמו שאר אפליקציות
 * הסוויטה) כדי לתמוך בעץ קטגוריות בעומק משתנה. */
sealed class Route {
    data object Home : Route()
    data class Browse(val categoryId: Long, val title: String) : Route()
    data class BookChapters(val bookId: Long) : Route()
    data class Reader(val bookId: Long, val topIndex: Int) : Route()
    data object Search : Route()
    data object Bookmarks : Route()
}
