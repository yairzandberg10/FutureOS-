package com.future.navigation.ui.transit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.future.navigation.data.transit.TripSchedule
import com.future.navigation.data.transit.TripScheduleSource
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.KeypadLazyColumn
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.nav.rememberFocusListState
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor

/**
 * לוח הנסיעה המלא של קו (בסגנון Moovit): כל התחנות לפי הסדר עם השעות. הפוקוס
 * מתחיל בתחנת העלייה ("עולים כאן") והרשימה גוללת אליה. מקשים: למעלה/למטה, BACK.
 */
@Composable
fun TripScreen(
    tripId: String,
    line: String,
    boardingStop: String?,
    onBack: () -> Unit,
) {
    val theme = LocalFutureTheme.current
    var schedule by remember { mutableStateOf<TripSchedule?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(tripId) {
        schedule = TripScheduleSource.load(tripId)
        loading = false
    }

    val stops = schedule?.stops.orEmpty()
    val boardingIndex = stops.indexOfFirst { it.name == boardingStop }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "קו $line" + (schedule?.headsign?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack,
        )
        when {
            loading -> Text("טוען לוח נסיעה…", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            stops.isEmpty() -> Text("לוח הנסיעה לא זמין כרגע", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            // key: הרשימה נבנית מחדש כשהלוח נטען, כדי שהפוקוס יתחיל בתחנת העלייה
            else -> key(schedule) {
                TripStopsList(stops, boardingIndex, hasBoarding = boardingStop != null, modifier = Modifier.weight(1f).fillMaxSize())
            }
        }
    }
}

@Composable
private fun TripStopsList(
    stops: List<com.future.navigation.data.transit.TripStop>,
    boardingIndex: Int,
    hasBoarding: Boolean,
    modifier: Modifier,
) {
    val theme = LocalFutureTheme.current
    val focus = rememberFocusListState(itemCount = stops.size, initialIndex = boardingIndex)
    KeypadLazyColumn(
        items = stops,
        focusState = focus,
        onSelect = { _, _ -> },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
    ) { index, stop, _ ->
        FutureListItem(
            title = stop.name,
            summary = listOfNotNull(
                (stop.arrivalSeconds ?: stop.departureSeconds)?.let { clock(it) },
                if (hasBoarding && index == boardingIndex) "עולים כאן" else null,
            ).joinToString(" · "),
            theme = theme,
            onClick = {},
        )
    }
}

private fun clock(seconds: Int): String = "%02d:%02d".format((seconds / 3600) % 24, (seconds % 3600) / 60)
