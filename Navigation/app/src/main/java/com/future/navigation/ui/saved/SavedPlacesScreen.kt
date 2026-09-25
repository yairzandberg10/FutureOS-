package com.future.navigation.ui.saved

import com.future.sharednav.icons.FutureIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.future.navigation.R
import com.future.navigation.data.gtfs.SavedPlaceEntity
import com.future.navigation.ui.home.RowIcon
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.favoriteColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType

@Composable
fun SavedPlacesScreen(viewModel: SavedPlacesViewModel, onBack: () -> Unit, onNavigateToPlace: (SavedPlaceEntity) -> Unit = {}) {
    val editingSlot by viewModel.editingSlot.collectAsState()

    if (editingSlot != EditingSlot.NONE) {
        AddressSearchScreen(viewModel, onBack = viewModel::cancelEditing)
        return
    }

    val theme = LocalFutureTheme.current
    val homePlace by viewModel.homePlace.collectAsState(initial = null)
    val workPlace by viewModel.workPlace.collectAsState(initial = null)
    val allPlaces by viewModel.allPlaces.collectAsState(initial = emptyList())
    val favorites = allPlaces.filter { it.isFavorite }
    val homeCardFocusRequester = remember { FocusRequester() }

    // פוקוס D-pad התחלתי על כרטיס "בית" - בלי זה נחיתה על המסך משאירה אותו
    // בלי שום פריט מודגש.
    LaunchedEffect(Unit) { runCatching { homeCardFocusRequester.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.saved_places_title),
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            item {
                FutureSectionHeader(stringResource(R.string.fixed_places), theme, inset = false)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing)) {
                    PinTile(FutureIcons.Home, stringResource(R.string.quick_home), homePlace, stringResource(R.string.add_home_address), theme, modifier = Modifier.weight(1f), focusRequester = homeCardFocusRequester) {
                        viewModel.startEditing(EditingSlot.HOME)
                    }
                    PinTile(FutureIcons.Work, stringResource(R.string.quick_work), workPlace, stringResource(R.string.add_work_address), theme, modifier = Modifier.weight(1f)) {
                        viewModel.startEditing(EditingSlot.WORK)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingMd)) {
                    FutureSectionHeader(stringResource(R.string.favorites), theme, inset = false, modifier = Modifier.weight(1f))
                    TopBarIconButton(
                        icon = FutureIcons.Add,
                        contentDescription = stringResource(R.string.favorites),
                        textColor = theme.textColor,
                        accentColor = theme.accentColor,
                        onClick = { viewModel.startEditing(EditingSlot.FAVORITE) },
                    )
                }
            }
            items(favorites, key = { it.id }) { place ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    FutureListItem(
                        title = place.label.ifBlank { place.address },
                        summary = place.address,
                        theme = theme,
                        onClick = { onNavigateToPlace(place) },
                        leading = { RowIcon(FutureIcons.LocationOn, theme) },
                        modifier = Modifier.weight(1f),
                    )
                    // כוכב המועדפים הוא החריג היחיד לכלל "אייקון לא נושא צבע משלו"
                    // (#FFC107, README של הדיזיין סיסטם).
                    TopBarIconButton(
                        icon = FutureIcons.Star,
                        contentDescription = stringResource(R.string.favorites),
                        textColor = theme.favoriteColor,
                        accentColor = theme.accentColor,
                        onClick = { viewModel.toggleFavorite(place) },
                    )
                    TopBarIconButton(
                        icon = FutureIcons.Delete,
                        contentDescription = "מחק",
                        textColor = theme.textColor,
                        accentColor = theme.accentColor,
                        onClick = { viewModel.delete(place) },
                    )
                }
            }
        }
    }
}

/** אריח "בית" / "עבודה" - אותו אריח של מסך הבית. */
@Composable
private fun PinTile(
    icon: ImageVector,
    label: String,
    place: SavedPlaceEntity?,
    emptyHint: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val type = rememberFutureType()
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier,
        idleBackgroundColor = theme.idleChipColor,
        contentPadding = FutureDimens.spacingSm,
        focusRequester = focusRequester
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                RowIcon(icon, theme)
                Text(label, color = theme.textColor, fontSize = type.title, fontWeight = FutureTypography.weightMedium, maxLines = 1)
            }
            Text(
                place?.address ?: emptyHint,
                fontSize = type.summary,
                color = theme.mutedTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AddressSearchScreen(viewModel: SavedPlacesViewModel, onBack: () -> Unit) {
    val theme = LocalFutureTheme.current
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        ScreenTopBar(
            title = stringResource(R.string.search_placeholder),
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack
        )
        FutureTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChanged,
            theme = theme,
            autoFocus = true,
            leading = {
                Icon(
                    FutureIcons.Search,
                    contentDescription = null,
                    tint = theme.mutedTextColor,
                    modifier = Modifier.size(FutureDimens.iconTopBar),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            items(results) { result ->
                val parts = result.label.split(", ", limit = 2)
                FutureListItem(
                    title = parts[0],
                    summary = parts.getOrNull(1),
                    theme = theme,
                    onClick = { viewModel.pickResult(result) },
                    leading = { RowIcon(FutureIcons.LocationOn, theme) },
                )
            }
        }
    }
}
