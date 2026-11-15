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
    class Entry(val phrasings: List<String>, val replies: List<String>)

    class Data(val entries: List<Entry>) {
        /** ניסוח -> השורה שלו, מהארוך לקצר. */
        internal val byPhrasing: List<Pair<String, Entry>> =
            entries.flatMap { e -> e.phrasings.map { it to e } }.sortedByDescending { it.first.length }
        val phrasingCount get() = byPhrasing.size
    }

    class Match(val phrasing: String, val reply: String, val exact: Boolean)

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
                if (phrasings.isEmpty() || replies.isEmpty()) null else Entry(phrasings, replies)
            }
            .toList()
    )

    private val STOP_WORDS = setOf(
        "אני", "אתה", "את", "אתם", "הוא", "היא", "אנחנו", "מה", "זה", "זאת", "לי", "לך", "לו", "לה", "של", "שלי",
        "שלך", "על", "עם", "יש", "אין", "לא", "כן", "גם", "רק", "עוד", "כל", "אם", "או", "אבל", "כי", "מי", "איך",
        "למה", "מתי", "איפה", "כמה", "הכי", "מאוד", "קצת", "ממש", "פה", "שם", "היום", "עכשיו", "אז", "טוב", "בסדר",
        "תגיד", "תגידי", "אותי", "אותך", "היה", "יהיה", "להיות", "ככה", "כזה", "כזאת", "עצמי", "די",
    )

    private fun words(s: String) = s.split(" ").filter { it.isNotBlank() }

    fun match(data: Data, rawText: String): Match? {
        val text = KnowledgeBase.normalize(rawText)
        if (text.isEmpty()) return null
        val padded = " $text "
        // 1. מדויקת
        data.byPhrasing.firstOrNull { (p, _) -> if (' ' in p) padded.contains(" $p ") else p == text }?.let { (p, e) ->
            return Match(p, e.replies.random(), exact = true)
        }
        // 2. חופשית - חפיפת מילים
        val textWords = words(text).toSet()
        var best: Pair<String, Entry>? = null
        var bestScore = 0.0
        for ((p, e) in data.byPhrasing) {
            val pw = words(p)
            if (pw.size < 2) continue
            val shared = pw.filter { it in textWords }
            if (shared.size < 2 || shared.all { it in STOP_WORDS }) continue
            val score = shared.size.toDouble() / pw.size
            if (score >= 0.6 && score > bestScore) {
                best = p to e
                bestScore = score
            }
        }
        return best?.let { (p, e) -> Match(p, e.replies.random(), exact = false) }
    }
}
