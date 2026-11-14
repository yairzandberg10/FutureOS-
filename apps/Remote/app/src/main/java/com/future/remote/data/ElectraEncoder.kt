package com.future.remote.data

/**
 * קידוד פרוטוקול Electra (מזגנים) - בניגוד לשלט טלוויזיה, שלט מזגן שולח
 * בכל לחיצה את *כל* מצב המזגן (הפעלה, מצב, טמפרטורה, מאוורר) כחבילה אחת
 * של 13 בייטים, לא פקודת "כפתור בודד". אלקטרה היא יצרנית ישראלית שהפרוטוקול
 * שלה משותף/מורשה גם למותגים כמו AUX, Frigidaire, Centek, AEG, Electrolux,
 * Delonghi ועוד - כך שיש סיכוי טוב שזה יעבוד גם על מזגנים לא-אלקטרה נפוצים
 * בישראל. מבוסס על התיעוד הפומבי של פרויקט הקוד הפתוח IRremoteESP8266
 * (github.com/crankyoldgit/IRremoteESP8266, MIT license) - היישום כאן נכתב
 * מחדש ב-Kotlin לפי המפרט, לא הועתק מהקוד המקורי.
 *
 * חשוב: לא הייתה לנו גישה למזגן אלקטרה אמיתי לבדוק מולו, אז יכול להיות
 * שיידרשו כיוונונים קטנים. אם כפתור לא עובד, הכי סביר שהבעיה בטיימינג
 * העדין (646/547/1647 מיקרו-שניות) - שווה לנסות מקרוב מאוד למקלט של המזגן.
 */
object ElectraEncoder {
    const val CARRIER_FREQUENCY_HZ = 38000

    private const val HEADER_MARK = 9166
    private const val HEADER_SPACE = 4470
    private const val BIT_MARK = 646
    private const val ONE_SPACE = 1647
    private const val ZERO_SPACE = 547
    private const val STATE_LENGTH = 13
    private const val TEMP_DELTA = 8
    private const val MIN_TEMP = 16
    private const val MAX_TEMP = 32

    enum class Mode(val code: Int) { AUTO(0b000), COOL(0b001), DRY(0b010), HEAT(0b100), FAN(0b110) }
    enum class FanSpeed(val code: Int) { AUTO(0b101), LOW(0b011), MED(0b010), HIGH(0b001) }

    private const val SWING_OFF = 0b111

    fun encode(power: Boolean, mode: Mode, fan: FanSpeed, tempCelsius: Int): IntArray {
        val temp = (tempCelsius.coerceIn(MIN_TEMP, MAX_TEMP) - TEMP_DELTA) and 0x1F
        val state = IntArray(STATE_LENGTH)

        state[0] = 0xC3
        state[1] = (SWING_OFF and 0x7) or (temp shl 3)
        state[2] = (SWING_OFF and 0x7) shl 5
        state[4] = (fan.code and 0x7) shl 5
        state[6] = (mode.code and 0x7) shl 5
        state[9] = if (power) (1 shl 5) else 0

        var checksum = 0
        for (i in 0 until STATE_LENGTH - 1) checksum += state[i]
        state[STATE_LENGTH - 1] = checksum and 0xFF

        val pattern = ArrayList<Int>()
        pattern.add(HEADER_MARK)
        pattern.add(HEADER_SPACE)
        for (byte in state) {
            for (bit in 0 until 8) {
                val isOne = (byte shr bit) and 1 == 1
                pattern.add(BIT_MARK)
                pattern.add(if (isOne) ONE_SPACE else ZERO_SPACE)
            }
        }
        pattern.add(BIT_MARK)
        return pattern.toIntArray()
    }
}
