package com.future.assistant.data

/**
 * מילת ההפעלה: "היי פיוטר", "פיוטר", "עוזר", "היי עוזר". בודק את תחילת המשפט
 * שתומלל בלבד, כי "עוזר" מופיע גם בדיבור רגיל ("זה עוזר לי").
 *
 * מחזיר null כשאין מילת הפעלה, ואחרת את מה שנאמר אחריה ("היי פיוטר מה השעה" ->
 * "מה השעה"; ריק כשנאמרה רק מילת ההפעלה).
 */
object WakeWord {
    /** איך Whisper כותב את "פיוטר" (Future). אחרי normalize, כלומר בלי גרש. */
    private val NAMES = setOf(
        "פיוטר", "פיוצר", "פיוטור", "פיוצור", "פיוטרר", "פיוטא", "פיוצא", "פיוטה", "פיוצה",
        "פיוטרס", "פיוצרס", "פיוטיר", "פיוצרר", "פיוצ", "פיוט", "future", "futur", "fewture",
    )
    /** רק אחרי "היי" - לבד הם שמות או מילים רגילות. */
    private val NAMES_AFTER_GREETING = setOf("פיטר", "פוטר", "פוצר", "פיור", "פיוש")
    private val ASSISTANT = setOf("עוזר", "עוזרת", "העוזר", "העוזרת")
    private val GREETINGS = setOf("היי", "הי", "האי", "היא", "הייי", "hey", "hi", "אהלן", "יא", "שלום")
    /** "עוזר לי", "עוזר מאוד" - דיבור רגיל, לא קריאה לעוזר. */
    private val NOT_A_CALL = setOf(
        "לי", "לך", "לו", "לה", "לנו", "לכם", "להם", "להן", "מאוד", "הרבה", "קצת", "בזה", "לזה", "את", "שם", "פה",
        "באמת", "ממש", "שלי", "שלך", "בבית", "בעבודה",
    )

    fun match(rawText: String): String? {
        val words = KnowledgeBase.normalize(rawText.lowercase()).split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        var i = 0
        val greeted = words[0] in GREETINGS
        if (greeted) i = 1
        val w = words.getOrNull(i) ?: return null
        val isName = w in NAMES || (greeted && w in NAMES_AFTER_GREETING) || (w.startsWith("פיו") && w.length in 4..7)
        val isAssistant = w in ASSISTANT && (greeted || words.getOrNull(i + 1) !in NOT_A_CALL)
        if (!isName && !isAssistant) return null
        return words.drop(i + 1).joinToString(" ")
    }
}
