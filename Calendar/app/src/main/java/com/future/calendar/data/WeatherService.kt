package com.future.calendar.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class DailyWeather(val date: LocalDate, val maxTemp: Double, val minTemp: Double, val weatherCode: Int, val currentTemp: Double? = null) {
    val emoji: String get() = weatherEmoji(weatherCode)
    val description: String get() = weatherDescription(weatherCode)
}

private fun weatherEmoji(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "🌤️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57 -> "🌦️"
    61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
    71, 73, 75, 77, 85, 86 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "בהיר"
    1, 2 -> "מעונן חלקית"
    3 -> "מעונן"
    45, 48 -> "ערפל"
    51, 53, 55, 56, 57 -> "טפטוף"
    61, 63, 65, 66, 67 -> "גשם"
    71, 73, 75, 77 -> "שלג"
    80, 81, 82 -> "ממטרים"
    85, 86 -> "ממטרי שלג"
    95, 96, 99 -> "סופת רעמים"
    else -> "לא ידוע"
}

/** תחזית מזג אוויר אמיתית ל-16 יום קדימה, דרך Open-Meteo - שירות ציבורי ללא צורך במפתח API. */
object WeatherService {
    /** מפה מתאריך לתחזית אותו יום - מאפשר לתאם לתאריך שמוצג במסך היום, לא רק "עכשיו". */
    fun fetchForecast(lat: Double, lon: Double): Map<LocalDate, DailyWeather>? {
        return try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current_weather=true&daily=weathercode,temperature_2m_max,temperature_2m_min" +
                    "&forecast_days=16&timezone=auto"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"

            if (connection.responseCode != 200) {
                connection.disconnect()
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

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
