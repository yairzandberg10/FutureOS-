package com.future.navigation.ui.routes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.gtfs.GtfsConfig
import com.future.navigation.data.gtfs.GtfsImportState
import com.future.navigation.data.gtfs.GtfsImporter
import com.future.navigation.data.gtfs.ImportPhase
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitJourneyPlanner
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.routing.DrivingRoute
import com.future.navigation.data.routing.RoutingRepository
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
    private val gtfsImporter: GtfsImporter
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    /** לא null בזמן הורדה/ייבוא אוטומטי של נתוני התחבורה - כדי שהמסך יראה "מוריד נתוני תחבורה" ולא רק "מחשב מסלול". */
    private val _downloadFraction = MutableStateFlow<Float?>(null)
    val downloadFraction = _downloadFraction.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _drivingRoute = MutableStateFlow<DrivingRoute?>(null)
    val drivingRoute = _drivingRoute.asStateFlow()

    private val _itineraries = MutableStateFlow<List<TransitItinerary>>(emptyList())
    val itineraries = _itineraries.asStateFlow()

    /** עבודת החיפוש הפעילה - מבוטלת אם מתבקש חיפוש חדש לפני שהקודם הסתיים,
     * כדי שתשובה איטית/ישנה לא תדרוס תוצאה טרייה יותר. */
    private var searchJob: Job? = null

    fun search(mode: TravelMode, destination: Destination) {
        val origin = LocationHelper.lastKnownLatLon(appContext)?.let { LatLng(it.first, it.second) }
        if (origin == null) {
            _error.value = "לא ניתן לאתר את המיקום הנוכחי"
            return
        }

        searchJob?.cancel()
        _loading.value = true
        _error.value = null
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
                        _itineraries.value = results
                        if (results.isEmpty()) _error.value = "לא נמצא מסלול תחבורה ציבורית ליעד הזה"
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "אירעה שגיאה בחישוב המסלול"
            } finally {
                _loading.value = false
            }
        }
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
        gtfsImporter.importFeed(appContext, neededBbox).collect { progress ->
            _downloadFraction.value = if (progress.phase == ImportPhase.DOWNLOADING) progress.fraction else null
            if (progress.phase == ImportPhase.DONE) {
                GtfsImportState.recordImportedBoundingBox(appContext, neededBbox)
            }
        }
        _downloadFraction.value = null
    }
}
