package com.future.navigation.data.routing

import kotlinx.serialization.Serializable

/**
 * צורת התשובה האמיתית של OSRM (router.project-osrm.org/route/v1/driving/...),
 * נבדקה ידנית מול השרת האמיתי עם קואורדינטות אמיתיות בתל אביב לפני שנכתב הקובץ
 * הזה - כולל שמות רחובות אמיתיים בעברית וכל סוגי ה-maneuver.type/modifier
 * שהוחזרו בפועל (depart/turn/new name/on ramp/arrive, left/slight left/
 * straight/slight right/right). שדות שלא נחוצים לאפליקציה (intersections,
 * driving_side, weight וכו') לא ממופים - ה-Json מוגדר עם ignoreUnknownKeys.
 */
@Serializable
data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList()
)

@Serializable
data class OsrmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: OsrmGeometry,
    val legs: List<OsrmLeg> = emptyList()
)

@Serializable
data class OsrmLeg(
    val distance: Double,
    val duration: Double,
    val summary: String = "",
    val steps: List<OsrmStep> = emptyList()
)

@Serializable
data class OsrmStep(
    val distance: Double,
    val duration: Double,
    val name: String = "",
    val geometry: OsrmGeometry,
    val maneuver: OsrmManeuver
)

@Serializable
data class OsrmManeuver(
    val location: List<Double>,
    val type: String,
    val modifier: String? = null
)

@Serializable
data class OsrmGeometry(
    val type: String,
    val coordinates: List<List<Double>> = emptyList()
)
