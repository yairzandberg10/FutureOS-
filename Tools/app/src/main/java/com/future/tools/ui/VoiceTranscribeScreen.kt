package com.future.tools.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import java.util.Locale

@Composable
fun VoiceTranscribeScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val hasPermission by rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)
    var isListening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("לחץ על המיקרופון כדי להתחיל") }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { statusText = "מקשיב..." }
            override fun onBeginningOfSpeech() { statusText = "שומע דיבור..." }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { statusText = "מעבד..." }
            override fun onError(error: Int) {
                isListening = false
                statusText = "לא זוהה דיבור - נסה שוב"
            }
            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    transcript = if (transcript.isBlank()) text else "$transcript $text"
                }
                statusText = "לחץ על המיקרופון כדי להתחיל"
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
        }
        try {
            recognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            statusText = "שגיאה בהפעלת זיהוי הדיבור"
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        isListening = false
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(
                    title = "תמלול קולי", theme = theme, onBack = onBack,
                    trailing = {
                        if (transcript.isNotBlank()) {
                            ToolsIconButton(Icons.Rounded.ContentCopy, "העתק", theme = theme) {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(ClipData.newPlainText("transcript", transcript))
                                Toast.makeText(context, "הטקסט הועתק", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                if (!hasPermission) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("נדרשת הרשאת מיקרופון כדי לתמלל", color = theme.textColor.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                } else if (recognizer == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("זיהוי דיבור אינו זמין במכשיר הזה", color = theme.textColor.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.textColor.copy(alpha = 0.06f))
                            .padding(16.dp)
                    ) {
                        Text(
                            transcript.ifBlank { "הטקסט המתומלל יופיע כאן" },
                            color = if (transcript.isBlank()) theme.textColor.copy(alpha = 0.35f) else theme.textColor,
                            fontSize = 16.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                        MicButton(isListening = isListening, theme = theme) {
                            if (isListening) stopListening() else startListening()
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(statusText, color = theme.textColor.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MicButton(isListening: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val baseColor = if (isListening) Color(0xFFFF6B6B) else theme.accentColor
    val bgColor = if (isFocused) baseColor else baseColor.copy(alpha = 0.85f)
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Mic, contentDescription = if (isListening) "עצור הקלטה" else "התחל הקלטה", tint = Color.Black, modifier = Modifier.size(32.dp))
    }
}
