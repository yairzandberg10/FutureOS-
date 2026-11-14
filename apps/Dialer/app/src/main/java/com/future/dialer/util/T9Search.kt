package com.future.dialer.util

import com.future.sharednav.t9.T9DigitMap

/** חיפוש חיזוי T9 על שמות אנשי קשר - מיפוי עברי בנוסף לאנגלי, כי שם איש קשר
 * יכול להיות בכל אחת מהשפות. בלי זה, שמות אנשי קשר בעברית לא היו נמצאים
 * בכלל בחיפוש T9 (המיפוי האנגלי הקבוע לא מכיל אף אות עברית). */
object T9Search {

    fun matches(name: String, query: String): Boolean {
        if (query.isEmpty()) return true
        val map = if (name.any { T9DigitMap.isHebrew(it) }) T9DigitMap.HEBREW else T9DigitMap.ENGLISH
        val normalizedName = if (map === T9DigitMap.ENGLISH) name.lowercase() else name
        return matchesSequence(normalizedName, query, map)
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
     * משפחה" והמשתמש עשוי לחפש לפי כל אחד מהם. זהה בדפוס למימוש המקביל
     * ב-Contact/Music (ר' T9Search שם) - dialer היה חסר את זה, כך שחיפוש
     * T9 בחייגן לא מצא אנשי קשר לפי שם משפחה. */
    fun matchesAnyWord(name: String, query: String): Boolean {
        if (query.isEmpty()) return true
        return name.split(' ', '-', '"', '\'').any { word -> word.isNotEmpty() && matches(word, query) }
    }
}
