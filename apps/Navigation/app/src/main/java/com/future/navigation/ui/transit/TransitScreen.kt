package com.future.navigation.ui.transit

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.FocusableItem

@Composable
fun TransitScreen(itinerary: TransitItinerary, onBack: () -> Unit) {
    var expandedLeg by remember { mutableStateOf(-1) }
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
        if (targets.isEmpty()) return@LaunchedEffect
        val alerted = HashSet<Int>()
        runCatching {
            com.future.navigation.data.location.LocationHelper.observeLocation(context, minIntervalMs = 5_000L, minDistanceMeters = 20f)
                .collect { location ->
                    val here = com.future.navigation.data.common.LatLng(location.latitude, location.longitude)
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

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.transit_itinerary_title),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )

        Surface(
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

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            itemsIndexed(itinerary.legs) { index, leg ->
                LegRow(
                    leg = leg,
                    isLast = index == itinerary.legs.size - 1,
                    expanded = expandedLeg == index,
                    onToggleExpand = { expandedLeg = if (expandedLeg == index) -1 else index },
                    focusRequester = if (index == firstRideIndex) firstRideFocusRequester else null
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun LegRow(leg: TransitLeg, isLast: Boolean, expanded: Boolean, onToggleExpand: () -> Unit, focusRequester: FocusRequester? = null) {
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
                Text(tr("הליכה"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    tr("%d דק׳ · %d מ׳").format((leg.walkDurationSeconds ?: 0) / 60, (leg.walkDistanceMeters ?: 0.0).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                FocusableItem(
                    onClick = onToggleExpand,
                    accentColor = MaterialTheme.colorScheme.primary,
                    contentPadding = 0.dp,
                    focusRequester = focusRequester
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
                    }
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
