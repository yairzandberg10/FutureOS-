package com.future.navigation.ui.transit

import androidx.activity.compose.BackHandler
import com.future.sharednav.i18n.tr
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme

import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg
import com.future.navigation.data.transit.TransitPlanning
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.FocusableItem

/**
 * onWalkTo: OK על רגל הליכה פותח ניווט ברגל אל נקודת הסיום שלה (תחנת העלייה,
 * מעבר בין תחנות, או היעד). ברירת המחדל לא עושה כלום, כדי שהקריאה הקיימת תעבוד.
 */
@Composable
fun TransitScreen(
    itinerary: TransitItinerary,
    onBack: () -> Unit,
    onWalkTo: (name: String, location: com.future.navigation.data.common.LatLng) -> Unit = { _, _ -> },
    onOpenTrip: (tripId: String, line: String, boardingStop: String) -> Unit = { _, _, _ -> },
) {
    var expandedLeg by remember { mutableStateOf(-1) }
    // מצב LIVE: ניווט בשלבים (מקש 1). המיקום החי משותף לשני המצבים (מרחק לתחנה).
    var live by remember { mutableStateOf(false) }
    var myLocation by remember { mutableStateOf<com.future.navigation.data.common.LatLng?>(null) }
    val firstRideFocusRequester = remember { FocusRequester() }
    val firstRideIndex = remember(itinerary) { itinerary.legs.indexOfFirst { it.type == LegType.RIDE } }

    // פוקוס D-pad התחלתי על קטע הנסיעה הראשון (WALK אינו לחיץ) - בלי זה נחיתה
    // על המסך משאירה אותו בלי שום פריט מודגש.
    LaunchedEffect(Unit) { if (firstRideIndex >= 0) firstRideFocusRequester.requestFocus() }

    // התראת "רדו בתחנה הבאה" (בסגנון Moovit): כל עוד המסך פתוח עוקבים אחרי המיקום,
    // ומתריעים בקול וברטט כשמתקרבים לתחנת הירידה של כל קטע נסיעה.
    val context = androidx.compose.ui.platform.LocalContext.current
    var getOffAlert by remember { mutableStateOf<String?>(null) }
    val tts = remember { arrayOfNulls<android.speech.tts.TextToSpeech>(1) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        tts[0] = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) tts[0]?.setLanguage(java.util.Locale.forLanguageTag("he-IL"))
        }
        onDispose {
            tts[0]?.shutdown()
            tts[0] = null
        }
    }
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(itinerary) {
        val targets = itinerary.legs.filter { it.type == LegType.RIDE && it.toLocation != null }
        val alerted = HashSet<Int>()
        runCatching {
            com.future.navigation.data.location.LocationHelper.observeLocation(context, minIntervalMs = 5_000L, minDistanceMeters = 20f)
                .collect { location ->
                    val here = com.future.navigation.data.common.LatLng(location.latitude, location.longitude)
                    myLocation = here
                    targets.forEachIndexed { index, leg ->
                        val distance = here.distanceMetersTo(leg.toLocation!!)
                        if (distance <= GET_OFF_ALERT_METERS && alerted.add(index)) {
                            val text = tr("רדו בתחנה הבאה: {0}", leg.toStopName)
                            getOffAlert = text
                            tts[0]?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "getoff")
                            vibrate(context)
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (com.future.sharednav.nav.digitForKey(event.key)) {
                "1" -> if (!live) { live = true; true } else false
                "9" -> if (live) { shareItinerary(context, itinerary); true } else false
                else -> false
            }
        }
    ) {
        ScreenTopBar(
            title = stringResource(R.string.transit_itinerary_title),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )

        // BACK ביציאה ממצב LIVE חוזר לרשימה (נרשם אחרי ScreenTopBar, כך שהוא קודם)
        BackHandler(enabled = live) { live = false }

        if (live) Unit else Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = FutureShapes.lg,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(stringResource(R.string.total_time), tr("%d דק׳").format(itinerary.totalDurationSeconds / 60))
                Stat(stringResource(R.string.arrival_time), secondsToClock(itinerary.arrivalSeconds))
                Stat(stringResource(R.string.walk_time), tr("%d דק׳").format(itinerary.legs.filter { it.type == LegType.WALK }.sumOf { it.walkDurationSeconds ?: 0 } / 60))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        val hasRealtimeLeg = remember(itinerary) { itinerary.legs.any { it.isRealtime } }
        Text(
            text = stringResource(if (hasRealtimeLeg) R.string.schedule_disclaimer_partial_realtime else R.string.schedule_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        getOffAlert?.let { alert ->
            Text(
                text = alert,
                color = LocalFutureTheme.current.dangerColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (!live) {
            Text(
                text = tr("1: ניווט LIVE בשלבים"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (live) {
            LiveSteps(itinerary = itinerary, here = myLocation, modifier = Modifier.weight(1f))
        } else LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            itemsIndexed(itinerary.legs) { index, leg ->
                LegRow(
                    leg = leg,
                    isLast = index == itinerary.legs.size - 1,
                    expanded = expandedLeg == index,
                    onToggleExpand = { expandedLeg = if (expandedLeg == index) -1 else index },
                    focusRequester = if (index == firstRideIndex) firstRideFocusRequester else null,
                    onWalk = leg.toLocation?.let { target ->
                        { onWalkTo(leg.toStopName, target) }
                    },
                    onTrip = leg.tripId?.let { id ->
                        { onOpenTrip(id, leg.routeShortName.orEmpty(), leg.fromStopName) }
                    },
                )
            }
        }
    }
}

/**
 * ניווט LIVE: שלב אחד בכל רגע (רגל = הליכה לנקודה, קטע נסיעה = עלייה + ירידה).
 * שמאל = הבא, ימין = הקודם (כיוון הקריאה של העברית). מרחק חי מ-myLocation.
 * לוח היציאות הבאות נטען פעם בשלב, לא בכל עדכון מיקום.
 */
@Composable
private fun LiveSteps(itinerary: TransitItinerary, here: com.future.navigation.data.common.LatLng?, modifier: Modifier = Modifier) {
    val theme = LocalFutureTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val legs = itinerary.legs
    var step by remember { mutableStateOf(0) }
    var boardInfo by remember { mutableStateOf<List<String>>(emptyList()) }
    val focus = remember { FocusRequester() }
    val leg = legs.getOrNull(step)

    LaunchedEffect(step) { runCatching { focus.requestFocus() } }
    LaunchedEffect(step) {
        boardInfo = emptyList()
        val ride = leg
        val from = ride?.fromLocation
        if (ride != null && ride.type == LegType.RIDE && from != null) {
            val board = runCatching { com.future.navigation.data.gtfs.ServerDepartures().boardAt(from, ride.fromStopName) }.getOrNull()
            boardInfo = board?.departures
                .orEmpty()
                .filter { it.line == ride.routeShortName }
                .take(3)
                .map { tr("%s בעוד %d דק׳").format(it.headsign, it.minutesAway) }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> { step = (step + 1).coerceAtMost(legs.size - 1); true }
                Key.DirectionRight -> { step = (step - 1).coerceAtLeast(0); true }
                else -> false
            }
        },
    ) {
        Text(
            text = tr("שלב %d מתוך %d").format(step + 1, legs.size),
            style = MaterialTheme.typography.labelMedium,
            color = theme.mutedTextColor,
        )
        Spacer(modifier = Modifier.height(FutureDimens.spacingXs))
        FocusableItem(
            onClick = { step = (step + 1).coerceAtMost(legs.size - 1) },
            accentColor = MaterialTheme.colorScheme.primary,
            focusRequester = focus,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(FutureDimens.spacingSm)) {
                if (leg != null) {
                    if (leg.type == LegType.WALK) {
                        Text(tr("לך אל %s").format(leg.toStopName), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            tr("%d דק׳ · %d מ׳").format((leg.walkDurationSeconds ?: 0) / 60, (leg.walkDistanceMeters ?: 0.0).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val target = leg.toLocation
                        if (here != null && target != null) {
                            Text(
                                tr("מרחק עכשיו: %d מ׳").format(here.distanceMetersTo(target).toInt()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Text(
                            tr("עלו על קו %s לכיוון %s").format(leg.routeShortName.orEmpty(), leg.routeLongName.orEmpty()),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        leg.departureSeconds?.let {
                            Text(
                                tr("יציאה %s מתחנה %s").format(TransitPlanning.clock(it), leg.fromStopName),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        boardInfo.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall, color = theme.mutedTextColor)
                        }
                        Spacer(modifier = Modifier.height(FutureDimens.spacingXs))
                        Text(
                            tr("לאחר %d תחנות · רדו בתחנת %s בשעה %s").format(
                                leg.intermediateStopNames.size + 1,
                                leg.toStopName,
                                leg.arrivalSeconds?.let { TransitPlanning.clock(it) }.orEmpty(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
        Text(
            text = tr("← הבא · → הקודם · 9 שיתוף ב-SMS · BACK חזרה לרשימה"),
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedTextColor,
        )
    }
}

/** שליחת סיכום המסלול ב-SMS. פותח את מסך ההודעה בלבד - השליחה בידי המשתמש. */
private fun shareItinerary(context: android.content.Context, itinerary: TransitItinerary) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:"))
        .putExtra("sms_body", TransitPlanning.shareText(itinerary))
    runCatching { context.startActivity(intent) }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun LegRow(
    leg: TransitLeg,
    isLast: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    focusRequester: FocusRequester? = null,
    onWalk: (() -> Unit)? = null,
    onTrip: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(38.dp)) {
            val badgeColor = when (leg.type) {
                LegType.WALK -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                // מספר הקו אינו בחירה ולא פוקוס, ולכן לא בצבע ההדגשה: הוא "מודגש"
                // בהיפוך - צבע הטקסט כרקע.
                LegType.RIDE -> MaterialTheme.colorScheme.onSurface
            }
            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = badgeColor) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (leg.type == LegType.RIDE) (leg.routeShortName?.takeIf { it.isNotBlank() } ?: "?") else "",
                        color = if (leg.type == LegType.RIDE) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = 20.dp)) {
            if (leg.type == LegType.WALK) {
                val walkText: @Composable () -> Unit = {
                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Text(tr("הליכה"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            tr("%d דק׳ · %d מ׳").format((leg.walkDurationSeconds ?: 0) / 60, (leg.walkDistanceMeters ?: 0.0).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (onWalk != null) {
                            Text(
                                tr("ניווט ברגל אל {0} (OK)", leg.toStopName),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (onWalk != null) {
                    // רגל הליכה שיש לה יעד: לחיצה (OK) פותחת ניווט ברגל אליו
                    FocusableItem(
                        onClick = onWalk,
                        accentColor = MaterialTheme.colorScheme.primary,
                        contentPadding = 0.dp,
                    ) { walkText() }
                } else {
                    walkText()
                }
            } else {
                FocusableItem(
                    onClick = onToggleExpand,
                    accentColor = MaterialTheme.colorScheme.primary,
                    contentPadding = 0.dp,
                    focusRequester = focusRequester,
                    // מקש 5 על קטע נסיעה פותח את לוח הנסיעה המלא של הקו (OK מרחיב תחנות)
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (onTrip != null && event.type == KeyEventType.KeyDown && event.key == Key.Five) {
                            onTrip()
                            true
                        } else {
                            false
                        }
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(leg.routeLongName?.takeIf { it.isNotBlank() } ?: leg.routeShortName ?: tr("קו"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            if (leg.isRealtime) {
                                Spacer(modifier = Modifier.width(8.dp))
                                RealtimeBadge(delaySeconds = leg.delaySeconds)
                            }
                        }
                        Text(
                            tr("מ{0} · אל {1}", leg.fromStopName, leg.toStopName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        // מתי האוטובוס יוצא מתחנת העלייה ומתי מגיעים לתחנת הירידה
                        val times = listOfNotNull(
                            leg.departureSeconds?.let { "עולים " + secondsToClock(it) },
                            leg.arrivalSeconds?.let { "יורדים " + secondsToClock(it) },
                            tr("%d תחנות").format(leg.intermediateStopNames.size + 2),
                        )
                        if (times.isNotEmpty()) {
                            Text(
                                times.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (onTrip != null) {
                    Text(
                        tr("5: לוח הנסיעה המלא של הקו"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                if (expanded && leg.intermediateStopNames.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
                        leg.intermediateStopNames.forEach { stopName ->
                            Text(
                                "· $stopName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealtimeBadge(delaySeconds: Int?) {
    val delayMinutes = (delaySeconds ?: 0) / 60
    val label = when {
        delaySeconds == null || kotlin.math.abs(delaySeconds) < 60 -> stringResource(R.string.realtime_on_time)
        delayMinutes > 0 -> stringResource(R.string.realtime_delayed, delayMinutes)
        else -> stringResource(R.string.realtime_early, -delayMinutes)
    }
    val success = LocalFutureTheme.current.successColor
    Surface(shape = FutureShapes.xs, color = success.copy(alpha = 0.15f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = FutureDimens.spacingXxs)) {
            Box(modifier = Modifier.size(6.dp).background(success, CircleShape))
            Spacer(modifier = Modifier.width(FutureDimens.spacingXs))
            Text(label, style = MaterialTheme.typography.labelSmall, color = success, fontWeight = FontWeight.Bold)
        }
    }
}

private fun secondsToClock(seconds: Int): String {
    val h = (seconds / 3600) % 24
    val m = (seconds % 3600) / 60
    return "%02d:%02d".format(h, m)
}

/** כמה מטרים לפני תחנת הירידה מתריעים - בערך תחנה אחת קודם בעיר. */
private const val GET_OFF_ALERT_METERS = 500.0

private fun vibrate(context: android.content.Context) {
    runCatching {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    }
}
