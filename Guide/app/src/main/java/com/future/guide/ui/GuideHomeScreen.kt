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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
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
    var rowFocused by remember { mutableStateOf(false) }
    // runCatching: בזמן אנימציית המעבר השורה עוד לא מחוברת, ו-requestFocus זורק
    // ניסיון יחיד לא מספיק: בזמן האנימציה השורה מתחברת בפריים מאוחר יותר - מנסים שוב בכל פריים.
    // R13 (GD-01): המסך היוצא (הפרטים) מחזיק פוקוס בזמן המעבר ונעלם בסופו, והפוקוס נופל.
    // לכן מחזירים את הפוקוס לשורה כל עוד היא לא ממוקדת, לזמן קצר בלבד (כמו המעבר).
    LaunchedEffect(Unit) {
        repeat(40) {
            if (!rowFocused) runCatching { firstRowFocusRequester.requestFocus() }
            withFrameNanos { }
        }
    }

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
                            modifier = if (index == focusIndex) Modifier.onFocusChanged { rowFocused = it.hasFocus } else Modifier,
                        )
                    }
                }
            }
        }
    }
}
