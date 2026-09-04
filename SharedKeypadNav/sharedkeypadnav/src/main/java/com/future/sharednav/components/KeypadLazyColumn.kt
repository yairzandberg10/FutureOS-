package com.future.sharednav.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.future.sharednav.nav.FocusListState
import com.future.sharednav.nav.keypadListNav

/**
 * LazyColumn שמגלגל אוטומטית לפריט הממוקד עם D-pad. זה הפתרון השיטתי לפער
 * שנמצא בסריקה: 59 קבצים במערכת משתמשים ב-LazyColumn/LazyRow/
 * LazyVerticalGrid, אבל רק 2 מהם (Terminal, FutureUI/RecentAppsScreen)
 * בפועל מגלגלים לפריט הממוקד. בכל שאר 57 הקבצים, ברשימה ארוכה מהמסך, אחרי
 * כמה לחיצות DOWN הפריט הממוקד יוצא מתחום המסך והמשתמש מאבד מגע איתו -
 * זה היה הפגם השיטתי הכי נפוץ שנמצא בכל ה-audit.
 *
 * שימוש טיפוסי:
 * ```
 * val focus = rememberFocusListState(items.size)
 * KeypadLazyColumn(items, focus, onSelect = { i, item -> ... }) { i, item, isFocused ->
 *     FocusableItem(...) { MyRow(item, isFocused) }
 * }
 * ```
 */
@Composable
fun <T> KeypadLazyColumn(
    items: List<T>,
    focusState: FocusListState,
    onSelect: (index: Int, item: T) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T, isFocused: Boolean) -> Unit,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }

    LazyColumn(
        state = focusState.listState,
        modifier = modifier
            .focusRequester(requester)
            .focusable()
            .keypadListNav(focusState, onSelect = { index -> onSelect(index, items[index]) }),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        itemsIndexed(items, key = key?.let { k -> { index: Int, item: T -> k(index, item) } }) { index, item ->
            itemContent(index, item, focusState.isFocused(index))
        }
    }
}
