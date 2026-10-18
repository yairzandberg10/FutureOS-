package com.future.terminal
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.future.sharednav.systemui.StatusBarInset

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.mutedTextColor
import androidx.compose.runtime.getValue

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.future.sharednav.components.AppDialog
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.ThemeClient
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.inputBarColor
import com.future.sharednav.theme.outputTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TerminalLine(val text: String, val isCommand: Boolean)

private const val MAX_LINES = 2000
private const val MAX_HISTORY = 50

// סימון של המעטפת לנתיב אחרי cd מוצלח. cd שנכשל לא מדפיס אותו, לכן הודעת השגיאה שלו
// לעולם לא נחשבת לנתיב נוכחי.
private const val CWD_MARKER = "__PWD__"

/** cd לבד (בלי שרשור, צינור או פקודה נוספת) - רק אלה מעדכנים את הנתיב הנוכחי. */
/** מסיר מירכאות כפולות או בודדות שעוטפות את כל הנתיב. */
private fun unquote(path: String): String =
    if (path.length >= 2 && ((path.startsWith('"') && path.endsWith('"')) || (path.startsWith("'") && path.endsWith("'"))))
        path.substring(1, path.length - 1)
    else path

private fun isPureCd(cmd: String): Boolean =
    (cmd == "cd" || cmd.startsWith("cd ")) && cmd.none { it in ";&|<>`()\n" }

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private val shell = ShellSession()

    override fun onDestroy() {
        // shell מריץ פקודות su -c אמיתיות עם הרשאות root. בלי הביטול הזה,
        // פקודה ארוכה (sleep, tail -f וכו') שהמשתמש יצא ממנה בלי לבטל ידנית
        // הייתה נשארת רצה ברקע ללא הגבלת זמן, בהרשאות root.
        shell.cancelCurrent()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // נשמרים גם אחרי שינוי תצורה/מוות תהליך (לא remember בלבד): נתיב, קלט והיסטוריה
            var cwd by rememberSaveable { mutableStateOf("/sdcard") }
            var previousCwd by rememberSaveable { mutableStateOf("/sdcard") }
            val history = rememberSaveable(
                saver = listSaver<SnapshotStateList<String>, String>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() }
                )
            ) { mutableStateListOf<String>() }
            var input by rememberSaveable { mutableStateOf("") }
            val lines = remember { mutableStateListOf<TerminalLine>() }
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            var isRunning by remember { mutableStateOf(false) }
            var showMenu by rememberSaveable { mutableStateOf(false) }
            var confirmClear by remember { mutableStateOf(false) }
            val context = LocalContext.current
            // מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לאפליקציה -
            // זו הדרך האמיתית שהוא פותח את התפריט.
            com.future.sharednav.nav.onOptionsKeyPress { showMenu = true }
            // בלי אף קריאת FocusRequester באפליקציה, שדה הפקודה - הפעולה המרכזית
            // של הטרמינל - לא מקבל פוקוס אוטומטי, ואין הבטחה שהקלדה תעבוד בכלל
            // בלי לחיצת כיוון ידנית קודם.
            val inputFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { inputFocusRequester.requestFocus() }
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
                if (history.lastOrNull() != cmd) history.add(cmd)
                if (history.size > MAX_HISTORY) history.removeAt(0)
                input = ""
                isRunning = true
                val dir = cwd
                scope.launch {
                    var newCwd: String? = null
                    val output = withContext(Dispatchers.IO) {
                        when {
                            cmd == "clear" -> null
                            isPureCd(cmd) -> {
                                // "~" ו-"cd" לבד מובילים ל-/sdcard (הבית של המשתמש כאן); "cd -"
                                // חוזר לתיקייה הקודמת. היעד מצוטט בגרשיים בודדים - בגרשיים
                                // כפולים "~" לא הורחב בכלל ונתיב עם " שבר את הפקודה.
                                // TRM-34: נתיב במירכאות (cd "My Dir") - המירכאות לא חלק מהשם.
                                val target = cmd.removePrefix("cd").trim().let { unquote(it) }.let {
                                    when {
                                        it.isBlank() || it == "~" -> "/sdcard"
                                        it.startsWith("~/") -> "/sdcard/" + it.removePrefix("~/")
                                        it == "-" -> previousCwd
                                        else -> it
                                    }
                                }
                                val raw = shell.runCommand(
                                    "cd ${shellQuote(dir)} 2>/dev/null; cd ${shellQuote(target)} && printf '\\n$CWD_MARKER%s\\n' \"\$(pwd)\""
                                )
                                val markerAt = raw.lastIndexOf(CWD_MARKER)
                                if (markerAt >= 0) {
                                    newCwd = raw.substring(markerAt + CWD_MARKER.length).trim()
                                    ""
                                } else {
                                    // cd נכשל: מציגים את השגיאה, והנתיב הנוכחי לא משתנה
                                    raw.trim()
                                }
                            }
                            else -> shell.runCommand("cd ${shellQuote(dir)} 2>/dev/null; $cmd")
                        }
                    }
                    // מצב Compose משתנה רק כאן, על ה-main thread (לא מתוך Dispatchers.IO)
                    newCwd?.let { if (it != cwd) { previousCwd = cwd; cwd = it } }
                    if (cmd == "clear") {
                        lines.clear()
                    } else if (!output.isNullOrBlank()) {
                        // שורה-שורה: התקרה של MAX_LINES סופרת שורות, ו-LazyColumn מרנדר רק את הנראות
                        output.trimEnd('\n').split('\n').forEach { lines.add(TerminalLine(it, isCommand = false)) }
                    }
                    // היסטוריה ארוכה בלי הגבלה (למשל logcat) ממלאת את הזיכרון ומאטה את הרשימה
                    if (lines.size > MAX_LINES) lines.removeRange(0, lines.size - MAX_LINES)
                    isRunning = false
                    // TRM-07: כפתור השליחה נעלם בזמן הריצה ולקח איתו את הפוקוס - מחזירים לשדה.
                    runCatching { inputFocusRequester.requestFocus() }
                    listState.animateScrollToItem((lines.size - 1).coerceAtLeast(0))
                }
            }

            // רק אזור פלט/קלט הפקודות (טקסט שורת-פקודה, בדרך כלל אנגלית/נתיבים)
            // נשאר בכיוון LTR - הכותרת ותפריט האפשרויות העבריים נשארים ב-RTL הטבעי.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    Column(modifier = Modifier.fillMaxSize().padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "טרמינל",
                                fontSize = FutureTypography.title,
                                fontWeight = FontWeight.Bold,
                                color = theme.accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                        // בתחתית/בראשית הרשימה החץ לא נצרך: הפוקוס עובר לשדה הקלט (למטה) או משם חזרה
                                        Key.DirectionDown -> if (listState.canScrollForward) {
                                            scope.launch { listState.animateScrollBy(150f) }; true
                                        } else false
                                        Key.DirectionUp -> if (listState.canScrollBackward) {
                                            scope.launch { listState.animateScrollBy(-150f) }; true
                                        } else false
                                        else -> false
                                    }
                                },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(lines) { line ->
                                Text(
                                    text = line.text,
                                    color = if (line.isCommand) theme.accentColor else theme.outputTextColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = FutureTypography.label
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.inputBarColor)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$", color = theme.mutedTextColor, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 6.dp))
                            // שדה הקלט של הדיזיין סיסטם (8% מילוי, 10dp, מסגרת 2dp בהדגשה בפוקוס),
                            // בגופן חד-רווח - היה TextField של Material עם קו תחתון, וריאנט שאין
                            // בעיצוב ("No underline variant exists; the field is always filled").
                            val inputInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val inputFocused by inputInteraction.collectIsFocusedAsState()
                            val inputRing by androidx.compose.animation.animateColorAsState(
                                if (inputFocused) theme.readableAccentColor else Color.Transparent,
                                FutureMotion.focusColorSpec,
                                label = "terminalInputRing",
                            )
                            androidx.compose.foundation.text.BasicTextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier
                                    .escapeTextFieldFocusTrap()
                                    // OK בתוך השדה מריץ את הפקודה. קודם רק כפתור השליחה הריץ,
                                    // ו-OK בשדה לא עשה כלום. מילה שעוד בהרכבה - המקלדת מאשרת
                                    // אותה ב-OK הראשון ולא מעבירה אותו לכאן.
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.DirectionCenter && input.isNotBlank() && !isRunning) {
                                            if (event.type == KeyEventType.KeyUp) runCommand(input)
                                            true
                                        } else false
                                    }
                                    .weight(1f)
                                    .focusRequester(inputFocusRequester)
                                    .clip(FutureShapes.textField)
                                    .background(theme.idleFieldColor)
                                    .border(FutureDimens.focusBorderControl, inputRing, FutureShapes.textField)
                                    .padding(FutureDimens.spacingSm),
                                textStyle = TextStyle(color = theme.textColor, fontFamily = FontFamily.Monospace, fontSize = FutureTypography.summary),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.readableAccentColor),
                                interactionSource = inputInteraction,
                                singleLine = true,
                                // URI ובלי אות גדולה או תיקון: מקלדת FutureOS לא מגדילה
                                // את האות הראשונה בשדה כזה, אחרת "ls" הופך ל-"Ls" ונכשל.
                                keyboardOptions = KeyboardOptions(
                                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                                    imeAction = ImeAction.Send,
                                ),
                                keyboardActions = KeyboardActions(onSend = { runCommand(input) })
                            )
                            if (isRunning) {
                                // כפתור ביטול פקודה תקועה: כשפקודה רצה, כפתור השליחה מושבת
                                // ואין שום דרך אחרת (בלי מסך מגע, בלי Ctrl+C) לעצור אותה -
                                // בלעדיו תהליך su תקוע יכול להישאר רץ ברקע ללא הגבלה.
                                TerminalIconButton(
                                    icon = FutureIcons.Cancel,
                                    contentDescription = "בטל",
                                    onClick = { shell.cancelCurrent() },
                                    theme = theme,
                                    tint = theme.dangerColor
                                )
                            } else {
                                TerminalIconButton(
                                    icon = FutureIcons.AutoMirrored.Send,
                                    contentDescription = "הרץ",
                                    onClick = { runCommand(input) },
                                    theme = theme,
                                    tint = theme.textColor,
                                    enabled = !isRunning
                                )
                            }
                        }
                        }
                    }

                    if (confirmClear) {
                        com.future.sharednav.components.ConfirmDialog(
                            message = "לנקות את כל הפלט וההיסטוריה שעל המסך?",
                            theme = theme,
                            confirmLabel = "נקה",
                            onCancel = { confirmClear = false },
                            onConfirm = {
                                confirmClear = false
                                lines.clear()
                            },
                        )
                    }

                    if (showMenu) {
                        TerminalOptionsMenu(
                            theme = theme,
                            onDismiss = { showMenu = false },
                            // אין חיצים למעלה/למטה פנויים (הם מנווטים), אז שחזור פקודה
                            // קודמת - הדבר הנפוץ ביותר בטרמינל - עובר דרך התפריט.
                            onRecallLast = history.lastOrNull()?.let { last ->
                                {
                                    showMenu = false
                                    input = last
                                    runCatching { inputFocusRequester.requestFocus() }
                                }
                            },
                            // TRM-11: ניקוי ההיסטוריה מבקש אישור.
                            onClear = {
                                showMenu = false
                                confirmClear = true
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
                                    com.future.sharednav.share.FutureShare.text(this@MainActivity, historyText)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}

/** עטיפה דקה סביב TopBarIconButton המשותף (מודול SharedKeypadNav) - חתימת
 * הקריאה נשארת זהה כדי שקריאות קיימות ב-Terminal לא ישתנו. */
@Composable
private fun TerminalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    theme: FutureTheme,
    tint: Color = theme.textColor,
    enabled: Boolean = true
) {
    com.future.sharednav.components.TopBarIconButton(icon, contentDescription, tint, theme.accentColor, onClick, enabled = enabled)
}

@Composable
private fun TerminalOptionsMenu(theme: FutureTheme, onDismiss: () -> Unit, onRecallLast: (() -> Unit)?, onClear: () -> Unit, onCopyLastOutput: () -> Unit, onShareHistory: () -> Unit) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = "טרמינל") {
        if (onRecallLast != null) FutureMenuRow("פקודה קודמת", FutureIcons.Refresh, theme, onRecallLast)
        FutureMenuRow("העתק פלט אחרון", FutureIcons.ContentCopy, theme, onCopyLastOutput)
        FutureMenuRow("שתף היסטוריה", FutureIcons.Share, theme, onShareHistory)
        FutureMenuRow("נקה היסטוריה", FutureIcons.Delete, theme, onClear, destructive = true)
    }
}


