package com.future.sfarim.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Search
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
import com.future.sfarim.ui.components.FocusableItem
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    "בלכתך בדרך",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (continueReading != null) {
                item {
                    FocusableItem(
                        onClick = { onContinueReading(continueReading) },
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = firstFocusRequester,
                    ) { isFocused ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = theme.accentColor)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("המשך קריאה", color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text(
                                    "${continueReading.bookTitle} — ${continueReading.segmentRef}",
                                    color = theme.textColor,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    HomeShortcut(
                        Icons.Rounded.Search, "חיפוש", theme, Modifier.weight(1f),
                        focusRequester = if (continueReading == null) firstFocusRequester else null,
                        onClick = onOpenSearch,
                    )
                    HomeShortcut(Icons.Rounded.Bookmark, "סימניות", theme, Modifier.weight(1f), onClick = onOpenBookmarks)
                }
            }
            item {
                Text(
                    "קטגוריות",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(categories, key = { it.id }) { category ->
                FocusableItem(
                    onClick = { onOpenCategory(category) },
                    theme = theme,
                    modifier = Modifier.fillMaxWidth(),
                ) { _ ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.LibraryBooks, contentDescription = null, tint = theme.textColor.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            category.nameHe?.takeIf { it.isNotBlank() } ?: category.nameEn,
                            color = theme.textColor,
                            fontSize = 16.sp,
                        )
                    }
                }
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
    FocusableItem(onClick = onClick, theme = theme, modifier = modifier, focusRequester = focusRequester) { _ ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = theme.accentColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, color = theme.textColor, fontSize = 13.sp)
        }
    }
}
