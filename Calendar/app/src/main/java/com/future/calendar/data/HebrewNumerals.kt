package com.future.calendar.data

/** ממיר מספר לגימטריה - הצורה המקובלת לציון דף גמרא ("קע״ו" ולא "176"). */
object HebrewNumerals {
    private val HUNDREDS = listOf(400 to "ת", 300 to "ש", 200 to "ר", 100 to "ק")
    private val TENS = listOf(90 to "צ", 80 to "פ", 70 to "ע", 60 to "ס", 50 to "נ", 40 to "מ", 30 to "ל", 20 to "כ", 10 to "י")
    private val UNITS = listOf(9 to "ט", 8 to "ח", 7 to "ז", 6 to "ו", 5 to "ה", 4 to "ד", 3 to "ג", 2 to "ב", 1 to "א")

    fun toHebrew(number: Int): String {
        if (number <= 0) return number.toString()
        var n = number
        val letters = StringBuilder()

        for ((value, letter) in HUNDREDS) {
            while (n >= value) { letters.append(letter); n -= value }
        }

        // ט״ו (15) וט״ז (16) במקום י״ה/י״ו, כדי לא לאיית את שם ה'
        val remainder = n % 100
        if (remainder == 15) {
            letters.append("טו"); n -= 15
        } else if (remainder == 16) {
            letters.append("טז"); n -= 16
        }

        for ((value, letter) in TENS) {
            while (n >= value) { letters.append(letter); n -= value }
        }
        for ((value, letter) in UNITS) {
            while (n >= value) { letters.append(letter); n -= value }
        }

        val s = letters.toString()
        return when (s.length) {
            0 -> number.toString()
            1 -> "$s׳"
            else -> s.dropLast(1) + "״" + s.last()
        }
    }
}
