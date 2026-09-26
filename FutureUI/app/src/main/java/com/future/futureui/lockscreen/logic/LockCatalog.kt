package com.future.futureui.lockscreen.logic

/** האפשרויות של כל פריט הניתן להתאמה אישית, עם השמות בעברית. */
object LockCatalog {
    val clockStyles = listOf("קלאסי", "דק", "מוערם", "אנלוגי", "צד")

    /** 0 = צבע ההדגשה של המערכת, השאר FutureAccents.presets לפי הסדר. */
    val clockColors = listOf("הדגשה", "לבן", "תכלת", "כתום", "ירוק", "סגול")

    val backgrounds = listOf("טפט", "טפט מטושטש", "שחור", "צבע הדגשה")

    val widgets = listOf("none", "battery", "alarm", "hebdate", "media", "notifications")
    fun widgetLabel(id: String) = when (id) {
        "battery" -> "סוללה"
        "alarm" -> "שעון מעורר"
        "hebdate" -> "תאריך עברי"
        "media" -> "מוזיקה"
        "notifications" -> "מונה התראות"
        else -> "ללא"
    }

    val shortcuts = listOf("none", "flashlight", "camera", "phone", "messages", "notes", "recorder", "calculator", "music")
    fun shortcutLabel(id: String) = when (id) {
        "flashlight" -> "פנס"
        "camera" -> "מצלמה"
        "phone" -> "טלפון"
        "messages" -> "הודעות"
        "notes" -> "פתקים"
        "recorder" -> "מקליט"
        "calculator" -> "מחשבון"
        "music" -> "מוזיקה"
        else -> "ללא"
    }

    fun shortcutPackage(id: String): String? = when (id) {
        "camera" -> "com.future.camera"
        "phone" -> "com.future.dialer"
        "messages" -> "com.future.messages"
        "notes" -> "com.future.notes"
        "recorder" -> "com.future.recorder"
        "calculator" -> "com.future.calculator"
        "music" -> "com.future.music"
        else -> null
    }

    fun <T> cycle(list: List<T>, current: T, delta: Int): T {
        val i = list.indexOf(current).coerceAtLeast(0)
        return list[(i + delta + list.size) % list.size]
    }

    fun cycleIndex(size: Int, current: Int, delta: Int): Int = ((current + delta) % size + size) % size
}
