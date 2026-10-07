package com.future.assistant.ui

import com.future.sharednav.theme.FutureAccents
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onGloballyPositioned
import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.assistant.asr.LocalSpeechEngine
import com.future.assistant.asr.PiperTts
import com.future.assistant.data.AssistantAction
import com.future.assistant.data.Beatbox
import com.future.assistant.data.CommandProcessor
import com.future.assistant.data.KnowledgeBase
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.scrimColor
import com.future.sharednav.theme.sectionHeaderColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * העוזר כחלונית צפה בתחתית המסך (כמו Gemini/Bixby) מעל האפליקציה שפתוחה
 * מתחת: מסך שקוף, שכבת העמעום של הדיאלוגים, וכרטיס בפינות 28dp (משטח
 * המערכת, כמו ההתראה הצפה). בתוכו: מה שנשמע, התשובה, גלי קול וכפתור
 * המיקרופון.
 */
@Composable
fun AssistantScreen(theme: FutureTheme, onExit: () -> Unit) {
    val context = LocalContext.current
    val type = rememberFutureType()
    val hasPermission by rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)
    // לא חוסמות את שאר העוזר אם המשתמש מסרב - רק "מה יש לי היום" ו"התקשר
    // ל..." לא יעבדו במלואן (יחזירו הודעה מתאימה במקום לקרוס).
    rememberRuntimePermission(Manifest.permission.READ_CALENDAR)
    rememberRuntimePermission(Manifest.permission.READ_CONTACTS)

    var state by remember { mutableStateOf(AssistantState.IDLE) }
    var heardText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("במה אפשר לעזור?") }
    var pendingClose by remember { mutableStateOf(false) }
    var modelFailed by remember { mutableStateOf(false) }
    // עוצמה 0..1 - מהמיקרופון בזמן הקשבה ומההשמעה בזמן דיבור. מניע את הגלים.
    var level by remember { mutableFloatStateOf(0f) }
    val micFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // מנוע Text-to-Speech נוירוני מקומי (ReNikud + Piper, ר' PiperTts) - מופע
    // אחד לתהליך, כך שפתיחה שנייה של העוזר לא טוענת את המודלים מחדש.
    val piperTts = remember { PiperTts.shared(context) }
    val speechEngine = remember { LocalSpeechEngine(context) }
    // גוגל בעברית כשאפשר, Whisper המקומי כגיבוי (ר' HybridSpeech).
    val speech = remember { com.future.assistant.asr.HybridSpeech(context, speechEngine) }

    fun speak(text: String, spoken: String = text, action: AssistantAction? = null) {
        state = AssistantState.SPEAKING
        responseText = text
        scope.launch(Dispatchers.IO) {
            piperTts.init()
            piperTts.speak(spoken) { level = it }
            if (action == AssistantAction.BEATBOX) Beatbox.play { level = it }
            withContext(Dispatchers.Main) {
                level = 0f
                state = AssistantState.IDLE
                if (pendingClose) {
                    delay(300)
                    onExit()
                } else {
                    runCatching { micFocus.requestFocus() }
                }
            }
        }
    }

    fun handleRecognizedText(text: String) {
        heardText = text
        if (text.isBlank()) {
            state = AssistantState.IDLE
            responseText = "לא זיהיתי דיבור, נסה שוב"
        } else {
            try {
                val result = CommandProcessor.process(context, text)
                pendingClose = result.shouldClose
                speak(result.responseText, result.speech, result.action)
            } catch (e: Exception) {
                pendingClose = false
                speak("משהו השתבש בביצוע הפקודה, נסה שוב")
            }
        }
    }

    // מאגר הידע ו-TTS נטענים ברקע מיד. מודל זיהוי הדיבור כבר בטעינה מ-onCreate
    // (MainActivity) - כאן רק מקבלים את התוצאה כדי להציג כישלון.
    LaunchedEffect(Unit) {
        LocalSpeechEngine.preloadAsync(context) { ok -> if (!ok) modelFailed = true }
        withContext(Dispatchers.IO) {
            launch { KnowledgeBase.preload(context) }
            launch { piperTts.init() }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) runCatching { micFocus.requestFocus() }
    }

    // יציאה מהעוזר באמצע האזנה (HOME, שיחה נכנסת) - המיקרופון נסגר מיד ולא ממשיך
    // להקליט ברקע עד מגבלת 30 השניות; ההקלטה נמחקת בלי תמלול.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && state == AssistantState.LISTENING) {
                state = AssistantState.IDLE
                speech.cancel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onMicClick() {
        when (state) {
            AssistantState.IDLE -> {
                heardText = ""
                state = AssistantState.LISTENING
                // ההקלטה מתחילה מיד, גם אם מודל Whisper עוד נטען - התמלול יחכה לו.
                speech.start(
                    onLevel = { level = it },
                    // גוגל מסיים לבד אחרי שקט - בלי לחכות ל-OK.
                    onAutoResult = { text ->
                        scope.launch(Dispatchers.Main) {
                            if (state == AssistantState.LISTENING) {
                                level = 0f
                                handleRecognizedText(text)
                            }
                        }
                    },
                )
            }
            AssistantState.LISTENING -> {
                state = AssistantState.THINKING
                level = 0f
                scope.launch {
                    val text = speech.stopAndTranscribe()
                    handleRecognizedText(text)
                }
            }
            else -> {}
        }
    }

    // הכרטיס עולה מלמטה כשהחלונית נפתחת.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.scrimColor)) {
            EdgeGlow(
                active = state != AssistantState.IDLE,
                level = level,
                thinking = state == AssistantState.THINKING,
                modifier = Modifier.fillMaxSize(),
            )
            AnimatedVisibility(
                visible = shown,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(FutureMotion.enter()) { it } + fadeIn(FutureMotion.enter()),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FutureDimens.spacingMd)
                        .background(theme.surfaceColor, FutureShapes.xxl)
                        .padding(horizontal = FutureDimens.spacingXl, vertical = FutureDimens.spacingLg),
                ) {
                    Text(
                        CommandProcessor.NAME,
                        color = theme.sectionHeaderColor,
                        fontSize = type.summary,
                        fontWeight = FutureTypography.weightMedium,
                    )
                    if (heardText.isNotBlank()) {
                        Text(
                            heardText,
                            color = theme.mutedTextColor,
                            fontSize = type.body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = FutureDimens.spacingSm),
                        )
                    }
                    Text(
                        when {
                            !hasPermission -> "נדרשת הרשאת מיקרופון"
                            modelFailed -> "טעינת מנוע זיהוי הדיבור נכשלה"
                            state == AssistantState.LISTENING -> "מקשיב"
                            state == AssistantState.THINKING -> "מתמלל"
                            else -> responseText
                        },
                        color = theme.textColor,
                        fontSize = type.title,
                        fontWeight = FutureTypography.weightMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = FutureDimens.spacingSm),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingLg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
                    ) {
                        SiriWave(
                            theme = theme,
                            level = level,
                            active = state != AssistantState.IDLE,
                            thinking = state == AssistantState.THINKING,
                            modifier = Modifier.weight(1f).height(WaveHeight),
                        )
                        if (hasPermission) {
                            MicButton(
                                state = state,
                                theme = theme,
                                focusRequester = micFocus,
                                autoFocus = true,
                                onClick = { onMicClick() },
                            )
                        }
                    }
                    Text(
                        text = "כשהחיבור זמין, קטע אודיו עשוי להישלח לשירות חיצוני לצורך תמלול.",
                        color = theme.mutedTextColor,
                        fontSize = type.summary,
                        modifier = Modifier.padding(top = FutureDimens.spacingSm),
                    )
                    Text(
                        if (state == AssistantState.LISTENING) "OK לסיום" else "OK לדיבור",
                        color = theme.mutedTextColor,
                        fontSize = type.summary,
                        modifier = Modifier.padding(top = FutureDimens.spacingSm),
                    )
                }
            }
        }
    }
}

/**
 * גלי קול בסגנון סירי: שלושה גלי סינוס שנעלמים בקצוות (פונקציית הנחתה
 * K/(K+x^4)), במשרעת שעוקבת אחרי עוצמת הקול. הצבע הוא ההדגשה בלבד, בשלוש
 * מדרגות של סולם השקיפות (100/55/30) - בלי גרדיאנט ובלי צבעים חדשים.
 * במנוחה - קו כמעט ישר; בתמלול - נשימה איטית קבועה.
 */
@Composable
private fun SiriWave(theme: FutureTheme, level: Float, active: Boolean, thinking: Boolean, modifier: Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) phase += (now - last) / 1_000_000_000f * if (active) 9f else 2.5f
                last = now
            }
        }
    }
    val target = when {
        thinking -> 0.25f + 0.1f * sin(phase * 0.6f)
        active -> 0.12f + 0.88f * level
        else -> 0.05f
    }
    val amp by animateFloatAsState(target, FutureMotion.fast(), label = "waveAmp")
    val accent = theme.readableAccentColor
    Canvas(modifier) {
        val mid = size.height / 2f
        val layers = listOf(Triple(1f, 1f, 2.dp.toPx()), Triple(0.7f, 0.55f, 1.5.dp.toPx()), Triple(0.45f, 0.3f, 1.dp.toPx()))
        layers.forEachIndexed { idx, (scale, alpha, stroke) ->
            val path = Path()
            val steps = 64
            for (i in 0..steps) {
                val x = i / steps.toFloat()
                val k = x * 4f - 2f // -2..2
                val atten = (4f / (4f + k.pow(4))).pow(2)
                val freq = 1.5f + idx * 0.6f
                val y = mid + sin(x * 2f * PI.toFloat() * freq - phase * (1f + idx * 0.3f)) *
                    atten * amp * scale * (size.height / 2f - stroke)
                if (i == 0) path.moveTo(x * size.width, y) else path.lineTo(x * size.width, y)
            }
            drawPath(path, accent.copy(alpha = alpha), style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

/**
 * כפתור המיקרופון - FutureButton בגרסת העיגול: מילוי אטום (ההדגשה, או צבע
 * הסכנה בזמן הקלטה), טבעת פוקוס 2dp בצבע הכפתור במרחק 2dp, והגדלה ל-1.02.
 */
@Composable
private fun MicButton(state: AssistantState, theme: FutureTheme, focusRequester: FocusRequester, autoFocus: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    // הפוקוס על המיקרופון ברגע שהכפתור באמת על המסך. קודם הבקשה נשלחה לפני
    // שהכרטיס (AnimatedVisibility) הופיע, נבלעה, ולא היה פוקוס בכלל.
    var autoFocused by remember { mutableStateOf(false) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fill = if (state == AssistantState.LISTENING) theme.dangerColor else theme.readableAccentColor
    val ring by animateFloatAsState(if (isFocused) 1f else 0f, FutureMotion.fast(), label = "micRing")
    val scale by animateFloatAsState(if (isFocused) FutureDimens.focusScale else 1f, FutureMotion.focusScaleSpec, label = "micScale")
    Box(
        modifier = Modifier
            .size(MicSize)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawWithCache {
                val stroke = FutureDimens.focusBorderControl.toPx()
                val outset = FutureDimens.spacingXxs.toPx() + stroke / 2f
                onDrawWithContent {
                    drawOutline(CircleShape.createOutline(size, layoutDirection, this), fill)
                    drawContent()
                    if (ring > 0f) drawCircle(fill.copy(alpha = ring), radius = size.minDimension / 2f + outset, style = Stroke(stroke))
                }
            }
            .focusRequester(focusRequester)
            .onGloballyPositioned {
                if (autoFocus && !autoFocused) {
                    autoFocused = true
                    runCatching { focusRequester.requestFocus() }
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(FutureIcons.Mic, contentDescription = "מיקרופון", tint = FutureContrast.onColor(fill), modifier = Modifier.size(FutureDimens.spacingXl))
    }
}

private val WaveHeight = 56.dp
private val MicSize = 56.dp

/**
 * זוהר צבעוני סביב שולי המסך בזמן הקשבה, תמלול ודיבור: מסגרת של גרדיאנט
 * מעגלי בארבע ההדגשות של הדיזיין סיסטם (ציאן, סגול, כתום, ירוק) שמסתובבת
 * סביב המסך, ועובייה פועם עם עוצמת הקול.
 *
 * התנועה מונעת ידנית מ-withFrameNanos ולא מ-animate*AsState: במכשיר מופעל
 * reduced-motion (קנה מידה 0 לאנימציות), שמבטל אנימציות רגילות - והזוהר
 * חייב להישאר גלוי ובולט גם אז.
 */
@Composable
private fun EdgeGlow(active: Boolean, level: Float, thinking: Boolean, modifier: Modifier) {
    var rotation by remember { mutableFloatStateOf(0f) }
    var presence by remember { mutableFloatStateOf(0f) }
    var pulse by remember { mutableFloatStateOf(0f) }
    val currentActive by rememberUpdatedState(active)
    val currentLevel by rememberUpdatedState(level)
    val currentThinking by rememberUpdatedState(thinking)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                // סיבוב מלא בכ-2.5 שניות בהקשבה, איטי יותר בתמלול.
                rotation = (rotation + dt * if (currentThinking) 80f else 150f) % 360f
                // כניסה ויציאה מהירות (~0.25 שנייה) אבל ברורות.
                val targetPresence = if (currentActive) 1f else 0f
                presence += (targetPresence - presence) * (dt * 8f).coerceAtMost(1f)
                // הפעימה עוקבת אחרי עוצמת הקול; בתמלול - נשימה קבועה.
                val targetPulse = if (currentThinking) 0.35f + 0.25f * sin(rotation / 180f * PI.toFloat() * 2f) else currentLevel
                pulse += (targetPulse - pulse) * (dt * 14f).coerceAtMost(1f)
            }
        }
    }
    if (presence < 0.01f) return
    val colors = remember {
        intArrayOf(
            FutureAccents.Cyan.toArgb(), FutureAccents.Purple.toArgb(),
            FutureAccents.Orange.toArgb(), FutureAccents.Green.toArgb(), FutureAccents.Cyan.toArgb(),
        )
    }
    Canvas(modifier) {
        val shader = android.graphics.SweepGradient(size.width / 2f, size.height / 2f, colors, null)
        shader.setLocalMatrix(android.graphics.Matrix().apply { setRotate(rotation, size.width / 2f, size.height / 2f) })
        val brush = ShaderBrush(shader)
        val base = 5.dp.toPx() + pulse * 7.dp.toPx()
        val corner = CornerRadius(18.dp.toPx())
        // שלוש שכבות מהדקה והחזקה לרחבה והשקופה - "זוהר" בלי blur, שלא זמין לפני API 31 בכל מקום.
        listOf(1f to 1f, 2.4f to 0.45f, 4.2f to 0.2f).forEach { (widthMul, alpha) ->
            val w = base * widthMul
            drawRoundRect(
                brush = brush,
                topLeft = Offset(w / 2f, w / 2f),
                size = Size(size.width - w, size.height - w),
                cornerRadius = corner,
                style = Stroke(width = w),
                alpha = alpha * presence,
            )
        }
    }
}
