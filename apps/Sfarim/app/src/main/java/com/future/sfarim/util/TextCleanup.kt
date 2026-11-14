package com.future.sfarim.util

private val HTML_TAG_REGEX = Regex("<[^>]*>")

/** מנקה תגיות HTML קלות שמופיעות בטקסט המקורי מספריא (כמו <big>, <i>, <sup>
 * להערות שוליים) - לא בונה AnnotatedString מלא, רק מסיר לתצוגה נקייה ב-MVP. */
fun stripHtmlTags(text: String): String {
    return text
        .replace(HTML_TAG_REGEX, "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .trim()
}
