package com.future.navigation.ui.navigate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.common.nearestDistanceMetersToPolyline
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.routing.DrivingRoute
import com.future.navigation.data.routing.Maneuver
import com.future.navigation.data.routing.RoutingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class NavigateUiState(
    val route: DrivingRoute,
    val currentLocation: LatLng?,
    val currentStepIndex: Int,
    val distanceToManeuverMeters: Double,
    val remainingDistanceMeters: Double,
    val remainingDurationSeconds: Double,
    val ended: Boolean = false,
    val muted: Boolean = false
) {
    val currentStep: Maneuver get() = route.steps.getOrElse(currentStepIndex) { route.steps.last() }
    val nextStep: Maneuver? get() = route.steps.getOrNull(currentStepIndex + 1)
}

class NavigateViewModel(
    private val appContext: Context,
    private val routingRepository: RoutingRepository,
    initialRoute: DrivingRoute,
    private val destination: LatLng
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NavigateUiState(
            route = initialRoute,
            currentLocation = LocationHelper.lastKnownLatLon(appContext)?.let { LatLng(it.first, it.second) },
            currentStepIndex = 0,
            distanceToManeuverMeters = 0.0,
            remainingDistanceMeters = initialRoute.distanceMeters,
            remainingDurationSeconds = initialRoute.durationSeconds
        )
    )
    val uiState = _uiState.asStateFlow()

    private var rerouting = false

    init {
        LocationHelper.observeLocation(appContext)
            .onEach { location -> onLocationUpdate(LatLng(location.latitude, location.longitude)) }
            .launchIn(viewModelScope)

        // בדיקה תקופתית של עומס פתאומי בכביש שכבר עליו המשתמש (Dynamic Rerouting) -
        // בנוסף לסטייה פיזית מהמסלול שכבר מטופלת ב-onLocationUpdate/triggerReroute.
        // HERE כבר מחשב כל מסלול עם פקקים בזמן אמת (ר' HereApi.kt), אז די לבקש
        // מסלול טרי מהמיקום הנוכחי מדי פעם ולהשוות משך.
        viewModelScope.launch {
            while (!_uiState.value.ended) {
                delay(TRAFFIC_RECHECK_INTERVAL_MS)
                val location = _uiState.value.currentLocation
                if (location != null && !_uiState.value.ended) {
                    checkForFasterRouteDueToTraffic(location)
                }
            }
        }
    }

    private fun onLocationUpdate(location: LatLng) {
        val state = _uiState.value
        if (state.ended) return

        val route = state.route
        var stepIndex = state.currentStepIndex
        // מתקדם למקטע הבא כשמתקרבים מספיק לנקודת התמרון של המקטע הנוכחי.
        while (stepIndex < route.steps.size - 1 &&
            location.distanceMetersTo(route.steps[stepIndex].location) < ARRIVAL_THRESHOLD_METERS
        ) {
            stepIndex++
        }

        val distanceToManeuver = location.distanceMetersTo(route.steps[stepIndex].location)
        val remainingDistance = distanceToManeuver + route.steps.drop(stepIndex + 1).sumOf { it.distanceMeters }
        val avgSpeed = if (route.durationSeconds > 0) route.distanceMeters / route.durationSeconds else 10.0
        val remainingDuration = if (avgSpeed > 0) remainingDistance / avgSpeed else 0.0

        val deviation = nearestDistanceMetersToPolyline(location, route.polyline)
        val arrivedAtDestination = location.distanceMetersTo(destination) < ARRIVAL_THRESHOLD_METERS

        _uiState.value = state.copy(
            currentLocation = location,
            currentStepIndex = stepIndex,
            distanceToManeuverMeters = distanceToManeuver,
            remainingDistanceMeters = remainingDistance,
            remainingDurationSeconds = remainingDuration,
            ended = state.ended || arrivedAtDestination
        )

        if (!rerouting && deviation > REROUTE_THRESHOLD_METERS && !arrivedAtDestination) {
            triggerReroute(location)
        }
    }

    private fun triggerReroute(from: LatLng) {
        rerouting = true
        viewModelScope.launch {
            val newRoute = routingRepository.getDrivingRoute(from, destination)
            if (newRoute != null) {
                _uiState.value = _uiState.value.copy(
                    route = newRoute,
                    currentStepIndex = 0,
                    remainingDistanceMeters = newRoute.distanceMeters,
                    remainingDurationSeconds = newRoute.durationSeconds
                )
            }
            rerouting = false
        }
    }

    /**
     * גרסה "עדינה" יותר של triggerReroute: לא מוחלפת בכל בדיקה, רק כשהמסלול
     * הטרי מהיר משמעותית (לא רק תנודת ETA זניחה) ממה שנשאר על המסלול הנוכחי -
     * כדי שעומס שהצטבר פתאום יגרום להחלפה בפועל, בלי "קפיצות" מסלול על הבדלים
     * של כמה שניות.
     */
    private suspend fun checkForFasterRouteDueToTraffic(from: LatLng) {
        if (rerouting || _uiState.value.ended) return
        rerouting = true
        try {
            val candidate = routingRepository.getDrivingRoute(from, destination) ?: return
            val state = _uiState.value
            if (state.ended) return
            if (candidate.durationSeconds < state.remainingDurationSeconds * TRAFFIC_REROUTE_IMPROVEMENT_FACTOR) {
                _uiState.value = state.copy(
                    route = candidate,
                    currentStepIndex = 0,
                    remainingDistanceMeters = candidate.distanceMeters,
                    remainingDurationSeconds = candidate.durationSeconds
                )
            }
        } finally {
            rerouting = false
        }
    }

    fun toggleMute() {
        _uiState.value = _uiState.value.copy(muted = !_uiState.value.muted)
    }

    fun endNavigation() {
        _uiState.value = _uiState.value.copy(ended = true)
    }

    companion object {
        private const val ARRIVAL_THRESHOLD_METERS = 25.0
        private const val REROUTE_THRESHOLD_METERS = 40.0
        private const val TRAFFIC_RECHECK_INTERVAL_MS = 90_000L
        private const val TRAFFIC_REROUTE_IMPROVEMENT_FACTOR = 0.9
    }
}
