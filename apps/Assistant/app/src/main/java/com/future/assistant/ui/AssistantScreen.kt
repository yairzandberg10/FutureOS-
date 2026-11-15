package com.future.assistant.ui

import com.future.sharednav.theme.FutureAccents
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
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
import androidx.compose.runtime.DisposableEffect
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
    val micGranted = rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)
    val hasPermission by micGranted
    // כפתור המיקרופון מבקש הרשאה כשהיא חסרה (ואין דרך אחרת לתקן מהמקלדת)
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted.value = it }
    // לא חוסמות את שאר העוזר אם המשתמש מסרב - רק "מה יש לי היום" ו"התקשר
    // ל..." לא יעבדו במלואן (יחזירו הודעה מתאימה במקום לקרוס).
    rememberRuntimePermission(Manifest.permission.READ_CALENDAR)
    rememberRuntimePermission(Manifest.permission.READ_CONTACTS)

    var state by remember { mutableStateOf(AssistantState.IDLE) }
    var heardText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf(IdlePrompt) }
    var noSpeech by remember { mutableStateOf(false) }
    // גלילת התשובה (כשהיא ארוכה מהכרטיס) בחיצים, ומצב הפוקוס שלה
    val answerScroll = rememberScrollState()
    var answerFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var pendingClose by remember { mutableStateOf(false) }
    var modelFailed by remember { mutableStateOf(false) }
    // עוצמה 0..1 - מהמיקרופון בזמן הקשבה ומההשמעה בזמן דיבור. מניע את הגלים.
    var level by remember { mutableFloatStateOf(0f) }
    val micFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // מנוע Text-to-Speech נוירוני מקומי (ReNikud + Piper, ר' PiperTts) - מופע
    // אחד לתהליך, כך שפתיחה שנייה של העוזר לא טוענת את המודלים מחדש.
    val piperTts = remember { PiperTts.shared(context) }
    // יציאה מהעוזר (BACK / סגירה) עוצרת את ההקראה, אחרת כל התור היה ממשיך להישמע
    DisposableEffect(Unit) { onDispose { piperTts.stop() } }
    val speechEngine = remember { LocalSpeechEngine(context) }
    // גוגל בעברית כשאפשר, Whisper המקומי כגיבוי (ר' HybridSpeech).
    val speech = remember { com.future.assistant.asr.HybridSpeech(context, speechEngine) }

    fun speak(text: String, spoken: String = text, action: AssistantAction? = null) {
        state = AssistantState.SPEAKING
        noSpeech = false
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
            noSpeech = true
            responseText = "לא זוהה דיבור"
        } else {
            // עיבוד הפקודה כולל IO כבד (רשימת אפליקציות, אנשי קשר, מאגר הידע) - לא על ה-main thread
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { CommandProcessor.process(context, text) } }
                result.onSuccess {
                    pendingClose = it.shouldClose
                    speak(it.responseText, it.speech, it.action)
                }.onFailure {
                    pendingClose = false
                    speak("משהו השתבש בביצוע הפקודה, נסה שוב")
                }
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
            // חזרה מהגדרות ההרשאות - מעדכנים את המצב (אחרת נשאר "נדרשת הרשאה" אחרי שאושרה)
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                micGranted.value = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onMicClick() {
        when (state) {
            AssistantState.IDLE -> {
                heardText = ""
                noSpeech = false
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
            // OK בזמן הקראה - עוצר אותה (נבדק בין משפטים, ר' PiperTts.speak)
            AssistantState.SPEAKING -> piperTts.stop()
            else -> {}
        }
    }

    // הטקסט שמוצג באזור התשובה - נקבע לפי המצב
    val answerText = when {
        !hasPermission -> "נדרשת הרשאת מיקרופון"
        modelFailed -> "טעינת מנוע זיהוי הדיבור נכשלה"
        state == AssistantState.LISTENING -> "מקשיב"
        state == AssistantState.THINKING -> "מתמלל"
        else -> responseText
    }
    // תשובה חדשה מתחילה מהתחלה
    LaunchedEffect(answerText) { answerScroll.scrollTo(0) }
    // גלילה אפשרית רק כשהתשובה ארוכה מהאזור - אז בלבד היא מקבלת פוקוס
    val answerScrollable = answerScroll.maxValue > 0

    // מצב מנוחה: בועה קומפקטית בשורה אחת (כמו העוזר של גמיני) - הנחיה + מיקרופון
    val compact = state == AssistantState.IDLE && !noSpeech && responseText == IdlePrompt && hasPermission && !modelFailed
    // הכרטיס עולה מלמטה כשהחלונית נפתחת.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            BottomGlow(
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
                        .background(theme.surfaceColor, BubbleShape)
                        .border(1.dp, theme.textColor.copy(alpha = 0.09f), BubbleShape)
                        .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
                ) {
                    val dim = theme.mutedTextColor
                    Column(modifier = Modifier.fillMaxWidth().then(if (compact) Modifier else Modifier.heightIn(min = 40.dp))) {
                        if (!compact) when (state) {
                            AssistantState.LISTENING -> Column(verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                                Text(
                                    "מקשיב",
                                    color = dim,
                                    fontSize = type.summary,
                                    fontWeight = FutureTypography.weightBold,
                                )
                                BlinkingCaret(theme.textColor)
                            }
                            AssistantState.THINKING -> Column(verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd)) {
                                if (heardText.isNotBlank()) {
                                    Text(heardText, color = dim, fontSize = type.summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                ShimmerLine(1f)
                                ShimmerLine(0.72f)
                            }
                            else -> {
                                if (heardText.isNotBlank()) {
                                    Text(
                                        heardText,
                                        color = dim,
                                        fontSize = type.summary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = FutureDimens.spacingSm),
                                    )
                                }
                                // תשובה ארוכה נגללת בחיצים במקום להיחתך אחרי ארבע שורות
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = AnswerMaxHeight)
                                        .verticalScroll(answerScroll)
                                        .then(
                                            if (answerScrollable) {
                                                Modifier
                                                    .focusable()
                                                    .onFocusChanged { answerFocused = it.isFocused }
                                                    .onKeyEvent { event ->
                                                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                                        val step = with(density) { AnswerScrollStep.toPx() }
                                                        when (event.key) {
                                                            // בקצה הגלילה החיצים עוברים לפוקוס הבא (כמו בשאר המסך)
                                                            Key.DirectionDown -> if (answerScroll.value < answerScroll.maxValue) {
                                                                scope.launch { answerScroll.animateScrollBy(step) }
                                                                true
                                                            } else false
                                                            Key.DirectionUp -> if (answerScroll.value > 0) {
                                                                scope.launch { answerScroll.animateScrollBy(-step) }
                                                                true
                                                            } else false
                                                            else -> false
                                                        }
                                                    }
                                                    .then(
                                                        if (answerFocused) Modifier.border(FutureDimens.focusBorderControl, theme.readableAccentColor, FutureShapes.md)
                                                        else Modifier,
                                                    )
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    val idlePrompt = state == AssistantState.IDLE && !noSpeech && responseText == IdlePrompt && hasPermission && !modelFailed
                                    Text(
                                        answerText,
                                        color = when {
                                            noSpeech || !hasPermission || modelFailed -> theme.dangerColor
                                            idlePrompt -> dim
                                            else -> theme.textColor
                                        },
                                        fontSize = type.title,
                                        fontWeight = FutureTypography.weightMedium,
                                    )
                                }
                                if (noSpeech) {
                                    Text(
                                        "לחץ על מקש האישור כדי לנסות שוב",
                                        color = dim,
                                        fontSize = type.summary,
                                        modifier = Modifier.padding(top = FutureDimens.spacingSm),
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else FutureDimens.spacingMd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
                    ) {
                        when (state) {
                            AssistantState.LISTENING -> SiriWave(
                                theme = theme,
                                level = level,
                                active = true,
                                thinking = false,
                                color = null,
                                modifier = Modifier.weight(1f).height(WaveHeight),
                            )
                            AssistantState.THINKING -> Text(
                                "מעבד",
                                fontSize = type.summary,
                                fontWeight = FutureTypography.weightBold,
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF4F8CFF), Color(0xFFB59CFF), Color(0xFF3FD0FF))),
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            else -> if (noSpeech && hasPermission && !modelFailed) {
                                Spacer(Modifier.weight(1f))
                            } else Text(
                                when {
                                    !hasPermission -> "OK לבקשת הרשאה"
                                    state == AssistantState.SPEAKING -> "OK להפסקה"
                                    compact -> IdlePrompt
                                    else -> "לחץ על מקש האישור כדי לדבר"
                                },
                                color = dim,
                                fontSize = if (compact) type.title else type.summary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // תמיד על המסך (גם בלי הרשאה) - בלעדיו אין פוקוס לתקן את ההרשאה
                        MicButton(
                            state = state,
                            noSpeech = noSpeech,
                            theme = theme,
                            focusRequester = micFocus,
                            autoFocus = true,
                            onClick = {
                                if (hasPermission) onMicClick()
                                else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        )
                    }
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
private fun SiriWave(theme: FutureTheme, level: Float, active: Boolean, thinking: Boolean, modifier: Modifier, color: Color? = null) {
    var phase by remember { mutableFloatStateOf(0f) }
    // הלולאה רצה מ-LaunchedEffect(Unit), לכן קוראים את הערך העדכני דרך rememberUpdatedState
    val currentActive by rememberUpdatedState(active)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                // במנוחה הפאזה עוצרת - הגל נשאר קו ישר ולא זז בלי שהעוזר מקשיב
                if (last != 0L && currentActive) phase += (now - last) / 1_000_000_000f * 9f
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
    val accent = color ?: theme.readableAccentColor
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
            drawPath(path, brush = Brush.horizontalGradient(listOf(Color(0xFF3FD0FF), Color(0xFF8E6CFF), Color(0xFFFF3D8B))), alpha = alpha, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

/**
 * כפתור המיקרופון - FutureButton בגרסת העיגול: מילוי אטום (ההדגשה, או צבע
 * הסכנה בזמן הקלטה), טבעת פוקוס 2dp בצבע הכפתור במרחק 2dp, והגדלה ל-1.02.
 */
@Composable
private fun MicButton(state: AssistantState, noSpeech: Boolean, theme: FutureTheme, focusRequester: FocusRequester, autoFocus: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    // הפוקוס על המיקרופון ברגע שהכפתור באמת על המסך. קודם הבקשה נשלחה לפני
    // שהכרטיס (AnimatedVisibility) הופיע, נבלעה, ולא היה פוקוס בכלל.
    var autoFocused by remember { mutableStateOf(false) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val live = state == AssistantState.LISTENING
    val fill = if (live) theme.textColor else theme.textColor.copy(alpha = 0.08f)
    val ringColor = theme.readableAccentColor
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
                    if (ring > 0f) drawCircle(ringColor.copy(alpha = ring), radius = size.minDimension / 2f + outset, style = Stroke(stroke))
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
        Icon(
            when { live -> FutureIcons.Stop; noSpeech -> FutureIcons.MicOff; else -> FutureIcons.Mic },
            contentDescription = "מיקרופון",
            tint = if (live) theme.surfaceColor else theme.textColor, modifier = Modifier.size(FutureDimens.spacingXl))
    }
}

/** סמן מהבהב בזמן הקשבה, בצבע הטקסט של הערכה (בהיר/כהה). */
@Composable
private fun BlinkingCaret(color: Color) {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            on = !on
        }
    }
    Box(
        Modifier
            .width(2.dp)
            .height(24.dp)
            .background(if (on) color else Color.Transparent, CircleShape),
    )
}

private const val IdlePrompt = "שאל כל דבר"
private val WaveHeight = 56.dp
private val MicSize = 48.dp
private val BubbleShape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
/** גובה מקסימלי לאזור התשובה - מעבר לו התשובה נגללת. */
private val AnswerMaxHeight = 160.dp
/** כמה גוללים בלחיצת חץ אחת. */
private val AnswerScrollStep = 48.dp

@Composable
private fun ShimmerLine(widthFraction: Float) {
    var shift by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            if (last != 0L) shift = (shift + (now - last) / 1_000_000_000f * 0.6f) % 1f
            last = now
        }
    }
    Canvas(Modifier.fillMaxWidth(widthFraction).height(22.dp)) {
        val w = size.width
        val x0 = -w + shift * 2f * w
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0x264F8CFF), Color(0xB38E6CFF), Color(0x263FD0FF), Color(0x264F8CFF)),
                startX = x0, endX = x0 + 2f * w,
            ),
            cornerRadius = CornerRadius(11.dp.toPx()),
        )
    }
}

/**
 * זוהר קשת בתחתית המסך (מאחורי הכרטיס): שמונה כתמי צבע שמרחפים לצדדים,
 * חזקים בזמן הקשבה/עיבוד/דיבור וחלשים במנוחה. התנועה מונעת ידנית מ-withFrameNanos
 * (reduced-motion מבטל אנימציות רגילות והזוהר חייב להישאר גלוי).
 */
@Composable
private fun BottomGlow(active: Boolean, level: Float, thinking: Boolean, modifier: Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    var presence by remember { mutableFloatStateOf(0.55f) }
    val currentActive by rememberUpdatedState(active)
    val currentLevel by rememberUpdatedState(level)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                time += dt * if (currentActive) 1.8f else 0.7f
                val target = if (currentActive) 1f else 0.55f
                presence += (target - presence) * (dt * 8f).coerceAtMost(1f)
            }
        }
    }
    val colors = remember {
        listOf(0xFFFF3B30, 0xFFFF9500, 0xFFFFD60A, 0xFF30D158, 0xFF00C8FF, 0xFF2F7BFF, 0xFFC04BFF, 0xFFFF3D8B).map { Color(it) }
    }
    Canvas(modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
        val edgeH = 150.dp.toPx()
        val shift = (time * 0.08f % 1f) * size.width * 2f
        val rainbow = colors + colors.first()
        drawRect(
            brush = Brush.horizontalGradient(
                rainbow.map { it.copy(alpha = 0.85f * presence) },
                startX = -shift, endX = size.width * 2f - shift,
                tileMode = androidx.compose.ui.graphics.TileMode.Repeated,
            ),
            topLeft = Offset(0f, size.height - edgeH), size = Size(size.width, edgeH),
        )
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = size.height - edgeH, endY = size.height),
            topLeft = Offset(0f, size.height - edgeH), size = Size(size.width, edgeH),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
        )
        val n = colors.size
        val boost = 1f + currentLevel * 0.35f
        colors.forEachIndexed { i, c ->
            val ph = time * (0.5f + 0.07f * i) + i * 1.3f
            val cx = size.width * (i + 0.5f) / n + sin(ph) * size.width * 0.07f
            val cy = size.height - 20.dp.toPx() - (sin(ph * 1.3f) * 0.5f + 0.5f) * 40.dp.toPx() * boost
            val r = size.width * 0.28f * boost
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(c.copy(alpha = 0.55f * presence), Color.Transparent),
                    center = Offset(cx, cy), radius = r,
                ),
                radius = r, center = Offset(cx, cy),
            )
        }
    }
}
