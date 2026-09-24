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
    data class Reader(val bookId: Long, val topIndex: Int) : Route()
    data object Search : Route()
    data object Bookmarks : Route()
}
