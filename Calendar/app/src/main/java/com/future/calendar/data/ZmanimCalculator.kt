package com.future.calendar.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class DayZmanim(
    val sunrise: LocalTime?,
    val sunset: LocalTime?,
    val chatzot: LocalTime?,
    val sofZmanShema: LocalTime?,
    val candleLighting: LocalTime?,
    val motzeiShabbat: LocalTime?
)

/**
 * חישוב זמני היום (זמנים הלכתיים) לפי מיקום נתון, בעזרת נוסחת השמש הסטנדרטית
 * של NOAA (ציבורית, ללא ספרייה חיצונית). דיוק של כדקה - מספיק לשימוש כללי,
 * אך לא תחליף למקור הלכתי מוסמך במקרים הדורשים דיוק לשנייה.
 */
object ZmanimCalculator {
    /** ירושלים - ברירת מחדל כשאין הרשאת מיקום או שירות מיקום זמין. */
    const val DEFAULT_LAT = 31.7683
    const val DEFAULT_LON = 35.2137

    fun calculate(date: LocalDate, lat: Double, lon: Double, zone: ZoneId = ZoneId.systemDefault()): DayZmanim {
        val sunrise = sunEvent(date, lat, lon, zone, rising = true)
        val sunset = sunEvent(date, lat, lon, zone, rising = false)

        val chatzot = if (sunrise != null && sunset != null) midpoint(sunrise, sunset) else null
        val sofZmanShema = if (sunrise != null && sunset != null) {
            val shaahZmanitMinutes = minutesBetween(sunrise, sunset) / 12.0
            sunrise.plusMinutes((shaahZmanitMinutes * 3).toLong())
        } else null

        val candleLighting = if (date.dayOfWeek == DayOfWeek.FRIDAY && sunset != null) sunset.minusMinutes(18) else null
        val motzeiShabbat = if (date.dayOfWeek == DayOfWeek.SATURDAY && sunset != null) sunset.plusMinutes(40) else null

        return DayZmanim(sunrise, sunset, chatzot, sofZmanShema, candleLighting, motzeiShabbat)
    }

    private fun midpoint(a: LocalTime, b: LocalTime): LocalTime {
        val minutesA = a.hour * 60 + a.minute
        val minutesB = b.hour * 60 + b.minute
        val mid = (minutesA + minutesB) / 2
        return LocalTime.of(mid / 60, mid % 60)
    }

    private fun minutesBetween(a: LocalTime, b: LocalTime): Double {
        return ((b.hour * 60 + b.minute) - (a.hour * 60 + a.minute)).toDouble()
    }

    /** נוסחת השמש הציבורית של NOAA לחישוב זריחה/שקיעה אסטרונומית. */
    private fun sunEvent(date: LocalDate, lat: Double, lon: Double, zone: ZoneId, rising: Boolean): LocalTime? {
        val jd = julianDay(date)
        val t = (jd - 2451545.0) / 36525.0

        val l0 = normalizeDegrees(280.46646 + t * (36000.76983 + t * 0.0003032))
        val m = 357.52911 + t * (35999.05029 - 0.0001537 * t)
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val mRad = Math.toRadians(m)
        val c = sin(mRad) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * mRad) * (0.019993 - 0.000101 * t) +
            sin(3 * mRad) * 0.000289
        val trueLong = l0 + c
        val omega = 125.04 - 1934.136 * t
        val apparentLong = trueLong - 0.00569 - 0.00478 * sin(Math.toRadians(omega))

        val e0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val obliqCorr = e0 + 0.00256 * cos(Math.toRadians(omega))

        val declRad = Math.asin(sin(Math.toRadians(obliqCorr)) * sin(Math.toRadians(apparentLong)))

        val y = tan(Math.toRadians(obliqCorr / 2.0)).let { it * it }
        val eqTimeMinutes = 4.0 * Math.toDegrees(
            y * sin(2 * Math.toRadians(l0)) -
                2 * e * sin(mRad) +
                4 * e * y * sin(mRad) * cos(2 * Math.toRadians(l0)) -
                0.5 * y * y * sin(4 * Math.toRadians(l0)) -
                1.25 * e * e * sin(2 * mRad)
        )

        val latRad = Math.toRadians(lat)
        val cosHourAngle = (cos(Math.toRadians(90.833)) / (cos(latRad) * cos(declRad))) - tan(latRad) * tan(declRad)
        if (cosHourAngle < -1.0 || cosHourAngle > 1.0) return null // שמש לא זורחת/שוקעת (קוטב)

        val hourAngleDeg = Math.toDegrees(acos(cosHourAngle))
        val solarNoonMinutesUtc = 720.0 - 4.0 * lon - eqTimeMinutes
        val eventMinutesUtc = if (rising) solarNoonMinutesUtc - 4.0 * hourAngleDeg else solarNoonMinutesUtc + 4.0 * hourAngleDeg

        val offsetHours = zone.rules.getOffset(date.atStartOfDay(zone).toInstant()).totalSeconds / 3600.0
        var localMinutes = eventMinutesUtc + offsetHours * 60.0
        localMinutes = ((localMinutes % 1440.0) + 1440.0) % 1440.0

        val hour = (localMinutes / 60.0).toInt().coerceIn(0, 23)
        val minute = (localMinutes % 60.0).toInt().coerceIn(0, 59)
        return LocalTime.of(hour, minute)
    }

    private fun julianDay(date: LocalDate): Double {
        var y = date.year
        var m = date.monthValue
        val d = date.dayOfMonth
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private fun normalizeDegrees(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }
}
