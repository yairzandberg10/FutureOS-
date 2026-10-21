package com.future.translate.ui
import com.future.sharednav.systemui.StatusBarInset

import com.future.sharednav.icons.FutureIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.translate.data.HistoryEntry
import com.future.translate.data.Languages
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureActionCell
import com.future.sharednav.components.FutureCapsule
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureIndeterminateProgressBar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSnackbarState
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.chevronColor
import com.future.sharednav.theme.favoriteColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.subtleTextColor

/**
 * מסך התרגום (ui_kits/translate): שתי גלולות שפה עם כפתור החלפה ביניהן,
 * כרטיס קלט (שפה, מונה תווים, שדה שנשבר לשורות), כרטיס תוצאה עם ארבע
 * פעולות (השמע · העתק · שתף · שמור), ושני התרגומים האחרונים.
 */
@Composable
fun TranslateScreen(
    viewModel: TranslateViewModel,
    theme: FutureTheme,
    snackbar: FutureSnackbarState,
    actions: TranslateActions,
    onPick: (source: Boolean) -> Unit,
    onOpenHistory: () -> Unit,
    onMenu: () -> Unit,
) {
    val type = rememberFutureType()
    val from by viewModel.from.collectAsState()
    val to by viewModel.to.collectAsState()
    val text by viewModel.text.collectAsState()
    val result by viewModel.result.collectAsState()
    val detected by viewModel.detected.collectAsState()
    val entries by viewModel.history.entries.collectAsState()
    val speaking by viewModel.speaker.speaking.collectAsState()

    val fromCapsule = remember { FocusRequester() }
    val toCapsule = remember { FocusRequester() }
    val textField = remember { FocusRequester() }
    val speakCell = remember { FocusRequester() }
    // בחזרה ממסך משנה הפוקוס חוזר לשדה הטקסט (לא לבורר "מ" כמו בכניסה הראשונה).
    // בחזרה מבורר השפות הפוקוס חוזר לקפסולה שממנה נפתח ("מ" או "אל").
    LaunchedEffect(Unit) {
        runCatching {
            when {
                viewModel.returnToText -> textField.requestFocus()
                viewModel.lastPickWasSource -> fromCapsule.requestFocus()
                else -> toCapsule.requestFocus()
            }
        }
        viewModel.returnToText = false
    }

    ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = "תרגום",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .escapeTextFieldFocusTrap()
                .padding(bottom = FutureDimens.spacingXl),
        ) {
            // ---- שורת השפות
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FutureDimens.screenPadding, end = FutureDimens.screenPadding, bottom = FutureDimens.spacingMd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                FutureCapsule(
                    label = "מ",
                    value = Languages.of(from).name,
                    theme = theme,
                    onClick = { viewModel.lastPickWasSource = true; onPick(true) },
                    focusRequester = fromCapsule,
                    modifier = Modifier.weight(1f),
                )
                TopBarIconButton(
                    icon = FutureIcons.SwapHoriz,
                    contentDescription = "החלף שפות",
                    textColor = theme.textColor,
                    accentColor = theme.accentColor,
                    onClick = {
                        viewModel.swap()
                        snackbar.show("השפות הוחלפו")
                    },
                )
                FutureCapsule(
                    label = "אל",
                    value = Languages.of(to).name,
                    theme = theme,
                    onClick = { viewModel.lastPickWasSource = false; onPick(false) },
                    focusRequester = toCapsule,
                    modifier = Modifier.weight(1f),
                )
            }

            // ---- הקלט
            val inputLanguage = if (from == Languages.AUTO) detected?.let { Languages.of(it) } else Languages.of(from)
            FutureCard(theme = theme) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = FutureDimens.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                from != Languages.AUTO -> Languages.of(from).name
                                inputLanguage != null -> "זוהתה ${inputLanguage.name}"
                                else -> Languages.auto.name
                            },
                            color = theme.sectionHeaderColor,
                            fontSize = type.summary,
                            letterSpacing = FutureTypography.trackingSection,
                            modifier = Modifier.weight(1f),
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text("${text.length}/1000", color = theme.subtleTextColor, fontSize = type.summary)
                        }
                    }
                    FutureTextField(
                        value = text,
                        onValueChange = viewModel::setText,
                        theme = theme,
                        placeholder = "הקלד טקסט לתרגום",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 8,
                        focusRequester = textField,
                        modifier = Modifier
                            .fillMaxWidth()
                            // יציאה מהשדה רושמת את התרגום בהיסטוריה - לא כל אות.
                            .onFocusChanged { if (!it.hasFocus) viewModel.commit() }
                            // ↓ מהשדה נוחת על הפעולה הראשונה ("השמע") ולא על "העתק" באמצע השורה
                            .focusProperties { down = if (result is Result.Done) speakCell else FocusRequester.Default },
                    )
                }
            }

            // ---- התוצאה
            if (text.isBlank()) {
                EmptyState(
                    icon = FutureIcons.Translate,
                    title = "אין מה לתרגם",
                    subtitle = "לחץ על אישור בשדה כדי להקליד",
                    textColor = theme.textColor,
                )
            } else {
                ResultCard(
                    viewModel = viewModel,
                    theme = theme,
                    targetName = Languages.of(to).name,
                    rtl = Languages.of(to).rtl,
                    result = result,
                    speaking = speaking,
                    saved = viewModel.isCurrentSaved(entries),
                    onSpeak = { done ->
                        viewModel.commit()
                        if (speaking) viewModel.speaker.stop()
                        else if (!viewModel.speaker.speak(done, to)) snackbar.show("אין הקראה ל${Languages.of(to).name}")
                    },
                    onCopy = { done ->
                        viewModel.commit()
                        actions.copy(done)
                        snackbar.show("הועתק")
                    },
                    onShare = { done ->
                        viewModel.commit()
                        actions.share(done)
                    },
                    onSave = {
                        when (viewModel.toggleSaved()) {
                            true -> snackbar.show("נשמר")
                            false -> snackbar.show("הוסר מהשמורים")
                            null -> Unit
                        }
                    },
                    onRetry = viewModel::retry,
                    speakFocus = speakCell,
                )
            }

            // ---- אחרונות
            val recent = entries.take(2)
            if (recent.isNotEmpty()) {
                FutureSectionHeader("אחרונות", theme)
                FutureCard(theme = theme) {
                    recent.forEachIndexed { index, entry ->
                        if (index > 0) FutureDivider(theme = theme)
                        // OK על תרגום אחרון מחזיר אותו למסך - קודם פתח את כל ההיסטוריה
                        // ושם היה צריך לחפש אותו שוב.
                        HistoryRow(entry, theme, showLanguages = false, onClick = { viewModel.commit(); viewModel.load(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    viewModel: TranslateViewModel,
    theme: FutureTheme,
    targetName: String,
    rtl: Boolean,
    result: Result,
    speaking: Boolean,
    saved: Boolean,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit = {},
    speakFocus: FocusRequester? = null,
) {
    val type = rememberFutureType()
    FutureCard(theme = theme) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = FutureDimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                targetName,
                color = theme.sectionHeaderColor,
                fontSize = type.summary,
                letterSpacing = FutureTypography.trackingSection,
            )
            when (result) {
                is Result.Done -> {
                    // כיוון הטקסט הוא של שפת היעד - אנגלית משמאל לימין גם בממשק RTL.
                    CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                        Text(
                            result.text,
                            color = theme.textColor,
                            fontSize = type.title,
                            fontWeight = FutureTypography.weightMedium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (speaking) FutureIndeterminateProgressBar(theme = theme)
                }
                Result.Translating, Result.Empty -> {
                    Text("מתרגם", color = theme.mutedTextColor, fontSize = type.title)
                    FutureIndeterminateProgressBar(theme = theme)
                }
                Result.Downloading -> {
                    Text("מוריד את $targetName", color = theme.mutedTextColor, fontSize = type.title)
                    FutureIndeterminateProgressBar(theme = theme)
                }
                is Result.Failed -> {
                    Text(result.message, color = theme.dangerColor, fontSize = type.title)
                    // T-10: אחרי כשל אפשר לנסות שוב בלי לשנות את הטקסט.
                    com.future.sharednav.components.FutureButton(
                        "נסה שוב",
                        theme,
                        onRetry,
                        variant = com.future.sharednav.components.FutureButtonVariant.Secondary,
                    )
                }
            }
        }
        // הפעולות מופיעות רק כשיש תרגום - מה שאי אפשר להפעיל לא מקבל פוקוס.
        if (result is Result.Done) {
            FutureDivider(theme = theme)
            Row(
                modifier = Modifier.padding(horizontal = FutureDimens.spacingMd, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                FutureActionCell(
                    icon = FutureIcons.AutoMirrored.VolumeUp,
                    label = "השמע",
                    theme = theme,
                    active = speaking,
                    height = ActionHeight,
                    onClick = { onSpeak(result.text) },
                    focusRequester = speakFocus,
                    modifier = Modifier.weight(1f),
                )
                FutureActionCell(
                    icon = FutureIcons.ContentCopy,
                    label = "העתק",
                    theme = theme,
                    height = ActionHeight,
                    onClick = { onCopy(result.text) },
                    modifier = Modifier.weight(1f),
                )
                FutureActionCell(
                    icon = FutureIcons.Share,
                    label = "שתף",
                    theme = theme,
                    height = ActionHeight,
                    onClick = { onShare(result.text) },
                    modifier = Modifier.weight(1f),
                )
                FutureActionCell(
                    icon = if (saved) FutureIcons.Star else FutureIcons.StarBorder,
                    label = "שמור",
                    theme = theme,
                    iconColor = if (saved) theme.favoriteColor else null,
                    height = ActionHeight,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * שורת היסטוריה: המקור, ומתחתיו התרגום (ובהיסטוריה המלאה גם הכיוון). כוכב
 * לשמור, חץ כניסה לשאר.
 */
@Composable
internal fun HistoryRow(
    entry: HistoryEntry,
    theme: FutureTheme,
    showLanguages: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val summary = if (showLanguages) {
        "${entry.dst} · ${Languages.of(entry.from).name} ← ${Languages.of(entry.to).name}"
    } else entry.dst
    FutureListItem(
        title = entry.src,
        summary = summary,
        theme = theme,
        onClick = onClick,
        focusRequester = focusRequester,
        trailing = {
            if (entry.saved) {
                Icon(FutureIcons.Star, contentDescription = "שמור", tint = theme.favoriteColor, modifier = Modifier.size(16.dp))
            } else {
                Icon(
                    FutureIcons.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = theme.chevronColor,
                    modifier = Modifier.size(FutureDimens.iconTopBar),
                )
            }
        },
    )
}

/** 60dp - תא פעולה מתחת לתרגום; האייקון יוצא 26dp. */
private val ActionHeight = 60.dp
