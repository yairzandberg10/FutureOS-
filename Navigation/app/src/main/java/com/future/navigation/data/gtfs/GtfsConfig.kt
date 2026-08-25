package com.future.navigation.data.gtfs

/** תיבה גיאוגרפית לסינון הייבוא - שינוי כאן הוא הדרך היחידה שצריך כדי לכסות אזור אחר. */
data class BoundingBox(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double) {
    /** true אם other נופלת לגמרי בתוך התיבה הזאת - משמש כדי לדעת שאין צורך בייבוא חוזר. */
    fun covers(other: BoundingBox): Boolean =
        other.minLat >= minLat && other.maxLat <= maxLat && other.minLon >= minLon && other.maxLon <= maxLon
}

object GtfsConfig {
    /**
     * כתובת ה-GTFS הסטטי הרשמי של משרד התחבורה - קובץ ZIP אמיתי, מתעדכן כל
     * לילה, כ-60 יום קדימה, ~130MB (כל התחבורה הציבורית בישראל). אומת ידנית
     * מול השרת בזמן כתיבת הקוד הזה (HTTP GET אמיתי, לא ניחוש).
     */
    const val FEED_URL = "https://gtfs.mot.gov.il/gtfsfiles/israel-public-transportation.zip"

    /**
     * גוש רחב יחסית סביב גוש דן (תל אביב והסביבה), כדי לכסות באמת "תחנות
     * קרובות אליי" למי שנמצא במטרופולין, בלי לייבא את כל הארץ. ניתן להרחיב
     * בקלות - זהו הקבוע היחיד שצריך לגעת בו כדי לכסות אזור אחר.
     */
    val TEL_AVIV_BBOX = BoundingBox(
        minLat = 31.95,
        maxLat = 32.25,
        minLon = 34.70,
        maxLon = 34.90
    )

    /**
     * תיבה גיאוגרפית סביב שתי נקודות (בד"כ המיקום הנוכחי והיעד שחיפש
     * המשתמש) עם ריפוד קבוע - כדי שהייבוא יכסה בפועל את מסלול הנסיעה,
     * ולא אזור קבוע-מראש שלא בהכרח קשור ליעד המבוקש.
     */
    fun boundingBoxCovering(
        a: com.future.navigation.data.common.LatLng,
        b: com.future.navigation.data.common.LatLng,
        paddingDegrees: Double = 0.05
    ): BoundingBox = BoundingBox(
        minLat = minOf(a.lat, b.lat) - paddingDegrees,
        maxLat = maxOf(a.lat, b.lat) + paddingDegrees,
        minLon = minOf(a.lon, b.lon) - paddingDegrees,
        maxLon = maxOf(a.lon, b.lon) + paddingDegrees
    )
}
