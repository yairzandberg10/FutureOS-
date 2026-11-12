package com.future.translate.data

import com.future.sharednav.i18n.tr

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
    val auto = Language(AUTO, tr("זיהוי שפה"), rtl = true)

    val all: List<Language> = listOf(
        Language("he", tr("עברית"), rtl = true),
        Language("en", tr("אנגלית")),
        Language("ar", tr("ערבית"), rtl = true),
        Language("ru", tr("רוסית")),
        Language("fr", tr("צרפתית")),
        Language("es", tr("ספרדית")),
        Language("de", tr("גרמנית")),
        Language("it", tr("איטלקית")),
        Language("pt", tr("פורטוגזית")),
        Language("uk", tr("אוקראינית")),
        Language("tr", tr("טורקית")),
        Language("fa", tr("פרסית"), rtl = true),
        Language("zh", tr("סינית")),
        Language("ja", tr("יפנית")),
        Language("ko", tr("קוריאנית")),
        Language("hi", tr("הינדי")),
        Language("ur", tr("אורדו"), rtl = true),
        Language("pl", tr("פולנית")),
        Language("ro", tr("רומנית")),
        Language("hu", tr("הונגרית")),
        Language("cs", tr("צ'כית")),
        Language("sk", tr("סלובקית")),
        Language("sl", tr("סלובנית")),
        Language("hr", tr("קרואטית")),
        Language("bg", tr("בולגרית")),
        Language("mk", tr("מקדונית")),
        Language("sq", tr("אלבנית")),
        Language("el", tr("יוונית")),
        Language("nl", tr("הולנדית")),
        Language("sv", tr("שוודית")),
        Language("no", tr("נורווגית")),
        Language("da", tr("דנית")),
        Language("fi", tr("פינית")),
        Language("is", tr("איסלנדית")),
        Language("et", tr("אסטונית")),
        Language("lv", tr("לטבית")),
        Language("lt", tr("ליטאית")),
        Language("be", tr("בלארוסית")),
        Language("ka", tr("גאורגית")),
        Language("ga", tr("אירית")),
        Language("cy", tr("וולשית")),
        Language("mt", tr("מלטית")),
        Language("ca", tr("קטלאנית")),
        Language("gl", tr("גליסית")),
        Language("eo", tr("אספרנטו")),
        Language("af", tr("אפריקאנס")),
        Language("sw", tr("סווהילי")),
        Language("ht", tr("קריאולית האיטית")),
        Language("id", tr("אינדונזית")),
        Language("ms", tr("מלאית")),
        Language("tl", tr("טגלוג")),
        Language("vi", tr("וייטנאמית")),
        Language("th", tr("תאית")),
        Language("bn", tr("בנגלית")),
        Language("gu", tr("גוג'ראטית")),
        Language("kn", tr("קנאדה")),
        Language("mr", tr("מראטהית")),
        Language("ta", tr("טמילית")),
        Language("te", tr("טלוגו")),
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
