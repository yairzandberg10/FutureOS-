package com.future.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.future.settings.feedback.FeedbackPayload
import com.future.settings.feedback.FeedbackRepository
import com.future.settings.feedback.FeedbackRepository.AppChoice
import com.future.settings.feedback.FeedbackRepository.SubmitResult
import com.future.settings.feedback.FeedbackType
import com.future.settings.ui.components.SettingDivider
import com.future.settings.ui.components.SettingHeader
import com.future.settings.ui.components.SettingItem
import com.future.settings.ui.components.SettingSwitch
import com.future.settings.ui.components.SettingsCard
import com.future.settings.ui.theme.ThemeConfig
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** שלבי המסך. הכול במסך אחד כדי שהטיוטה לא תאבד במעבר לתצוגה המקדימה וחזרה. */
private enum class FeedbackStep { Compose, PickApp, Preview, Done, History }

/**
 * "שלח משוב": בחירת סוג ואפליקציה, תיאור, האם לצרף לוג, ואז תצוגה מקדימה של בדיוק מה שנשלח.
 * initialPkg - כשנפתח מתפריט Options של אפליקציה אחרת (EXTRA_FEEDBACK_PKG), האפליקציה כבר נבחרה.
 */
@Composable
fun FeedbackScreen(navController: NavController, theme: ThemeConfig, initialPkg: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by rememberSaveable { mutableStateOf(FeedbackStep.Compose) }
    var type by rememberSaveable { mutableStateOf(FeedbackType.Bug) }
    var appPkg by rememberSaveable { mutableStateOf(initialPkg ?: FeedbackRepository.SYSTEM_PKG) }
    var text by rememberSaveable { mutableStateOf("") }
    var includeLog by rememberSaveable { mutableStateOf(FeedbackRepository.lastIncludeLog(context)) }
    var prepared by remember { mutableStateOf<FeedbackPayload?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SubmitResult?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    val app: AppChoice = remember(appPkg) {
        FeedbackRepository.appChoice(context, appPkg) ?: AppChoice(FeedbackRepository.SYSTEM_PKG, "מערכת כללית", null)
    }
    val logAvailable = remember(appPkg) { FeedbackRepository.canAttachLog(context, appPkg) }

    // בכל כניסה: שולחים מה שחיכה בתור ומושכים הודעות חדשות ("בטיפול", "תוקן בגרסה X")
    LaunchedEffect(Unit) {
        FeedbackRepository.flushOutbox(context)
        FeedbackRepository.pollInbox(context)
        refresh++
    }

    BackHandler(enabled = step != FeedbackStep.Compose) {
        if (step == FeedbackStep.Done) navController.popBackStack() else step = FeedbackStep.Compose
    }

    val title = when (step) {
        FeedbackStep.Compose -> "שלח משוב"
        FeedbackStep.PickApp -> "על איזו אפליקציה?"
        FeedbackStep.Preview -> "מה יישלח"
        FeedbackStep.Done -> "שלח משוב"
        FeedbackStep.History -> "המשובים שלי"
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(title, theme) {
                if (step == FeedbackStep.Compose || step == FeedbackStep.Done) navController.popBackStack() else step = FeedbackStep.Compose
            }
            when (step) {
                FeedbackStep.Compose -> ComposeStep(
                    theme = theme,
                    type = type,
                    onType = { type = it },
                    app = app,
                    onPickApp = { step = FeedbackStep.PickApp },
                    text = text,
                    onText = { text = it.take(FeedbackPayload.MAX_TEXT) },
                    includeLog = includeLog && logAvailable,
                    logAvailable = logAvailable,
                    onIncludeLog = { includeLog = it },
                    busy = busy,
                    historyCount = remember(refresh) { FeedbackRepository.history(context).size },
                    onHistory = { step = FeedbackStep.History },
                    onNext = {
                        if (text.isNotBlank() && !busy) {
                            busy = true
                            scope.launch {
                                prepared = FeedbackRepository.prepare(context, type, app, text, includeLog && logAvailable)
                                busy = false
                                step = FeedbackStep.Preview
                            }
                        }
                    },
                )

                FeedbackStep.PickApp -> PickAppStep(theme, appPkg) { picked ->
                    appPkg = picked
                    step = FeedbackStep.Compose
                }

                FeedbackStep.Preview -> prepared?.let { payload ->
                    PreviewStep(
                        theme = theme,
                        payload = payload,
                        busy = busy,
                        onRemoveLog = { prepared = payload.copy(log = null) },
                        onEdit = { step = FeedbackStep.Compose },
                        onSend = {
                            if (!busy) {
                                busy = true
                                scope.launch {
                                    result = FeedbackRepository.submit(context, payload)
                                    busy = false
                                    text = ""
                                    refresh++
                                    step = FeedbackStep.Done
                                }
                            }
                        },
                    )
                }

                FeedbackStep.Done -> DoneStep(theme, result, onHistory = { step = FeedbackStep.History }, onClose = { navController.popBackStack() })

                FeedbackStep.History -> HistoryStep(theme, remember(refresh) { FeedbackRepository.history(context) })
            }
        }
    }
}

@Composable
private fun ComposeStep(
    theme: ThemeConfig,
    type: FeedbackType,
    onType: (FeedbackType) -> Unit,
    app: AppChoice,
    onPickApp: () -> Unit,
    text: String,
    onText: (String) -> Unit,
    includeLog: Boolean,
    logAvailable: Boolean,
    onIncludeLog: (Boolean) -> Unit,
    busy: Boolean,
    historyCount: Int,
    onHistory: () -> Unit,
    onNext: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    LazyColumn {
        item { SettingHeader("סוג", theme) }
        item {
            SettingsCard(theme) {
                FeedbackType.entries.forEachIndexed { index, t ->
                    SettingItem(
                        title = t.label,
                        summary = when (t) {
                            FeedbackType.Bug -> "משהו לא עובד כמו שצריך"
                            FeedbackType.Idea -> "רעיון לשיפור"
                            FeedbackType.Other -> null
                        },
                        icon = if (t == type) FutureIcons.RadioButtonChecked else FutureIcons.RadioButtonUnchecked,
                        theme = theme,
                        showChevron = false,
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                    ) { onType(t) }
                    if (index < FeedbackType.entries.lastIndex) SettingDivider(theme)
                }
            }
        }
        item { SettingHeader("אפליקציה", theme) }
        item {
            SettingsCard(theme) {
                SettingItem(app.label, app.version?.let { "גרסה $it" } ?: "לחץ כדי לבחור", FutureIcons.Apps, theme, onClick = onPickApp)
            }
        }
        item { SettingHeader("מה קרה?", theme) }
        item {
            FutureTextField(
                value = text,
                onValueChange = onText,
                theme = theme.futureTheme,
                placeholder = if (type == FeedbackType.Idea) "מה היית רוצה שיהיה?" else "מה לחצת, מה קרה, ומה ציפית שיקרה",
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.spacingMd)
                    .escapeTextFieldFocusTrap(),
            )
        }
        item {
            Text(
                "${text.length}/${FeedbackPayload.MAX_TEXT}",
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = FutureTypography.label,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        item {
            SettingsCard(theme) {
                if (logAvailable) {
                    SettingSwitch("צרף לוג טכני", "תראה אותו לפני השליחה", includeLog, onIncludeLog, theme)
                } else {
                    SettingItem(
                        "לוג טכני לא זמין",
                        "לוג של אפליקציות אחרות יצורף אחרי המעבר ל-FutureOS המלא",
                        FutureIcons.Info,
                        theme,
                        showChevron = false,
                        infoFocusable = true,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (busy) FutureSpinner(theme = theme.futureTheme)
                else FutureButton("המשך לתצוגה מקדימה", theme.futureTheme, onNext, fillMaxWidth = true, enabled = text.isNotBlank())
            }
        }
        if (historyCount > 0) {
            item {
                SettingsCard(theme) {
                    SettingItem("המשובים שלי", "$historyCount משובים", FutureIcons.Chat, theme, onClick = onHistory)
                }
            }
        }
        item {
            Text(
                "המשוב נשלח לשרת FutureOS עם גרסת המערכת, דגם הטלפון ושם האפליקציה. מספרי טלפון, מיילים ומיקום מוסרים אוטומטית. לא נשלחים אנשי קשר, הודעות או IMEI.",
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = FutureTypography.label,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PickAppStep(theme: ThemeConfig, selected: String, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val apps = remember { FeedbackRepository.appChoices(context) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LazyColumn {
        item {
            SettingsCard(theme) {
                apps.forEachIndexed { index, a ->
                    SettingItem(
                        a.label,
                        a.version?.let { "גרסה $it" },
                        if (a.pkg == selected) FutureIcons.RadioButtonChecked else FutureIcons.RadioButtonUnchecked,
                        theme,
                        showChevron = false,
                        modifier = if (a.pkg == selected || (index == 0 && apps.none { it.pkg == selected })) Modifier.focusRequester(focus) else Modifier,
                    ) { onPick(a.pkg) }
                    if (index < apps.lastIndex) SettingDivider(theme)
                }
            }
        }
    }
}

@Composable
private fun PreviewStep(
    theme: ThemeConfig,
    payload: FeedbackPayload,
    busy: Boolean,
    onRemoveLog: () -> Unit,
    onEdit: () -> Unit,
    onSend: () -> Unit,
) {
    val sendFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { sendFocus.requestFocus() } }
    // הלוג מחולק לגושים של 8 שורות, כל גוש שורה שמקבלת פוקוס - כך גוללים בו במקשים
    val logChunks = remember(payload.log) { payload.log?.lines()?.chunked(8)?.map { it.joinToString("\n") } ?: emptyList() }

    LazyColumn {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
                horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                if (busy) {
                    FutureSpinner(theme = theme.futureTheme)
                } else {
                    FutureButton("שלח", theme.futureTheme, onSend, modifier = Modifier.weight(1f), focusRequester = sendFocus)
                    FutureButton("ערוך", theme.futureTheme, onEdit, modifier = Modifier.weight(1f), variant = FutureButtonVariant.Secondary)
                }
            }
        }
        item { SettingHeader("פרטים", theme) }
        item {
            SettingsCard(theme) {
                SettingItem("סוג", payload.type.label, null, theme, showChevron = false, infoFocusable = true)
                SettingDivider(theme)
                SettingItem("אפליקציה", listOfNotNull(payload.appLabel, payload.appVersion).joinToString(" "), null, theme, showChevron = false, infoFocusable = true)
                SettingDivider(theme)
                SettingItem("גרסת מערכת", payload.build, null, theme, showChevron = false, infoFocusable = true)
                SettingDivider(theme)
                SettingItem("דגם", payload.model, null, theme, showChevron = false, infoFocusable = true)
            }
        }
        item { SettingHeader("התיאור שלך (אחרי סינון)", theme) }
        item {
            SettingsCard(theme) {
                SettingItem(payload.text, null, null, theme, showChevron = false, infoFocusable = true)
            }
        }
        if (payload.log != null) {
            item { SettingHeader("לוג טכני (${payload.log.lines().size} שורות)", theme) }
            item {
                SettingsCard(theme) {
                    SettingItem("הסר את הלוג", "המשוב יישלח בלי לוג", FutureIcons.Info, theme, showChevron = false, onClick = onRemoveLog)
                }
            }
            items(logChunks) { chunk ->
                SettingsCard(theme) {
                    SettingItem(chunk, null, null, theme, showChevron = false, infoFocusable = true)
                }
            }
        }
        item {
            Text(
                "זה בדיוק מה שיישלח. מספרי טלפון, מיילים, מזהים ומיקום הוחלפו בסימונים כמו [phone].",
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = FutureTypography.label,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun DoneStep(theme: ThemeConfig, result: SubmitResult?, onHistory: () -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val (headline, detail) = when (result) {
        SubmitResult.Sent -> "המשוב נשלח, תודה" to "כשהמשוב ייכנס לטיפול או יתוקן, תראה את זה ב\"המשובים שלי\"."
        SubmitResult.Rejected -> "המשוב לא נשלח" to "השרת לא קיבל את המשוב. נסה לקצר את התיאור או להסיר את הלוג."
        else -> "המשוב נשמר" to "אין כרגע חיבור לשרת. המשוב יישלח אוטומטית בפעם הבאה שתיכנס למסך הזה."
    }
    LazyColumn {
        item {
            SettingsCard(theme) {
                SettingItem(headline, detail, FutureIcons.Send, theme, showChevron = false, infoFocusable = true)
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                FutureButton("סגור", theme.futureTheme, onClose, fillMaxWidth = true, focusRequester = focus)
                FutureButton("המשובים שלי", theme.futureTheme, onHistory, fillMaxWidth = true, variant = FutureButtonVariant.Secondary)
            }
        }
    }
}

@Composable
private fun HistoryStep(theme: ThemeConfig, history: List<FeedbackRepository.HistoryItem>) {
    val fmt = remember { SimpleDateFormat("d.M HH:mm", Locale.ROOT) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LazyColumn {
        if (history.isEmpty()) {
            item {
                Text(
                    "עוד לא שלחת משוב",
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = FutureTypography.body,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
        item {
            SettingsCard(theme) {
                history.forEachIndexed { index, h ->
                    val status = FeedbackRepository.statusLabel(h.status)
                    SettingItem(
                        "$status · ${h.appLabel} · ${fmt.format(Date(h.createdAt))}",
                        h.message ?: h.preview,
                        if (h.status == "fixed") FutureIcons.Check else FutureIcons.Chat,
                        theme,
                        showChevron = false,
                        infoFocusable = true,
                        modifier = if (index == 0) Modifier.focusRequester(focus) else Modifier,
                    )
                    if (index < history.lastIndex) SettingDivider(theme)
                }
            }
        }
    }
}
