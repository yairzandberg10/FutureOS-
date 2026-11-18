package com.future.navigation.ui.transit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.navigation.data.gtfs.ServerDepartures
import com.future.navigation.data.gtfs.StationBoard
import com.future.navigation.ui.navigation.Destination
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import kotlinx.coroutines.delay

/**
 * לוח יציאות של תחנה אחת (בסגנון Moovit/אגד): הקווים הבאים, לאן, ובעוד כמה
 * דקות. מתרענן כל 30 שניות כל עוד המסך פתוח. "נווט לתחנה" מתכנן מסלול אליה.
 */
@Composable
fun StationScreen(
    station: Destination,
    onBack: () -> Unit,
    onRouteHere: () -> Unit,
    source: ServerDepartures = remember { ServerDepartures() },
) {
    val theme = LocalFutureTheme.current
    var board by remember { mutableStateOf<StationBoard?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val routeFocus = remember { FocusRequester() }

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

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = station.name,
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack,
        )

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding)) {
            FutureButton("נווט לתחנה", theme, onRouteHere, fillMaxWidth = true, focusRequester = routeFocus)
        }

        val departures = board?.departures.orEmpty()
        when {
            loading -> Text("טוען זמני הגעה…", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            failed -> Text("אין חיבור לשרת - לא ניתן להציג זמני הגעה", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            departures.isEmpty() -> Text("אין יציאות קרובות מהתחנה הזו", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                items(departures) { departure ->
                    FutureListItem(
                        title = "קו ${departure.line}" + (departure.headsign.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        summary = if (departure.minutesAway == 0) "מגיע עכשיו" else "בעוד ${departure.minutesAway} דק׳",
                        theme = theme,
                        onClick = {},
                    )
                }
            }
        }
    }
}

private const val REFRESH_MS = 30_000L
