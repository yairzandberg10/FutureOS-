package com.future.dialer.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    /** שלושת הטאבים של ui_kits/calls, היומן ראשון ("log first"). */
    object CallLog : Screen("calllog")
    object Dialpad : Screen("dialpad")
    object Favorites : Screen("favorites")

    /** חיפוש לפי שם או ספרות - מתפריט האפשרויות. */
    object Search : Screen("search")

    /** מסך איש קשר: פעולות והיסטוריית השיחות עם המספר. */
    object Contact : Screen("contact/{name}/{number}") {
        fun createRoute(name: String, number: String): String =
            "contact/${encode(name)}/${encode(number)}"
    }

    object InCall : Screen("incall/{name}/{number}") {
        fun createRoute(name: String, number: String): String =
            "incall/${encode(name)}/${encode(number)}"
    }
}

/**
 * ניווט לא מקבל מחרוזת ריקה כארגומנט בנתיב - "-" מסמן "אין" ([decodeArg]).
 * Uri.encode ולא URLEncoder: URLEncoder הופך רווח ל-"+", והניווט לא מחזיר
 * אותו לרווח - "מיכל לוי" הגיע למסך השיחה כ"מיכל+לוי".
 */
private fun encode(value: String): String = Uri.encode(value.ifEmpty { "-" })

/** הצד השני של [encode]. */
fun decodeArg(value: String?): String = value?.takeIf { it != "-" }.orEmpty()
