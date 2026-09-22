package com.future.dialer.ui.incall

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureShapes
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.dialer.R
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.t9.T9DigitMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InCallScreen(
    name: String,
    phoneNumber: String,
    viewModel: InCallViewModel,
    onCallEnded: () -> Unit,
    onMinimize: () -> Unit = {}
) {
    val callState by viewModel.callState.collectAsState()
    val isRinging by viewModel.isRinging.collectAsState()
    val duration by viewModel.callDuration.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isDialpadVisible by viewModel.isDialpadVisible.collectAsState()
    val dtmfDigits by viewModel.dtmfDigits.collectAsState()
    val isQuickMessageVisible by viewModel.isQuickMessageVisible.collectAsState()
    val context = LocalContext.current

    // צבעי הסטטוס מגיעים מהערכה ולא מלוח Material: האדום/הירוק שהיו כאן
    // (F44336 / 4CAF50) אינם בפלטה של המערכת ולא הגיבו למצב כהה/בהיר.
    val futureTheme = com.future.sharednav.theme.LocalFutureTheme.current
    val onSurfaceColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceMuted = onSurfaceColor.copy(alpha = 0.6f)
    // ה-scrim מעל טפט הרקע המטושטש חייב להיות בניגוד ל-onBackground (הצבע שבו כתוב
    // הטקסט) כדי שהטקסט יישאר קריא בשני מצבי העיצוב: כהה (onBackground לבן -> scrim שחור)
    // ובהיר (onBackground שחור -> scrim לבן).
    val scrimColor = if (onSurfaceColor.luminance() > 0.5f) Color.Black else Color.White

    val answerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { viewModel.startDurationTimer() }
    LaunchedEffect(callState) {
        if (callState == null) onCallEnded()
    }
    // פוקוס ראשוני על כפתור המענה ברגע שמסך שיחה נכנסת מוצג - בלי זה אין שום
    // פריט ממוקד ב-D-pad, וטיפול המקש הגלובלי (onKeyDown) יכול "לחטוף" את הלחיצה.
    LaunchedEffect(Unit) {
        if (isRinging) answerFocusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            BlurredWallpaperBackground(scrimColor)
            // מזעור זמין רק כששיחה פעילה (לא בזמן צלצול) - כמו בטלפון אמיתי, שיחה
            // מצלצלת חייבת מענה/דחייה לפני שאפשר לצאת ממסך השיחה.
            if (!isRinging) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, end = 20.dp), contentAlignment = Alignment.TopEnd) {
                    FocusableItem(onClick = onMinimize, accentColor = MaterialTheme.colorScheme.primary) {
                        Box(
                            modifier = Modifier.size(40.dp).background(onSurfaceColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.minimize_call),
                                tint = onSurfaceColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            Column(
                modifier = Modifier.padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallerAvatar(
                    initial = name.take(1).uppercase(),
                    accentColor = MaterialTheme.colorScheme.primary,
                    onSurfaceColor = onSurfaceColor,
                    isPulsing = isRinging
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(name, fontSize = FutureTypography.display, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                Text(phoneNumber, fontSize = FutureTypography.title, color = onSurfaceMuted)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (isRinging) stringResource(R.string.incoming_call) else formatDuration(duration),
                    fontSize = FutureTypography.headline,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceColor.copy(alpha = 0.7f)
                )
                if (isRecording) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FiberManualRecord, contentDescription = null, tint = futureTheme.dangerColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.recording_in_progress), fontSize = FutureTypography.body, color = futureTheme.dangerColor)
                    }
                }
                if (!isRinging && isDialpadVisible) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = dtmfDigits.ifEmpty { "•" },
                        fontSize = FutureTypography.headline,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceColor,
                        modifier = Modifier
                            .background(onSurfaceColor.copy(alpha = 0.08f), FutureShapes.md)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DtmfKeypad(
                        onSurfaceColor = onSurfaceColor,
                        onDigit = { digit ->
                            viewModel.onDtmfDigitPressed(digit)
                            viewModel.onDtmfDigitReleased()
                        }
                    )
                }
                if (!isRinging && isQuickMessageVisible) {
                    Spacer(modifier = Modifier.height(20.dp))
                    QuickMessagePanel(onSurfaceColor = onSurfaceColor) { message ->
                        viewModel.sendQuickMessage(context, phoneNumber, message)
                    }
                }
            }

            if (isRinging) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RoundCallButton(icon = Icons.Rounded.CallEnd, background = futureTheme.dangerColor, onClick = { viewModel.reject() })
                    RoundCallButton(icon = Icons.Rounded.Call, background = futureTheme.successColor, onClick = { viewModel.answer() }, focusRequester = answerFocusRequester)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // ארבעה-חמישה אריחים בשתי עמודות, ולא שורה של עיגולים
                    // (ui_kits/calls). בשורה אחת התוויות נחתכו, והאריח נותן
                    // לכל פקד שטח פוקוס גדול מספיק במכשיר בלי מגע.
                    CallControlGrid {
                        CallActionIcon(
                            icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label = if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                            onClick = { viewModel.toggleMute() },
                            tint = onSurfaceColor,
                            modifier = Modifier.weight(1f),
                            isActive = isMuted
                        )
                        CallActionIcon(
                            icon = if (isSpeakerOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            label = if (isSpeakerOn) stringResource(R.string.handset) else stringResource(R.string.speaker),
                            onClick = { viewModel.toggleSpeaker() },
                            tint = onSurfaceColor,
                            modifier = Modifier.weight(1f),
                            isActive = isSpeakerOn
                        )
                        CallActionIcon(
                            icon = Icons.Rounded.Dialpad,
                            label = stringResource(R.string.keypad),
                            onClick = { viewModel.toggleDialpad() },
                            tint = onSurfaceColor,
                            modifier = Modifier.weight(1f),
                            isActive = isDialpadVisible
                        )
                        CallActionIcon(
                            icon = Icons.Rounded.FiberManualRecord,
                            label = if (isRecording) stringResource(R.string.stop_recording) else stringResource(R.string.record),
                            onClick = { viewModel.toggleRecording(context) },
                            tint = if (isRecording) futureTheme.dangerColor else onSurfaceColor,
                            modifier = Modifier.weight(1f),
                            isActive = isRecording
                        )
                        CallActionIcon(
                            icon = Icons.Rounded.Sms,
                            label = if (isQuickMessageVisible) stringResource(R.string.close_message) else stringResource(R.string.send_message),
                            onClick = { viewModel.toggleQuickMessage() },
                            tint = onSurfaceColor,
                            modifier = Modifier.weight(1f),
                            isActive = isQuickMessageVisible
                        )
                    }
                    com.future.sharednav.components.FutureButton(
                        text = stringResource(R.string.end_call),
                        theme = futureTheme,
                        variant = com.future.sharednav.components.FutureButtonVariant.Destructive,
                        fillMaxWidth = true,
                        onClick = { viewModel.hangUp() },
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
            }
        }
    }
}

/**
 * אווטאר עגול עם טבעת בצבע ההדגשה המשותף (במקום ריבוע מעוגל אפור שטוח) - נותן
 * למסך השיחה מראה "אמיתי" יותר. בזמן צלצול הטבעת "נושמת" (פועמת בעדינות) כדי
 * שהמסך ירגיש חי, בלי טקסט מהבהב או אנימציה תזזיתית.
 */
/**
 * האווטאר של הדיזיין סיסטם בגודל גיבור (Avatar.jsx: 88 ברשימה, 176px = 88dp
 * כגיבור; כאן 120dp כי הוא כל המסך). בזמן צלצול טבעת ב-30% מהטקסט "נושמת"
 * סביבו. בלי צל ובלי הילה בצבע ההדגשה: הצל היחיד במערכת הוא של כרטיס,
 * וההדגשה אינה קישוט.
 */
@Composable
private fun CallerAvatar(initial: String, accentColor: Color, onSurfaceColor: Color, isPulsing: Boolean) {
    val theme = com.future.sharednav.theme.LocalFutureTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RingPulseMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarPulseScale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .graphicsLayerScale(pulseScale)
                .border(2.dp, onSurfaceColor.copy(alpha = 0.3f), CircleShape)
        )
        com.future.sharednav.components.FutureAvatar(
            theme = theme,
            name = initial,
            size = 120.dp,
            contentColor = onSurfaceColor,
        )
    }
}

/** נשימה אחת של הטבעת בזמן צלצול - לולאה, ולא מעבר, ולכן מחוץ לסקאלת התנועה. */
private const val RingPulseMillis = 1100

private fun Modifier.graphicsLayerScale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

/**
 * רקע מסך השיחה: מילוי שטוח של הערכה. קודם זה היה טפט מטושטש (32dp) מתחת
 * להכהיה - אבל "there is no blur in this system" ו"the background is a flat
 * fill" (README של הדיזיין סיסטם).
 */
@Composable
private fun BlurredWallpaperBackground(@Suppress("UNUSED_PARAMETER") scrimColor: Color) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
}

/**
 * לוח DTMF אמיתי וניתן ל"לחיצה" (בפועל: ניווט מקלדת פיזי/D-pad ובחירה - אין מסך
 * מגע במכשיר) - לפני זה תצוגת הספרות שנלחצו הייתה טקסט בלבד בלי שום דבר לבחור בו.
 * מתן דגש דרך FocusableItem היחיד הזמין למקש בודד; אין gesture נפרד ל"לחיצה
 * ממושכת" בלי מסך מגע, אז כל בחירה שולחת טון DTMF קצר אחד.
 */
@Composable
private fun DtmfKeypad(onSurfaceColor: Color, onDigit: (Char) -> Unit) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )
    Column(
        modifier = Modifier.widthIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    DtmfKey(digit = digit, onSurfaceColor = onSurfaceColor, onClick = { onDigit(digit) })
                }
            }
        }
    }
}

@Composable
private fun DtmfKey(digit: Char, onSurfaceColor: Color, onClick: () -> Unit) {
    val letters = T9DigitMap.ENGLISH[digit].orEmpty()
    FocusableItem(onClick = onClick, accentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
        Box(
            modifier = Modifier.fillMaxSize().background(onSurfaceColor.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(digit.toString(), fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Medium, color = onSurfaceColor)
                if (letters.isNotEmpty()) {
                    Text(letters, fontSize = FutureTypography.caption, color = onSurfaceColor.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun QuickMessagePanel(onSurfaceColor: Color, onSend: (String) -> Unit) {
    Surface(
        modifier = Modifier.widthIn(max = 320.dp),
        shape = FutureShapes.lg,
        color = onSurfaceColor.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            InCallViewModel.quickMessages.forEach { message ->
                FocusableItem(onClick = { onSend(message) }, accentColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        text = message,
                        fontSize = FutureTypography.bodyLarge,
                        color = onSurfaceColor,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundCallButton(icon: ImageVector, background: Color, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 68.dp, focusRequester: FocusRequester? = null) {
    FocusableItem(onClick = onClick, accentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size), focusRequester = focusRequester) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = com.future.sharednav.theme.FutureContrast.onColor(background), modifier = Modifier.size(size / 2.5f))
        }
    }
}

/**
 * כפתור פעולה עגול עם רקע (השתקה/רמקול/מקלדת/הקלטה/הודעה) - קודם היה אייקון
 * חשוף בלי שום רקע, מה שנראה "לא גמור" ליד שני כפתורי השיחה העגולים והמלאים.
 * מצב פעיל (isActive) ממלא את העיגול בצבע ההדגשה, בדיוק כמו מסכי שיחה אמיתיים.
 */
@Composable
fun CallActionIcon(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface, isActive: Boolean = false, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    // tint/onPrimary נגזרים מה-theme במקום Color.White/Black קשיחים - בלי זה
    // האייקונים היו נעלמים על רקע בהיר (מצב לא-כהה) והניגוד במצב פעיל (isActive)
    // לא היה מובטח לצבע הדגשה שהמשתמש בחר.
    val theme = com.future.sharednav.theme.LocalFutureTheme.current
    // אריח: 66dp, רדיוס 16dp, רקע "זכוכית" - או 20% מההדגשה כשהפקד דלוק.
    FocusableItem(
        onClick = onClick,
        accentColor = accent,
        idleBackgroundColor = if (isActive) accent.copy(alpha = 0.20f) else theme.elevatedSurfaceColor,
        focusedBackgroundColor = if (isActive) accent.copy(alpha = 0.20f) else theme.elevatedSurfaceColor,
        cornerRadius = com.future.sharednav.theme.FutureShapes.radiusLg,
        scaleOnFocus = false,
        modifier = modifier.height(66.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) accent else tint.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                label,
                color = theme.mutedTextColor,
                fontSize = FutureTypography.summary,
                maxLines = 1,
            )
        }
    }
}

/** שתי עמודות של אריחי פקד, במרווח של 8dp (ui_kits/calls). */
@Composable
private fun CallControlGrid(content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            content()
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
