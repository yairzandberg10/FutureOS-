package com.future.assistant.ui

import android.Manifest
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.assistant.asr.EspeakTts
import com.future.assistant.asr.LocalSpeechEngine
import com.future.assistant.data.CommandProcessor
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING }

@Composable
fun AssistantScreen(theme: FutureTheme, onExit: () -> Unit) {
    val context = LocalContext.current
    val hasPermission by rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)
    // לא חוסמות את שאר העוזר אם המשתמש מסרב - רק "מה יש לי היום" ו"התקשר
    // ל..." לא יעבדו במלואן (יחזירו הודעה מתאימה במקום לקרוס).
    rememberRuntimePermission(Manifest.permission.READ_CALENDAR)
    rememberRuntimePermission(Manifest.permission.READ_CONTACTS)

    var state by remember { mutableStateOf(AssistantState.IDLE) }
    var heardText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("במה אפשר לעזור?") }
    var pendingClose by remember { mutableStateOf(false) }
    var modelReady by remember { mutableStateOf(false) }
    var modelFailed by remember { mutableStateOf(false) }
    val micFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // מנוע Text-to-Speech מקומי (eSpeak NG, native) - במכשירי הבדיקה אין
    // בכלל מנוע TTS מותקן ברמת המערכת, אז android.speech.tts.TextToSpeech
    // נכשל תמיד עם "not bound to TTS engine".
    val espeakTts = remember { EspeakTts(context) }

    fun speak(text: String) {
        state = AssistantState.SPEAKING
        responseText = text
        scope.launch(Dispatchers.IO) {
            espeakTts.speak(text)
            withContext(Dispatchers.Main) {
                state = AssistantState.IDLE
                if (pendingClose) {
                    delay(300)
                    onExit()
                } else {
                    micFocus.requestFocus()
                }
            }
        }
    }

    // מטפל אחיד לטקסט - בין אם הגיע מזיהוי דיבור מקומי ובין אם הוקלד ישירות.
    fun handleRecognizedText(text: String) {
        heardText = text
        if (text.isBlank()) {
            state = AssistantState.IDLE
            responseText = "לא זיהיתי דיבור, נסה שוב"
        } else {
            try {
                val result = CommandProcessor.process(context, text)
                pendingClose = result.shouldClose
                speak(result.responseText)
            } catch (e: Exception) {
                pendingClose = false
                speak("משהו השתבש בביצוע הפקודה, נסה שוב")
            }
        }
    }

    // מנוע זיהוי דיבור מקומי לגמרי (whisper.cpp, native) - מקליט ומתמלל
    // בתוך האפליקציה עצמה, בלי תלות בשירות זיהוי הדיבור של המערכת (שיכול
    // להיות חסום ברמת המכשיר, למשל ע"י Device Admin שחוסם RECORD_AUDIO
    // לאפליקציית המערכת שמבצעת את הזיהוי בפועל).
    val speechEngine = remember { LocalSpeechEngine(context) }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        try {
            withContext(Dispatchers.IO) {
                speechEngine.loadModel()
                espeakTts.init()
            }
            modelReady = true
        } catch (e: Exception) {
            modelFailed = true
        }
    }

    // פוקוס D-pad התחלתי על כפתור המיקרופון - בלי זה, לחיצה על OK במסך
    // הפתיחה לא עושה כלום (אין רכיב ממוקד לקבל את האירוע).
    LaunchedEffect(hasPermission, modelReady) {
        if (hasPermission && modelReady) micFocus.requestFocus()
    }

    fun onMicClick() {
        when (state) {
            AssistantState.IDLE -> {
                heardText = ""
                state = AssistantState.LISTENING
                scope.launch(Dispatchers.IO) { speechEngine.startRecording() }
            }
            AssistantState.LISTENING -> {
                state = AssistantState.THINKING
                scope.launch(Dispatchers.IO) {
                    val text = speechEngine.stopRecordingAndTranscribe()
                    withContext(Dispatchers.Main) { handleRecognizedText(text) }
                }
            }
            else -> {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == AssistantState.LISTENING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "micPulseScale"
    )
    // אנימציית "מדבר" - פעימה קטנה על טקסט התשובה בזמן שהתשובה מושמעת בקול.
    val speakPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == AssistantState.SPEAKING) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
        label = "speakPulseScale"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Text("עוזר קולי", color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!hasPermission) {
                            Text(
                                "נדרשת הרשאת מיקרופון כדי להשתמש בזיהוי דיבור",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else if (modelFailed) {
                            Text(
                                "טעינת מנוע זיהוי הדיבור נכשלה",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else if (!modelReady) {
                            Text(
                                "טוען מנוע זיהוי דיבור...",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        if (heardText.isNotBlank()) {
                            Text("“$heardText”", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                        }
                        Text(
                            when (state) {
                                AssistantState.LISTENING -> "מקליט... לחצו שוב כדי לסיים"
                                AssistantState.THINKING -> "רגע, מתמלל..."
                                else -> responseText
                            },
                            color = theme.textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.scale(speakPulse)
                        )
                    }
                }

                if (hasPermission && modelReady) {
                    MicButton(
                        state = state,
                        theme = theme,
                        pulseScale = pulseScale,
                        focusRequester = micFocus,
                        onClick = { onMicClick() }
                    )
                    Text(
                        if (state == AssistantState.LISTENING) "לחצו על OK כדי לסיים ולשלוח"
                        else "לחצו על OK כדי להתחיל להקליט",
                        color = theme.textColor.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MicButton(state: AssistantState, theme: FutureTheme, pulseScale: Float, focusRequester: FocusRequester, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val baseColor = if (state == AssistantState.LISTENING) theme.dangerColor else theme.accentColor
    val bgColor = if (isFocused) baseColor else baseColor.copy(alpha = 0.85f)
    Box(
        modifier = Modifier
            .size(84.dp)
            .scale(pulseScale)
            .background(bgColor, CircleShape)
            .focusRequester(focusRequester)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(Icons.Rounded.Mic, contentDescription = "מיקרופון", tint = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(32.dp))
    }
}
