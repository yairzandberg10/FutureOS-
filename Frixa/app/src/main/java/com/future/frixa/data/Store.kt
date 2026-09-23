package com.future.frixa.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Store(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val openingHours: String? = null,
)

/**
 * מקומות שמוכרים פריקסה - מ-OpenStreetMap (Overpass API), לא רשימה מומצאת:
 * כל מקום ששמו כולל "פריקסה"/Fricassé, או שהמטבח שלו מסומן טוניסאי. קודם
 * החיפוש סביב המיקום (30 ק"מ); בלי מיקום - בכל הארץ. התוצאה האחרונה נשמרת,
 * כך שהרשימה מוצגת גם בלי רשת.
 */
object FricasseStores {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"
    private const val PREFS = "fricasse_stores"
    private const val KEY = "last"
    private const val NAME_PATTERN = "פריקס|fricass|fricas|פריקאסה"

    fun cached(context: Context): List<Store> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.let(::parse).orEmpty()

    /** מחפש ברשת; זורק חריגה כשאין חיבור (הקורא מציג את השמור). */
    fun search(context: Context, near: Pair<Double, Double>?): List<Store> {
        val filter = if (near != null) {
            val (lat, lon) = near
            "(around:30000,$lat,$lon)"
        } else {
            "(area.il)"
        }
        val query = buildString {
            append("[out:json][timeout:25];")
            if (near == null) append("area[\"ISO3166-1\"=\"IL\"]->.il;")
            append("(")
            append("nwr[\"name\"~\"$NAME_PATTERN\",i]$filter;")
            append("nwr[\"cuisine\"~\"tunisian\",i]$filter;")
            append(");out center tags 60;")
        }
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("User-Agent", "FutureOS-Fricasse/1.0")
        connection.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray()) }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val stores = parse(body)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, body).apply()
        return stores
    }

    private fun parse(json: String): List<Store> = try {
        val elements: JSONArray = JSONObject(json).getJSONArray("elements")
        (0 until elements.length()).mapNotNull { i ->
            val e = elements.getJSONObject(i)
            val tags = e.optJSONObject("tags") ?: return@mapNotNull null
            val name = tags.optString("name:he").ifBlank { tags.optString("name") }.ifBlank { return@mapNotNull null }
            val center = e.optJSONObject("center")
            val lat = if (e.has("lat")) e.getDouble("lat") else center?.optDouble("lat") ?: return@mapNotNull null
            val lon = if (e.has("lon")) e.getDouble("lon") else center?.optDouble("lon") ?: return@mapNotNull null
            val address = listOf(
                listOf(tags.optString("addr:street"), tags.optString("addr:housenumber")).filter { it.isNotBlank() }.joinToString(" "),
                tags.optString("addr:city"),
            ).filter { it.isNotBlank() }.joinToString(", ")
            Store(
                id = e.optLong("id"),
                name = name,
                address = address,
                latitude = lat,
                longitude = lon,
                phone = tags.optString("phone").ifBlank { tags.optString("contact:phone") }.ifBlank { null },
                openingHours = tags.optString("opening_hours").ifBlank { null },
            )
        }.distinctBy { it.id }
    } catch (e: Exception) {
        emptyList()
    }
}
