package com.future.futureui.lockscreen.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.future.futureui.controlcenter.ui.components.focusEffect
import com.future.futureui.lockscreen.face.FaceEnroller
import com.future.futureui.lockscreen.face.FaceStatus
import com.future.futureui.lockscreen.face.FaceTemplateStore
import com.future.futureui.lockscreen.logic.LockCatalog
import com.future.futureui.lockscreen.logic.LockSettings
import com.future.futureui.lockscreen.logic.PinStore
import com.future.futureui.statusbar.service.StatusBarAccessibilityService
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography

private enum class Page { VERIFY, MAIN, NEW_PIN, CONFIRM_PIN, ENROLL }

private val Danger = Color(0xFFFF6B6B)

@Composable
fun LockSettingsScreen(modifier: Modifier = Modifier, onExit: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { LockSettings(context) }
    val faceStore = remember { FaceTemplateStore(context) }
    var page by remember { mutableStateOf(if (settings.pin.hasPin()) Page.VERIFY else Page.MAIN) }
    var newPin by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) } // מרענן את הערכים אחרי שינוי

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier.background(Color.Black)) {
            when (page) {
                Page.VERIFY -> PinPage(
                    title = "הזן את הקוד הנוכחי",
                    fixedLength = settings.pin.pinLength(),
                    lockoutMs = { settings.pin.remainingLockoutMs() },
                    onBack = onExit,
                    onDone = { pin ->
                        if (settings.pin.verifyPin(pin)) { settings.onStrongAuth(); page = Page.MAIN; null }
                        else "קוד שגוי"
                    }
                )
                Page.NEW_PIN -> PinPage(
                    title = "קוד חדש",
                    subtitle = "${PinStore.MIN_LENGTH} עד ${PinStore.MAX_LENGTH} ספרות, ואז OK",
                    fixedLength = null,
                    onBack = { page = Page.MAIN },
                    onDone = { pin -> newPin = pin; page = Page.CONFIRM_PIN; null }
                )
                Page.CONFIRM_PIN -> PinPage(
                    title = "הזן שוב לאישור",
                    fixedLength = newPin.length,
                    onBack = { newPin = ""; page = Page.NEW_PIN },
                    onDone = { pin ->
                        if (pin == newPin && settings.pin.setPin(pin)) {
                            settings.onStrongAuth()
                            newPin = ""
                            version++
                            Toast.makeText(context, "הקוד נשמר", Toast.LENGTH_SHORT).show()
                            page = Page.MAIN
                            null
                        } else "הקודים לא תואמים"
                    }
                )
                Page.ENROLL -> EnrollPage(settings, onFinish = { ok ->
                    if (ok) {
                        settings.faceEnabled = true
                        Toast.makeText(context, "הפנים נרשמו", Toast.LENGTH_SHORT).show()
                    }
                    version++
                    page = Page.MAIN
                })
                Page.MAIN -> key(version) {
                    MainPage(
                        settings = settings,
                        faceStore = faceStore,
                        onSetPin = { page = Page.NEW_PIN },
                        onEnroll = { page = Page.ENROLL },
                        onChanged = { version++ },
                        onExit = onExit,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------ עמוד ראשי

@Composable
private fun MainPage(
    settings: LockSettings,
    faceStore: FaceTemplateStore,
    onSetPin: () -> Unit,
    onEnroll: () -> Unit,
    onChanged: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val hasPin = settings.pin.hasPin()
    val enrolled = remember { faceStore.isEnrolled() }
    var enabled by remember { mutableStateOf(settings.enabled) }
    var autoLock by remember { mutableLongStateOf(settings.autoLockDelayMs) }
    var faceOn by remember { mutableStateOf(settings.faceEnabled && enrolled) }
    var faceStay by remember { mutableStateOf(settings.faceStayOnLock) }
    var sensitivity by remember { mutableIntStateOf(settings.faceSensitivity) }
    var privacy by remember { mutableIntStateOf(settings.notificationPrivacy) }
    var clockStyle by remember { mutableIntStateOf(settings.clockStyle) }
    var clockColor by remember { mutableIntStateOf(settings.clockColor) }
    var background by remember { mutableIntStateOf(settings.background) }
    var widgets by remember { mutableStateOf(settings.widgets) }
    var left by remember { mutableStateOf(settings.leftShortcut) }
    var right by remember { mutableStateOf(settings.rightShortcut) }
    var message by remember { mutableStateOf(settings.ownerMessage) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    BackHandler { onExit() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text("מסך נעילה ואבטחה", color = Color.White, fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Section("אבטחה") {
            ToggleRow("מסך נעילה", enabled, Modifier.focusRequester(firstFocus)) { enabled = it; settings.enabled = it }
            ValueRow("קוד נעילה", if (hasPin) "${settings.pin.pinLength()} ספרות" else "לא מוגדר", onClick = onSetPin)
            if (hasPin) {
                ValueRow("הסר קוד", "", danger = true) {
                    settings.pin.clearPin()
                    settings.faceEnabled = false
                    Toast.makeText(context, "הקוד הוסר", Toast.LENGTH_SHORT).show()
                    onChanged()
                }
                ValueRow("נעילה אוטומטית", LockSettings.autoLockLabel(autoLock)) {
                    autoLock = LockCatalog.cycle(LockSettings.AUTO_LOCK_OPTIONS, autoLock, 1)
                    settings.autoLockDelayMs = autoLock
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Section("זיהוי פנים") {
            if (!hasPin) {
                Hint("כדי להשתמש בזיהוי פנים צריך קודם להגדיר קוד - הוא הגיבוי כשהפנים לא מזוהות.")
            } else {
                ValueRow(if (enrolled) "רשום פנים מחדש" else "רישום פנים", if (enrolled) "רשום" else "", onClick = onEnroll)
                if (enrolled) {
                    ToggleRow("פתיחה בזיהוי פנים", faceOn) { faceOn = it; settings.faceEnabled = it }
                    ToggleRow("הישאר במסך הנעילה אחרי זיהוי", faceStay) { faceStay = it; settings.faceStayOnLock = it }
                    ValueRow("רגישות", LockSettings.SENSITIVITY_LABELS[sensitivity]) {
                        sensitivity = LockCatalog.cycleIndex(3, sensitivity, 1)
                        settings.faceSensitivity = sensitivity
                    }
                    ValueRow("מחק את נתוני הפנים", "", danger = true) {
                        faceStore.clear()
                        settings.faceEnabled = false
                        Toast.makeText(context, "נתוני הפנים נמחקו", Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                }
                Hint(
                    "הזיהוי נעשה רק על המכשיר ונשמר מוצפן - בלי תמונות. זה זיהוי בסיסי במצלמה רגילה, " +
                        "לא Face ID: תמונה טובה שלך עלולה לפתוח. הקוד נדרש תמיד אחרי הפעלה מחדש, " +
                        "אחרי 48 שעות ואחרי 5 ניסיונות כושלים."
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Section("התראות") {
            ValueRow("במסך הנעילה", LockSettings.PRIVACY_LABELS[privacy]) {
                privacy = LockCatalog.cycleIndex(3, privacy, 1)
                settings.notificationPrivacy = privacy
            }
        }

        Spacer(Modifier.height(20.dp))
        Section("עיצוב") {
            ValueRow("סגנון שעון", LockCatalog.clockStyles[clockStyle]) {
                clockStyle = LockCatalog.cycleIndex(LockCatalog.clockStyles.size, clockStyle, 1); settings.clockStyle = clockStyle
            }
            ValueRow("צבע השעון", LockCatalog.clockColors[clockColor]) {
                clockColor = LockCatalog.cycleIndex(LockCatalog.clockColors.size, clockColor, 1); settings.clockColor = clockColor
            }
            ValueRow("רקע", LockCatalog.backgrounds[background]) {
                background = LockCatalog.cycleIndex(LockCatalog.backgrounds.size, background, 1); settings.background = background
            }
            for (slot in 0 until LockSettings.WIDGET_SLOTS) {
                ValueRow("ווידג'ט ${slot + 1}", LockCatalog.widgetLabel(widgets[slot])) {
                    widgets = widgets.toMutableList().also { it[slot] = LockCatalog.cycle(LockCatalog.widgets, it[slot], 1) }
                    settings.widgets = widgets
                }
            }
            ValueRow("קיצור שמאלי (חזור)", LockCatalog.shortcutLabel(left)) {
                left = LockCatalog.cycle(LockCatalog.shortcuts, left, 1); settings.leftShortcut = left
            }
            ValueRow("קיצור ימני (תפריט)", LockCatalog.shortcutLabel(right)) {
                right = LockCatalog.cycle(LockCatalog.shortcuts, right, 1); settings.rightShortcut = right
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(60); settings.ownerMessage = message },
                label = { Text("הודעה במסך הנעילה") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    cursorColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Section("") {
            ValueRow("נעל עכשיו", "") {
                context.sendBroadcast(Intent(StatusBarAccessibilityService.ACTION_LOCK_NOW).setPackage(context.packageName))
                onExit()
            }
        }
        Spacer(Modifier.height(12.dp))
        Hint("במסך הנעילה: לחיצה ארוכה על תפריט פותחת עריכה מהירה של השעון, הווידג'טים והקיצורים.")
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    if (title.isNotEmpty()) {
        Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = FutureTypography.summary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(FutureShapes.xl)
            .background(Color.White.copy(alpha = 0.08f))
    ) { content() }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.55f), fontSize = FutureTypography.label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
}

@Composable
private fun ToggleRow(label: String, value: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .focusEffect(focused, FutureShapes.lg)
            .clip(FutureShapes.lg)
            .clickable(interactionSource = source, indication = null) { onChange(!value) }
            .focusable(interactionSource = source)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = FutureTypography.body, modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color.White, checkedThumbColor = Color.Black)
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, danger: Boolean = false, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .focusEffect(focused, FutureShapes.lg)
            .clip(FutureShapes.lg)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .focusable(interactionSource = source)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (danger) Danger else Color.White, fontSize = FutureTypography.body, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = Color.White.copy(alpha = 0.6f), fontSize = FutureTypography.body)
    }
}

// ------------------------------------------------------------------ קוד

/**
 * הזנת קוד במקשי הספרות. [fixedLength] = בדיקה אוטומטית כשהגיעו מספיק ספרות;
 * null = קוד חדש באורך חופשי, OK מאשר. [onDone] מחזיר הודעת שגיאה או null.
 */
@Composable
private fun PinPage(
    title: String,
    subtitle: String? = null,
    fixedLength: Int?,
    lockoutMs: () -> Long = { 0L },
    onBack: () -> Unit,
    onDone: (String) -> String?,
) {
    var pin by remember(title) { mutableStateOf("") }
    var error by remember(title) { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(title) { runCatching { focus.requestFocus() } }
    val maxLen = fixedLength ?: PinStore.MAX_LENGTH

    fun submit() {
        val lock = lockoutMs()
        if (lock > 0) { error = "נסה שוב בעוד ${(lock + 999) / 1000} שניות"; pin = ""; return }
        error = onDone(pin)
        pin = ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent true
                val d = digitForKey(e.key)
                when {
                    d != null -> {
                        if (pin.length < maxLen) { pin += d; error = null }
                        if (fixedLength != null && pin.length == fixedLength) submit()
                    }
                    e.key == Key.Back || e.key == Key.Backspace -> if (pin.isEmpty()) onBack() else pin = pin.dropLast(1)
                    e.key == Key.DirectionCenter || e.key == Key.Enter -> {
                        if (fixedLength == null) {
                            if (pin.length >= PinStore.MIN_LENGTH) submit() else error = "לפחות ${PinStore.MIN_LENGTH} ספרות"
                        }
                    }
                }
                true
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color.White, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = FutureTypography.summary, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(fixedLength ?: pin.length.coerceAtLeast(PinStore.MIN_LENGTH)) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) Color.White else Color.Transparent)
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(error ?: "", color = Danger, fontSize = FutureTypography.summary)
        Spacer(Modifier.height(16.dp))
        Text("חזור - מחיקה / ביטול", color = Color.White.copy(alpha = 0.45f), fontSize = FutureTypography.caption)
    }
}

// ------------------------------------------------------------- רישום פנים

@Composable
private fun EnrollPage(settings: LockSettings, onFinish: (Boolean) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) onFinish(false)
    }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var count by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf(FaceStatus.NO_FACE) }
    var error by remember { mutableStateOf<String?>(null) }
    val enroller = remember { FaceEnroller(context, settings) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(granted) {
        if (!granted) { launcher.launch(Manifest.permission.CAMERA); return@LaunchedEffect }
        runCatching { focus.requestFocus() }
        enroller.start(
            onPreview = { preview = it },
            onProgress = { c, s -> count = c; status = s },
            onDone = { onFinish(true) },
            onError = { error = it },
        )
    }
    DisposableEffect(Unit) { onDispose { enroller.stop() } }
    BackHandler { enroller.stop(); onFinish(false) }

    Column(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("רישום פנים", color = Color.White, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(3.dp, if (status == FaceStatus.SCANNING) Color.White else Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            preview?.let {
                Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { count / FaceEnroller.TARGET.toFloat() },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = error ?: when (status) {
                FaceStatus.TOO_FAR -> "קרב את הטלפון לפנים"
                FaceStatus.TOO_DARK -> "חשוך מדי - עבור למקום מואר"
                FaceStatus.SCANNING -> "הזז את הראש לאט במעגל"
                else -> "הבט במצלמה הקדמית"
            },
            color = if (error != null) Danger else Color.White.copy(alpha = 0.85f),
            fontSize = FutureTypography.body,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Text("חזור - ביטול", color = Color.White.copy(alpha = 0.45f), fontSize = FutureTypography.caption)
    }
}
