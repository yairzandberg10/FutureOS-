package com.future.sfarim.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em

private val TAG_REGEX = Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>")
private val IMG_REGEX = Regex("<img[^>]*>", RegexOption.IGNORE_CASE)
private val INVISIBLE_REGEX = Regex("[\\u200b\\u200c\\u200e\\u200f\\u202a-\\u202e\\u2060-\\u2069\\ufeff\\u00ad]")
private val ENTITY_REGEX = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);")
private val WS_REGEX = Regex("[ \\t\\r\\n\\u00a0\\u2009]+")
// קודי עריכה שנשארו בטקסט המקורי (@11, @77...), סימן "ציור כאן" (#) וסוגריים ריקים
private val JUNK_REGEX = Regex("@\\d{1,3}|#|\\(\\s*\\)|\\[\\s*\\]")
private val SENTENCE_END = Regex("[.:;?!][\u05b0-\u05c7\u0591-\u05af]*[)\\]\"']* ")
private val SPACE_BEFORE_PUNCT = Regex(" +([,.;:!?])(?=\\s|$)")

private fun decodeEntities(s: String): String {
    if (!s.contains('&')) return s
    return ENTITY_REGEX.replace(s) { m ->
        when (val e = m.groupValues[1]) {
            "nbsp", "thinsp", "ensp", "emsp" -> " "
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            "rlm", "lrm", "zwj", "zwnj" -> ""
            else -> if (e.startsWith("#")) {
                val code = if (e.length > 1 && (e[1] == 'x' || e[1] == 'X')) e.drop(2).toIntOrNull(16) else e.drop(1).toIntOrNull()
                if (code != null && code in 1..0x10FFFF && code != 0x200f && code != 0x200e) String(Character.toChars(code)) else ""
            } else m.value
        }
    }
}

private fun cleanChunk(raw: String): String =
    JUNK_REGEX.replace(INVISIBLE_REGEX.replace(decodeEntities(raw), ""), "")

/**
 * ממיר את ה-HTML הקל של ספריא לטקסט מעוצב: כותרות (<b>/<strong>) מודגשות, <big> גדול ומודגש,
 * <small> קטן, <br> שורה חדשה, הערות שוליים קטנות בסוגריים, תגיות ריקות ומרקרים
 * (@11, #, תמונות) נעלמים, ורווחים כפולים מתכווצים.
 */
fun formatBookText(html: String): AnnotatedString {
    val src = IMG_REGEX.replace(html, "")
    return buildAnnotatedString {
        val names = ArrayList<String>()
        var footnoteDepth = 0
        var markerDepth = 0
        var pos = 0

        fun lastChar(): Char? = if (length == 0) null else this.toAnnotatedString().text.last()

        var sinceBreak = 0
        val newlineRef = object { var value: () -> Unit = {} }

        fun appendPiece(piece: String) {
            var t = piece
            val last = lastChar()
            if (t.startsWith(" ") && (last == null || last == ' ' || last == '\n' || last == '(')) t = t.drop(1)
            if (t.isNotEmpty()) { append(t); sinceBreak += t.length }
        }

        fun appendText(rawChunk: String) {
            if (rawChunk.isEmpty() || markerDepth > 0) return
            // \n אמיתי בטקסט המקורי = מעבר פסקה (לא רווח)
            val lines = cleanChunk(rawChunk).split('\n')
            lines.forEachIndexed { li, line ->
                if (li > 0) { newlineRef.value(); sinceBreak = 0 }
                val t = WS_REGEX.replace(line, " ")
                if (t.isEmpty() || t == " ") { if (t == " ") appendPiece(" "); return@forEachIndexed }
                // קטע ארוך בלי שום שבירה: פסקה חדשה בסוף משפט אחרי ~700 תווים
                if (sinceBreak + t.length > 900 && footnoteDepth == 0) {
                    var rest = t
                    while (sinceBreak + rest.length > 900) {
                        val from = (700 - sinceBreak).coerceAtLeast(0)
                        val m2 = SENTENCE_END.find(rest, from) ?: break
                        appendPiece(rest.substring(0, m2.range.last + 1))
                        newlineRef.value(); sinceBreak = 0
                        rest = rest.substring(m2.range.last + 1)
                    }
                    appendPiece(rest)
                } else appendPiece(t)
            }
        }

        fun newline() {
            val txt = this.toAnnotatedString().text
            if (txt.isEmpty()) return
            var trim = 0
            while (trim < txt.length && txt[txt.length - 1 - trim] == ' ') trim++
            val nl = run {
                var n = 0
                var i = txt.length - 1 - trim
                while (i >= 0 && txt[i] == '\n') { n++; i-- }
                n
            }
            if (nl >= 2) return
            append("\n")
        }

        newlineRef.value = { newline(); sinceBreak = 0 }

        for (m in TAG_REGEX.findAll(src)) {
            appendText(src.substring(pos, m.range.first))
            pos = m.range.last + 1
            val closing = m.groupValues[1] == "/"
            val name = m.groupValues[2].lowercase()
            val attrs = m.groupValues[3]
            if (name == "br") { newline(); continue }
            if (name == "p" || name == "div" || name == "li" || name == "tr") {
                if (closing || name != "li") newline() else newline()
                continue
            }
            if (!closing) {
                if (attrs.trimEnd().endsWith("/")) continue
                val isFootnote = name == "i" && attrs.contains("footnote") && !attrs.contains("footnote-marker")
                val isMarker = name == "sup" && attrs.contains("footnote-marker")
                // דיבור המתחיל/כותרת = הדגשה רק בתחילת פסקה. <b> באמצע משפט (מילה מצוטטת,
                // הדגשה של המו"ל) מוצג כטקסט רגיל - אחרת חצי עמוד מודגש סתם.
                val atParagraphStart = run {
                    val last = lastChar()
                    last == null || last == '\n'
                }
                val style: SpanStyle? = when {
                    isFootnote -> SpanStyle(fontSize = 0.68.em, fontStyle = FontStyle.Italic)
                    isMarker -> null
                    name == "b" || name == "strong" ->
                        if (atParagraphStart && footnoteDepth == 0) SpanStyle(fontWeight = FontWeight.Bold) else SpanStyle()
                    name == "big" -> SpanStyle(fontSize = 1.15.em, fontWeight = FontWeight.Bold)
                    name == "small" -> SpanStyle(fontSize = 0.82.em)
                    name == "i" || name == "em" -> SpanStyle(fontStyle = FontStyle.Italic)
                    name == "sup" -> SpanStyle(fontSize = 0.7.em, baselineShift = BaselineShift.Superscript)
                    name == "sub" -> SpanStyle(fontSize = 0.7.em, baselineShift = BaselineShift.Subscript)
                    else -> SpanStyle()
                }
                names.add(if (isFootnote) "footnote" else if (isMarker) "marker" else name)
                if (isMarker) markerDepth++
                if (isFootnote) {
                    footnoteDepth++
                    val last = lastChar()
                    if (last != null && last != ' ' && last != '\n') append(" ")
                }
                pushStyle(style ?: SpanStyle())
            } else {
                val idx = names.lastIndexOf(if (name == "i" && names.lastIndexOf("footnote") > names.lastIndexOf("i")) "footnote" else name)
                if (idx < 0) continue
                // סוגר גם תגיות שנשארו פתוחות בתוכו
                while (names.size > idx) {
                    val n = names.removeAt(names.size - 1)
                    if (n == "marker") markerDepth--
                    if (n == "footnote") footnoteDepth--
                    pop()
                }
            }
        }
        appendText(src.substring(pos))
        while (names.isNotEmpty()) { names.removeAt(names.size - 1); pop() }
    }.let { trimEnds(it) }
}

private fun trimEnds(s: AnnotatedString): AnnotatedString {
    var end = s.length
    while (end > 0 && (s.text[end - 1] == ' ' || s.text[end - 1] == '\n')) end--
    var start = 0
    while (start < end && (s.text[start] == ' ' || s.text[start] == '\n')) start++
    var out = if (start == 0 && end == s.length) s else s.subSequence(start, end)
    // רווח לפני סימן פיסוק (נשאר מתגיות שנמחקו)
    return out
}

/** טקסט נקי בלי עיצוב, עם מעברי שורה - לשיתוף. */
fun plainBookText(html: String): String =
    SPACE_BEFORE_PUNCT.replace(formatBookText(html).text, "$1")

/** טקסט נקי בשורה אחת - לתצוגה מקדימה וסניפטים של חיפוש. */
fun stripHtmlTags(text: String): String =
    plainBookText(text).replace(Regex("\\s*\\n\\s*"), " ").trim()
