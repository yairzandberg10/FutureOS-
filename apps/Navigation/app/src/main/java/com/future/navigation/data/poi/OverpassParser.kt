package com.future.navigation.data.poi

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/**
 * שאילתת Overpass (OpenStreetMap, בלי מפתח) לקטגוריה סביב נקודה, ופירוק התשובה.
 * נקודות (node) מגיעות עם lat/lon; שטחים וקווים (way/relation) עם center, כי
 * השאילתה מבקשת "out center". בשני המקרים מחזירים מיקום אחד.
 */
object OverpassParser {
    const val ENDPOINT = "https://overpass-api.de/api/interpreter"
    const val MAX_RESULTS = 30

    /**
     * תקרה לשרת, רק כמנגנון ביטחון. השרת חותך לפי סדר האלמנטים (לא לפי מרחק),
     * לכן התקרה גבוהה מספיק כדי שהקרובים באמת יגיעו לפני המיון אצלנו.
     */
    private const val SERVER_ELEMENT_CAP = 300

    /** שרת מוותר אחרי 15 שניות, כדי שלא יחסום קטגוריה אחרת שנבחרה אחריה. */
    private const val SERVER_TIMEOUT_SECONDS = 15

    fun buildQuery(category: PoiCategory, origin: LatLng): String {
        val around = "(around:${category.radiusMeters},${coord(origin.lat)},${coord(origin.lon)})"
        val clauses = category.filters.joinToString("") { (key, value) ->
            "nwr[\"$key\"=\"$value\"]$around;"
        }
        return "[out:json][timeout:$SERVER_TIMEOUT_SECONDS];($clauses);out center $SERVER_ELEMENT_CAP;"
    }

    /**
     * מפרק את תשובת Overpass לרשימת מקומות: רק בתוך ישראל, בלי כפילויות, בלי
     * שמות ריקים (חניון בלי שם מקבל את שם הקטגוריה), ממוין לפי מרחק מהמשתמש.
     */
    fun parse(text: String, origin: LatLng, category: PoiCategory): List<PoiResult> {
        val elements = try {
            Json.parseToJsonElement(text).jsonObject["elements"]?.jsonArray
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        val places = elements.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val location = locationOf(obj) ?: return@mapNotNull null
            if (!IsraelBounds.contains(location)) return@mapNotNull null
            PoiResult(
                name = nameOf(obj) ?: category.label,
                location = location,
                distanceMeters = origin.distanceMetersTo(location),
            )
        }

        return PlaceSorting.dedupeNearby(
            items = places.sortedBy { it.distanceMeters },
            nameOf = { it.name },
            locationOf = { it.location },
        ).take(MAX_RESULTS)
    }

    private fun locationOf(obj: JsonObject): LatLng? {
        val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
        val lon = obj["lon"]?.jsonPrimitive?.doubleOrNull
        if (lat != null && lon != null) return LatLng(lat, lon)
        val center = obj["center"] as? JsonObject ?: return null
        val cLat = center["lat"]?.jsonPrimitive?.doubleOrNull ?: return null
        val cLon = center["lon"]?.jsonPrimitive?.doubleOrNull ?: return null
        return LatLng(cLat, cLon)
    }

    /** שם בעברית קודם, ואחריו השם הכללי או המותג. */
    private fun nameOf(obj: JsonObject): String? {
        val tags = obj["tags"] as? JsonObject ?: return null
        return listOf("name:he", "name", "brand")
            .firstNotNullOfOrNull { key -> tags[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } }
    }

    private fun coord(value: Double): String = String.format(Locale.US, "%.5f", value)
}
