package com.future.futurelauncher.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.future.futurelauncher.DefaultApps
import com.future.futurelauncher.DeveloperApps
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.systemui.StatusBarInset
import com.future.sharednav.t9.T9DigitMap
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** הגדרות הלאנצ'ר - נשמרות מקומית, ונקראות בכל ציור של מסך הבית. */
class LauncherPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("launcher_ui", Context.MODE_PRIVATE)

    var showLabels by mutableStateOf(prefs.getBoolean("labels", true))
        private set
    /** 0 קטן, 1 רגיל, 2 גדול. */
    var iconSize by mutableStateOf(prefs.getInt("icon_size", 1))
        private set
    var largeLabels by mutableStateOf(prefs.getBoolean("large_labels", false))
        private set
    var showPageIndicator by mutableStateOf(prefs.getBoolean("page_indicator", true))
        private set
    /** חצים בקצה הרשת חוזרים לצד השני. */
    var wrapNavigation by mutableStateOf(prefs.getBoolean("wrap", true))
        private set
    /** הכהיית הרקע מאחורי האייקונים: 0, 25, 50 (אחוזים). */
    var dimWallpaper by mutableStateOf(prefs.getInt("dim", 0))
        private set
    /** נעילת פריסה - אין עריכה, הזזה או מחיקה מהמסך. */
    var lockLayout by mutableStateOf(prefs.getBoolean("lock", false))
        private set

    fun updateShowLabels(v: Boolean) { showLabels = v; prefs.edit().putBoolean("labels", v).apply() }
    fun cycleIconSize() { iconSize = (iconSize + 1) % 3; prefs.edit().putInt("icon_size", iconSize).apply() }
    fun updateLargeLabels(v: Boolean) { largeLabels = v; prefs.edit().putBoolean("large_labels", v).apply() }
    fun updateShowPageIndicator(v: Boolean) { showPageIndicator = v; prefs.edit().putBoolean("page_indicator", v).apply() }
    fun updateWrapNavigation(v: Boolean) { wrapNavigation = v; prefs.edit().putBoolean("wrap", v).apply() }
    fun cycleDim() { dimWallpaper = when (dimWallpaper) { 0 -> 25; 25 -> 50; else -> 0 }; prefs.edit().putInt("dim", dimWallpaper).apply() }
    fun updateLockLayout(v: Boolean) { lockLayout = v; prefs.edit().putBoolean("lock", v).apply() }

    val iconSizeLabel: String get() = when (iconSize) { 0 -> "קטן"; 2 -> "גדול"; else -> "רגיל" }
}

/** מסך מלא מעל מסך הבית, מתחת לשורת המצב, בצבעי הערכה. */
@Composable
private fun LauncherOverlay(theme: FutureTheme, title: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .padding(top = StatusBarInset.HEIGHT_DP.dp),
        ) {
            ScreenTopBar(title = title, textColor = theme.textColor, accentColor = theme.accentColor)
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}

/**
 * הגדרות הלאנצ'ר - מסך רגיל ולא חלון קופץ. כל שורה מחליפה ערך ב-OK;
 * BACK חוזר למסך הבית.
 */
@Composable
fun LauncherSettingsScreen(
    theme: FutureTheme,
    prefs: LauncherPrefs,
    pageCount: Int,
    currentPage: Int,
    homePage: Int,
    restrictToDefaultApps: Boolean,
    onSetHome: () -> Unit,
    onAddPage: () -> Unit,
    onOpenWallpapers: () -> Unit,
    onOpenWidgets: () -> Unit,
    onResetLayout: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    var confirmReset by remember { mutableStateOf(false) }

    LauncherOverlay(theme, "הגדרות מסך הבית") {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            FutureSectionHeader("מראה", theme)
            FutureCard(theme = theme) {
                FutureSettingItem(title = "רקע", summary = "בחירת תמונת רקע", icon = FutureIcons.Wallpaper, theme = theme, focusRequester = first, onClick = onOpenWallpapers)
                FutureDivider(theme = theme)
                FutureSettingItem(title = "גודל אייקונים", summary = prefs.iconSizeLabel, icon = FutureIcons.Apps, theme = theme, showChevron = false, onClick = prefs::cycleIconSize)
                FutureDivider(theme = theme)
                SwitchRow("שמות אפליקציות", if (prefs.showLabels) "מוצגים" else "מוסתרים", prefs.showLabels, theme) { prefs.updateShowLabels(!prefs.showLabels) }
                FutureDivider(theme = theme)
                SwitchRow("טקסט גדול", "שמות האפליקציות בגודל גדול יותר", prefs.largeLabels, theme) { prefs.updateLargeLabels(!prefs.largeLabels) }
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = "הכהיית רקע",
                    summary = if (prefs.dimWallpaper == 0) "כבויה" else "${prefs.dimWallpaper}%",
                    icon = FutureIcons.Brightness6,
                    theme = theme,
                    showChevron = false,
                    onClick = prefs::cycleDim,
                )
                FutureDivider(theme = theme)
                SwitchRow("מחוון עמודים", "הנקודות בתחתית המסך", prefs.showPageIndicator, theme) { prefs.updateShowPageIndicator(!prefs.showPageIndicator) }
            }

            FutureSectionHeader("ניווט ופריסה", theme)
            FutureCard(theme = theme) {
                SwitchRow("ניווט מעגלי", "חץ בקצה הרשת חוזר לצד השני", prefs.wrapNavigation, theme) { prefs.updateWrapNavigation(!prefs.wrapNavigation) }
                FutureDivider(theme = theme)
                SwitchRow("נעילת פריסה", "בלי עריכה, הזזה או הסרה", prefs.lockLayout, theme) { prefs.updateLockLayout(!prefs.lockLayout) }
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = "הגדר כדף הבית",
                    summary = if (currentPage == homePage) "זה דף הבית" else "דף ${currentPage + 1} מתוך $pageCount",
                    icon = FutureIcons.Home,
                    theme = theme,
                    showChevron = false,
                    onClick = onSetHome,
                )
                FutureDivider(theme = theme)
                FutureSettingItem(title = "הוסף דף", summary = "$pageCount דפים", icon = FutureIcons.Add, theme = theme, showChevron = false, onClick = onAddPage)
                FutureDivider(theme = theme)
                FutureSettingItem(title = "ווידג'טים", summary = "הוספת ווידג'ט לדף הנוכחי", icon = FutureIcons.Widgets, theme = theme, onClick = onOpenWidgets)
            }

            FutureSectionHeader("מתקדם", theme)
            FutureCard(theme = theme) {
                FutureSettingItem(
                    title = "אפליקציות במסך הבית",
                    summary = if (restrictToDefaultApps) "אפליקציות FutureOS בלבד" else "כל האפליקציות המותקנות",
                    icon = FutureIcons.Info,
                    theme = theme,
                    showChevron = false,
                    onClick = null,
                )
                FutureDivider(theme = theme)
                FutureSettingItem(title = "איפוס פריסה", summary = "החזרת מסך הבית לברירת המחדל", icon = FutureIcons.RestartAlt, theme = theme, showChevron = false, onClick = { confirmReset = true })
                FutureDivider(theme = theme)
                FutureSettingItem(title = "מקשים", summary = "Options פעמיים - עריכה · OK מוחזק - הזזה", icon = FutureIcons.Keyboard, theme = theme, showChevron = false, onClick = null)
            }
        }
    }
    if (confirmReset) {
        ConfirmDialog(message = "לאפס את מסך הבית?", theme = theme, confirmLabel = "אפס", onCancel = { confirmReset = false }, onConfirm = {
            confirmReset = false
            onResetLayout()
        })
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    FutureSettingItem(
        title = title,
        summary = summary,
        theme = theme,
        showChevron = false,
        onClick = onToggle,
        trailing = { FutureSwitch(checked = checked, theme = theme) },
    )
}

/** ספרה -> כל האותיות שעל המקש, עברית ואנגלית (T9DigitMap). */
private fun lettersFor(digit: Char): String =
    (T9DigitMap.HEBREW[digit].orEmpty() + T9DigitMap.ENGLISH[digit].orEmpty()).lowercase()

/** האם תחילת אחת המילים בשם מתאימה לרצף הספרות. */
private fun matchesT9(label: String, digits: String): Boolean =
    label.lowercase().split(' ').any { word ->
        word.length >= digits.length && digits.indices.all { i -> word[i] in lettersFor(digits[i]) || word[i] == digits[i] }
    }

/**
 * בחירת אפליקציה להוספה - רשימה במסך מלא (במקום רשת צפופה בחלון): שורת
 * רשימה של המערכת לכל אפליקציה, שם בגודל גוף רגיל, פוקוס של שורה. ספרות
 * מסננות ב-T9 (עברית ואנגלית).
 */
@Composable
fun AppPickerScreen(
    theme: FutureTheme,
    pm: PackageManager,
    restrictToDefaultApps: Boolean,
    onUnlockCode: () -> Unit,
    onPick: (LauncherItem.App) -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LauncherItem.App>>(emptyList()) }
    var digits by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }

    LaunchedEffect(restrictToDefaultApps) {
        apps = withContext(Dispatchers.IO) {
            val devMode = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .filter { !restrictToDefaultApps || DefaultApps.isDefault(it.activityInfo.packageName) }
                .filter { devMode || !DeveloperApps.isDeveloperOnly(it.activityInfo.packageName) }
                .map { info ->
                    LauncherItem.App(
                        id = "app:${info.activityInfo.packageName}/${info.activityInfo.name}",
                        resolveInfo = info,
                        label = info.loadLabel(pm).toString(),
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }
    LaunchedEffect(digits) {
        if (digits.isEmpty()) return@LaunchedEffect
        // הקוד הסודי שפותח את כל האפליקציות (5357) נשאר כמו שהיה.
        if (digits.endsWith("5357")) { onUnlockCode(); digits = "" }
        delay(2500)
        digits = ""
    }
    val shown = remember(apps, digits) { if (digits.isEmpty()) apps else apps.filter { matchesT9(it.label, digits) } }
    LaunchedEffect(shown.firstOrNull()?.id) { if (shown.isNotEmpty()) runCatching { first.requestFocus() } }

    LauncherOverlay(theme, if (digits.isEmpty()) "הוספת אפליקציה" else "הוספת אפליקציה · $digits") {
        if (shown.isEmpty()) {
            EmptyState(icon = FutureIcons.Apps, title = if (apps.isEmpty()) "טוען" else "אין התאמות", textColor = theme.textColor)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val d = digitForKey(event.key) ?: return@onKeyEvent false
                    digits += d
                    true
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                itemsIndexed(shown, key = { _, app -> app.id }) { index, app ->
                    FutureListItem(
                        title = app.label,
                        theme = theme,
                        onClick = { onPick(app) },
                        focusRequester = if (index == 0) first else null,
                        leading = {
                            val icon = rememberAppIcon(app.resolveInfo, pm)
                            if (icon != null) {
                                Image(icon, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(percent = 28)))
                            }
                        },
                    )
                }
            }
        }
    }
}

/** ווידג'ט אחד שאפשר להוסיף, עם שם האפליקציה שלו. */
private class WidgetOption(val info: AppWidgetProviderInfo, val appLabel: String, val widgetLabel: String, val cells: String)

/**
 * בחירת ווידג'ט - רק ווידג'טים של אפליקציות שמותקנות ופעילות במכשיר (עם
 * מסך הפעלה), מקובצים לפי אפליקציה, ובהגבלה - רק של אפליקציות FutureOS.
 * קודם זה היה חלון עם כפתור אחד שפתח את בורר המערכת, שהציג גם ספקים של
 * אפליקציות מושבתות.
 */
@Composable
fun WidgetPickerScreen(
    theme: FutureTheme,
    pm: PackageManager,
    restrictToDefaultApps: Boolean,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    var options by remember { mutableStateOf<List<WidgetOption>?>(null) }
    val first = remember { FocusRequester() }

    LaunchedEffect(restrictToDefaultApps) {
        options = withContext(Dispatchers.IO) {
            val manager = AppWidgetManager.getInstance(context)
            val density = context.resources.displayMetrics.density
            manager.installedProviders
                .filter { info ->
                    val pkg = info.provider.packageName
                    val enabled = runCatching { pm.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)
                    enabled && pm.getLaunchIntentForPackage(pkg) != null &&
                        (!restrictToDefaultApps || DefaultApps.isDefault(pkg))
                }
                .map { info ->
                    val appLabel = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString() }.getOrDefault(info.provider.packageName)
                    val cols = (((info.minWidth / density) + 30) / 70).toInt().coerceIn(1, 4)
                    val rows = (((info.minHeight / density) + 30) / 70).toInt().coerceIn(1, 4)
                    WidgetOption(info, appLabel, info.loadLabel(pm), "$cols×$rows")
                }
                .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))
        }
    }
    LaunchedEffect(options?.size) { if (!options.isNullOrEmpty()) runCatching { first.requestFocus() } }

    LauncherOverlay(theme, "ווידג'טים") {
        val list = options
        when {
            list == null -> EmptyState(icon = FutureIcons.Widgets, title = "טוען", textColor = theme.textColor)
            list.isEmpty() -> EmptyState(icon = FutureIcons.Widgets, title = "אין ווידג'טים", subtitle = "אף אפליקציה מותקנת לא מציעה ווידג'ט", textColor = theme.textColor)
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                itemsIndexed(list, key = { _, o -> o.info.provider.flattenToString() }) { index, option ->
                    val showHeader = index == 0 || list[index - 1].appLabel != option.appLabel
                    if (showHeader) {
                        Text(option.appLabel, color = theme.mutedTextColor, fontSize = FutureTypography.summary, modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp))
                    }
                    FutureListItem(
                        title = option.widgetLabel,
                        summary = option.cells,
                        theme = theme,
                        onClick = { onPick(option.info) },
                        focusRequester = if (index == 0) first else null,
                        leading = {
                            val preview = remember(option.info.provider) {
                                runCatching { option.info.loadIcon(context, 0)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
                            }
                            if (preview != null) Image(preview, contentDescription = null, modifier = Modifier.size(36.dp))
                        },
                    )
                }
            }
        }
    }
}
