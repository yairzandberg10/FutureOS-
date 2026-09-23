package com.future.dialer.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.dialer.data.model.Contact
import com.future.dialer.ui.CallsViewModel
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme

/**
 * טאב המועדפים (ui_kits/calls) - שורות רשימה על רקע המסך, ואישור מחייג
 * ישירות. מועדף נוסף ממסך איש הקשר, והסימון נשמר באנשי הקשר של המערכת
 * (STARRED), כך שהוא משותף לאפליקציית אנשי הקשר.
 */
@Composable
fun FavoritesScreen(
    viewModel: CallsViewModel,
    onCall: (Contact) -> Unit,
    onMenu: () -> Unit,
) {
    val theme = LocalFutureTheme.current
    val favorites by viewModel.favorites.collectAsState()
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(favorites.isNotEmpty()) {
        if (favorites.isNotEmpty()) runCatching { firstRow.requestFocus() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "מועדפים",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            trailingIcon = Icons.Rounded.MoreVert,
            trailingContentDescription = "אפשרויות",
            onTrailingClick = onMenu,
        )
        if (favorites.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.StarBorder,
                title = "אין מועדפים",
                subtitle = "לחץ על מקש התפריט ובחר חיפוש כדי להוסיף",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, bottom = FutureDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                itemsIndexed(favorites, key = { _, it -> it.id }) { index, contact ->
                    FutureListItem(
                        title = contact.name,
                        summary = contact.phoneNumber,
                        theme = theme,
                        focusRequester = if (index == 0) firstRow else null,
                        onClick = { onCall(contact) },
                    )
                }
            }
        }
    }
}
