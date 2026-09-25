package com.future.remote.data

/**
 * שלטי מזגנים של החברות הגדולות בישראל (אלקטרה, תדיראן, טורנדו,
 * מיצובישי אלקטריק, פוג'יטסו, LG, Gree, Midea) והדגמים הנפוצים שלהן.
 *
 * שלט מזגן לא שולח "כפתור" - כל לחיצה שולחת את כל המצב (הפעלה, מצב,
 * טמפרטורה, מאוורר, תנודה) כחבילה אחת. לכן השלט באפליקציה מחזיק מצב
 * ([AcState]) ושולח אותו מחדש בכל שינוי, בדיוק כמו שלט אמיתי.
 *
 * הקידודים נכתבו מחדש ב-Kotlin לפי התיעוד הפומבי של IRremoteESP8266
 * (MIT) ושל IRTadiran - ולא נבדקו מול כל דגם. לכל מותג יש יותר מפרוטוקול
 * אחד, כי אותו מותג מוכר דגמים של כמה יצרנים (אלקטרה מ-2013 מייצרת אצל
 * Midea, תדיראן משתמשת בשלטי Gree, טורנדו עבדה עם Midea ועם Gree).
 */
enum class AcProtocol(val label: String) {
    ELECTRA("שלט YKR (קלאסי)"),
    MIDEA("שלט Midea (דגמים חדשים)"),
    TADIRAN("שלט תדיראן"),
    GREE("שלט Gree"),
    MITSUBISHI("שלט Mitsubishi Electric"),
    FUJITSU("שלט Fujitsu ARRAH2E"),
    LG("שלט LG"),
}

enum class AcCompany(val label: String, val protocols: List<AcProtocol>) {
    ELECTRA("אלקטרה", listOf(AcProtocol.ELECTRA, AcProtocol.MIDEA)),
    TADIRAN("תדיראן", listOf(AcProtocol.TADIRAN, AcProtocol.GREE)),
    TORNADO("טורנדו", listOf(AcProtocol.GREE, AcProtocol.MIDEA)),
    MITSUBISHI("מיצובישי אלקטריק", listOf(AcProtocol.MITSUBISHI)),
    FUJITSU("פוג'יטסו", listOf(AcProtocol.FUJITSU)),
    LG("LG", listOf(AcProtocol.LG)),
    GREE("Gree", listOf(AcProtocol.GREE)),
    MIDEA("Midea", listOf(AcProtocol.MIDEA)),
}

enum class AcMode(val label: String) { COOL("קירור"), HEAT("חימום"), FAN("מאוורר"), DRY("ייבוש"), AUTO("אוטומטי") }
enum class AcFan(val label: String) { AUTO("אוטו'"), LOW("נמוך"), MED("בינוני"), HIGH("גבוה") }

data class AcState(
    val power: Boolean = false,
    val mode: AcMode = AcMode.COOL,
    val temp: Int = 24,
    val fan: AcFan = AcFan.AUTO,
    val swing: Boolean = false,
)

object AcProtocols {
    const val CARRIER_HZ = 38000
    const val MIN_TEMP = 16
    const val MAX_TEMP = 30

    fun encode(protocol: AcProtocol, s: AcState): IntArray = when (protocol) {
        AcProtocol.ELECTRA -> ElectraEncoder.encode(
            power = s.power,
            mode = when (s.mode) {
                AcMode.COOL -> ElectraEncoder.Mode.COOL
                AcMode.HEAT -> ElectraEncoder.Mode.HEAT
                AcMode.FAN -> ElectraEncoder.Mode.FAN
                AcMode.DRY -> ElectraEncoder.Mode.DRY
                AcMode.AUTO -> ElectraEncoder.Mode.AUTO
            },
            fan = when (s.fan) {
                AcFan.AUTO -> ElectraEncoder.FanSpeed.AUTO
                AcFan.LOW -> ElectraEncoder.FanSpeed.LOW
                AcFan.MED -> ElectraEncoder.FanSpeed.MED
                AcFan.HIGH -> ElectraEncoder.FanSpeed.HIGH
            },
            tempCelsius = s.temp,
        )
        AcProtocol.MIDEA -> midea(s)
        AcProtocol.TADIRAN -> tadiran(s)
        AcProtocol.GREE -> gree(s)
        AcProtocol.MITSUBISHI -> mitsubishi(s)
        AcProtocol.FUJITSU -> fujitsu(s)
        AcProtocol.LG -> lg(s)
    }

    // ---- כלי עזר ----

    private class Pulses {
        val list = ArrayList<Int>()
        fun mark(us: Int) = list.add(us)
        fun space(us: Int) = list.add(us)
        /** בייטים, כל בייט מהסיבית הנמוכה (LSB first). */
        fun bytesLsb(bytes: IntArray, bitMark: Int, one: Int, zero: Int) {
            for (b in bytes) for (bit in 0 until 8) { mark(bitMark); space(if ((b shr bit) and 1 == 1) one else zero) }
        }
        fun toArray() = list.toIntArray()
    }

    private fun reverse8(v: Int): Int {
        var r = 0
        for (i in 0 until 8) if ((v shr i) and 1 == 1) r = r or (1 shl (7 - i))
        return r
    }

    // ---- Midea (48 סיביות, נשלח ואחריו הפוך) ----

    private fun midea(s: AcState): IntArray {
        val tick = 80
        val bitMark = 7 * tick; val one = 21 * tick; val zero = 7 * tick
        val hdr = 56 * tick; val gap = (56 + 7 + 7) * tick
        val mode = when (s.mode) { AcMode.COOL -> 0; AcMode.DRY -> 1; AcMode.AUTO -> 2; AcMode.HEAT -> 3; AcMode.FAN -> 4 }
        val fan = s.fan.ordinal // AUTO=0 LOW=1 MED=2 HIGH=3
        // byte5..byte0 (byte0 = checksum), נשלחים מהבייט הגבוה.
        val b = IntArray(6)
        b[5] = 0xA1
        b[4] = (mode and 7) or ((fan and 3) shl 3) or (if (s.power) 0x80 else 0)
        b[3] = 0x40 or ((s.temp.coerceIn(17, 30) - 17) and 0x1F)
        b[2] = 0xFF
        b[1] = 0xFF
        var sum = 0
        for (i in 1..5) sum += reverse8(b[i])
        b[0] = reverse8((256 - (sum and 0xFF)) and 0xFF)
        val p = Pulses()
        for (inverted in listOf(false, true)) {
            p.mark(hdr); p.space(hdr)
            for (i in 5 downTo 0) {
                val v = if (inverted) b[i].inv() and 0xFF else b[i]
                for (bit in 7 downTo 0) { p.mark(bitMark); p.space(if ((v shr bit) and 1 == 1) one else zero) }
            }
            p.mark(bitMark)
            if (!inverted) p.space(gap)
        }
        return p.toArray()
    }

    // ---- תדיראן (8 בייטים, פעמיים) ----

    private fun tadiran(s: AcState): IntArray {
        val code = IntArray(8)
        if (s.power) {
            val mode = when (s.mode) { AcMode.COOL, AcMode.AUTO -> 1; AcMode.HEAT -> 2; AcMode.FAN -> 3; AcMode.DRY -> 4 }
            val fan = s.fan.ordinal // 0..3
            code[0] = 0x01
            code[1] = ((1 + fan) shl 4) or mode
            code[2] = 2 * s.temp.coerceIn(MIN_TEMP, MAX_TEMP)
            code[5] = 0x30
            code[6] = if (s.swing) 0xC0 else 0
            var sum = 0
            for (i in 0 until 7) sum += code[i]
            val temp = code[2] / 2
            code[7] = (sum - (0xF * (3 + temp / 8) + ((code[1] shr 4) and 0xF) * 0xF + (if (s.swing) 0xB4 else 0))) and 0xFF
        } else {
            code[0] = 0x01; code[1] = 0x14; code[2] = 0x30; code[5] = 0xC0; code[7] = 0x15
        }
        val p = Pulses()
        repeat(2) { r ->
            p.mark(8000); p.space(4000)
            for (b in code) for (bit in 0 until 8) {
                val one = (b shr bit) and 1 == 1
                p.mark(if (one) 1618 else 545); p.space(if (one) 545 else 1618)
            }
            if (r == 0) { p.mark(1618); p.space(31000) }
        }
        p.mark(1618)
        return p.toArray()
    }

    // ---- Gree (8 בייטים בשני בלוקים) ----

    private fun gree(s: AcState): IntArray {
        val st = IntArray(8)
        val mode = when (s.mode) { AcMode.AUTO -> 0; AcMode.COOL -> 1; AcMode.DRY -> 2; AcMode.FAN -> 3; AcMode.HEAT -> 4 }
        val fan = if (s.mode == AcMode.DRY) 1 else s.fan.ordinal
        val temp = if (s.mode == AcMode.AUTO) 25 else s.temp.coerceIn(16, 30)
        st[0] = mode or (if (s.power) 0x08 else 0) or (fan shl 4) or (if (s.swing) 0x40 else 0)
        st[1] = (temp - 16) and 0xF
        st[2] = 0x20 or (if (s.power) 0x40 else 0) // Light, ModelA(YAW1F) כשדלוק
        st[3] = 0x50
        st[4] = if (s.swing) 0x01 else 0x00
        st[5] = 0x20
        st[6] = 0
        var sum = 10
        for (i in 0 until 4) sum += st[i] and 0xF
        for (i in 4 until 7) sum += st[i] shr 4
        st[7] = (sum and 0xF) shl 4
        val p = Pulses()
        p.mark(9000); p.space(4500)
        p.bytesLsb(st.copyOfRange(0, 4), 620, 1600, 540)
        // תחתית הבלוק הראשון: 0b010 (LSB first) ואז הפסקה.
        for (bit in listOf(0, 1, 0)) { p.mark(620); p.space(if (bit == 1) 1600 else 540) }
        p.mark(620); p.space(19980)
        p.bytesLsb(st.copyOfRange(4, 8), 620, 1600, 540)
        p.mark(620)
        return p.toArray()
    }

    // ---- Mitsubishi Electric (18 בייטים, נשלח פעמיים) ----

    private fun mitsubishi(s: AcState): IntArray {
        val st = IntArray(18)
        intArrayOf(0x23, 0xCB, 0x26, 0x01, 0x00).copyInto(st)
        st[5] = if (s.power) 0x20 else 0
        val mode = when (s.mode) { AcMode.AUTO -> 0b100; AcMode.COOL -> 0b011; AcMode.DRY -> 0b010; AcMode.HEAT -> 0b001; AcMode.FAN -> 0b111 }
        st[6] = mode shl 3
        st[7] = (s.temp.coerceIn(16, 31) - 16) and 0xF
        st[8] = when (s.mode) { AcMode.COOL -> 0x36; AcMode.DRY -> 0x32; AcMode.FAN -> 0x37; else -> 0x30 }
        val fan = when (s.fan) { AcFan.AUTO -> 0; AcFan.LOW -> 1; AcFan.MED -> 2; AcFan.HIGH -> 3 }
        val vane = if (s.swing) 0b111 else 0
        st[9] = fan or (vane shl 3) or (if (s.fan == AcFan.AUTO) 0x80 else 0)
        var sum = 0
        for (i in 0 until 17) sum += st[i]
        st[17] = sum and 0xFF
        val p = Pulses()
        repeat(2) { r ->
            p.mark(3400); p.space(1750)
            p.bytesLsb(st, 450, 1300, 420)
            p.mark(440)
            if (r == 0) p.space(15500)
        }
        return p.toArray()
    }

    // ---- Fujitsu ARRAH2E (16 בייטים; כיבוי = קוד קצר) ----

    private fun fujitsu(s: AcState): IntArray {
        val bytes = if (!s.power) {
            intArrayOf(0x14, 0x63, 0x00, 0x10, 0x10, 0x02, 0xFD)
        } else {
            val st = IntArray(16)
            intArrayOf(0x14, 0x63, 0x00, 0x10, 0x10, 0xFE, 0x09, 0x30).copyInto(st)
            st[8] = 0x01 or (((s.temp.coerceIn(16, 30) - 16) * 4) shl 2)
            val mode = when (s.mode) { AcMode.AUTO -> 0; AcMode.COOL -> 1; AcMode.DRY -> 2; AcMode.FAN -> 3; AcMode.HEAT -> 4 }
            st[9] = mode
            val fan = when (s.fan) { AcFan.AUTO -> 0; AcFan.HIGH -> 1; AcFan.MED -> 2; AcFan.LOW -> 3 }
            st[10] = fan or (if (s.swing) 0x10 else 0)
            st[14] = 0x20
            var sum = 0
            for (i in 7 until 15) sum += st[i]
            st[15] = (0 - sum) and 0xFF
            st
        }
        val p = Pulses()
        p.mark(3324); p.space(1574)
        p.bytesLsb(bytes, 448, 1182, 390)
        p.mark(448)
        return p.toArray()
    }

    // ---- LG (28 סיביות, מהסיבית הגבוהה) ----

    private fun lg(s: AcState): IntArray {
        val power = if (s.power) 0 else 3
        val mode = when (s.mode) { AcMode.COOL -> 0; AcMode.DRY -> 1; AcMode.FAN -> 2; AcMode.AUTO -> 3; AcMode.HEAT -> 4 }
        val fan = when (s.fan) { AcFan.LOW -> 0; AcFan.MED -> 2; AcFan.HIGH -> 4; AcFan.AUTO -> 5 }
        val temp = s.temp.coerceIn(16, 30) - 15
        // כיבוי הוא קוד קבוע (0x88C0051); הפעלה נושאת את כל המצב.
        var v = if (!s.power) 0x88C005 else
            (0x88 shl 16) or (power shl 14) or (mode shl 8) or (temp shl 4) or fan
        var sum = 0
        for (i in 0 until 6) sum += (v shr (i * 4)) and 0xF
        v = (v shl 4) or (sum and 0xF)
        val p = Pulses()
        p.mark(8500); p.space(4250)
        for (bit in 27 downTo 0) { p.mark(550); p.space(if ((v shr bit) and 1 == 1) 1600 else 550) }
        p.mark(550)
        return p.toArray()
    }
}
