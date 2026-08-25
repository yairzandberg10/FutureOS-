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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sfarim.data.LibraryBookmark
import com.future.sfarim.ui.components.FocusableItem
import com.future.sfarim.ui.components.ScreenTopBar
import com.future.sfarim.ui.theme.FutureTheme

@Composable
fun BookmarksScreen(
    bookmarks: List<LibraryBookmark>,
    theme: FutureTheme,
    onBack: () -> Unit,
    onOpenBookmark: (LibraryBookmark) -> Unit,
    onDeleteBookmark: (LibraryBookmark) -> Unit,
) {
    val firstFocusRequester = remember(bookmarks.firstOrNull()) { FocusRequester() }
    LaunchedEffect(bookmarks.firstOrNull()) {
        if (bookmarks.isNotEmpty()) firstFocusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = "סימניות", theme = theme, onBack = onBack)
                if (bookmarks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין סימניות עדיין", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(bookmarks, key = { _, it -> it.id }) { index, bookmark ->
                            BookmarkRow(
                                bookmark, theme,
                                focusRequester = if (index == 0) firstFocusRequester else null,
                                onClick = { onOpenBookmark(bookmark) },
                                onDelete = { onDeleteBookmark(bookmark) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: LibraryBookmark,
    theme: FutureTheme,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    FocusableItem(
        onClick = onClick,
        theme = theme,
        focusRequester = focusRequester,
        modifier = Modifier.fillMaxWidth().onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && (event.key == Key.Menu || event.key == Key.Settings)) {
                onDelete(); true
            } else false
        },
    ) { _ ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = theme.accentColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bookmark.bookTitle, color = theme.textColor, fontSize = 15.sp, maxLines = 1)
                Text(bookmark.segmentRef, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
            }
            // אייקון תצוגתי בלבד (לא focusable) - המחיקה כבר זמינה דרך מקש
            // Menu על השורה עצמה (למעלה). לפני התיקון האייקון היה עטוף
            // ב-TopBarIconButton שהוא focusable משלו, מקונן בתוך FocusableItem
            // שגם הוא focusable - שני יעדי פוקוס על אותה שורה שכנראה לא היו
            // באמת נגישים שניהם ב-D-pad.
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "מחק סימניה",
                tint = theme.textColor.copy(alpha = 0.6f),
            )
        }
    }
}
