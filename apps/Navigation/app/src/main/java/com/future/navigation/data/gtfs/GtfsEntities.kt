package com.future.navigation.data.gtfs

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ישויות Room למטמון המקומי של נתוני ה-GTFS (משרד התחבורה). השדות הם רק
 * אלה שהאפליקציה באמת צריכה מתוך מפרט GTFS המלא - לא כל עמודה אפשרית.
 * הפירוק בפועל מקובץ ה-CSV (GtfsImporter) מבוסס על שמות העמודות בשורת
 * הכותרת של כל קובץ, לא על מיקום קבוע - כך שסדר/עמודות נוספות בקובץ
 * הספציפי הזה לא שוברים את הפירוק.
 */
@Entity(tableName = "gtfs_stops")
data class StopEntity(
    @PrimaryKey val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double
)

@Entity(tableName = "gtfs_routes")
data class RouteEntity(
    @PrimaryKey val routeId: String,
    val shortName: String,
    val longName: String,
    /** קידוד GTFS route_type: 0=חשמלית/רק"ל, 2=רכבת, 3=אוטובוס וכו'. */
    val type: Int
)

@Entity(tableName = "gtfs_trips")
data class TripEntity(
    @PrimaryKey val tripId: String,
    val routeId: String,
    val serviceId: String,
    val headsign: String
)

@Entity(tableName = "gtfs_stop_times")
data class StopTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String,
    val stopId: String,
    /** שניות מחצות (יכול לעבור 86400 בנסיעות שחוצות חצות - זה תקין ב-GTFS). */
    val arrivalSeconds: Int,
    val departureSeconds: Int,
    val stopSequence: Int
)

@Entity(tableName = "gtfs_calendar")
data class CalendarEntity(
    @PrimaryKey val serviceId: String,
    val monday: Boolean,
    val tuesday: Boolean,
    val wednesday: Boolean,
    val thursday: Boolean,
    val friday: Boolean,
    val saturday: Boolean,
    val sunday: Boolean,
    /** תאריכים כ-YYYYMMDD (Int), כמו בפורמט הגולמי של GTFS. */
    val startDate: Int,
    val endDate: Int
)

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val isHome: Boolean = false,
    val isWork: Boolean = false,
    val isFavorite: Boolean = false
)
