package com.future.futurelauncher.ui

import com.future.sharednav.i18n.tr
import com.future.sharednav.systemui.StatusBarInset

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.input.key.Key
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
import com.future.sharednav.components.AppDialog
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.focusFillMenuColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.future.sharednav.t9.T9DigitMap
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.future.sharednav.focus.animateFocusColor

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
    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .padding(top = StatusBarInset.TITLE_GAP_DP.dp),
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

    LauncherOverlay(theme, tr("הגדרות מסך הבית")) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            FutureSectionHeader(tr("מראה"), theme)
            FutureCard(theme = theme) {
                FutureSettingItem(title = tr("רקע"), summary = tr("בחירת תמונת רקע"), icon = FutureIcons.Wallpaper, theme = theme, focusRequester = first, onClick = onOpenWallpapers)
                FutureDivider(theme = theme)
                FutureSettingItem(title = tr("גודל אייקונים"), summary = prefs.iconSizeLabel, icon = FutureIcons.Apps, theme = theme, showChevron = false, onClick = prefs::cycleIconSize)
                FutureDivider(theme = theme)
                SwitchRow(tr("שמות אפליקציות"), if (prefs.showLabels) tr("מוצגים") else tr("מוסתרים"), prefs.showLabels, theme) { prefs.updateShowLabels(!prefs.showLabels) }
                FutureDivider(theme = theme)
                SwitchRow(tr("טקסט גדול"), tr("שמות האפליקציות בגודל גדול יותר"), prefs.largeLabels, theme) { prefs.updateLargeLabels(!prefs.largeLabels) }
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = tr("הכהיית רקע"),
                    summary = if (prefs.dimWallpaper == 0) tr("כבויה") else "${prefs.dimWallpaper}%",
                    icon = FutureIcons.Brightness6,
                    theme = theme,
                    showChevron = false,
                    onClick = prefs::cycleDim,
                )
                FutureDivider(theme = theme)
                SwitchRow(tr("מחוון עמודים"), tr("הנקודות בתחתית המסך"), prefs.showPageIndicator, theme) { prefs.updateShowPageIndicator(!prefs.showPageIndicator) }
            }

            FutureSectionHeader(tr("ניווט ופריסה"), theme)
            FutureCard(theme = theme) {
                SwitchRow(tr("ניווט מעגלי"), tr("חץ בקצה הרשת חוזר לצד השני"), prefs.wrapNavigation, theme) { prefs.updateWrapNavigation(!prefs.wrapNavigation) }
                FutureDivider(theme = theme)
                SwitchRow(tr("נעילת פריסה"), tr("בלי עריכה, הזזה או הסרה"), prefs.lockLayout, theme) { prefs.updateLockLayout(!prefs.lockLayout) }
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = tr("הגדר כדף הבית"),
                    summary = if (currentPage == homePage) tr("זה דף הבית") else tr("דף {0} מתוך {1}", currentPage + 1, pageCount),
                    icon = FutureIcons.Home,
                    theme = theme,
                    showChevron = false,
                    onClick = onSetHome,
                )
                FutureDivider(theme = theme)
                FutureSettingItem(title = tr("הוסף דף"), summary = tr("{0} דפים", pageCount), icon = FutureIcons.Add, theme = theme, showChevron = false, onClick = onAddPage)
                FutureDivider(theme = theme)
                FutureSettingItem(title = tr("ווידג'טים"), summary = tr("הוספת ווידג'ט לדף הנוכחי"), icon = FutureIcons.Widgets, theme = theme, onClick = onOpenWidgets)
            }

            FutureSectionHeader(tr("מתקדם"), theme)
            FutureCard(theme = theme) {
                FutureSettingItem(
                    title = tr("אפליקציות במסך הבית"),
                    summary = if (restrictToDefaultApps) tr("אפליקציות FutureOS בלבד") else tr("כל האפליקציות המותקנות"),
                    icon = FutureIcons.Info,
                    theme = theme,
                    showChevron = false,
                    onClick = null,
                )
                FutureDivider(theme = theme)
                FutureSettingItem(title = tr("איפוס פריסה"), summary = tr("החזרת מסך הבית לברירת המחדל"), icon = FutureIcons.RestartAlt, theme = theme, showChevron = false, onClick = { confirmReset = true })
                FutureDivider(theme = theme)
                FutureSettingItem(title = tr("מקשים"), summary = "אפשרויות פעמיים - עריכה · החזקת אישור - הזזה", icon = FutureIcons.Keyboard, theme = theme, showChevron = false, onClick = null)
            }
        }
    }
    if (confirmReset) {
        ConfirmDialog(message = tr("לאפס את מסך הבית?"), theme = theme, confirmLabel = tr("אפס"), onCancel = { confirmReset = false }, onConfirm = {
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
 * בחירת אפליקציה להוספה (אדיט מוד) - חלון במעטפת של תפריט האפשרויות
 * (AppDialog + משטח ורדיוס של FutureOptionsMenu), לא מסך מלא: כל
 * האפליקציות ברשת של 4 בשורה, אייקון ושם מתחתיו. החצים זזים ברשת, OK
 * מוסיף, BACK סוגר. ספרות מסננות ב-T9 (עברית ואנגלית).
 */
@Composable
fun AppPickerScreen(
    theme: FutureTheme,
    pm: PackageManager,
    restrictToDefaultApps: Boolean,
    onUnlockCode: () -> Unit,
    onPick: (LauncherItem.App) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LauncherItem.App>>(emptyList()) }
    var digits by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }
    // יעד פוקוס לשורש הדיאלוג: גם כשאין התאמות (אין אף תא) הספרות עדיין נקלטות (FL-10)
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(restrictToDefaultApps) {
        // כשל בשליפה לא משאיר 'טוען' לנצח - רשימה ריקה מוצגת כ'אין התאמות' (FL-25)
        apps = runCatching { withContext(Dispatchers.IO) {
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
        } }.getOrDefault(emptyList())
    }
    LaunchedEffect(digits) {
        if (digits.isEmpty()) return@LaunchedEffect
        // הקוד הסודי שפותח את כל האפליקציות (5357) נשאר כמו שהיה.
        if (digits.endsWith("5357")) { onUnlockCode(); digits = "" }
        delay(2500)
        digits = ""
    }
    val shown = remember(apps, digits) { if (digits.isEmpty()) apps else apps.filter { matchesT9(it.label, digits) } }
    LaunchedEffect(shown.isEmpty()) {
        if (shown.isEmpty()) {
            delay(60)
            runCatching { rootFocus.requestFocus() }
        }
    }

    AppDialog(onDismissRequest = onDismiss, anchorTop = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FutureShapes.dialog)
                .background(theme.surfaceColor)
                .padding(vertical = 8.dp)
                .focusRequester(rootFocus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    // BACK מוחק ספרה אחת; רק בלי ספרות הוא סוגר את הבורר (FL-09)
                    if (event.key == Key.Back && digits.isNotEmpty()) {
                        digits = digits.dropLast(1)
                        return@onKeyEvent true
                    }
                    val d = digitForKey(event.key) ?: return@onKeyEvent false
                    digits += d
                    true
                },
        ) {
            Text(
                if (digits.isEmpty()) tr("הוספת אפליקציה") else "הוספת אפליקציה · " + "•".repeat(digits.length),
                color = theme.mutedTextColor,
                fontSize = FutureTypography.label,
                maxLines = 1,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            )
            if (shown.isEmpty()) {
                Text(
                    if (apps.isEmpty()) tr("טוען…") else tr("אין התאמות"),
                    color = theme.mutedTextColor,
                    fontSize = FutureTypography.body,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                LaunchedEffect(shown.firstOrNull()?.id) {
                    // הדיאלוג רץ בחלון משלו - בקשת פוקוס בפריים הראשון נבלעת.
                    delay(60)
                    runCatching { first.requestFocus() }
                }
                // עצלני: רק השורות הנראות נמדדות ומצוירות (FL-11)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    itemsIndexed(shown.chunked(PICKER_COLUMNS)) { rowIndex, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEachIndexed { colIndex, app ->
                                AppPickerCell(
                                    app = app,
                                    pm = pm,
                                    theme = theme,
                                    focusRequester = if (rowIndex == 0 && colIndex == 0) first else null,
                                    onClick = { onPick(app) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(PICKER_COLUMNS - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val PICKER_COLUMNS = 4

@Composable
private fun AppPickerCell(
    app: LauncherItem.App,
    pm: PackageManager,
    theme: FutureTheme,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = FutureShapes.sm
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(2.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusMotion(interaction)
            .clip(shape)
            .background(animateFocusColor(focused, theme.focusFillMenuColor, Color.Transparent).value)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .bringIntoViewOnFocus()
            .padding(vertical = 8.dp, horizontal = 2.dp),
    ) {
        val icon = rememberAppIcon(app.resolveInfo, pm)
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(percent = 28)))
            }
        }
        Text(
            app.label,
            color = animateFocusColor(focused, theme.textColor, theme.mutedTextColor).value,
            fontSize = FutureTypography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** ווידג'ט אחד שאפשר להוסיף, עם שם האפליקציה שלו. */
private class WidgetOption(val info: AppWidgetProviderInfo, val appLabel: String, val widgetLabel: String, val cells: String, val preview: androidx.compose.ui.graphics.ImageBitmap?)

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
    onPickBuiltin: (BuiltinWidget) -> Unit,
) {
    val context = LocalContext.current
    var options by remember { mutableStateOf<List<WidgetOption>?>(null) }
    val first = remember { FocusRequester() }

    LaunchedEffect(restrictToDefaultApps) {
        options = runCatching { withContext(Dispatchers.IO) {
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
                    // אותה נוסחת גודל כמו בהוספה בפועל (targetCell אם הוגדר), כדי שהתווית תתאים (FL-24)
                    val cols = (if (info.targetCellWidth > 0) info.targetCellWidth else (((info.minWidth / density) + 30) / 70).toInt()).coerceIn(1, 4)
                    val rows = (if (info.targetCellHeight > 0) info.targetCellHeight else (((info.minHeight / density) + 30) / 70).toInt()).coerceIn(1, 4)
                    // התצוגה המקדימה נטענת כאן ברקע, לא בגלילה על ה-main thread (FL-12)
                    val preview = runCatching { info.loadIcon(context, 0)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
                    WidgetOption(info, appLabel, info.loadLabel(pm), "$cols×$rows", preview)
                }
                .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))
        } }.getOrDefault(emptyList())
    }
    // הווידג'טים המובנים מוצגים מיד, בלי לחכות לרשימת הספקים - הפוקוס עליהם
    LaunchedEffect(Unit) { delay(30); runCatching { first.requestFocus() } }
    // ווידג'ט שהמכשיר לא תומך בו (צעדים בלי חיישן) לא מוצע
    val builtins = remember { BuiltinWidget.entries.filter { it.isSupported(context) } }

    LauncherOverlay(theme, tr("ווידג'טים")) {
        val list = options
        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            item(key = "builtin_header") {
                Text(tr("מובנים במסך הבית"), color = theme.mutedTextColor, fontSize = FutureTypography.summary, modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp))
            }
            itemsIndexed(builtins, key = { _, b -> "builtin:" + b.key }) { index, kind ->
                FutureListItem(
                    title = tr(kind.title),
                    summary = tr(kind.description) + " · " + "${kind.defaultSpanX}×${kind.defaultSpanY}",
                    theme = theme,
                    onClick = { onPickBuiltin(kind) },
                    focusRequester = if (index == 0) first else null,
                    leading = {
                        val icon = rememberPackageIcon(kind.appPackage)
                        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(percent = 28)))
                        else androidx.compose.material3.Icon(kind.fallbackIcon, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(28.dp))
                    },
                )
            }
            when {
                list == null -> item(key = "loading") {
                    Text(tr("טוען…"), color = theme.mutedTextColor, fontSize = FutureTypography.body, modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp))
                }
                list.isEmpty() -> item(key = "none") {
                    Text(tr("אף אפליקציה מותקנת לא מציעה ווידג'ט"), color = theme.mutedTextColor, fontSize = FutureTypography.body, modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp))
                }
                else -> itemsIndexed(list, key = { _, o -> o.info.provider.flattenToString() }) { index, option ->
                    val showHeader = index == 0 || list[index - 1].appLabel != option.appLabel
                    if (showHeader) {
                        Text(option.appLabel, color = theme.mutedTextColor, fontSize = FutureTypography.summary, modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp))
                    }
                    FutureListItem(
                        title = option.widgetLabel,
                        summary = option.cells,
                        theme = theme,
                        onClick = { onPick(option.info) },
                        leading = {
                            val preview = option.preview
                            if (preview != null) Image(preview, contentDescription = null, modifier = Modifier.size(36.dp))
                        },
                    )
                }
            }
        }
    }
}

/** אייקון ההפעלה של חבילה (לשורות של הווידג'טים המובנים), נטען ברקע. */
@Composable
private fun rememberPackageIcon(packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    return icon
}
