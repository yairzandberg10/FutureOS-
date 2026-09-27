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
    // אחרי חישוב מסלול שנכשל (אין רשת) לא מנסים שוב בכל עדכון מיקום - קודם
    // נשלחה בקשה כל שתי שניות לאורך כל הנסיעה בלי קליטה.
    private var lastRerouteAttemptMs = 0L

    /**
     * הנחיה קולית. קודם כפתור ההשתקה החליף רק את האייקון - לא הייתה הנחיה
     * קולית בכלל, ומי שנוהג לא יכול להסתכל על המסך. מנוע ה-TTS של המערכת
     * בעברית; אם אין קול עברי מותקן, פשוט שקט.
     */
    private var tts: android.speech.tts.TextToSpeech? = null
    private var ttsReady = false
    private var announcedStep = -1
    private var announcedNearStep = -1

    init {
        tts = android.speech.tts.TextToSpeech(appContext) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(java.util.Locale.forLanguageTag("he-IL"))
                ttsReady = result != null && result >= android.speech.tts.TextToSpeech.LANG_AVAILABLE
            }
        }

        LocationHelper.observeLocation(appContext)
            .onEach { location ->
                onLocationUpdate(
                    LatLng(location.latitude, location.longitude),
                    if (location.hasAccuracy()) location.accuracy.toDouble() else null
                )
            }
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

    private fun speak(text: String) {
        if (!ttsReady || _uiState.value.muted) return
        tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "nav")
    }

    private fun announce(state: NavigateUiState) {
        if (state.ended) return
        val step = state.currentStep
        if (state.currentStepIndex != announcedStep) {
            announcedStep = state.currentStepIndex
            announcedNearStep = -1
            speak("בעוד ${spokenDistance(state.distanceToManeuverMeters)}, ${maneuverPhrase(step)}")
        } else if (state.distanceToManeuverMeters < NEAR_MANEUVER_METERS && announcedNearStep != state.currentStepIndex) {
            announcedNearStep = state.currentStepIndex
            speak(maneuverPhrase(step))
        }
    }

    private fun onLocationUpdate(location: LatLng, accuracyMeters: Double? = null) {
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
        // אורך של מקטע הוא מהתמרון שלו עד הבא - המקטע שמתחיל בתמרון הקרוב עוד
        // לפנינו. קודם הוא לא נספר, והמרחק/הזמן שנותרו יצאו קצרים במקטע שלם.
        val remainingDistance = distanceToManeuver + route.steps.drop(stepIndex).sumOf { it.distanceMeters }
        val avgSpeed = if (route.durationSeconds > 0) route.distanceMeters / route.durationSeconds else 10.0
        val remainingDuration = if (avgSpeed > 0) remainingDistance / avgSpeed else 0.0

        val deviation = nearestDistanceMetersToPolyline(location, route.polyline)
        val arrivedAtDestination = location.distanceMetersTo(destination) < ARRIVAL_THRESHOLD_METERS

        val newState = state.copy(
            currentLocation = location,
            currentStepIndex = stepIndex,
            distanceToManeuverMeters = distanceToManeuver,
            remainingDistanceMeters = remainingDistance,
            remainingDurationSeconds = remainingDuration,
            ended = state.ended || arrivedAtDestination
        )
        _uiState.value = newState
        if (arrivedAtDestination && !state.ended) speak("הגעת ליעד") else announce(newState)

        // קיבוע לא מדויק (בין בניינים גבוהים, במנהרה) "סוטה" עשרות מטרים מהכביש
        // בלי שהרכב זז ממנו - לא מחשבים מסלול חדש לפיו.
        val reliable = accuracyMeters == null || accuracyMeters <= REROUTE_THRESHOLD_METERS
        val cooledDown = System.currentTimeMillis() - lastRerouteAttemptMs > REROUTE_COOLDOWN_MS
        if (!rerouting && reliable && cooledDown && deviation > REROUTE_THRESHOLD_METERS && !arrivedAtDestination) {
            triggerReroute(location)
        }
    }

    private fun triggerReroute(from: LatLng) {
        rerouting = true
        lastRerouteAttemptMs = System.currentTimeMillis()
        viewModelScope.launch {
            // finally: חריגה בחישוב השאירה קודם rerouting=true לתמיד, ומאז אף
            // סטייה מהמסלול לא חושבה מחדש עד סוף הנסיעה
            try {
                val newRoute = runCatching { routingRepository.getDrivingRoute(from, destination) }.getOrNull()
                if (newRoute != null && !_uiState.value.ended) {
                    applyNewRoute(newRoute)
                    speak("מחשב מסלול מחדש")
                }
            } finally {
                rerouting = false
            }
        }
    }

    private fun applyNewRoute(newRoute: DrivingRoute) {
        announcedStep = -1
        _uiState.value = _uiState.value.copy(
            route = newRoute,
            currentStepIndex = 0,
            remainingDistanceMeters = newRoute.distanceMeters,
            remainingDurationSeconds = newRoute.durationSeconds
        )
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
            val candidate = runCatching { routingRepository.getDrivingRoute(from, destination) }.getOrNull() ?: return
            val state = _uiState.value
            if (state.ended) return
            if (candidate.durationSeconds < state.remainingDurationSeconds * TRAFFIC_REROUTE_IMPROVEMENT_FACTOR) {
                applyNewRoute(candidate)
                speak("נמצא מסלול מהיר יותר")
            }
        } finally {
            rerouting = false
        }
    }

    fun toggleMute() {
        _uiState.value = _uiState.value.copy(muted = !_uiState.value.muted)
        if (_uiState.value.muted) tts?.stop()
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    fun endNavigation() {
        _uiState.value = _uiState.value.copy(ended = true)
    }

    companion object {
        private const val ARRIVAL_THRESHOLD_METERS = 25.0
        private const val REROUTE_THRESHOLD_METERS = 40.0
        private const val TRAFFIC_RECHECK_INTERVAL_MS = 90_000L
        private const val TRAFFIC_REROUTE_IMPROVEMENT_FACTOR = 0.9
        private const val REROUTE_COOLDOWN_MS = 10_000L
        private const val NEAR_MANEUVER_METERS = 120.0
    }
}

private fun spokenDistance(meters: Double): String = when {
    meters >= 1000 -> "%.1f קילומטר".format(meters / 1000).replace(".0 ", " ")
    meters >= 100 -> "${(meters / 50).toInt() * 50} מטר"
    else -> "${meters.toInt().coerceAtLeast(10) / 10 * 10} מטר"
}

private fun maneuverPhrase(step: Maneuver): String {
    val base = maneuverText(step)
    return step.streetName.takeIf { it.isNotBlank() && step.type != "arrive" }?.let { "$base ל$it" } ?: base
}
