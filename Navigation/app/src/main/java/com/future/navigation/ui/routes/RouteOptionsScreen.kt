package com.future.navigation.ui.routes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.routing.DrivingRoute
import com.future.navigation.ui.navigation.Destination
import com.future.navigation.ui.navigation.TravelMode
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.FocusableItem

@Composable
fun RouteOptionsScreen(
    viewModel: RouteOptionsViewModel,
    mode: TravelMode,
    destination: Destination,
    onBack: () -> Unit,
    onStartDriving: (DrivingRoute) -> Unit,
    onStartTransit: (TransitItinerary) -> Unit
) {
    val loading by viewModel.loading.collectAsState()
    val downloadFraction by viewModel.downloadFraction.collectAsState()
    val error by viewModel.error.collectAsState()
    val drivingRoute by viewModel.drivingRoute.collectAsState()
    val itineraries by viewModel.itineraries.collectAsState()

    LaunchedEffect(destination, mode) {
        viewModel.search(mode, destination)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.route_options_title),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = destination.address.ifBlank { destination.name },
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (downloadFraction != null) {
                            "מוריד נתוני תחבורה ציבורית לאזור הזה… %d%%".format((downloadFraction!! * 100).toInt())
                        } else {
                            stringResource(R.string.calculating_route)
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (downloadFraction != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { downloadFraction!! },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                error != null -> Text(
                    text = error ?: "",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                mode == TravelMode.DRIVE && drivingRoute != null -> DrivingRouteCard(drivingRoute!!)
                mode == TravelMode.TRANSIT -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(itineraries) { itinerary ->
                            ItineraryCard(itinerary, onClick = { onStartTransit(itinerary) })
                        }
                    }
                }
            }
        }

        if (mode == TravelMode.DRIVE && drivingRoute != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                PrimaryButton(stringResource(R.string.start_navigation)) { onStartDriving(drivingRoute!!) }
            }
        }
    }
}

@Composable
private fun DrivingRouteCard(route: DrivingRoute) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "%d דק׳".format((route.durationSeconds / 60).toInt()),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "%.1f ק״מ".format(route.distanceMeters / 1000.0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ItineraryCard(itinerary: TransitItinerary, onClick: () -> Unit) {
    FocusableItem(
        onClick = onClick,
        accentColor = MaterialTheme.colorScheme.primary,
        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        borderWidth = 2.dp,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%d דק׳".format((itinerary.totalDurationSeconds / 60)),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "הגעה " + secondsToClock(itinerary.arrivalSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = itinerary.legs.filter { it.type == LegType.RIDE }.joinToString(" + ") { it.routeShortName ?: it.routeLongName ?: "" },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.schedule_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    FocusableItem(
        onClick = onClick,
        accentColor = MaterialTheme.colorScheme.primary,
        idleBackgroundColor = MaterialTheme.colorScheme.primary,
        focusedBackgroundColor = MaterialTheme.colorScheme.primary,
        cornerRadius = 16.dp,
        scaleOnFocus = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun secondsToClock(seconds: Int): String {
    val h = (seconds / 3600) % 24
    val m = (seconds % 3600) / 60
    return "%02d:%02d".format(h, m)
}
