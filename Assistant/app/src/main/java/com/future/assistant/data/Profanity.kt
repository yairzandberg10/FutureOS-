package com.future.assistant.data

/**
 * זיהוי קללות וגידופים במשפט. ההתאמה ברמת מילה שלמה (אחרי הסרת אותיות
 * השימוש ו/ה/ש/ב/ל/מ/כ בתחילת מילה), כדי ש"כוס מים" או "חרטום" לא ייתפסו.
 * ביטויים של כמה מילים נבדקים כהכלה.
 */
object Profanity {
    private val WORDS = setOf(
        "זין", "זונה", "זונות", "שרמוטה", "שרמוטות", "שרמוט", "מניאק", "מניאקית", "מניאקים",
        "תזדיין", "תזדייני", "מזדיין", "מזדיינת", "זדיין", "חרא", "חארה",
        "אידיוט", "אידיוטית", "אידיוטים", "מטומטם", "מטומטמת", "דביל", "דבילית", "מפגר", "מפגרת",
        "טמבל", "טמבלית", "אהבל", "כוסאמק", "כוסעמק", "כוסית",
        "מנוול", "מנוולת", "חלאה", "חלאות", "קוקסינל", "שמוק",
        "פאק", "פאקינג", "בולשיט", "ביץ'", "ביץ", "fuck", "fucking", "shit", "bitch", "asshole", "damn",
    )

    private val PHRASES = listOf(
        "כוס אמק", "כוס אמא", "כוס עמק", "כוס אחותך", "בן זונה", "בת זונה", "לך תזדיין",
        "סתום את הפה", "סתום ת'פה", "סתמי את הפה", "לך לעזאזל", "וואט דה פאק",
    )

    private const val PREFIXES = "והשבלמכ"

    fun contains(text: String): Boolean {
        val lower = text.lowercase()
        if (PHRASES.any { lower.contains(it) }) return true
        return lower.split(Regex("[\\s,.!?;:\"]+")).any { raw ->
            val w = raw.trim()
            w.isNotEmpty() && (w in WORDS || (w.length > 3 && w[0] in PREFIXES && w.substring(1) in WORDS))
        }
    }
}
