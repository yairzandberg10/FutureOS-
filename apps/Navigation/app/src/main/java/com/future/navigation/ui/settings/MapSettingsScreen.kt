package com.future.navigation.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.future.navigation.ui.home.RowIcon
import com.future.navigation.ui.map.MapTheme
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor

/**
 * הגדרות מפה: תצוגה אוטומטי / בהירה / כהה. הבחירה נשמרת ב-[MapThemeStore] ומשפיעה
 * על MapConfig.styleUrl. מקשים בלבד: ↑/↓ לבחירה, OK לשמירה, BACK לחזרה.
 */
@Composable
fun MapSettingsScreen(onBack: () -> Unit) {
    val theme = LocalFutureTheme.current
    val store = MapThemeStore.get(LocalContext.current)
    val selected by store.themeFlow.collectAsState()
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "הגדרות מפה",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack,
        )

        FutureSectionHeader(text = "תצוגת מפה", theme = theme, inset = false)

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            OPTIONS.forEachIndexed { index, option ->
                val isSelected = option.theme == selected
                FutureListItem(
                    title = option.label,
                    summary = if (isSelected) "בשימוש" else null,
                    theme = theme,
                    onClick = { store.set(option.theme) },
                    leading = { RowIcon(option.icon, theme) },
                    trailing = if (isSelected) checkMark(theme) else null,
                    focusRequester = if (index == 0) firstFocus else null,
                )
            }
        }

        Text(
            text = "השינוי חל על המפה בפעם הבאה שהניווט נפתח.",
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedTextColor,
            modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
        )
    }
}

private class MapOption(val theme: MapTheme, val label: String, val icon: ImageVector)

private val OPTIONS = listOf(
    MapOption(MapTheme.AUTO, "אוטומטי (לפי השעה)", FutureIcons.Settings),
    MapOption(MapTheme.LIGHT, "בהירה", FutureIcons.WbSunny),
    MapOption(MapTheme.DARK, "כהה", FutureIcons.DarkMode),
)

/** סימון הבחירה בסוף השורה - בצבע הטקסט (לא בהדגשה), כמו שאר האייקונים. */
private fun checkMark(theme: FutureTheme): @Composable RowScope.() -> Unit = {
    Icon(
        FutureIcons.Check,
        contentDescription = null,
        tint = theme.textColor,
        modifier = Modifier.size(FutureDimens.iconTopBar),
    )
}
