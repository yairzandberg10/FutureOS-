package com.future.notes.data

/**
 * פתק כטקסט לשיתוף, ובחזרה. השיתוף עובר בחלון השיתוף של FutureUI (בדרך כלל
 * להודעות), ובטלפון FuturePhone השני "שמור בפתקים" בהודעה שולח את הטקסט
 * לכאן (ACTION_SEND) - השורה הראשונה מסמנת שזה פתק ונושאת את הכותרת,
 * ורשימה חוזרת להיות רשימה.
 */
object NoteShare {
    private const val MARKER = "פתק FutureOS: "

    fun format(note: Note): String {
        val body = if (note.isChecklist) Checklist.display(note.content) else note.content
        return MARKER + note.title.ifBlank { "ללא כותרת" } + "\n" + body
    }

    data class Parsed(val title: String, val content: String, val isChecklist: Boolean)

    fun parse(text: String, subject: String?): Parsed {
        var title = subject.orEmpty()
        var body = text.trim()
        if (body.startsWith(MARKER)) {
            title = body.substringBefore('\n').removePrefix(MARKER).trim()
            body = if ('\n' in body) body.substringAfter('\n') else ""
        }
        return if (Checklist.looksLikeChecklist(body)) {
            Parsed(title, Checklist.serialize(Checklist.parse(body)), true)
        } else {
            Parsed(title, body, false)
        }
    }
}
