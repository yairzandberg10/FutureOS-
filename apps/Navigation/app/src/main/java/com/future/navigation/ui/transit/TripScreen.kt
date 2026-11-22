package com.future.navigation.ui.transit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.lines.LineDeparture
import com.future.navigation.data.lines.LineHistory
import com.future.navigation.data.lines.LineLogic
import com.future.navigation.data.lines.LineTimes
import com.future.navigation.data.lines.SavedLine
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.transit.TripSchedule
import com.future.navigation.data.transit.TripScheduleSource
import com.future.navigation.data.transit.TripStop
import com.future.navigation.ui.lines.LineDailyScreen
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.KeypadLazyColumn
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.nav.onStarKeyPress
import com.future.sharednav.nav.rememberFocusListState
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.focusFillColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * דף קו (בסגנון אגד): כל התחנות של הנסיעה לפי הסדר עם השעות, הכיוון והמפעיל.
 * התחנה שלי מסומנת "התחנה שלך" עם ארבע היציאות הבאות של הקו ממנה.
 *
 * מקשים: למעלה/למטה - תחנות. 7 - החלפת כיוון (הנסיעה ההפוכה של אותו קו בתחנה קרובה).
 * * על תחנה - תפריט: לוח זמנים יומי בתחנה, הזכרה לרדת בה, שיתוף בהודעה. BACK - חזרה.
 * הקו נרשם ב"לאחרונה" (קווים) בכל פתיחה.
 */
@Composable
fun TripScreen(
    tripId: String,
    line: String,
    boardingStop: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val scope = rememberCoroutineScope()

    // החלפת כיוון מחליפה את הנסיעה המוצגת בלי לחזור לניווט
    var currentTripId by remember(tripId) { mutableStateOf(tripId) }
    var schedule by remember { mutableStateOf<TripSchedule?>(null) }
    var loading by remember { mutableStateOf(true) }
    var nextTimes by remember { mutableStateOf<List<LineDeparture>>(emptyList()) }
    var dailyStop by remember { mutableStateOf<TripStop?>(null) }
    var menuIndex by remember { mutableStateOf<Int?>(null) }
    var reminder by remember { mutableStateOf<TripStop?>(null) }
    var reached by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTripId) {
        loading = true
        schedule = TripScheduleSource.load(currentTripId)
        loading = false
    }

    val loaded = schedule
    val stops = loaded?.stops.orEmpty()
    // התחנה שלי היא תחנת העלייה מהמסך הקודם. -1 אם לא ידועה (למשל פתיחה מ"קווים")
    val myIndex = stops.indexOfFirst { it.name == boardingStop }
    val myStop = stops.getOrNull(myIndex)

    // הקו נרשם ב"לאחרונה" אחרי שנטען (גם כשנפתח ממסך התחנה, לא רק מחיפוש קווים)
    LaunchedEffect(loaded) {
        val s = loaded ?: return@LaunchedEffect
        if (s.line.isBlank()) return@LaunchedEffect
        val boarding = s.stops.firstOrNull { it.name == boardingStop } ?: s.stops.first()
        LineHistory.get(context).recordOpened(
            SavedLine(
                line = s.line,
                headsign = s.headsign,
                agency = s.agency,
                stopName = boarding.name,
                stopId = boarding.stopId,
                lat = boarding.location.lat,
                lon = boarding.location.lon,
            ),
        )
    }

    // היציאות הבאות של הקו בתחנה שלי, מתרעננות כל 30 שניות
    LaunchedEffect(loaded, myIndex) {
        val s = loaded ?: return@LaunchedEffect
        val stop = s.stops.getOrNull(myIndex)
        if (stop == null) {
            nextTimes = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            val now = System.currentTimeMillis()
            val fromStop = stop.stopId?.let { LineTimes.atStop(it, now) } ?: LineTimes.nearby(stop.location)
            nextTimes = LineLogic.nextForLine(fromStop.orEmpty(), s.line, s.headsign, now, NEXT_COUNT)
            delay(REFRESH_MS)
        }
    }

    // הזכרה לרדת: כל עוד המסך פתוח, מקשיבים למיקום ומתריעים ב-300 מ' מהתחנה
    LaunchedEffect(reminder) {
        reached = false
        val target = reminder ?: return@LaunchedEffect
        if (!LocationHelper.hasPermission(context)) return@LaunchedEffect
        LocationHelper.observeLocation(context).collect { location ->
            val close = LatLng(location.latitude, location.longitude).distanceMetersTo(target.location) <= ALERT_METERS
            if (close && !reached) {
                reached = true
                vibrate(context)
            }
        }
    }

    // החלפת כיוון: הנסיעה ההפוכה של אותו קו, מתוך היציאות ליד התחנה שלי
    fun switchDirection() {
        val s = loaded ?: return
        if (switching) return
        val at = (myStop ?: s.stops.first()).location
        scope.launch {
            switching = true
            val opposite = LineLogic.oppositeDirection(
                LineTimes.nearby(at).orEmpty(),
                s.line,
                s.headsign,
                at,
                System.currentTimeMillis(),
            )
            switching = false
            val nextId = opposite?.tripId
            if (nextId == null) {
                notice = "לא נמצאה נסיעה בכיוון הנגדי ליד התחנה שלך"
            } else {
                notice = null
                currentTripId = nextId
            }
        }
    }

    val daily = dailyStop
    if (daily != null) {
        LineDailyScreen(
            line = loaded?.line ?: line,
            headsign = loaded?.headsign.orEmpty(),
            stopName = daily.name,
            stopId = daily.stopId,
            onBack = { dailyStop = null },
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || digitForKey(event.key) != "7") return@onPreviewKeyEvent false
                switchDirection()
                true
            },
        ) {
            ScreenTopBar(
                title = "קו $line",
                textColor = MaterialTheme.colorScheme.onBackground,
                accentColor = MaterialTheme.colorScheme.primary,
                onBack = onBack,
            )

            val s = loaded
            when {
                loading -> Text("טוען לוח נסיעה…", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
                s == null || s.stops.isEmpty() -> Text("לוח הנסיעה לא זמין כרגע", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
                else -> {
                    val details = listOfNotNull(
                        s.headsign.takeIf { it.isNotBlank() }?.let { "לכיוון $it" },
                        s.agency,
                    ).joinToString(" · ")
                    if (details.isNotBlank()) {
                        Text(
                            details,
                            color = theme.mutedTextColor,
                            fontSize = type.summary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                        )
                    }
                    if (myStop != null) {
                        val times = nextTimes.joinToString(" · ") { LineLogic.clock(it.departsAtMillis) }
                        Text(
                            "התחנה שלך: ${myStop.name}" + (if (times.isNotEmpty()) " · הבאות: $times" else " · אין יציאות קרובות"),
                            color = theme.readableAccentColor,
                            fontSize = type.summary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXxs),
                        )
                    }
                    Text(
                        "7 - החלפת כיוון · Options - אפשרויות לתחנה",
                        color = theme.mutedTextColor,
                        fontSize = type.label,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                    )

                    val rem = reminder
                    if (rem != null) {
                        if (reached) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs)
                                    .background(theme.readableAccentColor, FutureShapes.dialog)
                                    .padding(FutureDimens.spacingMd),
                            ) {
                                Text(
                                    "הגעת לתחנה ${rem.name} - רד/י כאן",
                                    color = theme.onReadableAccentColor,
                                    fontSize = type.screenTitle,
                                    fontWeight = FutureTypography.weightBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            val armed = LocationHelper.hasPermission(context)
                            Text(
                                if (armed) "תזכורת: רד/י ב${rem.name}, אתריע כשאהיה ליד התחנה" else "אין הרשאת מיקום - התזכורת לא תפעל",
                                color = theme.mutedTextColor,
                                fontSize = type.label,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                            )
                        }
                    }
                    val note = notice
                    if (note != null) {
                        Text(
                            note,
                            color = theme.mutedTextColor,
                            fontSize = type.label,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                        )
                    }

                    // key: הרשימה נבנית מחדש כשהנסיעה מתחלפת, כדי שהפוקוס יתחיל בתחנה שלי
                    key(s) {
                        TripStopsList(
                            stops = s.stops,
                            boardingIndex = myIndex.coerceAtLeast(0),
                            hasBoarding = myIndex >= 0,
                            reminderName = rem?.name,
                            onStar = { index -> menuIndex = index },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }

            // תפריט * של תחנה
            val mi = menuIndex
            val menuStop = if (mi != null) s?.stops?.getOrNull(mi) else null
            if (s != null && menuStop != null) {
                val isReminder = reminder?.name == menuStop.name
                FutureOptionsMenu(
                    theme = theme,
                    onDismissRequest = { menuIndex = null },
                    header = menuStop.name,
                ) {
                    FutureMenuRow("לוח זמנים יומי בתחנה זו", FutureIcons.Schedule, theme, {
                        menuIndex = null
                        dailyStop = menuStop
                    })
                    FutureMenuRow(
                        if (isReminder) "בטל תזכורת" else "הזכר לי לרדת בתחנה זו",
                        FutureIcons.Alarm,
                        theme,
                        {
                            reminder = if (isReminder) null else menuStop
                            menuIndex = null
                        },
                    )
                    FutureMenuRow("שתף", FutureIcons.Share, theme, {
                        menuIndex = null
                        shareStop(context, s.line, s.headsign, menuStop.name)
                    })
                }
            }
        }
    }
}

@Composable
private fun TripStopsList(
    stops: List<TripStop>,
    boardingIndex: Int,
    hasBoarding: Boolean,
    reminderName: String?,
    onStar: (Int) -> Unit,
    modifier: Modifier,
) {
    val theme = LocalFutureTheme.current
    val focus = rememberFocusListState(itemCount = stops.size, initialIndex = boardingIndex)
    // * על תחנה פותח את התפריט שלה (לוח יומי, תזכורת, שיתוף)
    com.future.sharednav.nav.onOptionsKeyPress { onStar(focus.focusedIndex) }
    KeypadLazyColumn(
        items = stops,
        focusState = focus,
        onSelect = { _, _ -> },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
    ) { index, stop, isFocused ->
        val mine = hasBoarding && index == boardingIndex
        FutureListItem(
            title = stop.name,
            summary = listOfNotNull(
                (stop.arrivalSeconds ?: stop.departureSeconds)?.let { clock(it) },
                if (mine) "התחנה שלך" else null,
                if (reminderName == stop.name) "תזכורת" else null,
            ).joinToString(" · "),
            theme = theme,
            onClick = {},
            titleColor = if (mine) theme.readableAccentColor else theme.textColor,
            idleBackgroundColor = if (isFocused) theme.focusFillColor else Color.Transparent,
        )
    }
}

private fun shareStop(context: Context, line: String, headsign: String, stop: String) {
    val direction = if (headsign.isNotBlank()) " לכיוון $headsign" else ""
    val text = "אני נוסע/ת בקו $line$direction, ואני יורד/ת בתחנה $stop"
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                .putExtra("sms_body", text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun vibrate(context: Context) {
    runCatching {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    }
}

private fun clock(seconds: Int): String = "%02d:%02d".format((seconds / 3600) % 24, (seconds % 3600) / 60)

/** יציאות הקו הבאות שמוצגות בתחנה שלי. */
private const val NEXT_COUNT = 4

/** רענון היציאות הבאות, כמו במסך התחנה. */
private const val REFRESH_MS = 30_000L

/** מרחק מהתחנה שבו מתריעים על ההזכרה לרדת. */
private const val ALERT_METERS = 300.0
