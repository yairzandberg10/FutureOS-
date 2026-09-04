package com.future.navigation.ui.saved

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.gtfs.SavedPlaceEntity
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.FocusableItem

@Composable
fun SavedPlacesScreen(viewModel: SavedPlacesViewModel, onBack: () -> Unit, onNavigateToPlace: (SavedPlaceEntity) -> Unit = {}) {
    val editingSlot by viewModel.editingSlot.collectAsState()

    if (editingSlot != EditingSlot.NONE) {
        AddressSearchScreen(viewModel, onBack = viewModel::cancelEditing)
        return
    }

    val homePlace by viewModel.homePlace.collectAsState(initial = null)
    val workPlace by viewModel.workPlace.collectAsState(initial = null)
    val allPlaces by viewModel.allPlaces.collectAsState(initial = emptyList())
    val favorites = allPlaces.filter { it.isFavorite }
    val homeCardFocusRequester = remember { FocusRequester() }

    // פוקוס D-pad התחלתי על כרטיס "בית" - בלי זה נחיתה על המסך משאירה אותו
    // בלי שום פריט מודגש.
    LaunchedEffect(Unit) { homeCardFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.saved_places_title),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            item {
                Text(
                    stringResource(R.string.fixed_places),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PinCard(Icons.Default.Home, stringResource(R.string.quick_home), homePlace, stringResource(R.string.add_home_address), modifier = Modifier.weight(1f), focusRequester = homeCardFocusRequester) {
                        viewModel.startEditing(EditingSlot.HOME)
                    }
                    PinCard(Icons.Default.Work, stringResource(R.string.quick_work), workPlace, stringResource(R.string.add_work_address), modifier = Modifier.weight(1f)) {
                        viewModel.startEditing(EditingSlot.WORK)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.favorites),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                    FocusableItem(
                        onClick = { viewModel.startEditing(EditingSlot.FAVORITE) },
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        borderWidth = 2.dp,
                        cornerRadius = 20.dp
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(favorites, key = { it.id }) { place ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    FocusableItem(
                        onClick = { onNavigateToPlace(place) },
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        borderWidth = 2.dp,
                        cornerRadius = 16.dp
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(place.label.ifBlank { place.address }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(place.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1)
                            }
                        }
                    }
                    FocusableItem(
                        onClick = { viewModel.toggleFavorite(place) },
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        borderWidth = 2.dp,
                        cornerRadius = 20.dp
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
                    }
                    FocusableItem(
                        onClick = { viewModel.delete(place) },
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        borderWidth = 2.dp,
                        cornerRadius = 20.dp
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    place: SavedPlaceEntity?,
    emptyHint: String,
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onClick: () -> Unit
) {
    FocusableItem(
        onClick = onClick,
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        borderWidth = 2.dp,
        cornerRadius = 16.dp,
        focusRequester = focusRequester
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                place?.address ?: emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun AddressSearchScreen(viewModel: SavedPlacesViewModel, onBack: () -> Unit) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.search_placeholder),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            items(results) { result ->
                FocusableItem(
                    onClick = { viewModel.pickResult(result) },
                    accentColor = MaterialTheme.colorScheme.primary,
                    idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    borderWidth = 2.dp,
                    cornerRadius = 16.dp
                ) {
                    Text(result.label, modifier = Modifier.padding(12.dp), maxLines = 2)
                }
            }
        }
    }
}
