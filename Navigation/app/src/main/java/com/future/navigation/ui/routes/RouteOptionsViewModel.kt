package com.future.navigation.ui.routes

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
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.routing.DrivingRoute
import com.future.navigation.data.routing.RoutingRepository
import com.future.navigation.data.siri.TransitRealtimeEnricher
import com.future.navigation.ui.navigation.Destination
import com.future.navigation.ui.navigation.TravelMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RouteOptionsViewModel(
    private val appContext: Context,
    private val routingRepository: RoutingRepository,
    private val transitJourneyPlanner: TransitJourneyPlanner,
    private val gtfsImporter: GtfsImporter,
    private val transitRealtimeEnricher: TransitRealtimeEnricher
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    /** לא null בזמן הורדה/ייבוא אוטומטי של נתוני התחבורה - כדי שהמסך יראה "מוריד נתוני תחבורה" ולא רק "מחשב מסלול". */
    private val _downloadFraction = MutableStateFlow<Float?>(null)
    val downloadFraction = _downloadFraction.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** הורדה של נתוני התחבורה (~130MB) ברשת סלולרית מחכה לאישור המשתמש (F09). */
    private val _askCellularDownload = MutableStateFlow(false)
    val askCellularDownload = _askCellularDownload.asStateFlow()
    private var pendingSearch: Pair<TravelMode, Destination>? = null

    private val _drivingRoute = MutableStateFlow<DrivingRoute?>(null)
    val drivingRoute = _drivingRoute.asStateFlow()

    private val _itineraries = MutableStateFlow<List<TransitItinerary>>(emptyList())
    val itineraries = _itineraries.asStateFlow()

    /** עבודת החיפוש הפעילה - מבוטלת אם מתבקש חיפוש חדש לפני שהקודם הסתיים,
     * כדי שתשובה איטית/ישנה לא תדרוס תוצאה טרייה יותר. */
    private var searchJob: Job? = null
    /** מזהה החיפוש האחרון - רק החיפוש האחרון מכבה את מצב הטעינה (ביטול של קודם לא מכבה חדש). */
    private var searchId = 0

    fun search(mode: TravelMode, destination: Destination, allowCellularDownload: Boolean = false) {
        val id = ++searchId
        searchJob?.cancel()
        // תוצאות של יעד קודם לא נשארות על המסך - גם כשאין מיקום (אחרת מסלול ישן נפתח על יעד חדש)
        _drivingRoute.value = null
        _itineraries.value = emptyList()
        _error.value = null
        val origin = LocationHelper.lastKnownLatLon(appContext)?.let { LatLng(it.first, it.second) }
        if (origin == null) {
            _loading.value = false
            _error.value = "לא ניתן לאתר את המיקום הנוכחי"
            return
        }

        // הורדה גדולה ברשת סלולרית - קודם שואלים (ברירת המחדל: לא להוריד).
        if (mode == TravelMode.TRANSIT && !allowCellularDownload &&
            transitDownloadNeeded(origin, destination.location) && NetworkPolicy.isMetered(appContext)
        ) {
            pendingSearch = mode to destination
            _loading.value = false
            _askCellularDownload.value = true
            return
        }

        _loading.value = true
        searchJob = viewModelScope.launch {
            try {
                when (mode) {
                    TravelMode.DRIVE -> {
                        val route = routingRepository.getDrivingRoute(origin, destination.location)
                        _drivingRoute.value = route
                        if (route == null) _error.value = "לא נמצא מסלול נסיעה"
                    }
                    TravelMode.TRANSIT -> {
                        ensureTransitDataFor(origin, destination.location)
                        val now = System.currentTimeMillis() / 1000
                        val results = transitJourneyPlanner.findJourneys(origin, destination.location, now)
                        // שכבת זמן אמת (SIRI) - "best effort" בעיצוב (ר' TransitRealtimeEnricher),
                        // אף פעם לא זורקת ולא יכולה להפוך תוצאה תקינה לריקה.
                        _itineraries.value = transitRealtimeEnricher.enrich(results)
                        if (results.isEmpty()) _error.value = "לא נמצא מסלול תחבורה ציבורית ליעד הזה"
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "אירעה שגיאה בחישוב המסלול"
            } finally {
                if (id == searchId) _loading.value = false
            }
        }
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
