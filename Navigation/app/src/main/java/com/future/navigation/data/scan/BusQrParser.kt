package com.future.navigation.data.scan

import java.net.URI

/**
 * מה שהצלחנו להבין מהברקוד שליד דלת האוטובוס.
 *
 * [vehicleNumber] - מספר הרכב (לוחית רישוי), אם נמצא. ב-SIRI של משרד התחבורה
 * זה ה-VehicleRef, ודרכו מזהים באיזה קו האוטובוס נוסע עכשיו. null אם לא
 * הצלחנו לחלץ - ואז המשתמש בוחר את הקו מרשימה.
 */
data class BusQr(val raw: String, val vehicleNumber: String?)

/**
 * פענוח תוכן הברקוד - **המקום היחיד שצריך לשנות** כשמתברר הפורמט האמיתי.
 *
 * הפורמט של המדבקות לא מתפרסם, והאתרים שמתארים אותו לא היו נגישים בזמן
 * הכתיבה. לפי תיאור משני (לא מאומת) הקוד מכיל מספר עשרוני ארוך שבתוכו מספר
 * הרכב. עד שתגיע דוגמה אמיתית מסריקה, הפענוח זהיר:
 *  1. קישור - פרמטר בשם שמרמז על רכב (vehicle/bus/license/plate/v).
 *  2. אחרת - רצף ספרות באורך של לוחית רישוי ישראלית (7 או 8) שעומד לבד.
 * ובכל מקרה אחר - null, והזרימה ממשיכה לבחירת קו ידנית. ניחוש שגוי לא מזיק:
 * הקו נבחר אוטומטית רק אם SIRI מאשר שהרכב הזה באמת נמצא עכשיו בקו בתחנה
 * הקרובה (ר' BusScanRepository.identify).
 *
 * כשתגיע דוגמה אמיתית: להוסיף אותה ל-BusQrParserTest (app/src/test) ולעדכן כאן.
 */
object BusQrParser {

    private val VEHICLE_PARAM_NAMES = listOf("vehicle", "vehicleid", "vehicle_id", "bus", "busid", "license", "licence", "plate", "v")
    private val STANDALONE_PLATE = Regex("(?<!\\d)(\\d{7,8})(?!\\d)")

    fun parse(raw: String): BusQr {
        val text = raw.trim()
        return BusQr(text, fromUrl(text) ?: standalonePlate(text))
    }

    private fun fromUrl(text: String): String? {
        if (!text.startsWith("http://", ignoreCase = true) && !text.startsWith("https://", ignoreCase = true)) return null
        val query = runCatching { URI(text).rawQuery }.getOrNull() ?: return null
        val params = query.split('&').mapNotNull { pair ->
            val i = pair.indexOf('=')
            if (i <= 0) null else pair.substring(0, i).lowercase() to pair.substring(i + 1)
        }
        return params.firstOrNull { it.first in VEHICLE_PARAM_NAMES }
            ?.second
            ?.filter { it.isDigit() }
            ?.takeIf { it.length in 5..8 }
    }

    private fun standalonePlate(text: String): String? {
        val matches = STANDALONE_PLATE.findAll(text).map { it.value }.toList()
        // יותר ממועמד אחד = אין דרך לדעת מי מהם הרכב. עדיף לשאול את המשתמש.
        return matches.singleOrNull()
    }
}
