package com.future.sfarim.ui.screens
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.components.FutureActionCell

import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sfarim.data.LibraryCategory
import com.future.sfarim.data.ReadingProgressEntry
import com.future.sharednav.theme.FutureTheme

@Composable
fun HomeScreen(
    categories: List<LibraryCategory>,
    continueReading: ReadingProgressEntry?,
    theme: FutureTheme,
    onOpenCategory: (LibraryCategory) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onContinueReading: (ReadingProgressEntry) -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocusRequester.requestFocus() }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingMd),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            item {
                Text(
                    "בלכתך בדרך",
                    fontSize = FutureTypography.headline,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (continueReading != null) {
                item {
                    FutureListItem(
                        title = "${continueReading.bookTitle} · ${continueReading.segmentRef}",
                        summary = "המשך קריאה",
                        theme = theme,
                        onClick = { onContinueReading(continueReading) },
                        focusRequester = firstFocusRequester,
                        leading = { FutureAvatar(theme = theme, icon = Icons.Rounded.AutoStories) },
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    HomeShortcut(
                        FutureIcons.Search, "חיפוש", theme, Modifier.weight(1f),
                        focusRequester = if (continueReading == null) firstFocusRequester else null,
                        onClick = onOpenSearch,
                    )
                    HomeShortcut(FutureIcons.Bookmark, "סימניות", theme, Modifier.weight(1f), onClick = onOpenBookmarks)
                }
            }
            item {
                FutureSectionHeader("קטגוריות", theme, inset = false)
            }
            items(categories, key = { it.id }) { category ->
                FutureListItem(
                    title = category.nameHe?.takeIf { it.isNotBlank() } ?: category.nameEn,
                    theme = theme,
                    onClick = { onOpenCategory(category) },
                    leading = { FutureAvatar(theme = theme, icon = Icons.AutoMirrored.Rounded.LibraryBooks) },
                )
            }
        }
    }
}

@Composable
private fun HomeShortcut(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    FutureActionCell(
        icon = icon,
        label = label,
        theme = theme,
        onClick = onClick,
        modifier = modifier,
        focusRequester = focusRequester,
    )
}
