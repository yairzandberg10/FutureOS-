package com.future.navigation.data.gtfs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** שורה משורשרת של יציאה מתחנה: שעה + פרטי הקו/נסיעה, לתצוגת "היעדים הקרובים". */
data class DepartureRow(
    val tripId: String,
    val stopId: String,
    val arrivalSeconds: Int,
    val departureSeconds: Int,
    val stopSequence: Int,
    val serviceId: String,
    val routeShortName: String,
    val routeLongName: String,
    val tripHeadsign: String
)

@Dao
interface GtfsDao {

    @Query(
        """
        SELECT * FROM gtfs_stops
        WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon
        """
    )
    fun nearbyStops(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Flow<List<StopEntity>>

    @Query("SELECT * FROM gtfs_stops WHERE stopId = :stopId LIMIT 1")
    suspend fun stopById(stopId: String): StopEntity?

    /** גרסה חד-פעמית (לא Flow) של אותה שאילתה, לשימוש בחיפוש מסלול נקודתי. */
    @Query(
        """
        SELECT * FROM gtfs_stops
        WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon
        """
    )
    suspend fun stopsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<StopEntity>

    @Query(
        """
        SELECT st.tripId, st.stopId, st.arrivalSeconds, st.departureSeconds, st.stopSequence,
               t.serviceId AS serviceId, r.shortName AS routeShortName, r.longName AS routeLongName,
               t.headsign AS tripHeadsign
        FROM gtfs_stop_times st
        JOIN gtfs_trips t ON t.tripId = st.tripId
        JOIN gtfs_routes r ON r.routeId = t.routeId
        WHERE st.stopId = :stopId AND st.departureSeconds >= :afterSeconds
        ORDER BY st.departureSeconds ASC
        LIMIT :limit
        """
    )
    suspend fun departuresFromStop(stopId: String, afterSeconds: Int, limit: Int = 50): List<DepartureRow>

    /** כל הנסיעות שעוצרות בתחנה נתונה, לחיפוש מסלול ישיר/עם החלפה אחת. */
    @Query(
        """
        SELECT st.tripId, st.stopId, st.arrivalSeconds, st.departureSeconds, st.stopSequence,
               t.serviceId AS serviceId, r.shortName AS routeShortName, r.longName AS routeLongName,
               t.headsign AS tripHeadsign
        FROM gtfs_stop_times st
        JOIN gtfs_trips t ON t.tripId = st.tripId
        JOIN gtfs_routes r ON r.routeId = t.routeId
        WHERE st.stopId IN (:stopIds) AND st.departureSeconds >= :afterSeconds
        ORDER BY st.departureSeconds ASC
        """
    )
    suspend fun departuresFromStops(stopIds: List<String>, afterSeconds: Int): List<DepartureRow>

    @Query(
        """
        SELECT st.tripId, st.stopId, st.arrivalSeconds, st.departureSeconds, st.stopSequence,
               t.serviceId AS serviceId, r.shortName AS routeShortName, r.longName AS routeLongName,
               t.headsign AS tripHeadsign
        FROM gtfs_stop_times st
        JOIN gtfs_trips t ON t.tripId = st.tripId
        JOIN gtfs_routes r ON r.routeId = t.routeId
        WHERE st.tripId = :tripId
        ORDER BY st.stopSequence ASC
        """
    )
    suspend fun stopTimesForTrip(tripId: String): List<DepartureRow>

    @Query("SELECT * FROM gtfs_calendar WHERE serviceId = :serviceId LIMIT 1")
    suspend fun calendarForService(serviceId: String): CalendarEntity?

    @Query("SELECT COUNT(*) FROM gtfs_stops")
    suspend fun stopCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopTimes(stopTimes: List<StopTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendar(entries: List<CalendarEntity>)

    @Query("DELETE FROM gtfs_stops")
    suspend fun clearStops()

    @Query("DELETE FROM gtfs_routes")
    suspend fun clearRoutes()

    @Query("DELETE FROM gtfs_trips")
    suspend fun clearTrips()

    @Query("DELETE FROM gtfs_stop_times")
    suspend fun clearStopTimes()

    @Query("DELETE FROM gtfs_calendar")
    suspend fun clearCalendar()
}

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY isHome DESC, isWork DESC, label ASC")
    fun allPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE isHome = 1 LIMIT 1")
    fun homePlace(): Flow<SavedPlaceEntity?>

    @Query("SELECT * FROM saved_places WHERE isWork = 1 LIMIT 1")
    fun workPlace(): Flow<SavedPlaceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: SavedPlaceEntity): Long

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE saved_places SET isHome = 0 WHERE isHome = 1")
    suspend fun clearHome()

    @Query("UPDATE saved_places SET isWork = 0 WHERE isWork = 1")
    suspend fun clearWork()
}
