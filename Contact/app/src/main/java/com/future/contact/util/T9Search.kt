package com.future.contact.util

import com.future.sharednav.t9.T9DigitMap

/** חיפוש חיזוי T9 על שמות אנשי קשר - מיפוי עברי בנוסף לאנגלי, כי שם איש קשר
 * יכול להיות בכל אחת מהשפות. המיפוי עצמו מגיע מ-SharedKeypadNav (T9DigitMap).
 * זהה בדפוס למימוש המקביל ב-dialer/Music (ראו T9Search שם). */
object T9Search {
    private val T9_MAP_HE = T9DigitMap.HEBREW
    private val T9_MAP_EN = T9DigitMap.ENGLISH

    fun matches(name: String, query: String): Boolean {
        if (query.isEmpty()) return true
        val normalized = name.trim()
        val map = if (normalized.any { T9DigitMap.isHebrew(it) }) T9_MAP_HE else T9_MAP_EN
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

    /** true אם query (רצף ספרות T9) תואם קידומת של מילה כלשהי בתוך name (לא
     * רק תחילת המחרוזת כולה) - כי אנשי קשר נשמרים בדרך כלל כ"שם פרטי שם
     * משפחה" והמשתמש עשוי לחפש לפי כל אחד מהם. */
    fun matchesAnyWord(name: String, query: String): Boolean {
        if (query.isEmpty()) return true
        return name.split(' ', '-', '"', '\'').any { word -> word.isNotEmpty() && matches(word, query) }
    }
}
