package com.future.navigation.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * הפורמט האמיתי של מדבקות ה-QR עוד לא ידוע (ר' BusQrParser). כשתגיע דוגמה
 * מסריקה אמיתית - להוסיף אותה כאן עם מספר הרכב שמודבק על האוטובוס.
 */
class BusQrParserTest {

    @Test
    fun `url with a vehicle parameter`() {
        assertEquals("7801501", BusQrParser.parse("https://example.co.il/ride?op=3&vehicle=7801501").vehicleNumber)
        assertEquals("12345678", BusQrParser.parse("https://x.il/q?V=123-456-78").vehicleNumber)
    }

    @Test
    fun `standalone plate number`() {
        assertEquals("7801501", BusQrParser.parse("BUS 7801501").vehicleNumber)
        assertEquals("12345678", BusQrParser.parse("  12345678 ").vehicleNumber)
    }

    @Test
    fun `a long numeric code is not guessed`() {
        assertNull(BusQrParser.parse("000312345678901234567890123").vehicleNumber)
    }

    @Test
    fun `two candidates are not guessed`() {
        assertNull(BusQrParser.parse("1234567 7654321").vehicleNumber)
    }

    @Test
    fun `raw value is kept trimmed`() {
        assertEquals("abc", BusQrParser.parse("  abc \n").raw)
        assertNull(BusQrParser.parse("abc").vehicleNumber)
    }
}
