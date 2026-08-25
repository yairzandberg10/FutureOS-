package com.future.sfarim.util

/** ממיר מספר לגימטריה עברית (עם טיפול מיוחד ב-15/16 -> טו/טז) וממפה שמות
 * מבנה אנגליים (Chapter/Verse/Daf...) למונחים עבריים - תואם ל-build_library.py. */
object HebrewNumerals {
    private val LETTERS = listOf(
        1000 to "א'", 900 to "תתק", 400 to "ת", 300 to "ש", 200 to "ר", 100 to "ק",
        90 to "צ", 80 to "פ", 70 to "ע", 60 to "ס", 50 to "נ", 40 to "מ", 30 to "ל",
        20 to "כ", 19 to "יט", 18 to "יח", 17 to "יז", 16 to "טז", 15 to "טו",
        10 to "י", 9 to "ט", 8 to "ח", 7 to "ז", 6 to "ו", 5 to "ה", 4 to "ד",
        3 to "ג", 2 to "ב", 1 to "א",
    )

    fun toHebrewNumeral(n: Int): String {
        if (n <= 0 || n > 999) return n.toString()
        var remaining = n
        val sb = StringBuilder()
        while (remaining > 0) {
            val (value, letters) = LETTERS.first { remaining >= it.first }
            sb.append(letters)
            remaining -= value
        }
        return sb.toString()
    }

    private val SECTION_NAMES_HE = mapOf(
        "Chapter" to "פרק", "Verse" to "פסוק", "Daf" to "דף", "Line" to "שורה",
        "Halakhah" to "הלכה", "Siman" to "סימן", "Seif" to "סעיף", "Se'if" to "סעיף",
        "Section" to "סימן", "Paragraph" to "פסקה", "Mishnah" to "משנה",
        "Chelek" to "חלק", "Sha'ar" to "שער", "Perek" to "פרק", "Pasuk" to "פסוק",
        "Comment" to "ביאור", "Letter" to "אות", "Seif Katan" to "סעיף קטן",
        "Piska" to "פסקה", "Remez" to "רמז", "Year" to "שנה", "Responsum" to "תשובה",
        "Volume" to "כרך", "Page" to "עמוד", "Book" to "ספר", "Part" to "חלק",
        "Gate" to "שער", "Topic" to "נושא", "Passage" to "קטע", "Teaching" to "לימוד",
    )

    fun sectionNameHe(en: String?): String? = en?.let { SECTION_NAMES_HE[it] ?: it }

    /** תווית פרק לתצוגה ברשימת הפרקים, למשל "פרק א" או "דף ב". */
    fun chapterLabel(sectionNames: List<String>, topIndex: Int): String {
        val label = sectionNameHe(sectionNames.firstOrNull())
        val numeral = toHebrewNumeral(topIndex)
        return if (label != null) "$label $numeral" else numeral
    }
}
