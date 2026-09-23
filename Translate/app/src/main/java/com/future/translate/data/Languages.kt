package com.future.translate.data

/** שפה בבוררים. [code] הוא קוד BCP-47 כפי ש-ML Kit מכיר אותו. */
data class Language(val code: String, val name: String, val rtl: Boolean = false)

/**
 * כל השפות שמנוע התרגום שעל המכשיר (ML Kit) יודע לתרגם, בשמות עבריים.
 * הרשימה בערכת העיצוב (ui_kits/translate) היא דוגמה; כאן היא הרשימה
 * האמיתית - אמהרית ויידיש שבדוגמה לא נתמכות במנוע, ולכן אינן כאן.
 */
object Languages {

    const val AUTO = "auto"

    /** "זיהוי שפה" - רק בבורר של שפת המקור. */
    val auto = Language(AUTO, "זיהוי שפה", rtl = true)

    val all: List<Language> = listOf(
        Language("he", "עברית", rtl = true),
        Language("en", "אנגלית"),
        Language("ar", "ערבית", rtl = true),
        Language("ru", "רוסית"),
        Language("fr", "צרפתית"),
        Language("es", "ספרדית"),
        Language("de", "גרמנית"),
        Language("it", "איטלקית"),
        Language("pt", "פורטוגזית"),
        Language("uk", "אוקראינית"),
        Language("tr", "טורקית"),
        Language("fa", "פרסית", rtl = true),
        Language("zh", "סינית"),
        Language("ja", "יפנית"),
        Language("ko", "קוריאנית"),
        Language("hi", "הינדי"),
        Language("ur", "אורדו", rtl = true),
        Language("pl", "פולנית"),
        Language("ro", "רומנית"),
        Language("hu", "הונגרית"),
        Language("cs", "צ'כית"),
        Language("sk", "סלובקית"),
        Language("sl", "סלובנית"),
        Language("hr", "קרואטית"),
        Language("bg", "בולגרית"),
        Language("mk", "מקדונית"),
        Language("sq", "אלבנית"),
        Language("el", "יוונית"),
        Language("nl", "הולנדית"),
        Language("sv", "שוודית"),
        Language("no", "נורווגית"),
        Language("da", "דנית"),
        Language("fi", "פינית"),
        Language("is", "איסלנדית"),
        Language("et", "אסטונית"),
        Language("lv", "לטבית"),
        Language("lt", "ליטאית"),
        Language("be", "בלארוסית"),
        Language("ka", "גאורגית"),
        Language("ga", "אירית"),
        Language("cy", "וולשית"),
        Language("mt", "מלטית"),
        Language("ca", "קטלאנית"),
        Language("gl", "גליסית"),
        Language("eo", "אספרנטו"),
        Language("af", "אפריקאנס"),
        Language("sw", "סווהילי"),
        Language("ht", "קריאולית האיטית"),
        Language("id", "אינדונזית"),
        Language("ms", "מלאית"),
        Language("tl", "טגלוג"),
        Language("vi", "וייטנאמית"),
        Language("th", "תאית"),
        Language("bn", "בנגלית"),
        Language("gu", "גוג'ראטית"),
        Language("kn", "קנאדה"),
        Language("mr", "מראטהית"),
        Language("ta", "טמילית"),
        Language("te", "טלוגו"),
    )

    /** לבורר "כל השפות" - בסדר האלפבית העברי. */
    val alphabetical: List<Language> = all.sortedBy { it.name }

    private val byCode = (all + auto).associateBy { it.code }

    fun of(code: String): Language = byCode[code] ?: all.first()

    fun isSupported(code: String): Boolean = code in byCode && code != AUTO

    /**
     * קוד שזיהוי השפה החזיר, בצורה שהתרגום מכיר: ML Kit מחזיר לעברית את
     * הקוד הישן "iw" ולאינדונזית "in", וגרסה בכתב לטיני ("hi-Latn") מתורגמת
     * כשפת הבסיס. null - שפה שאין לה מודל תרגום.
     */
    fun normalize(tag: String): String? {
        val base = tag.substringBefore('-').lowercase()
        val code = when (base) {
            "iw" -> "he"
            "in" -> "id"
            "nb", "nn" -> "no"
            "fil" -> "tl"
            else -> base
        }
        return code.takeIf { isSupported(it) }
    }
}
