package com.future.navigation.data.routing

import com.future.navigation.ui.navigation.TravelMode

/**
 * פרופיל ניתוב של OSRM. נהיגה - השרת הציבורי (בלי מפתח). הליכה ורכיבה - השרת
 * של openstreetmap.de (אותו פורמט תשובה, בלי מפתח). כל השלושה מחזירים
 * GeoJSON + צעדים באותו מבנה, כך שהפירוק (OsrmParser) משותף.
 */
enum class RoutingProfile(val baseUrl: String, val path: String) {
    DRIVING("https://router.project-osrm.org", "driving"),
    FOOT("https://routing.openstreetmap.de/routed-foot", "foot"),
    BIKE("https://routing.openstreetmap.de/routed-bike", "bike");

    companion object {
        /** מיפוי מצב הנסיעה (מסך הבית) לפרופיל. תחבורה ציבורית לא משתמשת ב-OSRM. */
        fun forMode(mode: TravelMode): RoutingProfile = when (mode) {
            TravelMode.DRIVE -> DRIVING
            TravelMode.TRANSIT -> DRIVING
            TravelMode.WALK -> FOOT
            TravelMode.BIKE -> BIKE
        }
    }
}
