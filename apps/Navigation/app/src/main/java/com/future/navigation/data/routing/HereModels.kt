package com.future.navigation.data.routing

import kotlinx.serialization.Serializable

/**
 * צורת התשובה האמיתית של HERE Routing API v8 (router.hereapi.com/v8/routes),
 * לפי מפרט ה-OpenAPI הרשמי (docs.here.com/routing/reference/routing-api-v8-calculateroutes)
 * שנבדק ידנית בזמן כתיבת הקוד הזה - כולל הבדיקה החשובה ש-return=turnByTurnActions
 * הוא השדה שמכיל currentRoad/nextRoad/direction/severity (return=actions הרגיל
 * לא כולל את אלה). שדות שלא נחוצים לאפליקציה לא ממופים - ה-Json מוגדר עם
 * ignoreUnknownKeys.
 */
@Serializable
data class HereRouteResponse(
    val routes: List<HereRoute> = emptyList()
)

@Serializable
data class HereRoute(
    val sections: List<HereSection> = emptyList()
)

@Serializable
data class HereSection(
    val polyline: String = "",
    val travelSummary: HereSummary? = null,
    val summary: HereSummary? = null,
    val turnByTurnActions: List<HereAction> = emptyList()
)

@Serializable
data class HereSummary(
    val length: Double = 0.0,
    val duration: Double = 0.0,
    val baseDuration: Double? = null
)

/**
 * שדה יחיד ושטוח לכל סוגי הפעולות (depart/arrive/turn/roundaboutEnter/exit/keep
 * וכו') - במקום union מסוג פולימורפי לפי דיסקרימיננטור "action" כמו ב-OpenAPI
 * המקורי. כל שדה שלא רלוונטי לסוג הפעולה הספציפי פשוט לא מופיע ב-JSON
 * ונשאר null/ברירת מחדל - קל יותר לתחזוקה מלהחזיק hierarchy שלם של תת-מחלקות.
 *
 * direction (left/right/middle) + severity (light/quite/heavy) יחד מבטאים את
 * מה ש-OSRM קרא לו "modifier" (למשל slight left/sharp right) - ר' המיפוי
 * ב-RoutingRepository.kt.
 */
@Serializable
data class HereAction(
    val action: String = "",
    val duration: Double = 0.0,
    val length: Double = 0.0,
    val offset: Int = 0,
    val instruction: String = "",
    val direction: String? = null,
    val severity: String? = null,
    val currentRoad: HereRoadInfo? = null,
    val nextRoad: HereRoadInfo? = null
)

@Serializable
data class HereRoadInfo(
    val name: List<HereLocalizedString> = emptyList()
)

@Serializable
data class HereLocalizedString(
    val value: String = "",
    val language: String = ""
)
