package com.future.navigation.data.siri

/**
 * שורת תחזית הגעה בזמן אמת אחת, מפוענחת מתוך MonitoredStopVisit בתשובת
 * SIRI Stop Monitoring. הזמנים בשניות-מחצות (Asia/Jerusalem) - אותו יחידת
 * מידה שבה GTFS (data/gtfs) עובד, כדי שהשוואה/שיבוץ יהיו ישירים בלי המרה
 * נוספת בצד הקורא.
 */
data class RealtimeArrival(
    val lineRef: String?,
    val vehicleRef: String?,
    val aimedDepartureSeconds: Int?,
    val expectedDepartureSeconds: Int?
) {
    /** הפרש בשניות בין ההגעה הצפויה בפועל לזו המתוכננת - חיובי = איחור. null אם אין נתון תקף להשוואה. */
    val delaySeconds: Int?
        get() {
            val aimed = aimedDepartureSeconds ?: return null
            val expected = expectedDepartureSeconds ?: return null
            return expected - aimed
        }
}
