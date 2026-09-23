package com.future.dialer.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    /** שלושת הטאבים: יומן, אנשי קשר, מקלדת - מימין לשמאל, היומן ראשון. */
    object CallLog : Screen("calllog")
    object Dialpad : Screen("dialpad")
    /** אנשי קשר - במקום המועדפים (המועדפים ראשונים בראש הרשימה). */
    object Contacts : Screen("contacts")

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
