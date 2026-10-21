package com.future.files.ui

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.scrimColor
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.raisedSurfaceColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.components.FutureSpinner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.files.data.FileRepository
import com.future.files.data.ThumbnailCache
import androidx.compose.runtime.saveable.rememberSaveable
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.focus.animateFocusColor

@Composable
private fun ViewerHeader(title: String, theme: FutureTheme, onBack: () -> Unit) {
    // עטיפה דקה סביב ScreenTopBar המשותף (מודול SharedKeypadNav) - חתימת
    // הקריאה נשארת זהה כדי שקריאות קיימות ב-Files לא ישתנו.
    com.future.sharednav.components.ScreenTopBar(title = title, textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
}

/** קבצי קוד - מוצגים ונערכים משמאל לימין, בגופן מונו ועם מספרי שורות. */
private val CODE_EXTENSIONS = setOf(
    "kt", "kts", "java", "py", "js", "ts", "json", "xml", "html", "htm", "css", "yml", "yaml", "ini", "conf",
    "cfg", "sh", "bash", "c", "cpp", "h", "hpp", "gradle", "properties", "toml", "sql", "gitignore", "env", "bat",
    "ps1", "rb", "pl", "md", "csv", "log",
)

fun isCodeFile(file: File): Boolean = file.extension.lowercase() in CODE_EXTENSIONS

/** מקסימום בתים שנקראים מקובץ טקסט לתצוגה/עריכה. */
private const val TEXT_MAX_BYTES = 300_000

/** טקסט שנקרא מהקובץ. readOnly = הקובץ לא UTF-8, ולכן לא נערך (כדי לא לדרוס את הקידוד). */
private class LoadedText(val text: String, val readOnly: Boolean, val truncated: Boolean)

/** פענוח UTF-8 מחמיר. בקטע חלקי (קובץ שנחתך) זנב של תו לא שלם לא נחשב שגיאה. */
private fun decodeUtf8Strict(bytes: ByteArray, partial: Boolean): String? {
    val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val input = ByteBuffer.wrap(bytes)
    val output = CharBuffer.allocate(bytes.size)
    if (decoder.decode(input, output, !partial).isError) return null
    if (!partial && decoder.flush(output).isError) return null
    output.flip()
    return output.toString()
}

/** עוטף מספרים (כולל + ומקפים) ב-LRI/PDI לתצוגה בלבד, כך שבפסקה בעברית הם נשארים בסדר הנכון. */
private val NUMBER_PATTERN = Regex("""\+?\d[\d\-+().:/]*\d""")
private fun isolateNumbers(text: String): String = NUMBER_PATTERN.replace(text) { "\u2066" + it.value + "\u2069" }

private fun loadText(file: File): LoadedText? = try {
    // readNBytes(int) קיים רק מ-API 33, והמכשיר הוא API 31 - קריאה ידנית בלולאה.
    val bytes = file.inputStream().use { input ->
        val buffer = ByteArray(minOf(file.length(), TEXT_MAX_BYTES.toLong()).toInt())
        var read = 0
        while (read < buffer.size) {
            val n = input.read(buffer, read, buffer.size - read)
            if (n < 0) break
            read += n
        }
        if (read == buffer.size) buffer else buffer.copyOf(read)
    }
    val truncated = file.length() > TEXT_MAX_BYTES
    val utf8 = decodeUtf8Strict(bytes, truncated)
    if (utf8 != null) LoadedText(utf8, readOnly = false, truncated = truncated)
    else LoadedText(String(bytes, Charset.forName("windows-1255")), readOnly = true, truncated = truncated)
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

/**
 * קובץ טקסט או קוד: צפייה, ועריכה מתפריט Options ("ערוך" / "שמור").
 *
 * קוד נקרא ונערך משמאל לימין (LTR) בגופן מונו עם מספרי שורות - קוד בתוך
 * מסך RTL נשבר: סוגריים מתהפכים והזחה נדבקת לצד הלא נכון. טקסט רגיל נשאר
 * בכיוון של המסך. העריכה היא שדה הטקסט של הדיזיין סיסטם (TextArea) עם
 * המקלדת של המערכת. BACK בעריכה עם שינויים שואל לפני שהוא זורק אותם.
 */
@Composable
fun TextViewerScreen(file: File, theme: FutureTheme, onBack: () -> Unit, onMessage: (String) -> Unit = {}) {
    var content by remember(file) { mutableStateOf<String?>(null) }
    var loadFailed by remember(file) { mutableStateOf(false) }
    var truncated by remember(file) { mutableStateOf(false) }
    var readOnly by remember(file) { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(""))
    }
    var confirmDiscard by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FileRepository() }
    val code = remember(file) { isCodeFile(file) }
    val dirty = editing && draft.text != content

    LaunchedEffect(file) {
        val loaded = withContext(Dispatchers.IO) { loadText(file) }
        truncated = loaded?.truncated ?: false
        readOnly = loaded?.readOnly ?: false
        loadFailed = loaded == null
        content = loaded?.text
    }
    // בלי זה אין שום רכיב פוקוסבילי במסך הזה, ולכן אין דרך במקלדת/D-pad לגלול
    // קובץ טקסט שחורג מגובה המסך - dead end מוחלט למשתמש בלי מסך מגע.
    LaunchedEffect(editing) { if (!editing) runCatching { focusRequester.requestFocus() } }

    fun startEditing() {
        val text = content ?: return onMessage("לא ניתן לקרוא את הקובץ")
        if (truncated) return onMessage("הקובץ גדול מדי לעריכה")
        if (readOnly) return onMessage("הקובץ לא בקידוד UTF-8 - לקריאה בלבד")
        draft = androidx.compose.ui.text.input.TextFieldValue(text)
        editing = true
    }

    fun save() {
        val text = draft.text
        coroutineScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.saveText(file, text) }
            if (ok) {
                content = text
                editing = false
                onMessage("נשמר")
            } else {
                onMessage("לא ניתן לשמור את הקובץ")
            }
        }
    }

    com.future.sharednav.nav.onOptionsKeyPress { menuOpen = true }
    androidx.activity.compose.BackHandler(enabled = editing) {
        if (dirty) confirmDiscard = true else editing = false
    }

    val contentDirection = if (code) LayoutDirection.Ltr else LayoutDirection.Rtl
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            ViewerHeader(if (dirty) "${file.name} *" else file.name, theme, onBack = { if (!editing) onBack() })
            if (readOnly && !editing) {
                Text(
                    "קידוד לא נתמך · לקריאה בלבד",
                    color = theme.warningColor,
                    fontSize = FutureTypography.summary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (com.future.files.data.isScript(file) && !editing) {
                Text(
                    "סקריפט · נערך כאן, לא מורץ",
                    color = theme.warningColor,
                    fontSize = FutureTypography.summary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides contentDirection) {
                if (editing) {
                    com.future.sharednav.components.FutureTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        theme = theme,
                        singleLine = false,
                        autoFocus = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            autoCorrectEnabled = false,
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                        ),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .focusRequester(focusRequester)
                            .focusable().bringIntoViewOnFocus()
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                val step = 400f
                                when (event.key) {
                                    Key.DirectionDown -> {
                                        coroutineScope.launch { scrollState.animateScrollBy(step) }
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        coroutineScope.launch { scrollState.animateScrollBy(-step) }
                                        true
                                    }
                                    // OK פותח עריכה - הדרך הקצרה, בלי לפתוח תפריט.
                                    Key.DirectionCenter, Key.Enter -> { startEditing(); true }
                                    else -> false
                                }
                            }
                            .padding(16.dp)
                    ) {
                        val text = content
                        when {
                            loadFailed -> Text("לא ניתן לקרוא את הקובץ", color = theme.mutedTextColor, fontSize = FutureTypography.summary)
                            text == null -> Text("טוען", color = theme.mutedTextColor, fontSize = FutureTypography.summary)
                            code -> Row {
                                val lines = text.lines()
                                Text(
                                    (1..lines.size).joinToString("\n"),
                                    color = theme.textColor.copy(alpha = 0.3f),
                                    fontSize = FutureTypography.summary,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.padding(end = FutureDimens.spacingSm),
                                )
                                Text(text, color = theme.textColor, fontSize = FutureTypography.summary, fontFamily = FontFamily.Monospace)
                            }
                            else -> {
                                // FL-D12: מספרים (טלפון +972-50-...) מבודדים כשמאל-לימין, כדי שה-+ לא יקפוץ לסוף.
                                val shown = remember(text) { isolateNumbers(text) }
                                Text(shown, color = theme.textColor, fontSize = FutureTypography.summary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }

    if (menuOpen) {
        com.future.sharednav.components.FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = file.name) {
            if (editing) {
                com.future.sharednav.components.FutureMenuRow("שמור", FutureIcons.Save, theme, { menuOpen = false; save() })
                com.future.sharednav.components.FutureMenuRow("בטל שינויים", FutureIcons.Close, theme, { menuOpen = false; editing = false })
            } else {
                com.future.sharednav.components.FutureMenuRow("ערוך", FutureIcons.Edit, theme, { menuOpen = false; startEditing() })
            }
        }
    }
    if (confirmDiscard) {
        com.future.sharednav.components.ConfirmDialog(
            message = "לצאת בלי לשמור?",
            theme = theme,
            confirmLabel = "צא",
            onCancel = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; editing = false },
        )
    }
}

/**
 * תמונה במסך מלא. OK (או "פתח בגלריה" בתפריט הקבצים) פותח אותה בגלריה,
 * שם יש זום, מעבר בין תמונות ושיתוף.
 */
@Composable
fun ImageFileViewerScreen(file: File, theme: FutureTheme, onBack: () -> Unit, onOpenInGallery: () -> Unit = {}) {
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(file) { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    LaunchedEffect(file) {
        // מפענחים בגודל מוקטן למסך (לא בגודל המלא של התמונה) - כדי לא לקרוס על תמונה גדולה.
        val decoded = withContext(Dispatchers.IO) { decodeForScreen(file) }
        bitmap = decoded
        loadFailed = decoded == null
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                        onOpenInGallery(); true
                    } else false
                }
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            if (loadFailed) {
                Text(
                    "לא ניתן להציג את התמונה",
                    color = Color.White,
                    fontSize = FutureTypography.summary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Text(
                "OK · פתח בגלריה",
                color = Color.White,
                fontSize = FutureTypography.summary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FutureDimens.spacingLg)
                    .clip(FutureShapes.pill)
                    .background(theme.scrimColor)
                    .padding(horizontal = FutureDimens.spacingMd, vertical = 6.dp),
            )
            // אין כפתור חזרה מעל התמונה - מקש BACK הפיזי חוזר.
            androidx.activity.compose.BackHandler(onBack = onBack)
        }
    }
}

/** פענוח תמונה בגודל מתאים למסך (640x960) - inSampleSize מחושב מגודל התמונה האמיתי. */
private fun decodeForScreen(file: File): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
        inSampleSize = ThumbnailCache.calculateInSampleSize(bounds, 640, 960)
    })
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

/**
 * כפתור חזרה מעל תמונה. במנוחה - קפסולה בהכהיה של המערכת (60% שחור, קבוע
 * בשני המצבים ולכן תמיד קריא מעל תמונה); בפוקוס - מילוי בהדגשה עם הדיו שלה.
 */
@Composable
private fun ViewerBackChip(theme: FutureTheme, onBack: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) theme.accentColor else theme.scrimColor,
        FutureMotion.focusColorSpec,
        label = "viewerBackBg",
    )
    Box(
        modifier = Modifier
            .size(FutureDimens.rowHeightTopBarButton)
            .focusMotion(interactionSource, focusedScale = 1.1f)
            .clip(FutureShapes.pill)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onBack)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            FutureIcons.AutoMirrored.ArrowBack,
            contentDescription = "חזור",
            tint = animateFocusColor(isFocused, FutureContrast.onColor(theme.accentColor), Color.White).value,
            modifier = Modifier.size(FutureDimens.iconTopBar),
        )
    }
}

@Composable
fun AudioPlayerScreen(file: File, theme: FutureTheme, onBack: () -> Unit) {
    val player = remember { MediaPlayer() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var loadFailed by remember(file) { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0) }
    var positionMs by remember { mutableStateOf(0) }
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val audioAttributes = remember {
        android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
    // שיחה/ניווט קולי מהמערכת מושהים; בלי זה השמע ממשיך מעליהם.
    val focusRequest = remember {
        android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == android.media.AudioManager.AUDIOFOCUS_LOSS || change == android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    runCatching { player.pause() }
                    isPlaying = false
                }
            }
            .build()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            // יציאה מהמסך (מעבר לאפליקציה אחרת או מסך כבוי) - עוצרים את השמע.
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (isPlaying) runCatching { player.pause() }
                isPlaying = false
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(file) {
        try {
            player.reset()
            player.setAudioAttributes(audioAttributes)
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener {
                isPrepared = true
                durationMs = player.duration
            }
            player.setOnCompletionListener { isPlaying = false }
            player.setOnErrorListener { _, _, _ ->
                loadFailed = true
                android.widget.Toast.makeText(context, "לא ניתן לנגן קובץ זה", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            loadFailed = true
            android.widget.Toast.makeText(context, "לא ניתן לנגן קובץ זה", android.widget.Toast.LENGTH_SHORT).show()
        }
        onDispose {
            audioManager.abandonAudioFocusRequest(focusRequest)
            player.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = try { player.currentPosition } catch (e: Exception) { positionMs }
            delay(400)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            ViewerHeader(file.name, theme, onBack)
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(140.dp).clip(FutureShapes.xl).background(theme.elevatedSurfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    // כפתור ראשי של הדיזיין סיסטם: ההדגשה המתוקנת ב-70% במנוחה, מלאה
                    // בפוקוס עם מסגרת 2dp בצבע הטקסט.
                    val playOpacity by animateFloatAsState(if (isFocused) 1f else 0.7f, FutureMotion.fast(), label = "playOpacity")
                    val playRing by animateColorAsState(if (isFocused) theme.textColor else Color.Transparent, FutureMotion.focusColorSpec, label = "playRing")
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .alpha(playOpacity)
                            .background(theme.readableAccentColor)
                            .border(FutureDimens.focusBorderControl, playRing, CircleShape)
                            // clickable כבר פוקוסבילי (יעד פוקוס אחד בלבד). לפני הכנה/אחרי כשל - לחיצה לא עושה דבר.
                            .clickable(interactionSource = interactionSource, indication = null) {
                                if (!isPrepared || loadFailed) return@clickable
                                if (isPlaying) {
                                    player.pause()
                                    audioManager.abandonAudioFocusRequest(focusRequest)
                                } else {
                                    if (audioManager.requestAudioFocus(focusRequest) != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return@clickable
                                    player.start()
                                }
                                isPlaying = !isPlaying
                            }
                            .bringIntoViewOnFocus(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) FutureIcons.Pause else FutureIcons.PlayArrow,
                            contentDescription = if (isPlaying) "השהה" else "נגן",
                            tint = theme.onReadableAccentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(file.name, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(modifier = Modifier.height(12.dp))
                if (loadFailed) {
                    Text("לא ניתן לנגן קובץ זה", color = theme.dangerColor, fontSize = FutureTypography.summary)
                }
                if (durationMs > 0) {
                    val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(FutureShapes.xs).background(theme.textColor.copy(alpha = 0.15f))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(theme.accentColor, FutureShapes.xs))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${formatMs(positionMs)} / ${formatMs(durationMs)}", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.label)
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** כמה עמודי PDF מרונדרים נשמרים בזיכרון בכל רגע (סביב העמוד הנוכחי). */
private const val PDF_PAGE_CACHE_SIZE = 4

@Composable
fun PdfViewerScreen(file: File, theme: FutureTheme, onBack: () -> Unit) {
    var pageCount by remember(file) { mutableIntStateOf(0) }
    var isLoading by remember(file) { mutableStateOf(true) }
    var loadFailed by remember(file) { mutableStateOf(false) }
    // מטמון עמודים שעברו רינדור בפועל - עמוד נכנס לכאן רק כשהוא באמת מוצג,
    // לא מראש עבור כל ה-PDF. בלי זה PDF של עשרות עמודים היה מרנדר את כולן
    // ב-2x מיד עם הפתיחה, גם אם רק העמוד הראשון נגלל אליו בפועל - סיכון OOM.
    val pageCache = remember(file) { mutableStateMapOf<Int, Bitmap>() }
    val renderMutex = remember(file) { Mutex() }
    val rendererRef = remember(file) { mutableStateOf<PdfRenderer?>(null) }
    val pfdRef = remember(file) { mutableStateOf<ParcelFileDescriptor?>(null) }

    DisposableEffect(file) {
        onDispose {
            rendererRef.value?.close()
            pfdRef.value?.close()
        }
    }

    LaunchedEffect(file) {
        isLoading = true
        loadFailed = false
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = try {
                    PdfRenderer(pfd)
                } catch (e: Exception) {
                    pfd.close()
                    throw e
                }
                // המסך נסגר בזמן הטעינה - onDispose כבר רץ ולא יסגור את המשאבים האלה.
                if (!isActive) {
                    renderer.close()
                    pfd.close()
                    return@withContext
                }
                pfdRef.value = pfd
                rendererRef.value = renderer
                pageCount = renderer.pageCount
            } catch (e: Exception) {
                loadFailed = true
            }
        }
        isLoading = false
    }

    suspend fun renderPage(index: Int): Bitmap? = renderMutex.withLock {
        withContext(Dispatchers.IO) {
            val renderer = rendererRef.value ?: return@withContext null
            try {
                renderer.openPage(index).use { page ->
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    // בלי זה אין שום רכיב פוקוסבילי ברשימת העמודים ואין דרך במקלדת לגלול
    // PDF מרובה-עמודים - רק העמוד הראשון (הגלוי מיד) היה נגיש בפועל.
    // מחכים לסיום הטעינה: לפני כן הרשימה עדיין לא קיימת ולבקשת הפוקוס אין יעד.
    LaunchedEffect(isLoading) { if (!isLoading) runCatching { focusRequester.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.raisedSurfaceColor)) {
            ViewerHeader(file.name, theme, onBack)
            when {
                isLoading -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FutureSpinner(theme = theme, label = "טוען PDF")
                }
                loadFailed || pageCount == 0 -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("לא ניתן להציג את הקובץ", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary)
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .focusable().bringIntoViewOnFocus()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val step = 500f
                            when (event.key) {
                                Key.DirectionDown -> {
                                    coroutineScope.launch { listState.animateScrollBy(step) }
                                    true
                                }
                                Key.DirectionUp -> {
                                    coroutineScope.launch { listState.animateScrollBy(-step) }
                                    true
                                }
                                else -> false
                            }
                        },
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pageCount, key = { it }) { index ->
                        val bitmap = pageCache[index]
                        // המפתח כולל "העמוד חסר" - עמוד שפונה מהמטמון מרונדר שוב כשהוא חוזר להיות גלוי.
                        LaunchedEffect(index, bitmap == null) {
                            if (pageCache[index] == null) {
                                renderPage(index)?.let { rendered ->
                                    pageCache[index] = rendered
                                    while (pageCache.size > PDF_PAGE_CACHE_SIZE) {
                                        val farthest = pageCache.keys.maxByOrNull { kotlin.math.abs(it - index) } ?: break
                                        pageCache.remove(farthest)?.recycle()
                                    }
                                }
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(280.dp).background(theme.elevatedSurfaceColor))
                        }
                    }
                }
            }
        }
    }
}
