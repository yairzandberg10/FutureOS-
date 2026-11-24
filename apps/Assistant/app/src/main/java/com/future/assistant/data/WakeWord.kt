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
        if (isName || isAssistant) return words.drop(i + 1).joinToString(" ")
        return fuzzy(words)
    }

    /**
     * "היי פיוטר" כשהתמלול שיבש אותו ("היף יוצא", "הייפ יוטר"): מחברים את שתי
     * המילים הראשונות ומשווים במרחק עריכה ל-"היפיוטר". רק בתחילת המשפט, ורק
     * לצירוף הארוך - מילה קצרה לבד דומה מדי למילים רגילות.
     */
    private fun fuzzy(words: List<String>): String? {
        for (k in 1..2) {
            if (words.size < k) continue
            val joined = words.take(k).joinToString("") { finals(it) }
            if (joined.length < 6) continue
            if (FUZZY_TARGETS.any { distance(joined, it) <= 2 }) return words.drop(k).joinToString(" ")
        }
        return null
    }

    private val FUZZY_TARGETS = listOf("היפיוטר", "הייפיוטר", "היפיוצר", "הייפיוצר")

    private fun finals(w: String) = w.replace('ף', 'פ').replace('ץ', 'צ').replace('ך', 'כ').replace('ם', 'מ').replace('ן', 'נ')

    private fun distance(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            cur.copyInto(prev)
        }
        return prev[b.length]
    }
}
