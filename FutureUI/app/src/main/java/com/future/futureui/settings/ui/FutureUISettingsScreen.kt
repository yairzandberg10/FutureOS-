package com.future.futureui.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.future.futureui.controlcenter.ui.components.focusEffect
import com.future.futureui.lockscreen.logic.LockScreenLayoutManager
import com.future.futureui.statusbar.logic.StatusBarLayoutManager

private val CLOCK_STYLE_LABELS = listOf("עבה ובולט", "דק ואלגנטי", "קוביה (שעות/דקות)", "בינוני")
private val SHORTCUT_LABELS = mapOf(
    "phone" to "טלפון",
    "camera" to "מצלמה",
    "flashlight" to "פנס",
    "settings" to "הגדרות"
)
private val SHORTCUT_ORDER = listOf("phone", "camera", "flashlight", "settings")

/**
 * מסך ההתאמה האישית המרכזי של FutureUI - מרכז במקום אחד את כל ההגדרות
 * שאפשר לכוונן בכל חלקי המערכת (שורת מצב, מסך נעילה וכו'), כדי שלא יהיה
 * צריך לחפש כל הגדרה בנפרד. מרכז הבקרה עצמו כבר תומך בעריכה ישירה
 * (לחיצה ארוכה בתוכו) ולכן לא מופיע כאן.
 */
@Composable
fun FutureUISettingsScreen(
    modifier: Modifier = Modifier,
    onSetPinClick: () -> Unit = {},
    onRemovePinClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val statusBarLayout = remember { StatusBarLayoutManager(context) }
    val lockScreenLayout = remember { LockScreenLayoutManager(context) }
    var hasPin by remember { mutableStateOf(lockScreenLayout.hasPin()) }

    // מתעדכן כשחוזרים ממסך הגדרת/ביטול הקוד
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPin = lockScreenLayout.hasPin()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showBattery by remember { mutableStateOf(statusBarLayout.getShowBattery()) }
    var showBluetooth by remember { mutableStateOf(statusBarLayout.getShowBluetooth()) }
    var use24Hour by remember { mutableStateOf(statusBarLayout.getUse24HourClock()) }
    var suppressSystemBars by remember { mutableStateOf(statusBarLayout.getSuppressSystemBars()) }

    var clockStyle by remember { mutableIntStateOf(lockScreenLayout.getClockStyle()) }
    var leftShortcut by remember { mutableStateOf(lockScreenLayout.getLeftShortcut()) }
    var rightShortcut by remember { mutableStateOf(lockScreenLayout.getRightShortcut()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "התאמה אישית",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "שורת מצב") {
                SettingsToggleRow("הצג סוללה", showBattery) {
                    showBattery = it; statusBarLayout.saveShowBattery(it)
                }
                SettingsToggleRow("הצג Bluetooth", showBluetooth) {
                    showBluetooth = it; statusBarLayout.saveShowBluetooth(it)
                }
                SettingsToggleRow("שעון 24 שעות", use24Hour) {
                    use24Hour = it; statusBarLayout.saveUse24HourClock(it)
                }
                SettingsToggleRow("הסתר שורת מצב מקורית של אנדרואיד (דורש root)", suppressSystemBars) {
                    suppressSystemBars = it; statusBarLayout.saveSuppressSystemBars(it)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "מסך נעילה") {
                SettingsCycleRow(
                    label = "סגנון שעון",
                    value = CLOCK_STYLE_LABELS[clockStyle.coerceIn(0, CLOCK_STYLE_LABELS.size - 1)],
                    onNext = {
                        clockStyle = (clockStyle + 1) % CLOCK_STYLE_LABELS.size
                        lockScreenLayout.saveClockStyle(clockStyle)
                    }
                )
                SettingsCycleRow(
                    label = "קיצור שמאלי",
                    value = SHORTCUT_LABELS[leftShortcut] ?: leftShortcut,
                    onNext = {
                        leftShortcut = nextInOrder(leftShortcut)
                        lockScreenLayout.saveLeftShortcut(leftShortcut)
                    }
                )
                SettingsCycleRow(
                    label = "קיצור ימני",
                    value = SHORTCUT_LABELS[rightShortcut] ?: rightShortcut,
                    onNext = {
                        rightShortcut = nextInOrder(rightShortcut)
                        lockScreenLayout.saveRightShortcut(rightShortcut)
                    }
                )
                SettingsCycleRow(
                    label = "קוד נעילה",
                    value = if (hasPin) "מוגדר - הקש לביטול" else "לא מוגדר - הקש להגדרה",
                    onNext = { if (hasPin) onRemovePinClick() else onSetPinClick() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "מרכז הבקרה ניתן לעריכה ישירות בתוכו - לחיצה ארוכה על כפתור העריכה מאפשרת להוסיף, להסיר ולסדר מחדש כפתורים.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

private fun nextInOrder(current: String): String {
    val idx = SHORTCUT_ORDER.indexOf(current)
    return SHORTCUT_ORDER[(idx + 1).mod(SHORTCUT_ORDER.size)]
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusEffect(isFocused, shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onChange(!value) }
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color.White, checkedThumbColor = Color.Black)
        )
    }
}

@Composable
private fun SettingsCycleRow(label: String, value: String, onNext: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusEffect(isFocused, shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onNext
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}
