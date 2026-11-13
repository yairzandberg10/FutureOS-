package com.future.calendar.data

import com.future.sharednav.i18n.tr

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class DailyWeather(val date: LocalDate, val maxTemp: Double, val minTemp: Double, val weatherCode: Int, val currentTemp: Double? = null) {
    /** אייקון מזג האוויר - אייקון של המערכת (Rounded), לא אימוג'י: "No emoji. Anywhere." */
    val icon: androidx.compose.ui.graphics.vector.ImageVector get() = weatherIcon(weatherCode)
    val description: String get() = weatherDescription(weatherCode)
}

private fun weatherIcon(code: Int): androidx.compose.ui.graphics.vector.ImageVector = when (code) {
    0 -> com.future.sharednav.icons.FutureIcons.WbSunny
    1, 2 -> com.future.sharednav.icons.FutureIcons.WbCloudy
    3 -> com.future.sharednav.icons.FutureIcons.Cloud
    45, 48 -> com.future.sharednav.icons.FutureIcons.Dehaze
    51, 53, 55, 56, 57 -> com.future.sharednav.icons.FutureIcons.Grain
    61, 63, 65, 66, 67, 80, 81, 82 -> com.future.sharednav.icons.FutureIcons.Umbrella
    71, 73, 75, 77, 85, 86 -> com.future.sharednav.icons.FutureIcons.AcUnit
    95, 96, 99 -> com.future.sharednav.icons.FutureIcons.FlashOn
    else -> com.future.sharednav.icons.FutureIcons.Thermostat
}

private fun weatherDescription(code: Int): String = when (code) {
    0 -> tr("בהיר")
    1, 2 -> tr("מעונן חלקית")
    3 -> tr("מעונן")
    45, 48 -> tr("ערפל")
    51, 53, 55, 56, 57 -> tr("טפטוף")
    61, 63, 65, 66, 67 -> tr("גשם")
    71, 73, 75, 77 -> tr("שלג")
    80, 81, 82 -> tr("ממטרים")
    85, 86 -> tr("ממטרי שלג")
    95, 96, 99 -> tr("סופת רעמים")
    else -> tr("לא ידוע")
}

/** תחזית מזג אוויר אמיתית ל-16 יום קדימה, דרך Open-Meteo - שירות ציבורי ללא צורך במפתח API. */
object WeatherService {
    private fun coarse(v: Double): String = String.format(java.util.Locale.US, "%.2f", v)

    /** מפה מתאריך לתחזית אותו יום - מאפשר לתאם לתאריך שמוצג במסך היום, לא רק "עכשיו". */
    fun fetchForecast(lat: Double, lon: Double): Map<LocalDate, DailyWeather>? {
        return try {
            val url = URL(
                // מעוגל לשתי ספרות (~1 ק"מ): לתחזית זה לא משנה, ושירות חיצוני לא מקבל
                // את המיקום המדויק של המשתמש (הבית, העבודה) בכל רענון.
                "https://api.open-meteo.com/v1/forecast?latitude=${coarse(lat)}&longitude=${coarse(lon)}" +
                    "&current_weather=true&daily=weathercode,temperature_2m_max,temperature_2m_min" +
                    "&forecast_days=16&timezone=auto"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"

            // החיבור נסגר תמיד, גם כשקריאת הגוף זורקת חריגה
            val body = try {
                if (connection.responseCode != 200) return null
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

            val json = JSONObject(body)
            val daily = json.getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val codes = daily.getJSONArray("weathercode")
            val maxes = daily.getJSONArray("temperature_2m_max")
            val mins = daily.getJSONArray("temperature_2m_min")

            val current = json.optJSONObject("current_weather")
            val currentDate = current?.optString("time")?.take(10)
            val currentTemp = current?.optDouble("temperature")

            val result = LinkedHashMap<LocalDate, DailyWeather>()
            for (i in 0 until dates.length()) {
                val dateStr = dates.getString(i)
                val date = LocalDate.parse(dateStr)
                result[date] = DailyWeather(
                    date = date,
                    maxTemp = maxes.getDouble(i),
                    minTemp = mins.getDouble(i),
                    weatherCode = codes.getInt(i),
                    currentTemp = if (dateStr == currentDate) currentTemp else null
                )
            }
            result
        } catch (e: Exception) {
            null
        }
    }
}
