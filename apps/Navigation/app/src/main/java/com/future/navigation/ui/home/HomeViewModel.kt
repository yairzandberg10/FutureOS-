package com.future.navigation.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.geocoding.GeocodingRepository
import com.future.navigation.data.gtfs.GtfsDao
import com.future.navigation.data.gtfs.ServerDepartures
import com.future.navigation.data.gtfs.StopEntity
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.places.SavedPlaceRepository
import com.future.navigation.ui.navigation.TravelMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** departures: "5 בעוד 3 דק׳ · 18 בעוד 7 דק׳" כשהתחנה הגיעה מהשרת עם זמני יציאה חיים. */
data class NearbyStopUi(val stop: StopEntity, val distanceMeters: Double, val departures: String? = null)

class HomeViewModel(
    private val appContext: Context,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val gtfsDao: GtfsDao,
    private val geocodingRepository: GeocodingRepository,
    private val serverDepartures: ServerDepartures = ServerDepartures()
) : ViewModel() {

    /** תחנות וזמני יציאה מהשרת; null = עדיין לא נטען או שאין שרת, ואז מוצגות התחנות המקומיות. */
    private val _serverStops = MutableStateFlow<List<NearbyStopUi>?>(null)
    private var serverStopsJob: Job? = null
    private var serverStopsAt: LatLng? = null
    private var serverStopsAtMs = 0L

    private val _mode = MutableStateFlow(TravelMode.DRIVE)
    val mode = _mode.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    /** תחנות שנמצאו בחיפוש לפי שם (במצב תחבורה ציבורית, מהשרת). */
    private val _stationResults = MutableStateFlow<List<com.future.navigation.data.gtfs.StationHit>>(emptyList())
    val stationResults = _stationResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching = _searching.asStateFlow()

    /** true כשהחיפוש האחרון נכשל (אין רשת), כדי להבדיל מ"לא נמצאו תוצאות" (F31). */
    private val _searchFailed = MutableStateFlow(false)
    val searchFailed = _searchFailed.asStateFlow()

    /** מיקום חי - רץ רק כשמסך הבית בחזית (ר' HomeScreen), כדי לא לצרוך סוללה ברקע (F32). */
    private var liveLocationJob: Job? = null

    val homePlace = savedPlaceRepository.homePlace
    val workPlace = savedPlaceRepository.workPlace

    private val localNearbyStops = _currentLocation
        .flatMapLatest { location ->
            if (location == null) {
                flowOf(emptyList())
            } else {
                val radiusDeg = NEARBY_STOP_RADIUS_METERS / 111_320.0
                // מעלת אורך קצרה ממעלת רוחב (בישראל בערך 85%) - בלי התיקון התיבה
                // הייתה צרה מדי מזרח-מערב, ותחנות במרחק 600-700 מ' לא הופיעו.
                val lonRadiusDeg = radiusDeg / kotlin.math.cos(Math.toRadians(location.lat)).coerceAtLeast(0.1)
                gtfsDao.nearbyStops(
                    minLat = location.lat - radiusDeg, maxLat = location.lat + radiusDeg,
                    minLon = location.lon - lonRadiusDeg, maxLon = location.lon + lonRadiusDeg
                ).map { stops ->
                    stops
                        .map { NearbyStopUi(it, location.distanceMetersTo(LatLng(it.lat, it.lon))) }
                        .filter { it.distanceMeters <= NEARBY_STOP_RADIUS_METERS }
                        .sortedBy { it.distanceMeters }
                        // שני צדי הרחוב באותו שם = תחנה אחת (כמו ברשימה מהשרת), והשם הוא מפתח הרשימה
                        .distinctBy { it.stop.name }
                        .take(6)
                }
            }
        }

    val nearbyStops = combine(_serverStops, localNearbyStops) { server, local -> server ?: local }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * מרענן תחנות וזמני יציאה מהשרת: כשעוברים למצב תחבורה ציבורית, ואחר כך לכל
     * היותר פעם בדקה או אחרי תזוזה של 150 מ'. לא מוריד שום נתוני GTFS.
     */
    private fun refreshServerStops(force: Boolean = false) {
        if (_mode.value != TravelMode.TRANSIT) return
        val location = _currentLocation.value ?: return
        val fresh = System.currentTimeMillis() - serverStopsAtMs < SERVER_STOPS_REFRESH_MS
        val near = serverStopsAt?.let { it.distanceMetersTo(location) < SERVER_STOPS_MOVE_METERS } == true
        if (!force && fresh && near) return
        if (serverStopsJob?.isActive == true) return
        serverStopsJob = viewModelScope.launch {
            val boards = serverDepartures.near(location) ?: return@launch
            serverStopsAt = location
            serverStopsAtMs = System.currentTimeMillis()
            _serverStops.value = boards.map { board ->
                NearbyStopUi(
                    stop = StopEntity(board.name, board.name, board.location.lat, board.location.lon),
                    distanceMeters = location.distanceMetersTo(board.location),
                    departures = board.departures.take(3).joinToString(" · ") { d ->
                        if (d.minutesAway == 0) "${d.line} עכשיו" else "${d.line} בעוד ${d.minutesAway} דק׳"
                    }.ifBlank { null },
                )
            }.sortedBy { it.distanceMeters }.distinctBy { it.stop.name }
        }
    }

    private var searchJob: Job? = null

    init {
        refreshLocation()
    }

    fun refreshLocation() {
        _currentLocation.value = LocationHelper.lastKnownLatLon(appContext)?.let { LatLng(it.first, it.second) }
        refreshServerStops()
    }

    fun startLiveLocation() {
        if (liveLocationJob?.isActive == true) return
        liveLocationJob = viewModelScope.launch {
            // דגימה מדוללת (כ-30 שניות או 20 מ'), מספיקה לסמן על המפה המקדימה.
            LocationHelper.observeLocation(appContext, minIntervalMs = 30_000L, minDistanceMeters = 20f).collect { loc ->
                _currentLocation.value = LatLng(loc.latitude, loc.longitude)
                refreshServerStops()
            }
        }
    }

    fun stopLiveLocation() {
        liveLocationJob?.cancel()
        liveLocationJob = null
    }

    fun setMode(mode: TravelMode) {
        _mode.value = mode
        refreshServerStops(force = true)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _stationResults.value = emptyList()
            _searchFailed.value = false
            _searching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _searching.value = true
            try {
                delay(SEARCH_DEBOUNCE_MS)
                // שגיאת רשת זרקה קודם מתוך ה-coroutine והפילה את האפליקציה
                val outcome = try {
                    geocodingRepository.search(query) to false
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList<GeocodeResult>() to true
                }
                _searchResults.value = outcome.first
                _searchFailed.value = outcome.second
                // במצב תחבורה ציבורית מחפשים גם תחנה בשם הזה (כמו באגד/Moovit)
                val around = _currentLocation.value
                _stationResults.value = if (_mode.value == TravelMode.TRANSIT && around != null && query.trim().length >= 2) {
                    serverDepartures.searchStations(around, query.trim()).orEmpty()
                } else {
                    emptyList()
                }
            } finally {
                if (_searchQuery.value == query) _searching.value = false
            }
        }
    }

    companion object {
        private const val NEARBY_STOP_RADIUS_METERS = 700.0
        private const val SEARCH_DEBOUNCE_MS = 500L
        private const val SERVER_STOPS_REFRESH_MS = 60_000L
        private const val SERVER_STOPS_MOVE_METERS = 150.0
    }
}
