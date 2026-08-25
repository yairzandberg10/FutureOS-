package com.future.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.future.terminal.theme.ThemeClient
import com.future.terminal.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TerminalLine(val text: String, val isCommand: Boolean)

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private val shell = ShellSession()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var cwd by remember { mutableStateOf("/sdcard") }
            var input by remember { mutableStateOf("") }
            val lines = remember { mutableStateListOf<TerminalLine>() }
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            var isRunning by remember { mutableStateOf(false) }
            var showMenu by remember { mutableStateOf(false) }
            val context = LocalContext.current
            var theme by remember {
                mutableStateOf(
                    ThemeClient.getTheme(this@MainActivity).let {
                        FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                    }
                )
            }

            // מרענן את העיצוב בכל חזרה למסך (למשל אחרי שינוי מצב כהה/בהיר או
            // צבע הדגשה באפליקציית ההגדרות) בלי לבנות מחדש את כל ה-Activity.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        val shared = ThemeClient.getTheme(this@MainActivity)
                        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            fun runCommand(cmdRaw: String) {
                val cmd = cmdRaw.trim()
                if (cmd.isEmpty() || isRunning) return
                lines.add(TerminalLine("$cwd $ $cmd", isCommand = true))
                input = ""
                isRunning = true
                scope.launch {
                    val output = withContext(Dispatchers.IO) {
                        when {
                            cmd == "clear" -> null
                            cmd.startsWith("cd ") || cmd == "cd" -> {
                                val target = cmd.removePrefix("cd").trim()
                                val newDir = shell.runCommand(
                                    "cd \"$cwd\" 2>/dev/null; cd \"${target.ifBlank { "/sdcard" }}\" 2>/dev/null && pwd"
                                ).trim()
                                if (newDir.isNotBlank()) cwd = newDir
                                ""
                            }
                            else -> shell.runCommand("cd \"$cwd\" 2>/dev/null; $cmd")
                        }
                    }
                    if (cmd == "clear") {
                        lines.clear()
                    } else if (!output.isNullOrBlank()) {
                        lines.add(TerminalLine(output.trimEnd('\n'), isCommand = false))
                    }
                    isRunning = false
                    listState.animateScrollToItem((lines.size - 1).coerceAtLeast(0))
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && (event.key == Key.Menu || event.key == Key.Settings)) {
                                showMenu = true
                                true
                            } else false
                        },
                    color = theme.backgroundColor
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "טרמינל",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.accentColor,
                                modifier = Modifier.weight(1f)
                            )
                            TerminalIconButton(Icons.Rounded.MoreVert, "אפשרויות", { showMenu = true }, theme)
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .focusable()
                                // גלילת הפלט ב-D-pad: אין מסך מגע במכשיר, אז בלי onKeyEvent
                                // כאן אין שום דרך לגלול פלט ארוך מהמסך - המקשים היו נבלעים
                                // קודם ע"י ה-Surface החיצוני בלי לזוז בכלל.
                                .onKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                    when (event.key) {
                                        Key.DirectionDown -> {
                                            scope.launch { listState.animateScrollBy(150f) }; true
                                        }
                                        Key.DirectionUp -> {
                                            scope.launch { listState.animateScrollBy(-150f) }; true
                                        }
                                        else -> false
                                    }
                                },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(lines) { line ->
                                Text(
                                    text = line.text,
                                    color = if (line.isCommand) theme.accentColor else Color(0xFFD0D0D0),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF111111))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$", color = theme.accentColor, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 6.dp))
                            TextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = theme.accentColor
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { runCommand(input) })
                            )
                            if (isRunning) {
                                // כפתור ביטול פקודה תקועה: כשפקודה רצה, כפתור השליחה מושבת
                                // ואין שום דרך אחרת (בלי מסך מגע, בלי Ctrl+C) לעצור אותה -
                                // בלעדיו תהליך su תקוע יכול להישאר רץ ברקע ללא הגבלה.
                                TerminalIconButton(
                                    icon = Icons.Rounded.Cancel,
                                    contentDescription = "בטל",
                                    onClick = { shell.cancelCurrent() },
                                    theme = theme,
                                    tint = Color(0xFFFF6B6B)
                                )
                            } else {
                                TerminalIconButton(
                                    icon = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "הרץ",
                                    onClick = { runCommand(input) },
                                    theme = theme,
                                    tint = theme.accentColor,
                                    enabled = !isRunning
                                )
                            }
                        }
                    }

                    if (showMenu) {
                        TerminalOptionsMenu(
                            theme = theme,
                            onDismiss = { showMenu = false },
                            onClear = {
                                showMenu = false
                                lines.clear()
                            },
                            onCopyLastOutput = {
                                showMenu = false
                                val lastOutput = lines.lastOrNull { !it.isCommand }?.text
                                if (!lastOutput.isNullOrBlank()) {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard.setPrimaryClip(ClipData.newPlainText("terminal", lastOutput))
                                }
                            },
                            onShareHistory = {
                                showMenu = false
                                val historyText = lines.joinToString("\n") { it.text }
                                if (historyText.isNotBlank()) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, historyText)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun TerminalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    theme: FutureTheme,
    tint: Color = theme.accentColor,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by androidx.compose.animation.animateColorAsState(
        if (isFocused) theme.accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
        label = "termIconBtnBg"
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .focusable(interactionSource = interactionSource, enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TerminalOptionsMenu(theme: FutureTheme, onDismiss: () -> Unit, onClear: () -> Unit, onCopyLastOutput: () -> Unit, onShareHistory: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(vertical = 8.dp)
        ) {
            TerminalMenuRow("העתק פלט אחרון", Icons.Rounded.ContentCopy, onCopyLastOutput, theme)
            TerminalMenuRow("שתף היסטוריה", Icons.Rounded.Share, onShareHistory, theme)
            TerminalMenuRow("נקה היסטוריה", Icons.Rounded.DeleteSweep, onClear, theme, isDestructive = true)
        }
    }
}

@Composable
private fun TerminalMenuRow(label: String, icon: ImageVector, onClick: () -> Unit, theme: FutureTheme, isDestructive: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by androidx.compose.animation.animateColorAsState(
        if (isFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        label = "termMenuRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isDestructive) Color(0xFFFF6B6B) else theme.accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = if (isDestructive) Color(0xFFFF6B6B) else theme.textColor, fontSize = 15.sp)
    }
}
