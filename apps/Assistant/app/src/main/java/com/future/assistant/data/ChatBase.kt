package com.future.assistant.data

import android.content.Context

/**
 * שיחה חופשית - תשובות מוכנות למשפטים של שיחה רגילה ("אני עצוב היום",
 * "מה אתה אוהב לעשות", "ספר לי עוד"), מתוך assets/knowledge/chat.tsv.
 * בלי אינטרנט ובלי מודל: כל שורה היא ניסוחים|מופרדים<TAB>תשובה|תשובה, ונבחרת
 * תשובה אקראית מהשורה.
 *
 * התאמה בשני שלבים:
 *  1. מדויקת - הניסוח הארוך ביותר שמופיע במשפט כמילים שלמות (ניסוח של מילה
 *     אחת רק כשהוא כל המשפט, כדי ש"טוב" לא יתפוס כל משפט עם "טוב").
 *  2. חופשית - חפיפת מילים: רוב המילים של הניסוח מופיעות במשפט, לפחות שתיים,
 *     ולפחות אחת מהן לא מילת קישור.
 */
object ChatBase {
    /** followUps - תשובות המשך לאותו נושא, כשהמשתמש אומר אחר כך "למה" / "ספר לי עוד". */
    class Entry(val phrasings: List<String>, val replies: List<String>, val followUps: List<String> = emptyList())

    class Data(val entries: List<Entry>) {
        /** ניסוח -> השורה שלו, מהארוך לקצר. */
        internal val byPhrasing: List<Pair<String, Entry>> =
            entries.flatMap { e -> e.phrasings.map { it to e } }.sortedByDescending { it.first.length }
        val phrasingCount get() = byPhrasing.size

        /** לחפיפה החופשית: המילים של כל ניסוח רב-מילי, מחושבות פעם אחת. */
        internal val multi: List<Pair<String, Entry>> = byPhrasing.filter { ' ' in it.first }
        internal val multiWords: List<List<String>> = multi.map { words(it.first) }
        internal val multiForms: List<List<Set<String>>> = multiWords.map { ws -> ws.map { forms(it) } }
        /** צורת מילה -> הניסוחים שמכילים אותה, כדי לא לעבור על כל ~35 אלף הניסוחים בכל שאלה. */
        internal val byForm: Map<String, IntArray> = run {
            val m = HashMap<String, MutableList<Int>>()
            multiForms.forEachIndexed { i, fs -> fs.flatten().toSet().forEach { m.getOrPut(it) { ArrayList() }.add(i) } }
            m.mapValues { it.value.toIntArray() }
        }
    }

    class Match(val phrasing: String, val reply: String, val exact: Boolean, val entry: Entry? = null)

    @Volatile private var loaded: Data? = null

    fun preload(context: Context) {
        runCatching { data(context) }
    }

    fun match(context: Context, text: String): Match? = runCatching { match(data(context), text) }.getOrNull()

    private fun data(context: Context): Data =
        loaded ?: synchronized(this) {
            loaded ?: load(context.assets.open("knowledge/chat.tsv").bufferedReader().use { it.readText() })
                .also { loaded = it }
        }

    fun load(tsv: String): Data = Data(
        tsv.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size < 2) return@mapNotNull null
                val phrasings = parts[0].split("|").map { KnowledgeBase.normalize(it) }.filter { it.isNotEmpty() }
                val replies = parts[1].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                val followUps = parts.getOrNull(2)?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                if (phrasings.isEmpty() || replies.isEmpty()) null else Entry(phrasings, replies, followUps)
            }
            .toList()
    )

    private val STOP_WORDS = setOf(
        "אני", "אתה", "את", "אתם", "הוא", "היא", "אנחנו", "מה", "זה", "זאת", "לי", "לך", "לו", "לה", "של", "שלי",
        "שלך", "על", "עם", "יש", "אין", "לא", "כן", "גם", "רק", "עוד", "כל", "אם", "או", "אבל", "כי", "מי", "איך",
        "למה", "מתי", "איפה", "כמה", "הכי", "מאוד", "קצת", "ממש", "פה", "שם", "היום", "עכשיו", "אז", "טוב", "בסדר",
        "תגיד", "תגידי", "אותי", "אותך", "היה", "יהיה", "להיות", "ככה", "כזה", "כזאת", "עצמי", "די",
    )

    internal fun words(s: String) = s.split(" ").filter { it.isNotBlank() }

    /** "והחתול" / "בבית" / "שהלכתי" -> גם בלי אותיות השימוש בהתחלה, כדי שהחפיפה תתפוס נטיות. */
    internal fun forms(w: String): Set<String> {
        val out = mutableSetOf(w)
        var x = w
        while (x.length > 3 && x[0] in "והבלשמכ") {
            x = x.substring(1)
            out += x
        }
        return out
    }

    fun match(data: Data, rawText: String): Match? {
        val text = KnowledgeBase.normalize(rawText)
        if (text.isEmpty()) return null
        val padded = " $text "
        // 1. מדויקת
        data.byPhrasing.firstOrNull { (p, _) -> if (' ' in p) padded.contains(" $p ") else p == text }?.let { (p, e) ->
            return Match(p, e.replies.random(), exact = true, entry = e)
        }
        // 2. חופשית - חפיפת מילים
        val textWords = words(text).flatMap { forms(it) }.toSet()
        val candidates = java.util.TreeSet<Int>()
        for (w in textWords) data.byForm[w]?.forEach { candidates.add(it) }
        var best: Pair<String, Entry>? = null
        var bestScore = 0.0
        // לפי הסדר של byPhrasing (מהארוך לקצר), כמו קודם - בתיקו הניסוח הארוך מנצח
        for (i in candidates) {
            val pw = data.multiWords[i]
            val pf = data.multiForms[i]
            val shared = pw.filterIndexed { k, _ -> pf[k].any { it in textWords } }
            if (shared.size < 2 || shared.all { it in STOP_WORDS }) continue
            val score = shared.size.toDouble() / pw.size
            if (score >= 0.6 && score > bestScore) {
                best = data.multi[i]
                bestScore = score
            }
        }
        return best?.let { (p, e) -> Match(p, e.replies.random(), exact = false, entry = e) }
    }

    // ---------------------------------------------------------------------
    // המשך שיחה: "למה", "ספר לי עוד", ומשפטים שלא הבנו באמצע שיחה
    // ---------------------------------------------------------------------

    private val FOLLOW_UPS = setOf(
        "למה", "למה ככה", "למה זה", "ספר לי עוד", "תספר לי עוד", "תספרי לי עוד", "ספרי לי עוד", "ספר עוד", "תספר עוד",
        "עוד", "ועוד", "מה עוד", "ומה עוד", "תמשיך", "תמשיכי", "המשך", "באמת", "ברצינות", "כמו מה", "למשל", "תן דוגמה",
        "תני דוגמה", "איך", "איך זה", "תסביר", "תסבירי", "תרחיב", "תרחיבי", "מה זאת אומרת", "מה הכוונה", "ואז",
        "ואז מה", "נו", "וואו באמת", "רציני", "על אמת",
    )

    fun isFollowUp(rawText: String): Boolean = KnowledgeBase.normalize(rawText) in FOLLOW_UPS

    /** תשובות כשהמשפט לא מוכר אבל אנחנו באמצע שיחה - ממשיכים לדבר במקום "לא הבנתי". */
    private val KEEP_GOING = listOf(
        "מעניין. ספר לי קצת יותר על זה?",
        "אני מקשיב. מה עוד קורה?",
        "וואו, לא חשבתי על זה ככה. איך הגעת לזה?",
        "הבנתי. ומה אתה מרגיש לגבי זה?",
        "אני פה איתך. רוצה להמשיך לספר?",
        "נשמע שיש פה סיפור. מה קרה אחר כך?",
        "את זה עוד לא שמעתי. תסביר לי?",
        "יפה. ומה דעתך על זה?",
        "אני לא בטוח שהבנתי עד הסוף. אפשר לנסח אחרת?",
        "אוקיי, אני איתך. על מה עוד בא לך לדבר?",
    )

    fun keepGoingReply(): String = KEEP_GOING.random()
}
