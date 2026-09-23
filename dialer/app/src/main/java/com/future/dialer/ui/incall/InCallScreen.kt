package com.future.dialer.ui.incall

import android.telecom.Call
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.dialer.R
import com.future.dialer.ui.CallFormat
import com.future.dialer.ui.requestFocusWhenAttached
import com.future.sharednav.components.FutureActionCell
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.sectionHeaderColor

/**
 * מסכי השיחה (ui_kits/calls): שיחה נכנסת, שיחה פעילה, וסיום שיחה.
 *
 * - נכנסת: אווטאר, שם ומספר, ו"ענה" / "דחה" ככפתורים מלאים. מקש התפריט
 *   פותח הודעה מהירה, ושליחה שלה דוחה את השיחה.
 * - פעילה: מונה זמן, אריחי פקד בשתי עמודות (השתק, רמקול, המתנה, מקלדת,
 *   הקלטה, הודעה) ו"סיים שיחה". חזרה ממזערת - השיחה ממשיכה עם פס בראש
 *   שאר המסכים; מקש הניתוק מנתק.
 * - סיום: המשך השיחה, "התקשר שוב" / "חזור ליומן". רק לשיחה שנענתה - שיחה
 *   שנדחתה או לא נענתה חוזרת ישר ליומן.
 */
@Composable
fun InCallScreen(
    name: String,
    phoneNumber: String,
    viewModel: InCallViewModel,
    onCallEnded: () -> Unit,
    onCallAgain: (String) -> Unit,
) {
    val theme = LocalFutureTheme.current
    val context = LocalContext.current
    val callState by viewModel.callState.collectAsState()
    val isRinging by viewModel.isRinging.collectAsState()
    val duration by viewModel.callDuration.collectAsState()
    val quickMessage by viewModel.isQuickMessageVisible.collectAsState()

    // אחרי שהשיחה נגמרת callState הוא null והמשך מתאפס - שומרים את מה שהיה.
    var wasAnswered by remember { mutableStateOf(false) }
    var lastDuration by remember { mutableLongStateOf(0L) }
    var ended by remember { mutableStateOf(false) }
    LaunchedEffect(callState, duration) {
        if (callState == Call.STATE_ACTIVE || callState == Call.STATE_HOLDING) wasAnswered = true
        if (duration > 0) lastDuration = duration
    }
    LaunchedEffect(Unit) { viewModel.startDurationTimer() }
    LaunchedEffect(callState) {
        if (callState == null) {
            viewModel.closeQuickMessage()
            if (wasAnswered) ended = true else onCallEnded()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            when {
                ended -> EndedCall(
                    name = name,
                    phoneNumber = phoneNumber,
                    duration = lastDuration,
                    theme = theme,
                    onCallAgain = { onCallAgain(phoneNumber) },
                    onBackToLog = onCallEnded,
                )
                isRinging -> IncomingCall(
                    name = name,
                    phoneNumber = phoneNumber,
                    theme = theme,
                    onAnswer = viewModel::answer,
                    onReject = viewModel::reject,
                )
                else -> ActiveCall(
                    name = name,
                    phoneNumber = phoneNumber,
                    callState = callState,
                    duration = duration,
                    viewModel = viewModel,
                    theme = theme,
                    onHoldUnsupported = {
                        Toast.makeText(context, "השיחה לא תומכת בהמתנה", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    if (quickMessage && !ended) {
        FutureOptionsMenu(theme = theme, onDismissRequest = viewModel::closeQuickMessage, header = "הודעה מהירה") {
            InCallViewModel.quickMessages.forEach { message ->
                FutureMenuRow(message, null, theme, onClick = {
                    // בשיחה מצלצלת ההודעה היא התשובה - שולחים ודוחים.
                    val rejecting = isRinging
                    viewModel.sendQuickMessage(context, phoneNumber, message)
                    if (rejecting) viewModel.reject()
                })
            }
        }
    }
}

// ---------------------------------------------------------------- נכנסת

@Composable
private fun IncomingCall(
    name: String,
    phoneNumber: String,
    theme: FutureTheme,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
) {
    val type = rememberFutureType()
    val answer = remember { FocusRequester() }
    LaunchedEffect(Unit) { answer.requestFocusWhenAttached() }
    // שיחה מצלצלת נשארת עד מענה או דחייה, כמו בטלפון אמיתי.
    BackHandler(enabled = true) {}

    Column(
        modifier = Modifier.fillMaxSize().padding(FutureDimens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            Text(
                stringResource(R.string.incoming_call),
                color = theme.mutedTextColor,
                fontSize = type.label,
                letterSpacing = FutureTypography.trackingSection,
            )
            CallerAvatar(name, phoneNumber, theme, IncomingAvatar)
            Text(
                name,
                color = theme.textColor,
                fontSize = type.display,
                fontWeight = FutureTypography.weightBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (name != phoneNumber) Number(phoneNumber, theme)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            FutureButton(stringResource(R.string.answer), theme, onAnswer, fillMaxWidth = true, focusRequester = answer)
            FutureButton(stringResource(R.string.decline), theme, onReject, variant = FutureButtonVariant.Destructive, fillMaxWidth = true)
        }
    }
}

// ---------------------------------------------------------------- פעילה

@Composable
private fun ActiveCall(
    name: String,
    phoneNumber: String,
    callState: Int?,
    duration: Long,
    viewModel: InCallViewModel,
    theme: FutureTheme,
    onHoldUnsupported: () -> Unit,
) {
    val type = rememberFutureType()
    val context = LocalContext.current
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isOnHold by viewModel.isOnHold.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isDialpadVisible by viewModel.isDialpadVisible.collectAsState()
    val quickMessage by viewModel.isQuickMessageVisible.collectAsState()
    val dtmfDigits by viewModel.dtmfDigits.collectAsState()

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FutureDimens.spacingXl, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CallerAvatar(name, phoneNumber, theme, ActiveAvatar)
            Text(
                name,
                color = theme.textColor,
                fontSize = type.screenTitle,
                fontWeight = FutureTypography.weightBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
            )
            val status = when {
                isOnHold -> "בהמתנה"
                callState == Call.STATE_DIALING || callState == Call.STATE_CONNECTING -> stringResource(R.string.dialing)
                else -> null
            }
            if (status != null) {
                Text(status, color = theme.mutedTextColor, fontSize = type.body)
            } else {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(CallFormat.duration(duration), color = theme.successColor, fontSize = type.body)
                }
            }
            if (isDialpadVisible) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        dtmfDigits.ifEmpty { " " },
                        color = theme.textColor,
                        fontSize = type.headline,
                        maxLines = 1,
                    )
                }
            }
            if (isRecording) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.FiberManualRecord, contentDescription = null, tint = theme.dangerColor, modifier = Modifier.size(12.dp))
                    Text(stringResource(R.string.recording_in_progress), color = theme.dangerColor, fontSize = type.summary)
                }
            }
        }

        // האריחים של ActionGrid בשתי עמודות. ארבעת הראשונים הם של הערכה;
        // הקלטה והודעה הם יכולות קיימות של החייגן, באותו רכיב.
        val controls = listOf(
            Control(if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                stringResource(if (isMuted) R.string.unmute else R.string.mute), isMuted) { viewModel.toggleMute() },
            Control(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.speaker), isSpeakerOn) { viewModel.toggleSpeaker() },
            Control(if (isOnHold) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                if (isOnHold) "המשך" else "המתנה", isOnHold) { if (!viewModel.toggleHold()) onHoldUnsupported() },
            Control(Icons.Rounded.Dialpad, stringResource(R.string.keypad), isDialpadVisible) { viewModel.toggleDialpad() },
            Control(Icons.Rounded.FiberManualRecord,
                stringResource(if (isRecording) R.string.stop_recording else R.string.record), isRecording,
                iconColor = if (isRecording) theme.dangerColor else null) { viewModel.toggleRecording(context) },
            Control(Icons.Rounded.Sms, stringResource(R.string.send_message), quickMessage) { viewModel.toggleQuickMessage() },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            controls.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                    row.forEachIndexed { i, c ->
                        FutureActionCell(
                            icon = c.icon,
                            label = c.label,
                            theme = theme,
                            onClick = c.onClick,
                            active = c.active,
                            iconColor = c.iconColor,
                            height = ControlHeight,
                            focusRequester = if (rowIndex == 0 && i == 0) first else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FutureDimens.spacingXl, end = FutureDimens.spacingXl, top = FutureDimens.spacingMd, bottom = 20.dp),
        ) {
            FutureButton(
                text = stringResource(R.string.end_call),
                theme = theme,
                onClick = viewModel::hangUp,
                variant = FutureButtonVariant.Destructive,
                fillMaxWidth = true,
            )
        }
    }
}

private class Control(
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val iconColor: androidx.compose.ui.graphics.Color? = null,
    val onClick: () -> Unit,
)

// ---------------------------------------------------------------- סיום

@Composable
private fun EndedCall(
    name: String,
    phoneNumber: String,
    duration: Long,
    theme: FutureTheme,
    onCallAgain: () -> Unit,
    onBackToLog: () -> Unit,
) {
    val type = rememberFutureType()
    val again = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { again.requestFocus() } }
    BackHandler(onBack = onBackToLog)

    Column(
        modifier = Modifier.fillMaxSize().padding(FutureDimens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd, Alignment.CenterVertically),
    ) {
        CallerAvatar(name, phoneNumber, theme, ActiveAvatar)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                name,
                color = theme.textColor,
                fontSize = type.screenTitle,
                fontWeight = FutureTypography.weightBold,
                textAlign = TextAlign.Center,
            )
            Text("השיחה הסתיימה · ${CallFormat.duration(duration)}", color = theme.mutedTextColor, fontSize = type.body)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            FutureButton("התקשר שוב", theme, onCallAgain, fillMaxWidth = true, focusRequester = again)
            FutureButton("חזור ליומן", theme, onBackToLog, variant = FutureButtonVariant.Quiet, fillMaxWidth = true)
        }
    }
}

// ---------------------------------------------------------------- עזרים

/** האווטאר של המערכת; מספר בלי שם מקבל אייקון ולא "ראשי תיבות" של ספרות. */
@Composable
private fun CallerAvatar(name: String, phoneNumber: String, theme: FutureTheme, size: Dp) {
    val hasName = name.isNotBlank() && name != phoneNumber
    FutureAvatar(
        theme = theme,
        name = if (hasName) name else null,
        icon = if (hasName) null else Icons.Rounded.Person,
        size = size,
    )
}

/** מספר טלפון לא מתהפך בתוך שורה עברית. */
@Composable
private fun Number(phoneNumber: String, theme: FutureTheme) {
    val type = rememberFutureType()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(phoneNumber, color = theme.mutedTextColor, fontSize = type.body)
    }
}

/** 100dp - אווטאר השיחה הנכנסת (200px בערכה). */
private val IncomingAvatar = 100.dp

/** 80dp - אווטאר השיחה הפעילה והסיום (160px בערכה). */
private val ActiveAvatar = 80.dp

/** 60dp - אריח פקד בשיחה; שש משבצות בשלוש שורות נכנסות מעל "סיים שיחה". */
private val ControlHeight = 60.dp
