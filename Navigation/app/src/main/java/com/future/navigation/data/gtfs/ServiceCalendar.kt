package com.future.navigation.data.gtfs

import java.time.DayOfWeek

/**
 * האם שירות GTFS (calendar.txt) פעיל בתאריך וביום נתונים. משותף לתכנון
 * המסלול (TransitJourneyPlanner) ולזיהוי האוטובוס שנסרק (BusScanRepository).
 */
suspend fun GtfsDao.isServiceActive(serviceId: String, dateInt: Int, dayOfWeek: DayOfWeek): Boolean {
    val calendar = calendarForService(serviceId) ?: return false
    if (dateInt < calendar.startDate || dateInt > calendar.endDate) return false
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> calendar.monday
        DayOfWeek.TUESDAY -> calendar.tuesday
        DayOfWeek.WEDNESDAY -> calendar.wednesday
        DayOfWeek.THURSDAY -> calendar.thursday
        DayOfWeek.FRIDAY -> calendar.friday
        DayOfWeek.SATURDAY -> calendar.saturday
        DayOfWeek.SUNDAY -> calendar.sunday
    }
}
