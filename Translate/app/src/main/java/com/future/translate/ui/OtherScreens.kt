package com.future.translate.ui

import com.future.sharednav.icons.FutureIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.translate.data.Language
import com.future.translate.data.Languages
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureRoundCapsule
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSnackbarState
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.FutureTabItem
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.subtleTextColor

// ================================================================ בורר שפה

/**
 * "תרגם מ" / "תרגם אל": חיפוש, כרטיס "אחרונות", ו"כל השפות" עם סימון על
 * השפה הנוכחית. "זיהוי שפה" מופיע רק בבורר של שפת המקור.
 */
@Composable
fun LanguagePickerScreen(
    viewModel: TranslateViewModel,
    theme: FutureTheme,
    source: Boolean,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val current by (if (source) viewModel.from else viewModel.to).collectAsState()
    val recentCodes by viewModel.recentLanguages.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()

    val all = remember(source) { (if (source) listOf(Languages.auto) else emptyList()) + Languages.alphabetical }
    val list = remember(q, all) { all.filter { it.name.contains(q) } }
    val recents = if (q.isEmpty()) recentCodes.map { Languages.of(it) } else emptyList()

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = if (source) "תרגם מ" else "תרגם אל",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .escapeTextFieldFocusTrap()
                .padding(bottom = FutureDimens.spacingXl),
        ) {
            FutureTextField(
                value = query,
                onValueChange = { query = it },
                theme = theme,
                placeholder = "חפש שפה",
                autoFocus = true,
                leading = { Icon(FutureIcons.Search, contentDescription = null, tint = theme.subtleTextColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, bottom = 6.dp),
            )
            if (recents.isNotEmpty()) {
                FutureSectionHeader("אחרונות", theme)
                LanguageCard(recents, current, theme, onSelect)
            }
            FutureSectionHeader("כל השפות", theme)
            if (list.isEmpty()) {
                EmptyState(
                    icon = FutureIcons.SearchOff,
                    title = "לא נמצאה שפה",
                    subtitle = "נסה מילה אחרת",
                    textColor = theme.textColor,
                )
            } else {
                LanguageCard(list, current, theme, onSelect)
            }
        }
    }
}

@Composable
private fun LanguageCard(languages: List<Language>, current: String, theme: FutureTheme, onSelect: (String) -> Unit) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    FutureCard(theme = theme) {
        languages.forEachIndexed { index, language ->
            if (index > 0) FutureDivider(theme = theme)
            FutureListItem(
                title = language.name,
                theme = theme,
                onClick = { onSelect(language.code) },
                trailing = if (language.code == current) {
                    { Icon(FutureIcons.Check, contentDescription = "נבחרה", tint = accent, modifier = Modifier.size(FutureDimens.iconSettingRow)) }
                } else null,
            )
        }
    }
}

// ================================================================ היסטוריה

/**
 * היסטוריה: לשוניות "הכול" / "שמורים" (הלשונית נבחרת כשהפוקוס עובר אליה,
 * כמו בערכה), ושורות שאישור עליהן טוען את התרגום חזרה למסך הראשי.
 */
@Composable
fun HistoryScreen(
    viewModel: TranslateViewModel,
    theme: FutureTheme,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onOpen: (com.future.translate.data.HistoryEntry) -> Unit,
) {
    val entries by viewModel.history.entries.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val list = if (tab == 1) entries.filter { it.saved } else entries

    val firstTab = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstTab.requestFocus() } }

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "היסטוריה",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = FutureDimens.spacingXl)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.spacingLg, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                listOf("הכול", "שמורים").forEachIndexed { index, label ->
                    FutureTabItem(
                        label = label,
                        selected = tab == index,
                        theme = theme,
                        onClick = { tab = index },
                        focusRequester = if (index == 0) firstTab else null,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) tab = index },
                    )
                }
            }
            if (list.isEmpty()) {
                if (tab == 1) {
                    EmptyState(
                        icon = FutureIcons.StarBorder,
                        title = "אין תרגומים שמורים",
                        subtitle = "לחץ על שמור בתרגום כדי להוסיף",
                        textColor = theme.textColor,
                    )
                } else {
                    EmptyState(
                        icon = FutureIcons.History,
                        title = "אין תרגומים",
                        subtitle = "לחץ על חזרה והקלד טקסט לתרגום",
                        textColor = theme.textColor,
                    )
                }
            } else {
                FutureCard(theme = theme) {
                    list.forEachIndexed { index, entry ->
                        if (index > 0) FutureDivider(theme = theme)
                        HistoryRow(entry, theme, showLanguages = true, onClick = { onOpen(entry) })
                    }
                }
            }
        }
    }
}

// ================================================================ שיחה

/**
 * מצב שיחה: בועות של כל משפט עם התרגום שלו, ומיקרופון גדול בתחתית. התור
 * עובר לצד השני אחרי כל משפט; חיצי ימין/שמאל על המיקרופון מחליפים דובר.
 * במכשיר בלי זיהוי דיבור המיקרופון הופך למקלדת - אישור פותח שדה הקלדה.
 */
@Composable
fun TalkScreen(
    viewModel: TranslateViewModel,
    theme: FutureTheme,
    actions: TranslateActions,
    onBack: () -> Unit,
    onMenu: () -> Unit,
) {
    val type = rememberFutureType()
    val lines by viewModel.talk.collectAsState()
    val second by viewModel.talkSecondSpeaker.collectAsState()
    val listening by viewModel.listener.listening.collectAsState()
    val (a, b) = viewModel.talkLanguages()
    val speakerCode = if (second) b else a
    val speakerName = Languages.of(speakerCode).name
    val canListen = viewModel.listener.available
    var typing by remember { mutableStateOf(false) }

    val mic = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { mic.requestFocus() } }
    val scroll = rememberScrollState()
    LaunchedEffect(lines.size) { scroll.animateScrollTo(scroll.maxValue) }

    fun activate() {
        when {
            listening -> viewModel.listener.stop()
            canListen -> actions.requestMic {
                viewModel.listener.listen(speakerCode) { heard -> if (heard != null) viewModel.addTalkLine(heard) }
            }
            else -> typing = true
        }
    }

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "שיחה",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = {
            viewModel.listener.stop()
            onBack()
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = FutureDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
            ) {
                if (lines.isEmpty()) {
                    EmptyState(
                        icon = FutureIcons.Forum,
                        title = "אין עדיין שיחה",
                        subtitle = "${Languages.of(a).name} ו${Languages.of(b).name}",
                        textColor = theme.textColor,
                    )
                }
                lines.forEach { line -> TalkBubble(line, firstSide = line.language == a, theme = theme) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, top = 20.dp, bottom = FutureDimens.spacingXl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // מי מדבר עכשיו - הצ'יפ הנבחר. מתחלף בחיצים על המיקרופון.
                Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                    FutureChip(Languages.of(a).name, theme = theme, selected = !second)
                    FutureChip(Languages.of(b).name, theme = theme, selected = second)
                }
                FutureRoundCapsule(
                    icon = when {
                        listening -> FutureIcons.GraphicEq
                        canListen -> FutureIcons.Mic
                        else -> FutureIcons.Keyboard
                    },
                    theme = theme,
                    active = listening,
                    contentDescription = if (canListen) "דבר" else "הקלד",
                    focusRequester = mic,
                    onClick = ::activate,
                    modifier = Modifier.onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || listening) return@onKeyEvent false
                        if (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) {
                            viewModel.switchTalkSpeaker()
                            true
                        } else false
                    },
                )
                Text(
                    when {
                        listening -> "מקשיב · $speakerName"
                        canListen -> "לחץ על אישור כדי לדבר ב$speakerName"
                        else -> "לחץ על אישור כדי להקליד ב$speakerName"
                    },
                    color = theme.mutedTextColor,
                    fontSize = type.body,
                )
            }
        }
    }

    if (typing) {
        InputDialog(
            title = speakerName,
            theme = theme,
            confirmLabel = "תרגם",
            onDismiss = { typing = false },
            onConfirm = { typed ->
                typing = false
                viewModel.addTalkLine(typed.trim())
            },
        )
    }
}

@Composable
private fun TalkBubble(line: TalkLine, firstSide: Boolean, theme: FutureTheme) {
    val type = rememberFutureType()
    val language = Languages.of(line.language)
    val target = Languages.of(line.translationLanguage)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (firstSide) Alignment.CenterStart else Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .widthIn(max = BubbleMaxWidth)
                .clip(FutureShapes.xl)
                .background(if (firstSide) theme.surfaceColor else theme.elevatedSurfaceColor)
                .padding(horizontal = 14.dp, vertical = FutureDimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                language.name,
                color = theme.sectionHeaderColor,
                fontSize = type.summary,
                letterSpacing = FutureTypography.trackingSection,
            )
            CompositionLocalProvider(LocalLayoutDirection provides if (language.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Text(line.text, color = theme.textColor, fontSize = type.bodyLarge)
            }
            CompositionLocalProvider(LocalLayoutDirection provides if (target.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Text(line.translation ?: "מתרגם", color = theme.mutedTextColor, fontSize = type.body)
            }
        }
    }
}

/** 84% מרוחב המסך (320dp) - הבועה לא נמתחת מקצה לקצה. */
private val BubbleMaxWidth = 269.dp

// ================================================================ הורדת שפה

/**
 * הורדת שפות לשימוש בלי רשת: השפות שכבר הורדו (אישור מוחק), והשאר (אישור
 * מוריד). כל שפה היא בערך 30MB.
 */
@Composable
fun DownloadsScreen(
    viewModel: TranslateViewModel,
    theme: FutureTheme,
    snackbar: FutureSnackbarState,
    onBack: () -> Unit,
) {
    val downloaded by viewModel.downloaded.collectAsState()
    val downloading by viewModel.downloading.collectAsState()
    var deleting by remember { mutableStateOf<Language?>(null) }
    LaunchedEffect(Unit) { viewModel.refreshDownloaded() }

    val have = Languages.alphabetical.filter { it.code in downloaded }
    val rest = Languages.alphabetical.filter { it.code !in downloaded }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "הורדת שפה",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = FutureDimens.spacingXl)) {
            if (have.isNotEmpty()) {
                FutureSectionHeader("הורדו", theme)
                FutureCard(theme = theme) {
                    have.forEachIndexed { index, language ->
                        if (index > 0) FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = language.name,
                            summary = "זמינה בלי רשת",
                            icon = FutureIcons.DownloadDone,
                            theme = theme,
                            showChevron = false,
                            focusRequester = if (index == 0) first else null,
                            onClick = { deleting = language },
                        )
                    }
                }
            }
            FutureSectionHeader("זמינות להורדה", theme)
            FutureCard(theme = theme) {
                rest.forEachIndexed { index, language ->
                    if (index > 0) FutureDivider(theme = theme)
                    val busy = language.code in downloading
                    FutureSettingItem(
                        title = language.name,
                        summary = if (busy) "מוריד" else "לא הורדה",
                        icon = FutureIcons.Download,
                        theme = theme,
                        showChevron = false,
                        focusRequester = if (have.isEmpty() && index == 0) first else null,
                        onClick = {
                            if (!busy) viewModel.download(language.code) { ok ->
                                snackbar.show(if (ok) "${language.name} זמינה בלי רשת" else "ההורדה נכשלה")
                            }
                        },
                        trailing = if (busy) {
                            { FutureSpinner(theme = theme, size = 20.dp) }
                        } else null,
                    )
                }
            }
        }
    }

    deleting?.let { language ->
        ConfirmDialog(
            message = "למחוק את ${language.name} מהמכשיר?",
            theme = theme,
            onCancel = { deleting = null },
            onConfirm = {
                deleting = null
                viewModel.delete(language.code)
            },
        )
    }
}

// ================================================================ הגדרות

@Composable
fun SettingsScreen(viewModel: TranslateViewModel, theme: FutureTheme, onBack: () -> Unit) {
    val saveHistory by viewModel.saveHistory.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = "הגדרות",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FutureCard(theme = theme) {
                FutureSettingItem(
                    title = "שמירת היסטוריה",
                    summary = if (saveHistory) "מופעל" else "כבוי",
                    icon = FutureIcons.History,
                    theme = theme,
                    showChevron = false,
                    focusRequester = first,
                    onClick = { viewModel.setSaveHistory(!saveHistory) },
                    trailing = { FutureSwitch(checked = saveHistory, theme = theme) },
                )
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = "הורדה ב-Wi-Fi בלבד",
                    summary = if (wifiOnly) "מופעל" else "כבוי",
                    icon = FutureIcons.Wifi,
                    theme = theme,
                    showChevron = false,
                    onClick = { viewModel.setWifiOnly(!wifiOnly) },
                    trailing = { FutureSwitch(checked = wifiOnly, theme = theme) },
                )
            }
        }
    }
}
