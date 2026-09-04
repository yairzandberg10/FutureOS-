package com.future.remote.data

/**
 * קידוד פרוטוקול NEC סטנדרטי - הפרוטוקול הנפוץ ביותר במזגנים/מאווררים/
 * מערכות שמע זולות. מאפשר להזין כתובת+פקודה (הקודים שהיצרן מפרסם) בלי
 * לדעת את תבנית ה-IR הגולמית. מבוסס על תיאור הפרוטוקול הסטנדרטי:
 * פתיח 9000us הדלקה + 4500us כיבוי, 32 סיביות (כתובת, שלילת-כתובת, פקודה,
 * שלילת-פקודה) LSB-first כאשר '0' = 560us הדלקה+560us כיבוי ו-'1' = 560us
 * הדלקה+1690us כיבוי, ולבסוף פולס סגירה של 560us הדלקה.
 */
object NecEncoder {
    const val CARRIER_FREQUENCY_HZ = 38000

    fun encode(address: Int, command: Int): IntArray {
        val addr = address and 0xFF
        val addrInv = addr.inv() and 0xFF
        val cmd = command and 0xFF
        val cmdInv = cmd.inv() and 0xFF

        val pattern = ArrayList<Int>()
        pattern.add(9000)
        pattern.add(4500)

        fun appendByte(byte: Int) {
            for (bit in 0 until 8) {
                val isOne = (byte shr bit) and 1 == 1
                pattern.add(560)
                pattern.add(if (isOne) 1690 else 560)
            }
        }

        appendByte(addr)
        appendByte(addrInv)
        appendByte(cmd)
        appendByte(cmdInv)
        pattern.add(560)

        return pattern.toIntArray()
    }
}
