package com.future.dialer.ui.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.dialer.data.model.Contact
import com.future.dialer.ui.CallsViewModel
import com.future.dialer.ui.requestFocusWhenAttached
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.favoriteColor
import java.text.Collator
import java.util.Locale

/**
 * טאב אנשי הקשר (במקום המועדפים): כל אנשי הקשר, מועדפים ראשונים ואחריהם
 * שמות בעברית ואז באותיות לועזיות. אישור פותח את כרטיס איש הקשר; ספרה
 * עוברת למקלדת (MainActivity), כמו ביומן.
 */
@Composable
fun ContactsTabScreen(
    viewModel: CallsViewModel,
    onOpen: (Contact) -> Unit,
) {
    val theme = LocalFutureTheme.current
    val contacts by viewModel.contacts.collectAsState()
    val sorted = remember(contacts) {
        val collator = Collator.getInstance(Locale.forLanguageTag("he")).apply { strength = Collator.PRIMARY }
        contacts.sortedWith(
            compareByDescending<Contact> { it.isFavorite }
                .thenBy { scriptBucket(it.name) }
                .thenComparator { a, b -> collator.compare(a.name, b.name) }
        )
    }
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(sorted.isNotEmpty()) {
        if (sorted.isNotEmpty()) firstRow.requestFocusWhenAttached()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "אנשי קשר", textColor = theme.textColor, accentColor = theme.accentColor)
        if (sorted.isEmpty()) {
            EmptyState(
                icon = FutureIcons.Contacts,
                title = "אין אנשי קשר",
                subtitle = "לחץ על מקש התפריט כדי לפתוח את אנשי הקשר",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingXs),
        ) {
            itemsIndexed(sorted, key = { _, c -> c.id }) { index, contact ->
                FutureListItem(
                    title = contact.name,
                    summary = contact.phoneNumber,
                    theme = theme,
                    onClick = { onOpen(contact) },
                    focusRequester = if (index == 0) firstRow else null,
                    leading = { FutureAvatar(theme = theme, name = contact.name, photoUri = contact.photoUri) },
                    trailing = {
                        if (contact.isFavorite) {
                            Icon(FutureIcons.Star, contentDescription = "מועדף", tint = theme.favoriteColor, modifier = Modifier.size(FutureDimens.iconMenuRow))
                        }
                    },
                )
            }
        }
    }
}

/** עברית קודם, אחריה אותיות לועזיות, ובסוף ספרות וסימנים. */
private fun scriptBucket(name: String): Int {
    val first = name.trim().firstOrNull() ?: return 3
    return when {
        first in '֐'..'׿' -> 0
        first.isLetter() && first.code < 0x0250 -> 1
        first.isLetter() -> 2
        else -> 3
    }
}
