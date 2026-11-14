package com.future.music.util

import com.future.sharednav.t9.T9DigitMap

/** חיפוש חיזוי T9 על שמות שירים/אמנים/אלבומים - מיפוי עברי (פריסת מקלדת T9
 * עברית סטנדרטית) בנוסף למיפוי האנגלי, כי תוכן המוזיקה יכול להיות בשתי השפות.
 * המיפוי עצמו מגיע מ-SharedKeypadNav (T9DigitMap) - כולל האותיות הסופיות
 * על מקש 9 (ךםןףץ) שבעבר היו חסרות כאן. */
object T9Search {
    private val T9_MAP_HE = T9DigitMap.HEBREW
    private val T9_MAP_EN = T9DigitMap.ENGLISH

    fun matches(name: String, query: String): Boolean {
        if (query.isEmpty()) return true
        val normalized = name.trim()
        val map = if (normalized.any { it in '֐'..'׿' }) T9_MAP_HE else T9_MAP_EN
        return matchesSequence(if (map === T9_MAP_EN) normalized.lowercase() else normalized, query, map)
    }

    private fun matchesSequence(name: String, query: String, map: Map<Char, String>): Boolean {
        if (query.length > name.length) return false
        for (i in query.indices) {
            val digit = query[i]
            val charAtName = name[i]
            val possibleChars = map[digit] ?: digit.toString()
            if (charAtName !in possibleChars) return false
        }
        return true
    }

    /** true אם query (רצף ספרות T9) תואם קידומת של מילה כלשהי בתוך name (לא רק תחילת המחרוזת כולה). */
    fun matchesAnyWord(name: String, query: String): Boolean {
        if (query.isEmpty()) return true
        return name.split(' ', '-', '"', '\'').any { word -> word.isNotEmpty() && matches(word, query) }
    }
}
