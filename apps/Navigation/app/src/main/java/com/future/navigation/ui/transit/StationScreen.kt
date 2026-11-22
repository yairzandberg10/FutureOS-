package com.future.navigation.ui.transit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import com.future.navigation.data.gtfs.ServerDepartures
import com.future.navigation.data.gtfs.StationBoard
import com.future.navigation.data.gtfs.StationDeparture
import com.future.navigation.data.gtfs.nextDepartureOfSameLine
import com.future.navigation.data.transit.FavoriteStation
import com.future.navigation.data.transit.FavoriteStations
import com.future.navigation.ui.navigation.Destination
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.nav.onPoundKeyPress
import com.future.sharednav.nav.onStarKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.secondaryTextColor
import kotlinx.coroutines.delay

/**
 * לוח יציאות של תחנה אחת (בסגנון Moovit/אגד): הקווים הבאים, לאן, השעה, בעוד כמה
 * דקות, זמן אמת ורציף. מקשים: ספרות = סינון לפי מספר קו, # = ניקוי הסינון,
 * * = שמירה/הסרה מהמועדפים, OK על קו = לוח הנסיעה המלא שלו.
 * מתרענן כל 30 שניות כל עוד המסך פתוח. "נווט לתחנה" מתכנן מסלול אליה.
 */
@Composable
fun StationScreen(
    station: Destination,
    onBack: () -> Unit,
    onRouteHere: () -> Unit,
    onOpenTrip: (tripId: String, line: String, stopName: String) -> Unit = { _, _, _ -> },
    source: ServerDepartures = remember { ServerDepartures() },
) {
    val theme = LocalFutureTheme.current
    val favorites = FavoriteStations.get(LocalContext.current)
    val savedStations by favorites.favorites.collectAsState()
    val saved = savedStations.any { it.name == station.name }
    var board by remember { mutableStateOf<StationBoard?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var lineFilter by remember { mutableStateOf("") }
    val routeFocus = remember { FocusRequester() }

    // * ו-# נצרכים במערכת (FutureUI) ומגיעים לאפליקציה כשידור בלחיצה קצרה
    com.future.navigation.ui.common.onStarKey { favorites.toggle(FavoriteStation(station.name, station.location)) }
    com.future.navigation.ui.common.onPoundKey { lineFilter = "" }

    LaunchedEffect(station) {
        runCatching { routeFocus.requestFocus() }
        while (true) {
            val result = source.boardAt(station.location, station.name)
            failed = result == null && board == null
            if (result != null) board = result
            loading = false
            delay(REFRESH_MS)
        }
    }

    val departures = board?.departures.orEmpty()
    val shown = departures.filter { lineFilter.isEmpty() || it.line.startsWith(lineFilter) }

    Column(
        modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            // ספרה מסננת את הלוח לפי תחילת מספר הקו (לא נשלחת לפריטים)
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val digit = digitForKey(event.key) ?: return@onPreviewKeyEvent false
            if (lineFilter.length < MAX_FILTER_DIGITS) lineFilter += digit
            true
        },
    ) {
        ScreenTopBar(
            title = station.name,
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack,
        )

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding)) {
            FutureButton("נווט לתחנה", theme, onRouteHere, fillMaxWidth = true, focusRequester = routeFocus)
        }

        // מספר התחנה הרשמי, מתחת לשם התחנה
        board?.stopCode?.let { code ->
            Text(
                text = "תחנה מס׳ $code",
                style = MaterialTheme.typography.labelSmall,
                color = theme.mutedTextColor,
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
            )
        }

        // שורת מצב: סינון הקו הנוכחי ואם התחנה שמורה
        Text(
            text = listOfNotNull(
                if (lineFilter.isNotEmpty()) "סינון קו: $lineFilter (# לניקוי)" else null,
                if (saved) "שמורה במועדפים (* להסרה)" else "* לשמירה במועדפים",
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedTextColor,
            modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXxs),
        )

        when {
            loading -> Text("טוען זמני הגעה…", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            failed -> Text("אין חיבור לשרת - לא ניתן להציג זמני הגעה", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            departures.isEmpty() -> Text("אין יציאות קרובות מהתחנה הזו", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            shown.isEmpty() -> Text("אין קו $lineFilter בתחנה הזו", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                items(shown) { departure ->
                    // היציאה הבאה של אותו קו ואותו כיוון, מתוך הלוח המלא (לא המסונן)
                    val next = nextDepartureOfSameLine(departures, departures.indexOf(departure))
                    FutureListItem(
                        title = "קו ${departure.line}" + (departure.headsign.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        summary = summaryOf(departure, next),
                        summaryMaxLines = 2,
                        theme = theme,
                        onClick = {
                            departure.tripId?.let { onOpenTrip(it, departure.line, station.name) }
                        },
                        // סמן זמן אמת: כשהשעה מגיעה מהמיקום החי של הנסיעה ולא מהלו"ז
                        trailing = if (departure.realtime) realtimeMarker(theme) else null,
                    )
                }
            }
        }
    }
}

/**
 * שורת פירוט ליציאה: שעה, בעוד כמה דקות, היציאה הבאה של הקו ("ואחריו 06:53") ורציף.
 * זמן אמת מוצג בנפרד, בסוף השורה (ר' [realtimeMarker]).
 */
private fun summaryOf(departure: StationDeparture, next: StationDeparture?): String = listOfNotNull(
    departure.departsAtSeconds?.let { clock(it) },
    if (departure.minutesAway == 0) "מגיע עכשיו" else "בעוד ${departure.minutesAway} דק׳",
    next?.departsAtSeconds?.let { "ואחריו ${clock(it)}" },
    departure.platform?.let { "רציף $it" },
).joinToString(" · ")

/** סמן "זמן אמת" בסוף השורה: בצבע טקסט משני (לא בהדגשה - ההדגשה היא לפוקוס בלבד). */
private fun realtimeMarker(theme: FutureTheme): @Composable RowScope.() -> Unit = {
    Text(
        text = "זמן אמת",
        style = MaterialTheme.typography.labelSmall,
        color = theme.secondaryTextColor,
    )
}

private fun clock(seconds: Int): String = "%02d:%02d".format((seconds / 3600) % 24, (seconds % 3600) / 60)

private const val REFRESH_MS = 30_000L
private const val MAX_FILTER_DIGITS = 4
