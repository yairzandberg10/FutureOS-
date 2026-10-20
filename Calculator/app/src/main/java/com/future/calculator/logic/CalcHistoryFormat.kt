package com.future.calculator.logic

/**
 * פורמט שמירת ההיסטוריה בהעדפות - משותף למחשבון ולווידג'ט, כדי שלא יהיו שני עותקים
 * של המפתח, של המפריד ושל הפענוח. שורה לכל חישוב: ביטוי ותוצאה מופרדים ב-SEP.
 */
object CalcHistoryFormat {
    const val PREFS = "calculator"
    const val KEY = "history"
    const val SEP = "\u001F"
    const val MAX = 50

    fun parse(raw: String): List<CalcHistoryEntry> =
        raw.split('\n').filter { it.isNotEmpty() }.mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size == 2) CalcHistoryEntry(parts[0], parts[1]) else null
        }

    fun serialize(entries: List<CalcHistoryEntry>): String =
        entries.joinToString("\n") { it.expression + SEP + it.result }
}
