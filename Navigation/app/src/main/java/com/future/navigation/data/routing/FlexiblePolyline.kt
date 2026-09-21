package com.future.navigation.data.routing

import com.future.navigation.data.common.LatLng

/**
 * מפענח ל-"Flexible Polyline" - הפורמט שבו HERE Routing API v8 מחזיר את
 * צורת המסלול (polyline). זהו פורמט שונה מ-Google Encoded Polyline (יש בו
 * header עם precision/third-dimension, לא רק דלתא מקודד).
 *
 * הלוגיקה הזו הופכת נאמנה למימוש הרשמי של HERE (MIT, heremaps/flexible-polyline,
 * java/src/com/here/flexpolyline/PolylineEncoderDecoder.java) - רק צד הפענוח
 * (decode), כי האפליקציה רק צריכה לקרוא polylines שחוזרים מהשרת, לא לקודד.
 * ה-third dimension (level/altitude/elevation) לא רלוונטי למסלולי נהיגה
 * ומדולג בפענוח כאן.
 */
object FlexiblePolyline {
    private const val FORMAT_VERSION = 1L

    private val ENCODING_TABLE =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()

    private val DECODING_TABLE = IntArray(256) { -1 }.also { table ->
        ENCODING_TABLE.forEachIndexed { value, char -> table[char.code] = value }
    }

    fun decode(encoded: String): List<LatLng> {
        if (encoded.isBlank()) return emptyList()

        val cursor = Cursor(encoded)
        val version = decodeUnsignedVarint(cursor)
        if (version != FORMAT_VERSION) return emptyList()

        val header = decodeUnsignedVarint(cursor)
        val precision = (header and 0x0F).toInt()
        // thirdDimension/thirdDimPrecision מפוענחים ב-header אבל לא נחוצים כאן
        // (רק lat/lng), ר' התיעוד למעלה.
        val thirdDimensionType = ((header shr 4) and 0x07)
        val hasThirdDimension = thirdDimensionType != 0L
        val thirdDimPrecision = ((header shr 7) and 0x0F).toInt()

        val latConverter = ValueConverter(precision)
        val lngConverter = ValueConverter(precision)
        val zConverter = ValueConverter(thirdDimPrecision)

        val points = mutableListOf<LatLng>()
        while (cursor.hasNext()) {
            val lat = latConverter.decodeNext(cursor)
            val lng = lngConverter.decodeNext(cursor)
            if (hasThirdDimension) zConverter.decodeNext(cursor)
            points += LatLng(lat = lat, lon = lng)
        }
        return points
    }

    private class Cursor(private val text: String) {
        var index = 0
        fun hasNext(): Boolean = index < text.length
        fun next(): Char = text[index++]
    }

    private class ValueConverter(precision: Int) {
        private val multiplier = Math.pow(10.0, precision.toDouble())
        private var lastValue = 0L

        fun decodeNext(cursor: Cursor): Double {
            var value = decodeUnsignedVarint(cursor)
            if ((value and 1L) != 0L) value = value.inv()
            value = value shr 1
            lastValue += value
            return lastValue / multiplier
        }
    }

    private fun decodeUnsignedVarint(cursor: Cursor): Long {
        var shift = 0
        var result = 0L
        while (cursor.hasNext()) {
            val char = cursor.next()
            val value = decodeChar(char)
            require(value >= 0) { "תו לא חוקי ב-flexible polyline: '$char'" }
            result = result or ((value.toLong() and 0x1F) shl shift)
            if ((value and 0x20) == 0) return result
            shift += 5
        }
        throw IllegalArgumentException("סוף מחרוזת לא צפוי ב-flexible polyline")
    }

    private fun decodeChar(char: Char): Int {
        if (char.code >= DECODING_TABLE.size) return -1
        return DECODING_TABLE[char.code]
    }
}
