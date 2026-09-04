package com.future.sfarim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.future.sfarim.data.LibraryBook
import com.future.sfarim.data.LibraryCategory
import com.future.sfarim.ui.components.FocusableItem
import com.future.sfarim.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

sealed class BrowseEntry {
    abstract val sortOrder: Int
    data class Cat(val category: LibraryCategory) : BrowseEntry() {
        override val sortOrder get() = category.sortOrder
    }
    data class Bk(val book: LibraryBook) : BrowseEntry() {
        override val sortOrder get() = book.sortOrder
    }
}

private fun keyOf(entry: BrowseEntry): String = when (entry) {
    is BrowseEntry.Cat -> "c${entry.category.id}"
    is BrowseEntry.Bk -> "b${entry.book.id}"
}

@Composable
fun BrowseScreen(
    title: String,
    childCategories: List<LibraryCategory>,
    books: List<LibraryBook>,
    theme: FutureTheme,
    onBack: () -> Unit,
    onOpenCategory: (LibraryCategory) -> Unit,
    onOpenBook: (LibraryBook) -> Unit,
    // המפתח (ראו keyOf) של הפריט שנפתח לאחרונה מתוך המסך הזה - כשחוזרים
    // "אחורה" הפוקוס צריך לשוב אליו בדיוק, לא תמיד לפריט הראשון ברשימה.
    lastSelectedKey: String? = null,
) {
    val entries = (childCategories.map { BrowseEntry.Cat(it) } + books.map { BrowseEntry.Bk(it) })
        .sortedBy { it.sortOrder }

    val rowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(entries.map { keyOf(it) }) {
        val target = entries.firstOrNull { keyOf(it) == lastSelectedKey } ?: entries.firstOrNull()
        target?.let { rowFocusRequesters.getOrPut(keyOf(it)) { FocusRequester() }.requestFocus() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = title, theme = theme, onBack = onBack)
                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין תוכן בקטגוריה זו", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(entries, key = { _, it -> keyOf(it) }) { _, entry ->
                            val rowFocusRequester = rowFocusRequesters.getOrPut(keyOf(entry)) { FocusRequester() }
                            when (entry) {
                                is BrowseEntry.Cat -> BrowseRow(
                                    icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                                    label = entry.category.nameHe?.takeIf { it.isNotBlank() } ?: entry.category.nameEn,
                                    theme = theme,
                                    focusRequester = rowFocusRequester,
                                    onClick = { onOpenCategory(entry.category) },
                                )
                                is BrowseEntry.Bk -> BrowseRow(
                                    icon = Icons.Rounded.Book,
                                    label = entry.book.displayTitle,
                                    theme = theme,
                                    focusRequester = rowFocusRequester,
                                    onClick = { onOpenBook(entry.book) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    theme: FutureTheme,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    FocusableItem(onClick = onClick, theme = theme, modifier = Modifier.fillMaxWidth(), focusRequester = focusRequester) { _ ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = theme.textColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, color = theme.textColor, fontSize = 15.sp, maxLines = 2)
        }
    }
}
