package com.future.guide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.guide.data.GUIDE_APPS
import com.future.sharednav.theme.FutureTheme

@Composable
fun GuideHomeScreen(theme: FutureTheme, focusedAppId: String? = null, onOpen: (String) -> Unit) {
    val firstRowFocusRequester = remember { FocusRequester() }
    val focusIndex = GUIDE_APPS.indexOfFirst { it.id == focusedAppId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = focusIndex)
    // runCatching: בזמן אנימציית המעבר השורה עוד לא מחוברת, ו-requestFocus זורק
    LaunchedEffect(Unit) { runCatching { firstRowFocusRequester.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                GuideHeader(title = "מדריך למשתמש", theme = theme)
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(GUIDE_APPS) { index, app ->
                        GuideAppRow(
                            app.icon,
                            app.packageName,
                            app.name,
                            app.subtitle,
                            theme = theme,
                            onClick = { onOpen(app.id) },
                            focusRequester = if (index == focusIndex) firstRowFocusRequester else null,
                        )
                    }
                }
            }
        }
    }
}
