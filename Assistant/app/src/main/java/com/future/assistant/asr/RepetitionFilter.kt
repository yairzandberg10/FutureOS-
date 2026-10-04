package com.future.assistant.asr

/**
 * Whisper נוטה "להיתקע" ולחזור על אותו משפט או אותה מילה עד סוף החלון
 * ("מה השעה מה השעה מה השעה"). הפרמטרים ב-whisper_jni.cpp מצמצמים את זה,
 * וכאן מנקים את מה שנשאר: רצף של אותו n-gram ברצף מקוצר למופע אחד.
 * מילה בודדת מקוצרת רק מ-3 חזרות ומעלה - "לא לא" יכול להיות דיבור אמיתי.
 */
object RepetitionFilter {
    fun collapse(text: String): String {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
        if (words.size < 2) return text.trim()
        var changed = true
        while (changed) {
            changed = false
            for (n in words.size / 2 downTo 1) {
                var i = 0
                while (i + 2 * n <= words.size) {
                    var reps = 1
                    while (i + (reps + 1) * n <= words.size && same(words, i, i + reps * n, n)) reps++
                    if (reps >= (if (n == 1) 3 else 2)) {
                        repeat((reps - 1) * n) { words.removeAt(i + n) }
                        changed = true
                    }
                    i++
                }
            }
        }
        return words.joinToString(" ")
    }

    private fun same(words: List<String>, a: Int, b: Int, n: Int): Boolean {
        for (k in 0 until n) if (norm(words[a + k]) != norm(words[b + k])) return false
        return true
    }

    private fun norm(w: String) = w.trim { it in " ,.!?;:-\"'" }
}
