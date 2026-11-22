package com.future.navigation.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.sharednav.i18n.tr
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.geocoding.GeocodingRepository
import com.future.navigation.data.gtfs.GtfsDao
import com.future.navigation.data.gtfs.SavedPlaceEntity
import com.future.navigation.data.gtfs.ServerDepartures
import com.future.navigation.data.gtfs.StationHit
import com.future.navigation.data.gtfs.StopEntity
import com.future.navigation.data.gtfs.isStationCodeQuery
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.places.RecentPlace
import com.future.navigation.data.places.RecentPlaceRepository
import com.future.navigation.data.places.SavedPlaceRepository
import com.future.navigation.data.poi.IsraelBounds
import com.future.navigation.data.poi.PlaceSorting
import com.future.navigation.data.poi.PoiCategory
import com.future.navigation.data.poi.PoiRepository
import com.future.navigation.data.poi.PoiResult
import com.future.navigation.ui.navigation.TravelMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** departures: "5 בעוד 3 דק׳ · 18 בעוד 7 דק׳" כשהתחנה הגיעה מהשרת עם זמני יציאה חיים. */
data class NearbyStopUi(
    val stop: StopEntity,
    val distanceMeters: Double,
    val departures: String? = null,
    /** מספר התחנה הרשמי ("תחנה מס׳ 63273"), null כשלא ידוע (למשל תחנות מקומיות). */
    val stopCode: String? = null,
)

/** תוצאת חיפוש עם המרחק מהמשתמש (null = המיקום עדיין לא ידוע). */
data class SearchHit(val result: GeocodeResult, val distanceMeters: Double?)

/** מצב חיפוש בקרבת מקום לקטגוריה שנבחרה. */
data class PoiUiState(
    val category: PoiCategory? = null,
    val loading: Boolean = false,
    val results: List<PoiResult> = emptyList(),
    val failed: Boolean = false,
    val noLocation: Boolean = false,
    val outsideIsrael: Boolean = false,
)

class HomeViewModel(
    private val appContext: Context,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val gtfsDao: GtfsDao,
    private val geocodingRepository: GeocodingRepository,
    private val serverDepartures: ServerDepartures = ServerDepartures(),
    // ברירות מחדל: MainActivity לא צריך לשנות כדי שזה יעבוד
    private val recentPlaces: RecentPlaceRepository = RecentPlaceRepository(appContext),
    private val poiRepository: PoiRepository = PoiRepository(),
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

    /** תוצאות החיפוש: בלי כפילויות, עם המרחק מהמשתמש, והקרובות ראשונות. */
    val searchHits: StateFlow<List<SearchHit>> = combine(_searchResults, _currentLocation) { results, origin ->
        val unique = PlaceSorting.dedupeNearby(results, nameOf = { it.label }, locationOf = { it.location })
        PlaceSorting.sortNearest(unique, origin) { it.location }
            .map { SearchHit(it, origin?.distanceMetersTo(it.location)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    /** היעדים האחרונים (החדש ראשון). */
    val recents: StateFlow<List<RecentPlace>> = recentPlaces.recents

    /** המועדפים - מסומנים ב-isFavorite בטבלת המקומות השמורים. */
    val favorites: StateFlow<List<SavedPlaceEntity>> = savedPlaceRepository.allPlaces
        .map { list -> list.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _poi = MutableStateFlow(PoiUiState())
    val poi: StateFlow<PoiUiState> = _poi.asStateFlow()
    private var poiJob: Job? = null

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
                    stopCode = board.stopCode,
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
        // ה-lastKnown יכול להיות ממקום אחר - מבקשים מיקום אמיתי ומעדכנים כשהוא מגיע
        viewModelScope.launch {
            val fresh = LocationHelper.currentLatLon(appContext)?.let { LatLng(it.first, it.second) } ?: return@launch
            if (fresh != _currentLocation.value) {
                _currentLocation.value = fresh
                refreshServerStops()
            }
        }
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
                // שגיאת רשת נזרקת מהריפוזיטורי ונתפסת כאן - לא מפילה את האפליקציה
                val outcome = try {
                    geocodingRepository.search(query) to false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList<GeocodeResult>() to true
                }
                _searchResults.value = outcome.first
                _searchFailed.value = outcome.second
                // מספר תחנה (3-6 ספרות) - התחנה עצמה ראשונה ברשימה
                val text = query.trim()
                val byCode = if (isStationCodeQuery(text)) {
                    findStationByCode(text)
                } else {
                    null
                }
                // במצב תחבורה ציבורית מחפשים גם תחנה בשם הזה (כמו באגד/Moovit)
                val around = _currentLocation.value
                val byName = if (byCode == null && _mode.value == TravelMode.TRANSIT && around != null && text.length >= 2) {
                    serverDepartures.searchStations(around, text).orEmpty()
                } else {
                    emptyList()
                }
                _stationResults.value = (listOfNotNull(byCode) + byName.filter { it.location != byCode?.location })
                    .map { withStationNumber(it) }
            } finally {
                if (_searchQuery.value == query) _searching.value = false
            }
        }
    }

    /**
     * תחנה לפי מספר התחנה. קודם התחנות הקרובות שכבר נטענו (בלי רשת), ואחר כך חיפוש
     * ברשת שמאמת את המספר בלוח היציאות של כל מועמדת (ר' ServerDepartures.findStopByCode).
     */
    private suspend fun findStationByCode(code: String): StationHit? {
        _serverStops.value?.firstOrNull { it.stopCode == code }?.let { nearby ->
            return StationHit(nearby.stop.name, LatLng(nearby.stop.lat, nearby.stop.lon), emptyList(), code)
        }
        return serverDepartures.findStopByCode(code, _currentLocation.value ?: DEFAULT_CENTER)
    }

    /**
     * מספר התחנה לתחנה שנמצאה בשם: לוקחים אותו מהתחנה הקרובה שכבר נטענה (באותו
     * מקום, עד 80 מ'). בלי תשובה מהרשת - בלי מספר, והשורה נשארת כמו שהיא.
     */
    private fun withStationNumber(hit: StationHit): StationHit {
        if (hit.stopCode != null) return hit
        val code = _serverStops.value
            ?.firstOrNull { it.stopCode != null && LatLng(it.stop.lat, it.stop.lon).distanceMetersTo(hit.location) < SAME_PLACE_METERS }
            ?.stopCode
        return if (code != null) hit.copy(stopCode = code) else hit
    }

    /** שמירת מקום שנבחר בחיפוש כמועדף (בלי כפילות לאותה כתובת - ר' SavedPlaceRepository). */
    fun addFavoritePlace(result: GeocodeResult) {
        viewModelScope.launch {
            savedPlaceRepository.addFavorite(
                label = result.label.substringBefore(","),
                address = result.label,
                lat = result.location.lat,
                lon = result.location.lon,
            )
        }
    }

    // ---- קטגוריות: מקומות בקרבת מקום ----

    /** בחירת קטגוריה; לחיצה שנייה על אותה קטגוריה מבטלת את הבחירה. */
    fun toggleCategory(category: PoiCategory) {
        if (_poi.value.category == category) {
            clearCategory()
            return
        }
        poiJob?.cancel()
        val origin = _currentLocation.value
        if (origin == null) {
            _poi.value = PoiUiState(category = category, noLocation = true)
            return
        }
        if (!IsraelBounds.contains(origin)) {
            _poi.value = PoiUiState(category = category, outsideIsrael = true)
            return
        }
        _poi.value = PoiUiState(category = category, loading = true)
        poiJob = viewModelScope.launch {
            val state = try {
                PoiUiState(category = category, results = poiRepository.nearby(category, origin))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PoiUiState(category = category, failed = true)
            }
            // תוצאה של קטגוריה שכבר הוחלפה לא נכתבת מעל הבחירה החדשה
            if (_poi.value.category == category) _poi.value = state
        }
    }

    fun clearCategory() {
        poiJob?.cancel()
        _poi.value = PoiUiState()
    }

    // ---- יעדים אחרונים, מועדפים, בית ועבודה ----

    /** נקרא בכל בחירת יעד מהמסך, כך שהאחרונים נשמרים בלי לגעת במסלול הניווט. */
    fun rememberRecent(result: GeocodeResult) {
        recentPlaces.add(
            RecentPlace(
                title = result.label.substringBefore(","),
                address = result.label,
                lat = result.location.lat,
                lon = result.location.lon,
            )
        )
    }

    fun removeRecent(target: PlaceTarget) {
        recentPlaces.remove(
            RecentPlace(target.title, target.address, target.location.lat, target.location.lon)
        )
    }

    /** מוסיף למועדפים, או מסיר אם המקום כבר שם. */
    fun toggleFavorite(target: PlaceTarget) {
        viewModelScope.launch {
            val existing = favorites.value.firstOrNull { it.address == target.address }
            if (existing != null) {
                savedPlaceRepository.toggleFavorite(existing)
            } else {
                savedPlaceRepository.addFavorite(
                    label = target.title,
                    address = target.address,
                    lat = target.location.lat,
                    lon = target.location.lon,
                )
            }
        }
    }

    fun saveAsHome(target: PlaceTarget) {
        viewModelScope.launch {
            savedPlaceRepository.setHome(tr("בית"), target.address, target.location.lat, target.location.lon)
        }
    }

    fun saveAsWork(target: PlaceTarget) {
        viewModelScope.launch {
            savedPlaceRepository.setWork(tr("עבודה"), target.address, target.location.lat, target.location.lon)
        }
    }

    companion object {
        private const val NEARBY_STOP_RADIUS_METERS = 700.0
        private const val SEARCH_DEBOUNCE_MS = 500L
        private const val SERVER_STOPS_REFRESH_MS = 60_000L
        private const val SERVER_STOPS_MOVE_METERS = 150.0
        /** תחנה "באותו מקום" כשמחברים מספר לשם: עד 80 מ' */
        private const val SAME_PLACE_METERS = 80.0
        /** מרכז ישראל - מוצא לחיפוש מספר תחנה כשאין מיקום עדיין */
        private val DEFAULT_CENTER = LatLng(31.5, 34.9)
    }
}
