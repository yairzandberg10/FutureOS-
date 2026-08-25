package com.future.navigation.data.gtfs

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * פירוק CSV מינימלי אך תקין ל-GTFS: תומך בשדות מצוטטים עם פסיקים/מרכאות
 * כפולות פנימיים (כנדרש בתקן RFC 4180 ש-GTFS מבוסס עליו). קריאה היא לפי
 * *שם* עמודה מתוך שורת הכותרת, לא לפי מיקום קבוע - כך שסדר עמודות שונה או
 * עמודות נוספות בקובץ הספציפי הזה לא שוברות את הפירוק.
 */
class GtfsCsvReader(inputStream: InputStream) {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
    private val columnIndex: Map<String, Int>

    init {
        val headerLine = reader.readLine() ?: ""
        val headers = parseCsvLine(headerLine)
        columnIndex = headers.mapIndexed { index, name -> name.trim() to index }.toMap()
    }

    fun hasColumn(name: String): Boolean = columnIndex.containsKey(name)

    /**
     * מריץ callback לכל שורה, כ-Map משם עמודה לערך (ריק אם השורה קצרה מהצפוי).
     * suspend (לא inline) כי ה-callback בפועל קורא לפונקציות suspend של Room
     * (insertStops וכו') בתוך GtfsImporter.
     */
    suspend fun forEachRow(action: suspend (row: Map<String, String>) -> Unit) {
        var line = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                val fields = parseCsvLine(line)
                val row = HashMap<String, String>(columnIndex.size)
                for ((name, idx) in columnIndex) {
                    row[name] = fields.getOrElse(idx) { "" }
                }
                action(row)
            }
            line = reader.readLine()
        }
    }

    fun close() = reader.close()
}

fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields.add(current.toString())
    return fields
}

/** "HH:MM:SS" -> שניות מחצות. HH יכול לעבור 24 בנסיעות שחוצות חצות - זה תקין ב-GTFS. */
fun parseGtfsTimeToSeconds(time: String): Int? {
    val parts = time.trim().split(":")
    if (parts.size != 3) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val s = parts[2].toIntOrNull() ?: return null
    return h * 3600 + m * 60 + s
}
