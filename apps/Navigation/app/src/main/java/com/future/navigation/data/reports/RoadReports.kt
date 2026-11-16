package com.future.navigation.data.reports

import com.future.navigation.data.backend.FutureServer
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** סוגי דיווחי הדרך של הקהילה (בסגנון Waze). key הוא הערך שהשרת מכיר; digit הוא המקש שמדווח אותו. */
enum class ReportType(val key: String, val digit: Int, val label: String, val spoken: String) {
    POLICE("police", 1, "משטרה", "משטרה"),
    ACCIDENT("accident", 2, "תאונה", "תאונה"),
    HAZARD("hazard", 3, "מפגע בדרך", "מפגע בדרך"),
    TRAFFIC("traffic", 4, "פקק", "פקק"),
    CLOSURE("closure", 5, "חסימה", "חסימת כביש");

    companion object {
        fun fromKey(key: String): ReportType? = entries.firstOrNull { it.key == key }
        fun fromDigit(digit: Int): ReportType? = entries.firstOrNull { it.digit == digit }
    }
}

data class RoadReport(
    val id: String,
    val type: ReportType,
    val location: LatLng,
    val confirms: Int,
    val ageSeconds: Int,
)

/**
 * דיווחי דרך דרך שרת FutureOS. כל פעולה מחזירה null/false בכל כשל (אין שרת,
 * אין רשת, מכסה) - דיווחים הם תוספת, ולעולם לא מפילים את הניווט.
 */
class RoadReportsRepository {

    suspend fun nearby(at: LatLng, radiusMeters: Int = 3000): List<RoadReport>? {
        val body = FutureServer.nearbyReportsJson(at, radiusMeters) ?: return null
        return try {
            val root = NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null
            (root["reports"] as? JsonArray).orEmpty().mapNotNull { element ->
                val row = element as? JsonObject ?: return@mapNotNull null
                val type = ReportType.fromKey(row.text("type") ?: return@mapNotNull null) ?: return@mapNotNull null
                RoadReport(
                    id = row.text("id") ?: return@mapNotNull null,
                    type = type,
                    location = LatLng(
                        row.number("lat") ?: return@mapNotNull null,
                        row.number("lng") ?: return@mapNotNull null,
                    ),
                    confirms = row.number("confirms")?.toInt() ?: 1,
                    ageSeconds = row.number("ageSeconds")?.toInt() ?: 0,
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submit(type: ReportType, at: LatLng): Boolean = FutureServer.submitReport(type.key, at) != null

    suspend fun vote(id: String, stillThere: Boolean): Boolean = FutureServer.voteReport(id, stillThere) != null

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()
}
