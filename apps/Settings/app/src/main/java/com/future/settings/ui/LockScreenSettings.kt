package com.future.settings.ui

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.future.settings.theme.LockSettingsClient
import com.future.settings.theme.LockState
import com.future.settings.ui.components.SettingDivider
import com.future.settings.ui.components.SettingHeader
import com.future.settings.ui.components.SettingItem
import com.future.settings.ui.components.SettingSwitch
import com.future.settings.ui.components.SettingsCard
import com.future.settings.ui.theme.ThemeConfig
import com.future.sharednav.components.InputDialog
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor

private enum class LockPage { VERIFY, MAIN, NEW_PIN, CONFIRM_PIN }

private const val PIN_MIN = 4
private const val PIN_MAX = 8

/**
 * מסך הנעילה: קוד, זיהוי פנים, התראות ועיצוב. היה מסך נפרד בתוך FutureUI; עכשיו
 * הכול כאן, וההגדרות נשמרות ב-FutureUI דרך LockSettingsClient. כשמוגדר קוד צריך
 * להזין אותו לפני שרואים משהו, ויציאה מהמסך (או מהאפליקציה) דורשת אותו שוב.
 * רק צילום הפנים עצמו נפתח ב-FutureUI, כי המצלמה ותבנית הפנים שייכות לו.
 */
@Composable
fun LockScreenSettingsScreen(navController: NavController, theme: ThemeConfig) {
    val context = LocalContext.current
    var token by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf(LockSettingsClient.state(context, null)) }
    var page by remember { mutableStateOf(if (state.hasPin && !state.authorized) LockPage.VERIFY else LockPage.MAIN) }
    var newPin by remember { mutableStateOf("") }
    // צילום הפנים נפתח מעל - היציאה הזו לא מבטלת את הכניסה בקוד.
    var enrolling by remember { mutableStateOf(false) }
    var confirmClearPin by remember { mutableStateOf(false) }
    var confirmDeleteFace by remember { mutableStateOf(false) }
    var editMessage by remember { mutableStateOf(false) }

    fun reload() {
        state = LockSettingsClient.state(context, token)
        // האסימון פג (10 דקות) או ש-FutureUI הופעל מחדש - שוב קוד
        if (state.hasPin && !state.authorized && page != LockPage.VERIFY) {
            token = null
            page = LockPage.VERIFY
        }
    }

    fun act(id: String, extras: android.os.Bundle.() -> Unit = {}) {
        val r = LockSettingsClient.action(context, token, id, extras)
        (r.toast ?: r.error)?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        reload()
    }

    // קוד ונתוני פנים - לא בצילומי מסך ולא בכרטיס של האחרונות
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            LockSettingsClient.end(context)
        }
    }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (!enrolling) {
                    // יציאה מהאפליקציה = צריך שוב קוד בפעם הבאה
                    token = null
                    LockSettingsClient.end(context)
                    newPin = ""
                    if (state.hasPin) page = LockPage.VERIFY
                    confirmClearPin = false
                    confirmDeleteFace = false
                    editMessage = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    enrolling = false
                    reload()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        when (page) {
            LockPage.VERIFY -> PinPage(
                title = tr("הזן את הקוד הנוכחי"),
                theme = theme,
                fixedLength = state.pinLength,
                onBack = { navController.popBackStack() },
                onDone = { pin ->
                    val r = LockSettingsClient.verify(context, pin)
                    if (r.token != null) {
                        token = r.token
                        page = LockPage.MAIN
                        reload()
                        null
                    } else r.error ?: tr("קוד שגוי")
                },
            )
            LockPage.NEW_PIN -> PinPage(
                title = tr("קוד חדש"),
                subtitle = tr("{0} עד {1} ספרות, ואז OK", PIN_MIN, PIN_MAX),
                theme = theme,
                fixedLength = null,
                onBack = { page = LockPage.MAIN },
                // מדיניות קוד (1234, 0000, 2580, שנים) נבדקת ב-FutureUI כבר כאן, לפני האישור
                onDone = { pin ->
                    LockSettingsClient.checkPin(context, pin) ?: run { newPin = pin; page = LockPage.CONFIRM_PIN; null }
                },
            )
            LockPage.CONFIRM_PIN -> PinPage(
                title = tr("הזן שוב לאישור"),
                theme = theme,
                fixedLength = newPin.length,
                onBack = { newPin = ""; page = LockPage.NEW_PIN },
                onDone = { pin ->
                    if (pin != newPin) tr("הקודים לא תואמים")
                    else {
                        val r = LockSettingsClient.setPin(context, token, pin)
                        if (r.token != null) {
                            token = r.token
                            newPin = ""
                            r.toast?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            page = LockPage.MAIN
                            reload()
                            null
                        } else r.error ?: tr("לא זמין")
                    }
                },
            )
            LockPage.MAIN -> MainPage(
                state = state,
                theme = theme,
                onBack = { navController.popBackStack() },
                onSetPin = { page = LockPage.NEW_PIN },
                onClearPin = { confirmClearPin = true },
                onEnroll = {
                    val intent = Intent(LockSettingsClient.ACTION_ENROLL)
                        .setPackage(LockSettingsClient.FUTURE_UI_PACKAGE)
                        .putExtra("token", token)
                    enrolling = true
                    runCatching { context.startActivity(intent) }.onFailure {
                        enrolling = false
                        Toast.makeText(context, tr("לא זמין"), Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteFace = { confirmDeleteFace = true },
                onEditMessage = { editMessage = true },
                onLockNow = {
                    act("lock_now")
                    token = null
                    navController.popBackStack()
                },
                act = { id, extras -> act(id, extras) },
            )
        }
    }

    if (confirmClearPin) {
        ConfirmDialog(
            theme = theme,
            title = tr("הסר קוד"),
            message = tr("בלי קוד, כל מי שמחזיק את הטלפון יוכל לפתוח אותו. גם זיהוי הפנים יכובה."),
            confirmLabel = tr("הסר קוד"),
            onConfirm = {
                confirmClearPin = false
                act("clear_pin")
                token = null
            },
            onDismiss = { confirmClearPin = false },
        )
    }
    if (confirmDeleteFace) {
        ConfirmDialog(
            theme = theme,
            title = tr("מחק את נתוני הפנים"),
            message = tr("התבנית תימחק מהמכשיר וזיהוי הפנים יכובה. אפשר לרשום פנים מחדש בכל עת."),
            confirmLabel = tr("מחק"),
            onConfirm = {
                confirmDeleteFace = false
                act("delete_face")
            },
            onDismiss = { confirmDeleteFace = false },
        )
    }
    if (editMessage) {
        InputDialog(
            title = tr("הודעה במסך הנעילה"),
            theme = theme.futureTheme,
            initialValue = state.message,
            allowBlank = true,
            onDismiss = { editMessage = false },
            onConfirm = { text ->
                editMessage = false
                act("message") { putString("text", text.take(60)) }
            },
        )
    }
}

@Composable
private fun MainPage(
    state: LockState,
    theme: ThemeConfig,
    onBack: () -> Unit,
    onSetPin: () -> Unit,
    onClearPin: () -> Unit,
    onEnroll: () -> Unit,
    onDeleteFace: () -> Unit,
    onEditMessage: () -> Unit,
    onLockNow: () -> Unit,
    act: (String, android.os.Bundle.() -> Unit) -> Unit,
) {
    Column {
        SmallHeader(tr("מסך נעילה ואבטחה"), theme, onBack)
        if (!state.linked) {
            LazyColumn {
                item {
                    SettingsCard(theme) {
                        SettingItem(tr("מסך נעילה, קוד וזיהוי פנים"), tr("לא זמין"), FutureIcons.Lock, theme, showChevron = false, infoFocusable = true)
                    }
                }
            }
            return@Column
        }
        LazyColumn {
            item { SettingHeader(tr("אבטחה"), theme) }
            item {
                SettingsCard(theme) {
                    SettingSwitch(tr("מסך נעילה"), null, state.enabled, { v -> act("enabled") { putBoolean("value", v) } }, theme)
                    SettingDivider(theme)
                    SettingItem(
                        tr("קוד נעילה"),
                        if (state.hasPin) tr("{0} ספרות", state.pinLength) else tr("לא מוגדר"),
                        FutureIcons.Pin,
                        theme,
                        onClick = onSetPin,
                    )
                    if (state.hasPin) {
                        SettingDivider(theme)
                        SettingItem(tr("נעילה אוטומטית"), state.autoLock, FutureIcons.Timer, theme, showChevron = false) { act("auto_lock") {} }
                        SettingDivider(theme)
                        SettingItem(tr("הסר קוד"), null, FutureIcons.Delete, theme, onClick = onClearPin)
                    }
                }
            }

            item { SettingHeader(tr("זיהוי פנים"), theme) }
            item {
                if (!state.hasPin) {
                    Hint(tr("כדי להשתמש בזיהוי פנים צריך קודם להגדיר קוד - הוא הגיבוי כשהפנים לא מזוהות."), theme)
                } else {
                    SettingsCard(theme) {
                        SettingItem(
                            if (state.enrolled) tr("רשום פנים מחדש") else tr("רישום פנים"),
                            if (state.enrolled) tr("רשום") else null,
                            FutureIcons.Face,
                            theme,
                            onClick = onEnroll,
                        )
                        if (state.enrolled) {
                            SettingDivider(theme)
                            SettingSwitch(tr("פתיחה בזיהוי פנים"), null, state.faceOn, { v -> act("face_on") { putBoolean("value", v) } }, theme)
                            SettingDivider(theme)
                            SettingSwitch(tr("הישאר במסך הנעילה אחרי זיהוי"), null, state.faceStay, { v -> act("face_stay") { putBoolean("value", v) } }, theme)
                            SettingDivider(theme)
                            SettingItem(tr("רגישות"), state.sensitivity, FutureIcons.Tune, theme, showChevron = false) { act("sensitivity") {} }
                            SettingDivider(theme)
                            SettingItem(tr("מחק את נתוני הפנים"), null, FutureIcons.Delete, theme, onClick = onDeleteFace)
                        }
                    }
                }
            }
            if (state.hasPin) {
                item {
                    Hint(
                        tr("הזיהוי נעשה רק על המכשיר ונשמר מוצפן - בלי תמונות. זה זיהוי בסיסי במצלמה רגילה, ") +
                            tr("לא Face ID: תמונה טובה שלך עלולה לפתוח. הקוד נדרש תמיד אחרי הפעלה מחדש, ") +
                            tr("אחרי 48 שעות ואחרי 5 ניסיונות כושלים."),
                        theme,
                    )
                }
            }

            item { SettingHeader(tr("התראות"), theme) }
            item {
                SettingsCard(theme) {
                    SettingItem(tr("במסך הנעילה"), state.privacy, FutureIcons.Notifications, theme, showChevron = false) { act("privacy") {} }
                }
            }

            item { SettingHeader(tr("עיצוב"), theme) }
            item {
                SettingsCard(theme) {
                    SettingItem(tr("סגנון שעון"), state.clockStyle, FutureIcons.Schedule, theme, showChevron = false) { act("clock_style") {} }
                    SettingDivider(theme)
                    SettingItem(tr("צבע השעון"), state.clockColor, FutureIcons.Brightness6, theme, showChevron = false) { act("clock_color") {} }
                    SettingDivider(theme)
                    SettingItem(tr("רקע"), state.background, FutureIcons.Image, theme, showChevron = false) { act("background") {} }
                    state.widgets.forEachIndexed { slot, label ->
                        SettingDivider(theme)
                        SettingItem(tr("ווידג'ט {0}", slot + 1), label, FutureIcons.Widgets, theme, showChevron = false) {
                            act("widget") { putInt("slot", slot) }
                        }
                    }
                    SettingDivider(theme)
                    SettingItem(tr("קיצור שמאלי (חזור)"), state.left, FutureIcons.Apps, theme, showChevron = false) { act("left") {} }
                    SettingDivider(theme)
                    SettingItem(tr("קיצור ימני (תפריט)"), state.right, FutureIcons.Apps, theme, showChevron = false) { act("right") {} }
                    SettingDivider(theme)
                    SettingItem(tr("הודעה במסך הנעילה"), state.message.ifBlank { tr("לא מוגדר") }, FutureIcons.Edit, theme, onClick = onEditMessage)
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
            item {
                SettingsCard(theme) {
                    SettingItem(tr("נעל עכשיו"), null, FutureIcons.Lock, theme, showChevron = false, onClick = onLockNow)
                }
            }
            item { Hint(tr("במסך הנעילה: לחיצה ארוכה על תפריט פותחת עריכה מהירה של השעון, הווידג'טים והקיצורים."), theme) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Hint(text: String, theme: ThemeConfig) {
    Text(
        text,
        color = theme.futureTheme.mutedTextColor,
        fontSize = FutureTypography.label,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
    )
}

/**
 * הזנת קוד במקשי הספרות. [fixedLength] = בדיקה אוטומטית כשהגיעו מספיק ספרות;
 * null = קוד חדש באורך חופשי, OK מאשר. [onDone] מחזיר הודעת שגיאה או null.
 */
@Composable
private fun PinPage(
    title: String,
    theme: ThemeConfig,
    subtitle: String? = null,
    fixedLength: Int?,
    onBack: () -> Unit,
    onDone: (String) -> String?,
) {
    var pin by remember(title) { mutableStateOf("") }
    var error by remember(title) { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(title) { runCatching { focus.requestFocus() } }
    val maxLen = fixedLength ?: PIN_MAX
    val ink = theme.textColor
    val danger = theme.futureTheme.dangerColor

    fun submit() {
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
                            if (pin.length >= PIN_MIN) submit() else error = tr("לפחות {0} ספרות", PIN_MIN)
                        }
                    }
                }
                true
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = ink, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(subtitle, color = theme.futureTheme.mutedTextColor, fontSize = FutureTypography.summary, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(fixedLength ?: pin.length.coerceAtLeast(PIN_MIN)) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) ink else Color.Transparent)
                        .border(1.5.dp, ink.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(error ?: "", color = danger, fontSize = FutureTypography.summary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(tr("חזור - מחיקה / ביטול"), color = theme.futureTheme.mutedTextColor, fontSize = FutureTypography.caption)
    }
}
