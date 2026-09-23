package com.future.futureui.controlcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.futureui.ui.theme.ShellGlass
import com.future.futureui.ui.theme.shellFocusRing
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.keyboard.KeyboardLanguage
import com.future.sharednav.keyboard.KeyboardSettingsClient
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "שפות מקלדת" מתוך מרכז הבקרה: לכל שפה - כבויה / הקלדה בלבד / הקלדה וניבוי.
 * OK מחליף מצב; # במקלדת עובר רק על השפות הפעילות, והניבוי רץ רק בשפות
 * שסומנו לו. BACK סוגר (ControlCenterAccessibilityService).
 */
@Composable
fun KeyboardLanguagesPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val theme = LocalFutureTheme.current
    var languages by remember { mutableStateOf<List<KeyboardLanguage>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }
    var selected by remember { mutableStateOf(0) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(selected) { if (languages.isNotEmpty()) listState.animateScrollToItem(selected) }

    fun toggle(index: Int) {
        val lang = languages.getOrNull(index) ?: return
        val updated = KeyboardSettingsClient.cycleLanguage(context, lang)
        languages = languages.map { if (it.code == lang.code) updated else it }
    }

    LaunchedEffect(Unit) {
        languages = withContext(Dispatchers.IO) { KeyboardSettingsClient.languages(context) }
            .sortedByDescending { it.enabled }
        loaded = true
    }
    LaunchedEffect(loaded) { if (loaded) runCatching { firstFocus.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // הפוקוס נשאר כולו בחלונית (אינדקס נבחר) - החצים לא "בורחים" למרכז הבקרה שמאחור.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShellGlass.scrim(theme))
                .focusRequester(firstFocus)
                .focusable()
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent true
                    when (e.key) {
                        Key.DirectionDown -> selected = (selected + 1).coerceAtMost((languages.size - 1).coerceAtLeast(0))
                        Key.DirectionUp -> selected = (selected - 1).coerceAtLeast(0)
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> toggle(selected)
                        else -> return@onKeyEvent false
                    }
                    true
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(FutureShapes.dialog)
                    .background(ShellGlass.panel(theme))
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    "שפות מקלדת",
                    color = ShellGlass.ink(theme),
                    fontSize = FutureTypography.screenTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Text(
                    "OK מחליף: כבויה · הקלדה · הקלדה וניבוי",
                    color = ShellGlass.inkMuted(theme),
                    fontSize = FutureTypography.caption,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
                )
                when {
                    !loaded -> Unit
                    languages.isEmpty() -> Text(
                        "המקלדת לא מותקנת",
                        color = ShellGlass.inkMuted(theme),
                        fontSize = FutureTypography.body,
                        modifier = Modifier.padding(20.dp),
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.heightIn(max = 520.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(languages, key = { _, l -> l.code }) { index, lang ->
                            LanguageRow(lang = lang, focused = index == selected)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(lang: KeyboardLanguage, focused: Boolean) {
    val theme = LocalFutureTheme.current
    val shape = FutureShapes.lg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (focused) ShellGlass.tileFocused(theme) else ShellGlass.tile(theme))
            .shellFocusRing(focused, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            lang.name,
            color = if (lang.enabled) ShellGlass.ink(theme) else ShellGlass.inkMuted(theme),
            fontSize = FutureTypography.bodyLarge,
            fontWeight = if (lang.enabled) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        val on = lang.enabled
        Text(
            KeyboardSettingsClient.stateLabel(lang),
            color = if (on) ShellGlass.onInk(theme) else ShellGlass.inkMuted(theme),
            fontSize = FutureTypography.caption,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(FutureShapes.pill)
                .background(if (on) ShellGlass.on(theme) else ShellGlass.tile(theme))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
