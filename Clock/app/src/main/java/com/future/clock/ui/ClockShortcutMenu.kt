package com.future.clock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.future.clock.data.ClockShortcuts
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureTheme

/**
 * תפריט מקש Options של שעון העצר והטיימר: "הוסף כאפליקציה" - הקיצור
 * העצמאי במסך הבית (activity-alias, ClockShortcuts). קודם זה ישב בלשונית
 * "שעון", שהוסרה. [extra] - שורות נוספות של המסך עצמו.
 */
@Composable
fun ClockShortcutMenu(
    route: ClockRoute,
    title: String,
    theme: FutureTheme,
    onMessage: (String) -> Unit,
    extra: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    onOptionsKeyPress { open = !open }
    if (!open) return
    val pinned = ClockShortcuts.isPinnedToHome(context, route)
    FutureOptionsMenu(theme = theme, onDismissRequest = { open = false }, header = title) {
        extra?.invoke { open = false }
        FutureMenuRow(if (pinned) "הסר מרשימת האפליקציות" else "הוסף כאפליקציה", FutureIcons.Apps, theme, {
            open = false
            ClockShortcuts.setPinnedToHome(context, route, !pinned)
            onMessage(if (pinned) "$title הוסר מרשימת האפליקציות" else "$title נוסף לרשימת האפליקציות")
        })
    }
}
