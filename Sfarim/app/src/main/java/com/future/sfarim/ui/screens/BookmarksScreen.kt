package com.future.sfarim.ui.screens

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.mutedTextColor
import androidx.compose.foundation.layout.size

import com.future.sharednav.theme.FutureTypography
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
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
import com.future.sfarim.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

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
    // לפני התיקון, מקש Menu על שורת סימניה מחק אותה מיידית בלי שום אישור -
    // לחיצה אחת בטעות ואיבדת סימניה שמורה לצמיתות.
    var pendingDelete by remember { mutableStateOf<LibraryBookmark?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = "סימניות", theme = theme, onBack = onBack)
                if (bookmarks.isEmpty()) {
                    EmptyState(
                        icon = FutureIcons.Bookmark,
                        title = "אין סימניות עדיין",
                        textColor = theme.textColor,
                    )
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
                                onDelete = { pendingDelete = bookmark },
                            )
                        }
                    }
                }
            }

            pendingDelete?.let { bookmark ->
                ConfirmDialog(
                    message = "למחוק את הסימניה \"${bookmark.bookTitle}\"?",
                    surfaceColor = theme.surfaceColor,
                    textColor = theme.textColor,
                    dangerColor = theme.dangerColor,
                    onCancel = { pendingDelete = null },
                    onConfirm = {
                        onDeleteBookmark(bookmark)
                        pendingDelete = null
                    },
                )
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
    FutureListItem(
        title = bookmark.bookTitle,
        summary = bookmark.segmentRef,
        theme = theme,
        onClick = onClick,
        focusRequester = focusRequester,
        modifier = Modifier.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && (event.key == Key.Menu || event.key == Key.Settings)) {
                onDelete(); true
            } else false
        },
        leading = { com.future.sfarim.ui.components.RowIcon(FutureIcons.Bookmark, theme) },
        // אייקון תצוגתי בלבד (לא focusable) - המחיקה זמינה דרך מקש Menu על
        // השורה עצמה. שני יעדי פוקוס על אותה שורה לא נגישים שניהם ב-D-pad.
        trailing = {
            Icon(
                FutureIcons.Delete,
                contentDescription = "מחק סימניה",
                tint = theme.mutedTextColor,
                modifier = Modifier.size(FutureDimens.iconTopBar),
            )
        },
    )
}
