package com.future.navigation.ui.routes

import com.future.sharednav.i18n.tr

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.gtfs.GtfsConfig
import com.future.navigation.data.gtfs.GtfsImportState
import com.future.navigation.data.network.NetworkPolicy
import com.future.navigation.data.gtfs.GtfsImporter
import com.future.navigation.data.gtfs.ImportPhase
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitJourneyPlanner
import com.future.navigation.data.gtfs.TransitServerPlanner
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.routing.DrivingRoute
import com.future.navigation.data.routing.RouteLookup
import com.future.navigation.data.routing.OsrmRouting
import com.future.navigation.data.routing.RoutingProfile
import com.future.navigation.data.routing.RoutingRepository
import com.future.navigation.data.siri.TransitRealtimeEnricher
import com.future.navigation.data.transit.ISRAEL_ZONE
import com.future.navigation.data.transit.TransitPlanning
import com.future.navigation.data.transit.TransitPrefs
import com.future.navigation.data.transit.TransitPrefsStore
import com.future.navigation.data.transit.TripWhen
import kotlin.math.abs
import com.future.navigation.ui.navigation.Destination
import com.future.navigation.ui.navigation.TravelMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** מספר המסלולים המקסימלי שמוצגים (המסלול הראשי + חלופות). */
private const val MAX_ALTERNATIVES = 3

class RouteOptionsViewModel(
    private val appContext: Context,
    private val routingRepository: RoutingRepository,
    private val transitJourneyPlanner: TransitJourneyPlanner,
    private val gtfsImporter: GtfsImporter,
    private val transitRealtimeEnricher: TransitRealtimeEnricher,
    private val transitServerPlanner: TransitServerPlanner = TransitServerPlanner()
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    /** לא null בזמן הורדה/ייבוא אוטומטי של נתוני התחבורה - כדי שהמסך יראה "מוריד נתוני תחבורה" ולא רק "מחשב מסלול". */
    private val _downloadFraction = MutableStateFlow<Float?>(null)
    val downloadFraction = _downloadFraction.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** true כשההודעה בשגיאה היא שגיאת שרת/רשת - אז מוצג כפתור "נסה שוב". */
    private val _serverError = MutableStateFlow(false)
    val serverError = _serverError.asStateFlow()

    private var lastSearch: Triple<TravelMode, Destination, Boolean>? = null

    /** הורדה של נתוני התחבורה (~130MB) ברשת סלולרית מחכה לאישור המשתמש (F09). */
    private val _askCellularDownload = MutableStateFlow(false)
    val askCellularDownload = _askCellularDownload.asStateFlow()
    private var pendingSearch: Pair<TravelMode, Destination>? = null

    /** הימנעות מכבישי אגרה (נשמר בין נסיעות) - מקש 1 במסך האפשרויות. */
    private val _avoidTolls = MutableStateFlow(com.future.navigation.data.routing.DrivingPrefs.avoidTolls)
    val avoidTolls = _avoidTolls.asStateFlow()

    /** "מתי לצאת": בעוד כמה דקות (0 = עכשיו) - מקש 3 עובר בין 0/15/30/60/120. */
    private val _departInMinutes = MutableStateFlow(0)
    val departInMinutes = _departInMinutes.asStateFlow()

    /** "מתי" לתחבורה ציבורית (מסך המתי + הזזה של ±10 דק'). נפרד מ-departInMinutes של הנהיגה. */
    private val _tripWhen = MutableStateFlow(TripWhen())
    val tripWhen = _tripWhen.asStateFlow()

    /** העדפות תחבורה (מסלול ואמצעים), נשמרות ב-SharedPreferences. */
    private val _transitPrefs = MutableStateFlow(TransitPrefsStore.load(appContext))
    val transitPrefs = _transitPrefs.asStateFlow()

    fun setTripWhen(choice: TripWhen) {
        _tripWhen.value = choice
        retry()
    }

    fun shiftTripTime(minutes: Int) {
        val current = _tripWhen.value
        _tripWhen.value = current.copy(shiftMinutes = current.shiftMinutes + minutes)
        retry()
    }

    fun setTransitPrefs(value: TransitPrefs) {
        _transitPrefs.value = value
        TransitPrefsStore.save(appContext, value)
        retry()
    }

    fun toggleAvoidTolls() {
        val next = !_avoidTolls.value
        _avoidTolls.value = next
        com.future.navigation.data.routing.DrivingPrefs.avoidTolls = next
        retry()
    }

    fun cycleDepartureTime() {
        val options = listOf(0, 15, 30, 60, 120)
        _departInMinutes.value = options[(options.indexOf(_departInMinutes.value) + 1) % options.size]
        retry()
    }

    private fun departAtIso(): String? {
        val minutes = _departInMinutes.value
        if (minutes == 0) return null
        return java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Jerusalem"))
            .plusMinutes(minutes.toLong())
            .withNano(0)
            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private val _drivingRoute = MutableStateFlow<DrivingRoute?>(null)
    val drivingRoute = _drivingRoute.asStateFlow()

    /** כל החלופות למסלול (עד 3). הראשונה היא ברירת המחדל. */
    private val _alternatives = MutableStateFlow<List<DrivingRoute>>(emptyList())
    val alternatives = _alternatives.asStateFlow()

    /** האינדקס של המסלול שנבחר ברשימה (D-pad, לחיצה על חלופה). */
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex = _selectedIndex.asStateFlow()

    fun selectRoute(index: Int) {
        val list = _alternatives.value
        if (index !in list.indices) return
        _selectedIndex.value = index
        _drivingRoute.value = list[index]
    }

    private fun showRoutes(routes: List<DrivingRoute>) {
        _alternatives.value = routes
        _selectedIndex.value = 0
        _drivingRoute.value = routes.firstOrNull()
    }

    private val _itineraries = MutableStateFlow<List<TransitItinerary>>(emptyList())
    val itineraries = _itineraries.asStateFlow()

    /** עבודת החיפוש הפעילה - מבוטלת אם מתבקש חיפוש חדש לפני שהקודם הסתיים,
     * כדי שתשובה איטית/ישנה לא תדרוס תוצאה טרייה יותר. */
    private var searchJob: Job? = null
    /** מזהה החיפוש האחרון - רק החיפוש האחרון מכבה את מצב הטעינה (ביטול של קודם לא מכבה חדש). */
    private var searchId = 0

    /** נקודת מוצא שנבחרה ידנית (null = המיקום הנוכחי). נשמרת לחיפוש חוזר (retry). */
    private var chosenOrigin: LatLng? = null

    fun search(mode: TravelMode, destination: Destination, allowCellularDownload: Boolean = false, from: LatLng? = chosenOrigin) {
        val id = ++searchId
        chosenOrigin = from
        // יעד חדש מתחיל תמיד מ"עכשיו" - שעה/קווים אחרונים מחיפוש קודם לא נגררים אליו
        val previous = lastSearch?.second
        if (previous != null && previous.location != destination.location) _tripWhen.value = TripWhen()
        lastSearch = Triple(mode, destination, allowCellularDownload)
        _serverError.value = false
        searchJob?.cancel()
        // תוצאות של יעד קודם לא נשארות על המסך - גם כשאין מיקום (אחרת מסלול ישן נפתח על יעד חדש)
        _drivingRoute.value = null
        _itineraries.value = emptyList()
        _error.value = null
        _alternatives.value = emptyList()
        _selectedIndex.value = 0
        // המיקום הכי מדויק שיש (ר' LocationHelper.bestLastKnown) - נקודת המוצא של המסלול
        val origin = from ?: LocationHelper.lastKnownLatLon(appContext)?.let { LatLng(it.first, it.second) }
        if (origin == null) {
            _loading.value = false
            _error.value = tr("לא ניתן לאתר את המיקום הנוכחי")
            return
        }

        _loading.value = true
        searchJob = viewModelScope.launch {
            try {
                when (mode) {
                    TravelMode.DRIVE -> when (val lookup = routingRepository.getDrivingRoute(origin, destination.location, departAtIso())) {
                        is RouteLookup.Found -> {
                            // חלופות מ-OSRM (נהיגה בלי שעת יציאה בלבד - הן לא מתחשבות בזמן עתידי).
                            // מסלול שכמעט זהה למסלול הראשי (באורך) לא נכנס לרשימה.
                            val extra = if (departAtIso() == null) {
                                OsrmRouting.fetchAll(origin, destination.location, RoutingProfile.DRIVING, alternatives = true).orEmpty()
                            } else {
                                emptyList()
                            }
                            val distinct = extra.filter { abs(it.distanceMeters - lookup.route.distanceMeters) > 100.0 }
                            showRoutes(listOf(lookup.route) + distinct.take(MAX_ALTERNATIVES - 1))
                        }
                        RouteLookup.NoRoute -> _error.value = tr("לא נמצא מסלול נסיעה")
                        RouteLookup.ServerError -> {
                            _error.value = "שגיאת שרת – נסה שוב"
                            _serverError.value = true
                        }
                    }
                    TravelMode.WALK, TravelMode.BIKE -> {
                        // הליכה/רכיבה: OSRM של openstreetmap.de, עם חלופות
                        when (val routes = OsrmRouting.fetchAll(origin, destination.location, RoutingProfile.forMode(mode), alternatives = true)) {
                            null -> {
                                _error.value = "שגיאת שרת – נסה שוב"
                                _serverError.value = true
                            }
                            else -> if (routes.isEmpty()) {
                                _error.value = tr("לא נמצא מסלול")
                            } else {
                                showRoutes(routes.take(MAX_ALTERNATIVES))
                            }
                        }
                    }
                    TravelMode.TRANSIT -> {
                        // המסלול מחושב ברשת (Transitous), בלי להוריד את קובץ ה-GTFS הארצי.
                        // נתונים מקומיים משמשים רק אם כבר יובאו בעבר ומכסים את האזור.
                        val planTime = TransitPlanning.planTime(_tripWhen.value, java.time.ZonedDateTime.now(ISRAEL_ZONE))
                        val online = transitServerPlanner.findJourneys(
                            origin,
                            destination.location,
                            planTime.iso,
                            planTime.arriveBy,
                            TransitPlanning.planParams(_transitPrefs.value),
                        )
                        val localCovers = !transitDownloadNeeded(origin, destination.location)
                        // MOTIS מחזיר גם מסלולים שיוצאים לפני הזמן, איטיים ונשלטים - מסננים וממיינים
                        val requestedSeconds = planTime.iso
                            ?.let { runCatching { java.time.ZonedDateTime.parse(it) }.getOrNull() }
                            ?.let { TransitPlanning.secondsOfDay(it) }
                            ?: TransitPlanning.secondsOfDay(java.time.ZonedDateTime.now(ISRAEL_ZONE))
                        val ranked = online?.let {
                            com.future.navigation.data.transit.TransitRanking.rank(it, requestedSeconds, planTime.arriveBy)
                        }
                        val results = when {
                            !ranked.isNullOrEmpty() -> ranked
                            localCovers -> transitJourneyPlanner.findJourneys(origin, destination.location, System.currentTimeMillis() / 1000)
                            online == null -> {
                                _error.value = "אין חיבור לשירות התחבורה הציבורית – נסה שוב"
                                _serverError.value = true
                                return@launch
                            }
                            else -> online
                        }
                        android.util.Log.i("RouteOptions", "transit: online=${online?.size} localCovers=$localCovers -> ${results.size}")
                        // שכבת זמן אמת (SIRI) - "best effort" בעיצוב (ר' TransitRealtimeEnricher),
                        // אף פעם לא זורקת ולא יכולה להפוך תוצאה תקינה לריקה.
                        _itineraries.value = transitRealtimeEnricher.enrich(results)
                        if (results.isEmpty()) {
                            val p = _transitPrefs.value
                            // אוטובוס כבוי (או העדפה אחרת) הוא לרוב הסיבה - אומרים את זה במקום "אין מסלול"
                            val filtered = !p.bus || !p.rail || !p.lightRail ||
                                p.preference != com.future.navigation.data.transit.RoutePreference.RECOMMENDED
                            _error.value = if (filtered) tr("לא נמצא מסלול בהעדפות שבחרת - מקש 2 לשינוי ההעדפות")
                                else tr("לא נמצא מסלול תחבורה ציבורית ליעד הזה")
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = tr("אירעה שגיאה בחישוב המסלול")
            } finally {
                if (id == searchId) _loading.value = false
            }
        }
    }

    /** מנסה שוב את החיפוש האחרון (אחרי שגיאת שרת). */
    fun retry() {
        val (mode, destination, allow) = lastSearch ?: return
        search(mode, destination, allowCellularDownload = allow)
    }

    fun confirmCellularDownload() {
        _askCellularDownload.value = false
        val (mode, destination) = pendingSearch ?: return
        pendingSearch = null
        search(mode, destination, allowCellularDownload = true)
    }

    fun cancelCellularDownload() {
        _askCellularDownload.value = false
        pendingSearch = null
    }

    private fun transitDownloadNeeded(origin: LatLng, destination: LatLng): Boolean {
        val neededBbox = GtfsConfig.boundingBoxCovering(origin, destination)
        val alreadyImported = GtfsImportState.lastImportedBoundingBox(appContext)
        return !(alreadyImported != null && alreadyImported.covers(neededBbox))
    }

    /**
     * מוריד ומייבא אוטומטית רק את נתוני ה-GTFS הרלוונטיים למסלול המבוקש (מוצא+יעד),
     * בלי לדרוש עדכון ידני מראש - ר' GtfsSetupScreen שנשאר ככפתור גיבוי/ידני נפרד.
     * מדלג על ההורדה אם התיבה של המסלול הזה כבר מכוסה מייבוא קודם.
     */
    private suspend fun ensureTransitDataFor(origin: LatLng, destination: LatLng) {
        val neededBbox = GtfsConfig.boundingBoxCovering(origin, destination)
        val alreadyImported = GtfsImportState.lastImportedBoundingBox(appContext)
        if (alreadyImported != null && alreadyImported.covers(neededBbox)) return

        _downloadFraction.value = 0f
        // finally: הורדה שנקטעה (רשת, ביטול) השאירה קודם את פס ההורדה על המסך לתמיד
        try {
            gtfsImporter.importFeed(appContext, neededBbox).collect { progress ->
                _downloadFraction.value = if (progress.phase == ImportPhase.DOWNLOADING) progress.fraction else null
                if (progress.phase == ImportPhase.DONE) {
                    GtfsImportState.recordImportedBoundingBox(appContext, neededBbox)
                }
            }
        } finally {
            _downloadFraction.value = null
        }
    }
}
