package com.future.notes.data

/**
 * פריטי פתק-רשימה, שמורים כטקסט רגיל ב-content: "[x] " לפריט מסומן,
 * "[ ] " לפריט פתוח, שורה לכל פריט. כך החיפוש ממשיך לעבוד על התוכן,
 * ופתק שמשותף כטקסט נשאר קריא.
 */
data class ChecklistItem(val text: String, val checked: Boolean)

object Checklist {
    fun parse(content: String): List<ChecklistItem> = content.lines()
        .filter { it.isNotBlank() }
        .map { line ->
            when {
                line.startsWith("[x] ", ignoreCase = true) -> ChecklistItem(line.drop(4), true)
                line.startsWith("[ ] ") -> ChecklistItem(line.drop(4), false)
                line.startsWith("☑ ") -> ChecklistItem(line.drop(2), true)
                line.startsWith("☐ ") -> ChecklistItem(line.drop(2), false)
                else -> ChecklistItem(line, false)
            }
        }

    fun serialize(items: List<ChecklistItem>): String =
        items.joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }

    /** תצוגה קריאה (לרשימה ולשיתוף) - ☑/☐ במקום הסימון הפנימי. */
    fun display(content: String): String =
        parse(content).joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }

    /** טקסט משותף שנראה כמו רשימה (☐/☑ או [ ]/[x] בכל שורה). */
    fun looksLikeChecklist(text: String): Boolean {
        val lines = text.lines().filter { it.isNotBlank() }
        return lines.isNotEmpty() && lines.all {
            it.startsWith("☐ ") || it.startsWith("☑ ") || it.startsWith("[ ] ") || it.startsWith("[x] ", ignoreCase = true)
        }
    }
}
