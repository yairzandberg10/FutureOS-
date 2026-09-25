package com.future.messages.data

import android.content.Context

/**
 * השיחות שהועברו לארכיון. לספק ה-SMS אין עמודת ארכיון ציבורית, אז הרשימה
 * נשמרת כאן לפי thread_id. שיחה בארכיון נשארת שם גם כשמגיעה אליה הודעה
 * חדשה - היא יוצאת רק ב"הוצא מהארכיון".
 */
class ArchiveStore(context: Context) {
    private val prefs = context.getSharedPreferences("archive", Context.MODE_PRIVATE)

    fun archivedIds(): Set<Long> =
        prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    fun setArchived(threadId: Long, archived: Boolean): Set<Long> {
        val next = archivedIds().let { if (archived) it + threadId else it - threadId }
        prefs.edit().putStringSet(KEY, next.map { it.toString() }.toSet()).apply()
        return next
    }

    private companion object {
        const val KEY = "thread_ids"
    }
}
