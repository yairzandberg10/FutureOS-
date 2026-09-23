package com.future.dialer.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.future.dialer.data.model.Contact
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.subtleTextColor

/**
 * חיפוש (ui_kits/calls): שדה אחד מעל תוצאות חיות לפי שם או ספרות. אישור
 * על תוצאה פותח את מסך איש הקשר. השדה מקבל פוקוס עם הפתיחה, וחץ למטה יורד
 * לתוצאות.
 */
@Composable
fun SearchScreen(
    viewModel: ContactsViewModel,
    onBack: () -> Unit,
    onOpen: (Contact) -> Unit,
) {
    val theme = LocalFutureTheme.current
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.contacts.collectAsState()

    // חיפוש חדש בכל כניסה - שאילתה ישנה לא נשארת תלויה בשדה.
    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { viewModel.onSearchQueryChanged("") }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "חיפוש",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack,
        )
        FutureTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChanged,
            theme = theme,
            placeholder = "שם או מספר",
            autoFocus = true,
            leading = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = theme.subtleTextColor,
                    modifier = Modifier.padding(end = 2.dp),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, bottom = FutureDimens.spacingMd)
                .escapeTextFieldFocusTrap(),
        )
        if (results.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "אין תוצאות",
                subtitle = "נסה שם או ספרות אחרות",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, bottom = FutureDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
            ) {
                items(results, key = { it.id }) { contact ->
                    FutureListItem(
                        title = contact.name,
                        summary = contact.phoneNumber,
                        theme = theme,
                        onClick = { onOpen(contact) },
                    )
                }
            }
        }
    }
}
