package com.future.navigation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.gtfs.SavedPlaceEntity
import com.future.navigation.ui.map.LocationPreviewMap
import com.future.navigation.ui.navigation.TravelMode
import com.future.sharednav.focus.FocusableItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDestinationPicked: (GeocodeResult) -> Unit,
    onOpenSavedPlaces: () -> Unit,
    onOpenGtfsSetup: () -> Unit
) {
    val mode by viewModel.mode.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val homePlace by viewModel.homePlace.collectAsState(initial = null)
    val workPlace by viewModel.workPlace.collectAsState(initial = null)
    val nearbyStops by viewModel.nearbyStops.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (currentLocation != null) "%.4f, %.4f".format(currentLocation!!.lat, currentLocation!!.lon)
                       else stringResource(R.string.current_location_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            FocusableItem(onClick = onOpenGtfsSetup, accentColor = MaterialTheme.colorScheme.primary, cornerRadius = 999.dp) {
                Icon(Icons.Default.Place, contentDescription = stringResource(R.string.gtfs_setup_title), tint = MaterialTheme.colorScheme.primary)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(140.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            LocationPreviewMap(location = currentLocation)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            SegmentButton(
                label = stringResource(R.string.mode_drive),
                selected = mode == TravelMode.DRIVE,
                modifier = Modifier.weight(1f)
            ) { viewModel.setMode(TravelMode.DRIVE) }
            Spacer(modifier = Modifier.width(8.dp))
            SegmentButton(
                label = stringResource(R.string.mode_transit),
                selected = mode == TravelMode.TRANSIT,
                modifier = Modifier.weight(1f)
            ) { viewModel.setMode(TravelMode.TRANSIT) }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (query.isNotBlank()) {
                item {
                    Text(
                        text = if (searching) "מחפש…" else "תוצאות חיפוש",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                items(results) { result ->
                    FocusableItem(
                        onClick = { onDestinationPicked(result) },
                        accentColor = MaterialTheme.colorScheme.primary,
                        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        borderWidth = 2.dp,
                        cornerRadius = 16.dp
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                result.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickPlaceCard(
                            icon = Icons.Default.Home,
                            label = stringResource(R.string.quick_home),
                            emptyHint = stringResource(R.string.add_home_address),
                            place = homePlace,
                            modifier = Modifier.weight(1f)
                        ) { homePlace?.let { onDestinationPicked(GeocodeResult(it.address, com.future.navigation.data.common.LatLng(it.lat, it.lon))) } ?: onOpenSavedPlaces() }
                        QuickPlaceCard(
                            icon = Icons.Default.Work,
                            label = stringResource(R.string.quick_work),
                            emptyHint = stringResource(R.string.add_work_address),
                            place = workPlace,
                            modifier = Modifier.weight(1f)
                        ) { workPlace?.let { onDestinationPicked(GeocodeResult(it.address, com.future.navigation.data.common.LatLng(it.lat, it.lon))) } ?: onOpenSavedPlaces() }
                    }
                }

                if (mode == TravelMode.TRANSIT) {
                    item {
                        Text(
                            text = stringResource(R.string.nearby_stops),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    items(nearbyStops) { nearby ->
                        FocusableItem(
                            onClick = { },
                            accentColor = MaterialTheme.colorScheme.primary,
                            idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            borderWidth = 2.dp,
                            cornerRadius = 16.dp
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(nearby.stop.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("%.0f מ׳".format(nearby.distanceMeters), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusableItem(
        onClick = onClick,
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        idleBackgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        cornerRadius = 999.dp,
        scaleOnFocus = false
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun QuickPlaceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    emptyHint: String,
    place: SavedPlaceEntity?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FocusableItem(
        onClick = onClick,
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        borderWidth = 2.dp,
        cornerRadius = 16.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    place?.address ?: emptyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
        }
    }
}
