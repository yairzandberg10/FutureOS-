package com.future.navigation.ui.navigate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.common.nearestDistanceMetersToPolyline
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.reports.ReportType
import com.future.navigation.data.reports.RoadReport
import com.future.navigation.data.reports.RoadReportsRepository
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
    val muted: Boolean = false,
    /** דיווח הדרך הקרוב ביותר שעל המסלול לפנינו (עד ~1.5 ק"מ), או null. */
    val nearestReport: RoadReport? = null,
    val nearestReportMeters: Double = 0.0,
    /** הודעה קצרה אחרי דיווח/הצבעה ("דווח: משטרה"), נמחקת אחרי כמה שניות. */
    val reportMessage: String? = null,
    /** מהירות נוכחית מה-GPS בקמ"ש, או null כשהמיקום לא כולל מהירות. */
    val speedKmh: Int? = null
) {
    val currentStep: Maneuver get() = route.steps.getOrElse(currentStepIndex) { route.steps.last() }
    val nextStep: Maneuver? get() = route.steps.getOrNull(currentStepIndex + 1)
}

class NavigateViewModel(
    private val appContext: Context,
    private val routingRepository: RoutingRepository,
    initialRoute: DrivingRoute,
    private val destination: LatLng,
    private val reportsRepository: RoadReportsRepository = RoadReportsRepository(),
    /** שם היעד לדף השיתוף ("בדרך לבית"). */
    private val destinationLabel: String = ""
) : ViewModel() {

    private var reports: List<RoadReport> = emptyList()
    private val announcedReports = HashSet<String>()
    private var reportMessageJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(
        NavigateUiState(
            route = initialRoute,
            currentLocation = LocationHelper.lastKnownLatLon(appContext)?.let { LatLng(it.first, it.second) },
            currentStepIndex = firstManeuverIndex(initialRoute),
            distanceToManeuverMeters = initialRoute.steps.firstOrNull()?.takeIf { firstManeuverIndex(initialRoute) == 1 }?.distanceMeters ?: 0.0,
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
                _uiState.value = _uiState.value.copy(
                    speedKmh = if (location.hasSpeed()) (location.speed * 3.6f).toInt() else null
                )
                onLocationUpdate(
                    LatLng(location.latitude, location.longitude),
                    if (location.hasAccuracy()) location.accuracy.toDouble() else null
                )
            }
            .launchIn(viewModelScope)

        startReportsPolling()

        // בדיקה תקופתית של עומס פתאומי בכביש שכבר עליו המשתמש (Dynamic Rerouting) -
        // בנוסף לסטייה פיזית מהמסלול שכבר מטופלת ב-onLocationUpdate/triggerReroute.
        // HERE כבר מחשב כל מסלול עם פקקים בזמן אמת (ר' HereApi.kt), אז די לבקש
        // מסלול טרי מהמיקום הנוכחי מדי פעם ולהשוות משך.
        viewModelScope.launch {
            // אחרי כל כשל ברשת ממתינים פי 2 יותר (עד פי 8) - בלי רשת לא פונים לשרת כל 90 שניות
            var failures = 0
            while (!_uiState.value.ended) {
                delay(TRAFFIC_RECHECK_INTERVAL_MS * (1L shl minOf(failures, 3)))
                val location = _uiState.value.currentLocation
                if (location != null && !_uiState.value.ended) {
                    failures = if (checkForFasterRouteDueToTraffic(location)) 0 else failures + 1
                }
            }
        }
    }

    /** מרענן את דיווחי הדרך מסביב כל חצי דקה; כשל ברשת פשוט משאיר את הרשימה הקודמת. */
    private fun startReportsPolling() {
        viewModelScope.launch {
            while (!_uiState.value.ended) {
                val here = _uiState.value.currentLocation
                if (here != null) {
                    reportsRepository.nearby(here)?.let { reports = it }
                    updateNearestReport(here)
                }
                delay(REPORTS_REFRESH_MS)
            }
        }
    }

    /** הדיווח הקרוב ביותר שנמצא על המסלול (עד 60 מ' ממנו) ובטווח התראה; מקריא פעם אחת לכל דיווח. */
    private fun updateNearestReport(here: LatLng) {
        val route = _uiState.value.route
        val candidate = reports
            .filter { nearestDistanceMetersToPolyline(it.location, route.polyline) <= REPORT_ON_ROUTE_METERS }
            .map { it to here.distanceMetersTo(it.location) }
            .filter { it.second <= REPORT_LOOKAHEAD_METERS }
            .minByOrNull { it.second }
        _uiState.value = _uiState.value.copy(nearestReport = candidate?.first, nearestReportMeters = candidate?.second ?: 0.0)
        if (candidate != null && candidate.second <= REPORT_ANNOUNCE_METERS && announcedReports.add(candidate.first.id)) {
            speak("${candidate.first.type.spoken} בעוד ${spokenDistance(candidate.second)}")
        }
    }

    private fun flashReportMessage(text: String) {
        reportMessageJob?.cancel()
        _uiState.value = _uiState.value.copy(reportMessage = text)
        reportMessageJob = viewModelScope.launch {
            delay(REPORT_MESSAGE_MS)
            _uiState.value = _uiState.value.copy(reportMessage = null)
        }
    }

    /** דיווח על המיקום הנוכחי (מקשים 1-5 במסך). */
    /** דיווחים ושיתוף נסיעה צריכים את שרת FutureOS. בלעדיו - הודעה ברורה במקום כשל שקט. */
    val serverFeatures: Boolean get() = com.future.navigation.data.backend.FutureServer.isAvailable

    fun explainNoServer() = flashReportMessage("דיווחים ושיתוף נסיעה יעבדו כשהשרת יחובר")

    fun report(type: ReportType) {
        val here = _uiState.value.currentLocation ?: return flashReportMessage("אין מיקום - לא ניתן לדווח")
        viewModelScope.launch {
            val ok = reportsRepository.submit(type, here)
            flashReportMessage(if (ok) "תודה! דווח: ${type.label}" else "הדיווח נכשל - נסו שוב")
            if (ok) reportsRepository.nearby(here)?.let { reports = it }
            updateNearestReport(here)
        }
    }

    /** הצבעה על הדיווח שמוצג כרגע: "עדיין שם" (6) או "לא שם" (7). */
    fun voteNearestReport(stillThere: Boolean) {
        val report = _uiState.value.nearestReport ?: return
        viewModelScope.launch {
            val ok = reportsRepository.vote(report.id, stillThere)
            flashReportMessage(if (ok) "תודה על העדכון" else "העדכון נכשל - נסו שוב")
            if (ok && !stillThere) {
                reports = reports.filter { it.id != report.id }
                _uiState.value.currentLocation?.let(::updateNearestReport)
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
        if (arrivedAtDestination && !state.ended) {
            speak("הגעת ליעד")
            endShare()
        } else {
            announce(newState)
            maybeUpdateShare(location)
        }

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
                // runCatching היה בולע CancellationException - ביטול ה-ViewModel לא נעצר
                val newRoute = try {
                    routingRepository.getDrivingRoute(from, destination).routeOrNull
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
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
            currentStepIndex = firstManeuverIndex(newRoute),
            distanceToManeuverMeters = newRoute.steps.firstOrNull()?.takeIf { firstManeuverIndex(newRoute) == 1 }?.distanceMeters ?: 0.0,
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
    /** מחזיר false רק כשהבדיקה נכשלה ברשת - אז הלולאה מחכה יותר לפני הניסיון הבא. */
    private suspend fun checkForFasterRouteDueToTraffic(from: LatLng): Boolean {
        if (rerouting || _uiState.value.ended) return true
        rerouting = true
        try {
            val candidate = (try {
                routingRepository.getDrivingRoute(from, destination).routeOrNull
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }) ?: return false
            val state = _uiState.value
            if (state.ended) return true
            if (candidate.durationSeconds < state.remainingDurationSeconds * TRAFFIC_REROUTE_IMPROVEMENT_FACTOR) {
                applyNewRoute(candidate)
                speak("נמצא מסלול מהיר יותר")
            }
            return true
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
        endShare()
    }

    // ---------- שיתוף נסיעה (מקש 3) ----------

    private var shareId: String? = null
    private var lastShareUpdateMs = 0L

    /** קישור השיתוף, לפתיחת הודעה למי שהמשתמש יבחר. null עד שנוצר. נצרך פעם אחת. */
    private val _shareLink = MutableStateFlow<String?>(null)
    val shareLink = _shareLink.asStateFlow()

    fun consumeShareLink() {
        _shareLink.value = null
    }

    private fun etaMs(): Long = System.currentTimeMillis() + (_uiState.value.remainingDurationSeconds * 1000).toLong()

    fun shareTrip() {
        val here = _uiState.value.currentLocation ?: return flashReportMessage("אין מיקום - לא ניתן לשתף")
        viewModelScope.launch {
            val body = com.future.navigation.data.backend.FutureServer.createShareJson(here, etaMs(), destinationLabel)
            val root = body?.let { runCatching { com.future.navigation.data.network.NetworkModule.json.parseToJsonElement(it) }.getOrNull() }
                as? kotlinx.serialization.json.JsonObject
            val id = (root?.get("id") as? kotlinx.serialization.json.JsonPrimitive)?.content
            val url = (root?.get("url") as? kotlinx.serialization.json.JsonPrimitive)?.content
            if (id == null || url == null) {
                flashReportMessage("השיתוף נכשל - נסו שוב")
                return@launch
            }
            shareId = id
            lastShareUpdateMs = System.currentTimeMillis()
            _shareLink.value = url
            flashReportMessage("השיתוף פעיל - שעת ההגעה מתעדכנת")
        }
    }

    /** עדכון מיקום ושעת הגעה לשיתוף הפעיל, לכל היותר פעם בדקה. */
    private fun maybeUpdateShare(location: LatLng) {
        val id = shareId ?: return
        val now = System.currentTimeMillis()
        if (now - lastShareUpdateMs < SHARE_UPDATE_MS) return
        lastShareUpdateMs = now
        viewModelScope.launch {
            com.future.navigation.data.backend.FutureServer.updateShare(id, location, etaMs())
        }
    }

    private fun endShare() {
        val id = shareId ?: return
        shareId = null
        // לא תלוי ב-viewModelScope: הוא מבוטל כשיוצאים מהמסך
        com.future.navigation.data.backend.FirebaseBackend.scope.launch {
            com.future.navigation.data.backend.FutureServer.updateShare(id, null, null, ended = true)
        }
    }

    companion object {
        /**
         * "יציאה לדרך" הוא נקודת ההתחלה עצמה, ולא הוראה שימושית - מתחילים מהתמרון
         * הראשון האמיתי ("בעוד 300 מ' פנה שמאלה").
         */
        fun firstManeuverIndex(route: DrivingRoute): Int =
            if (route.steps.size > 1 && route.steps[0].type == "depart") 1 else 0

        private const val ARRIVAL_THRESHOLD_METERS = 25.0
        private const val REROUTE_THRESHOLD_METERS = 40.0
        private const val TRAFFIC_RECHECK_INTERVAL_MS = 90_000L
        private const val TRAFFIC_REROUTE_IMPROVEMENT_FACTOR = 0.9
        private const val REROUTE_COOLDOWN_MS = 10_000L
        private const val NEAR_MANEUVER_METERS = 120.0
        private const val REPORTS_REFRESH_MS = 30_000L
        private const val REPORT_ON_ROUTE_METERS = 60.0
        private const val REPORT_LOOKAHEAD_METERS = 1500.0
        private const val REPORT_ANNOUNCE_METERS = 600.0
        private const val REPORT_MESSAGE_MS = 3_000L
        private const val SHARE_UPDATE_MS = 60_000L
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
