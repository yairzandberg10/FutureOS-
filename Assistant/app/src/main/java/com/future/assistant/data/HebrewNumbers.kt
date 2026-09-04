package com.future.assistant.data

/**
 * המרת מילות מספר בעברית (בין 0 ל-199) למספרים - כי בדיבור טבעי אנשים
 * אומרים "שבע כפול שבע" ולא "7 כפול 7" (ראו נתוני MASSIVE - qa_maths).
 * לא תומך במאות/אלפים מעבר ל-199, זה מספיק לפקודות חשבון קוליות.
 */
object HebrewNumbers {
    private val ONES = mapOf(
        "אפס" to 0, "אחד" to 1, "אחת" to 1, "שתיים" to 2, "שני" to 2, "שתי" to 2,
        "שלוש" to 3, "שלושה" to 3, "ארבע" to 4, "ארבעה" to 4, "חמש" to 5, "חמישה" to 5,
        "שש" to 6, "שישה" to 6, "שבע" to 7, "שבעה" to 7, "שמונה" to 8,
        "תשע" to 9, "תשעה" to 9, "עשר" to 10, "עשרה" to 10
    )

    private val TEENS = mapOf(
        "אחת עשרה" to 11, "אחד עשר" to 11, "שתים עשרה" to 12, "שנים עשר" to 12,
        "שלוש עשרה" to 13, "ארבע עשרה" to 14, "חמש עשרה" to 15, "שש עשרה" to 16,
        "שבע עשרה" to 17, "שמונה עשרה" to 18, "תשע עשרה" to 19
    )

    private val TENS = mapOf(
        "עשרים" to 20, "שלושים" to 30, "ארבעים" to 40, "חמישים" to 50,
        "שישים" to 60, "שבעים" to 70, "שמונים" to 80, "תשעים" to 90
    )

    private val HUNDRED = mapOf("מאה" to 100, "מאתיים" to 200)

    /** מוצא עד שני מספרים (כמילים) בטקסט, לפי סדר הופעה. תומך בצירופים
     * כמו "עשרים ושבע" (27) ו"מאה עשרים" (120). */
    fun findNumbers(text: String, limit: Int = 2): List<Int> {
        val words = text.split(" ", "-").filter { it.isNotBlank() }
        val results = mutableListOf<Int>()
        var i = 0
        while (i < words.size && results.size < limit) {
            val w = words[i].removePrefix("ו")
            val twoWord = if (i + 1 < words.size) "$w ${words[i + 1]}" else null

            when {
                twoWord != null && TEENS.containsKey(twoWord) -> {
                    results.add(TEENS.getValue(twoWord)); i += 2
                }
                HUNDRED.containsKey(w) -> {
                    var value = HUNDRED.getValue(w)
                    i += 1
                    if (i < words.size) {
                        val next = words[i].removePrefix("ו")
                        val rest = parseSingleToken(next)
                        if (rest != null) { value += rest; i += 1 }
                    }
                    results.add(value)
                }
                TENS.containsKey(w) -> {
                    var value = TENS.getValue(w)
                    i += 1
                    if (i < words.size) {
                        val next = words[i].removePrefix("ו")
                        if (ONES.containsKey(next)) { value += ONES.getValue(next); i += 1 }
                    }
                    results.add(value)
                }
                TEENS.containsKey(w) -> {
                    results.add(TEENS.getValue(w)); i += 1
                }
                ONES.containsKey(w) -> {
                    results.add(ONES.getValue(w)); i += 1
                }
                else -> i += 1
            }
        }
        return results
    }

    private fun parseSingleToken(token: String): Int? =
        ONES[token] ?: TENS[token] ?: TEENS[token]
}
