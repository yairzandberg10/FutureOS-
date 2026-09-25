package com.future.calendar.data
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Thermostat

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
