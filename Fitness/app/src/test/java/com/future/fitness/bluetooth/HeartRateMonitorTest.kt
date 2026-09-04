package com.future.fitness.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * בדיקות פענוח BLE טהורות (בלי GATT/Bluetooth אמיתי) - כל פונקציה כאן היא
 * טרנספורמציה נתונים->נתונים לפי מפרט Bluetooth SIG, אז אפשר לבדוק אותה
 * ישירות עם בתים מוכנים-מראש בלי חיבור אמיתי לחיישן.
 */
class HeartRateMonitorTest {

    @Test
    fun `parseHeartRate reads UINT8 value when flag bit is clear`() {
        // flags=0x00 (UINT8), value=72
        val data = byteArrayOf(0x00, 72)
        assertEquals(72, HeartRateMonitor.parseHeartRate(data))
    }

    @Test
    fun `parseHeartRate reads UINT16 little-endian value when flag bit is set`() {
        // flags=0x01 (UINT16), value = 0x00F0 = 240, little-endian bytes: F0 00
        val data = byteArrayOf(0x01, 0xF0.toByte(), 0x00)
        assertEquals(240, HeartRateMonitor.parseHeartRate(data))
    }

    @Test
    fun `parseHeartRate returns null for empty or truncated data`() {
        assertNull(HeartRateMonitor.parseHeartRate(byteArrayOf()))
        assertNull(HeartRateMonitor.parseHeartRate(byteArrayOf(0x01, 0xF0.toByte()))) // missing high byte
    }

    @Test
    fun `parseRunningSpeedAndCadence reads speed and cadence without stride length`() {
        // flags=0x00 (no stride length), speed raw = 512 (1/256 m per s units) => 2.0 m/s => 7.2 km/h, cadence=170
        val speedRaw = 512
        val data = byteArrayOf(0x00, (speedRaw and 0xFF).toByte(), ((speedRaw shr 8) and 0xFF).toByte(), 170.toByte())
        val reading = HeartRateMonitor.parseRunningSpeedAndCadence(data)
        assertEquals(7.2, reading!!.speedKmh, 0.001)
        assertEquals(170, reading.cadenceSpm)
        assertNull(reading.strideLengthM)
    }

    @Test
    fun `parseRunningSpeedAndCadence reads stride length when flag bit is set`() {
        // flags=0x01 (stride length present), stride raw = 150 (1/100 m units) => 1.50 m
        val data = byteArrayOf(0x01, 0x00, 0x00, 160.toByte(), 150.toByte(), 0)
        val reading = HeartRateMonitor.parseRunningSpeedAndCadence(data)
        assertEquals(1.50, reading!!.strideLengthM!!, 0.001)
    }

    @Test
    fun `parseRunningSpeedAndCadence returns null for truncated data`() {
        assertNull(HeartRateMonitor.parseRunningSpeedAndCadence(byteArrayOf(0x00, 0x00, 0x00)))
    }

    @Test
    fun `parseCyclingPower reads instantaneous power without crank data`() {
        // flags=0x0000 (no optional fields), power=250 watts
        val data = byteArrayOf(0x00, 0x00, 250.toByte(), 0x00)
        val reading = HeartRateMonitor.parseCyclingPower(data)
        assertEquals(250, reading!!.watts)
        assertNull(reading.crank)
    }

    @Test
    fun `parseCyclingPower handles negative power correctly as signed value`() {
        // power = -10 watts as INT16 little-endian: 0xFFF6
        val data = byteArrayOf(0x00, 0x00, 0xF6.toByte(), 0xFF.toByte())
        val reading = HeartRateMonitor.parseCyclingPower(data)
        assertEquals(-10, reading!!.watts)
    }

    @Test
    fun `parseCyclingPower reads crank revolution data when flag bit 5 is set`() {
        // flags = 0x0020 (crank revolution data present), power=200, revolutions=1000, eventTime=5000
        val flags = 0x0020
        val revolutions = 1000
        val eventTime = 5000
        val data = byteArrayOf(
            (flags and 0xFF).toByte(), ((flags shr 8) and 0xFF).toByte(),
            200.toByte(), 0,
            (revolutions and 0xFF).toByte(), ((revolutions shr 8) and 0xFF).toByte(),
            (eventTime and 0xFF).toByte(), ((eventTime shr 8) and 0xFF).toByte(),
        )
        val reading = HeartRateMonitor.parseCyclingPower(data)
        assertEquals(200, reading!!.watts)
        assertEquals(CrankSample(1000, 5000), reading.crank)
    }

    @Test
    fun `cadenceRpmFromCrankSamples returns null for the first sample`() {
        assertNull(HeartRateMonitor.cadenceRpmFromCrankSamples(null, CrankSample(100, 1024)))
    }

    @Test
    fun `cadenceRpmFromCrankSamples computes rpm from revolution and time deltas`() {
        // 10 revolutions over 1024 (=1 second) time units => 10 rev/s => 600 rpm
        val previous = CrankSample(cumulativeRevolutions = 0, eventTime1024ths = 0)
        val current = CrankSample(cumulativeRevolutions = 10, eventTime1024ths = 1024)
        assertEquals(600, HeartRateMonitor.cadenceRpmFromCrankSamples(previous, current))
    }

    @Test
    fun `cadenceRpmFromCrankSamples handles UINT16 wraparound correctly`() {
        // revolutions wrap from 65530 to 5: forward delta = (65536-65530)+5 = 11
        // event time wraps from 65000 to 488: forward delta = (65536-65000)+488 = 1024 (=1s)
        val previous = CrankSample(cumulativeRevolutions = 65530, eventTime1024ths = 65000)
        val current = CrankSample(cumulativeRevolutions = 5, eventTime1024ths = 488)
        // 11 revolutions in 1 second => 660 rpm
        assertEquals(660, HeartRateMonitor.cadenceRpmFromCrankSamples(previous, current))
    }

    @Test
    fun `cadenceRpmFromCrankSamples returns null when no time has elapsed`() {
        val sample = CrankSample(100, 1024)
        assertNull(HeartRateMonitor.cadenceRpmFromCrankSamples(sample, sample))
    }
}
