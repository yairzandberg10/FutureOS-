package com.future.futurelauncher.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.future.futurelauncher.DefaultApps
import com.future.sharednav.focus.animateFocusColor
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.systemui.StatusBarInset
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.focusFillMenuColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.widget.WidgetTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** ווידג'ט של ספק (אפליקציה מותקנת) שאפשר להוסיף. */
private class WidgetOption(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val cols: Int,
    val rows: Int,
    val icon: ImageBitmap?,
    val tint: Color,
)

/** אפליקציה בבורר: הווידג'טים המובנים שהיא מזינה ואלה של הספקים שלה. */
private class WidgetGroup(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap?,
    val builtins: List<BuiltinWidget>,
    val providers: List<WidgetOption>,
) {
    val count: Int get() = builtins.size + providers.size
}

/** הגודל של תא ברשת מסך הבית (בערך) - לפיו התצוגה המקדימה מצוירת ומוקטנת. */
private val CellW = 76.dp
private val CellH = 80.dp
private val PreviewMaxW = 210.dp
private val PreviewMaxH = 104.dp

/**
 * בחירת ווידג'ט בסגנון One UI: כותרת גדולה, ומתחתיה רשימת אפליקציות בכרטיסים
 * מעוגלים - אייקון, שם ומספר הווידג'טים. OK על אפליקציה פותח אותה (אחת בכל
 * פעם, כמו אקורדיון) ומציג שורה של תצוגות מקדימות; ימינה/שמאלה ביניהן, OK
 * מוסיף, BACK סוגר את האפליקציה הפתוחה (ובלי אפליקציה פתוחה - את הבורר).
 *
 * הווידג'טים המובנים מקובצים עם האפליקציה שמזינה אותם (שעון, יומן...) ומוצגים
 * חיים, בגודל אמיתי ומוקטנים. לווידג'ט של ספק מוצגת תבנית iOS שלו (אין דרך
 * לצייר RemoteViews לפני שהווידג'ט קיים). רק אפליקציות מותקנות ופעילות עם מסך
 * הפעלה, ובהגבלה - רק של אפליקציות FutureOS.
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
    var groups by remember { mutableStateOf<List<WidgetGroup>?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    fun rowRequester(pkg: String) = rowFocus.getOrPut(pkg) { FocusRequester() }
    val firstTile = remember { FocusRequester() }
    var backDown by remember { mutableStateOf(false) }

    LaunchedEffect(restrictToDefaultApps) {
        groups = runCatching { withContext(Dispatchers.IO) { loadGroups(context, pm, restrictToDefaultApps) } }.getOrDefault(emptyList())
    }
    // הפוקוס על האפליקציה הראשונה ברגע שהרשימה מוכנה
    LaunchedEffect(groups != null) {
        val first = groups?.firstOrNull() ?: return@LaunchedEffect
        delay(30)
        runCatching { rowRequester(first.pkg).requestFocus() }
    }
    // פתיחת אפליקציה מעבירה את הפוקוס לתצוגה המקדימה הראשונה שלה
    LaunchedEffect(expanded) {
        if (expanded == null) return@LaunchedEffect
        delay(80)
        runCatching { firstTile.requestFocus() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .padding(top = StatusBarInset.TITLE_GAP_DP.dp)
                .onKeyEvent { event ->
                    // BACK עם אפליקציה פתוחה - סוגר אותה בלבד (גם ה-DOWN וגם ה-UP
                    // נבלעים כאן, כדי שהלאנצ'ר לא יסגור את הבורר כולו)
                    if (event.key != Key.Back) return@onKeyEvent false
                    val open = expanded
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            backDown = open != null
                            open != null
                        }
                        KeyEventType.KeyUp -> {
                            if (!backDown || open == null) return@onKeyEvent false
                            backDown = false
                            expanded = null
                            runCatching { rowRequester(open).requestFocus() }
                            true
                        }
                        else -> false
                    }
                },
        ) {
            val list = groups
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            ) {
                item(key = "header") { PickerHeader(theme, list) }
                when {
                    list == null -> item(key = "loading") { PickerNote(theme, tr("טוען…")) }
                    list.isEmpty() -> item(key = "none") { PickerNote(theme, tr("אף אפליקציה מותקנת לא מציעה ווידג'ט")) }
                    else -> items(list, key = { it.pkg }) { group ->
                        val isOpen = expanded == group.pkg
                        WidgetGroupCard(
                            group = group,
                            expanded = isOpen,
                            theme = theme,
                            rowFocus = rowRequester(group.pkg),
                            firstTile = if (isOpen) firstTile else null,
                            onToggle = { expanded = if (isOpen) null else group.pkg },
                            onPick = onPick,
                            onPickBuiltin = onPickBuiltin,
                        )
                    }
                }
            }
        }
    }
}

/** הכותרת הגדולה של One UI - ממורכזת, עם מספר הווידג'טים מתחתיה; נגללת עם הרשימה. */
@Composable
private fun PickerHeader(theme: FutureTheme, groups: List<WidgetGroup>?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 28.dp),
    ) {
        Text(tr("ווידג'טים"), color = theme.textColor, fontSize = 30.sp, fontWeight = FontWeight.Normal)
        if (!groups.isNullOrEmpty()) {
            Text(
                tr("{0} ווידג'טים · {1} אפליקציות", groups.sumOf { it.count }, groups.size),
                color = theme.mutedTextColor,
                fontSize = FutureTypography.summary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun PickerNote(theme: FutureTheme, text: String) {
    Text(
        text,
        color = theme.mutedTextColor,
        fontSize = FutureTypography.body,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
    )
}

/** כרטיס של אפליקציה: השורה שלה, ומתחתיה (כשהיא פתוחה) שורת התצוגות המקדימות. */
@Composable
private fun WidgetGroupCard(
    group: WidgetGroup,
    expanded: Boolean,
    theme: FutureTheme,
    rowFocus: FocusRequester,
    firstTile: FocusRequester?,
    onToggle: () -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onPickBuiltin: (BuiltinWidget) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(theme.surfaceColor),
    ) {
        WidgetAppRow(group, expanded, theme, rowFocus, onToggle)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            LazyRow(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                itemsIndexed(group.builtins, key = { _, b -> "b:" + b.key }) { index, kind ->
                    WidgetPreviewTile(
                        title = tr(kind.title),
                        cols = kind.defaultSpanX,
                        rows = kind.defaultSpanY,
                        theme = theme,
                        focusRequester = if (index == 0) firstTile else null,
                        onClick = { onPickBuiltin(kind) },
                    ) { BuiltinWidgetContent(kind, kind.defaultSpanX, kind.defaultSpanY, theme) }
                }
                itemsIndexed(group.providers, key = { _, o -> o.info.provider.flattenToString() }) { index, option ->
                    WidgetPreviewTile(
                        title = option.widgetLabel,
                        cols = option.cols,
                        rows = option.rows,
                        theme = theme,
                        focusRequester = if (index == 0 && group.builtins.isEmpty()) firstTile else null,
                        onClick = { onPick(option.info) },
                    ) { ProviderWidgetMock(option) }
                }
            }
        }
    }
}

/** שורת אפליקציה בסגנון One UI: אייקון, שם, "N ווידג'טים" וחץ פתיחה. */
@Composable
private fun WidgetAppRow(
    group: WidgetGroup,
    expanded: Boolean,
    theme: FutureTheme,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusMotion(interaction)
            .clip(RoundedCornerShape(26.dp))
            .background(animateFocusColor(focused, theme.focusFillMenuColor, Color.Transparent).value)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .bringIntoViewOnFocus()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            val icon = group.icon
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(percent = 28)))
            } else {
                Icon(FutureIcons.Widgets, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(group.label, color = theme.textColor, fontSize = FutureTypography.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (group.count == 1) tr("ווידג'ט אחד") else tr("{0} ווידג'טים", group.count),
                color = theme.mutedTextColor,
                fontSize = FutureTypography.summary,
                maxLines = 1,
            )
        }
        Icon(
            if (expanded) FutureIcons.KeyboardArrowUp else FutureIcons.KeyboardArrowDown,
            contentDescription = null,
            tint = theme.mutedTextColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * תצוגה מקדימה: הווידג'ט מצויר בגודל האמיתי שלו ברשת ומוקטן כך שייכנס, עם
 * השם והגודל מתחתיו. בפוקוס - מסגרת בצבע ההדגשה סביב הווידג'ט.
 */
@Composable
private fun WidgetPreviewTile(
    title: String,
    cols: Int,
    rows: Int,
    theme: FutureTheme,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val realW = CellW * cols
    val realH = CellH * rows
    val scale = minOf(PreviewMaxW / realW, PreviewMaxH / realH, 1f)
    val w = realW * scale
    val h = realH * scale
    val ring = animateFocusColor(focused, theme.accentColor, Color.Transparent).value
    val ringShape = RoundedCornerShape(FutureShapes.radiusXl * scale + 4.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(maxOf(w + 8.dp, 92.dp))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusMotion(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .bringIntoViewOnFocus(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(w + 8.dp, h + 8.dp)
                .border(2.5.dp, ring, ringShape)
                .padding(4.dp),
        ) {
            Box(Modifier.size(w, h), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .requiredSize(realW, realH)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                ) { content() }
            }
        }
        Text(
            title,
            color = animateFocusColor(focused, theme.textColor, theme.mutedTextColor).value,
            fontSize = FutureTypography.caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text("$cols×$rows", color = theme.mutedTextColor, fontSize = FutureTypography.caption, maxLines = 1)
    }
}

/** תבנית iOS של ווידג'ט ספק - כמו layout/future_widget: כותרת בגוון, אייקון בפינה, השם בגדול למטה. */
@Composable
private fun ProviderWidgetMock(option: WidgetOption) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(FutureShapes.xl)
            .background(WidgetSurface)
            .padding(horizontal = 16.dp, vertical = if (option.rows > 1) 14.dp else 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                option.appLabel,
                color = option.tint,
                style = ws(FutureTypography.label, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val icon = option.icon
            if (icon != null) {
                Spacer(Modifier.width(8.dp))
                Image(icon, contentDescription = null, modifier = Modifier.size(18.dp).clip(RoundedCornerShape(percent = 28)))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            option.widgetLabel,
            color = ShellInk,
            style = ws(20.sp, FontWeight.Bold),
            maxLines = if (option.rows > 1) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** הקבוצות: כל חבילה שיש לה ווידג'ט מובנה או ספק, ממוינות לפי שם. רץ ברקע. */
private fun loadGroups(context: android.content.Context, pm: PackageManager, restrictToDefaultApps: Boolean): List<WidgetGroup> {
    val density = context.resources.displayMetrics.density
    val iconCache = mutableMapOf<String, ImageBitmap?>()
    val tintCache = mutableMapOf<String, Color>()
    fun iconOf(pkg: String): ImageBitmap? = iconCache.getOrPut(pkg) {
        runCatching {
            val bmp = pm.getApplicationIcon(pkg).toBitmap(96, 96)
            tintCache[pkg] = Color(WidgetTint.fromBitmap(bmp))
            bmp.asImageBitmap()
        }.getOrNull()
    }
    fun labelOf(pkg: String): String? = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull()

    val providers = AppWidgetManager.getInstance(context).installedProviders
        .filter { info ->
            val pkg = info.provider.packageName
            val enabled = runCatching { pm.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)
            enabled && pm.getLaunchIntentForPackage(pkg) != null &&
                (!restrictToDefaultApps || DefaultApps.isDefault(pkg))
        }
        .map { info ->
            val pkg = info.provider.packageName
            // אותה נוסחת גודל כמו בהוספה בפועל (targetCell אם הוגדר), כדי שהתווית תתאים (FL-24)
            val cols = (if (info.targetCellWidth > 0) info.targetCellWidth else (((info.minWidth / density) + 30) / 70).toInt()).coerceIn(1, 4)
            val rows = (if (info.targetCellHeight > 0) info.targetCellHeight else (((info.minHeight / density) + 30) / 70).toInt()).coerceIn(1, 4)
            val icon = iconOf(pkg)
            WidgetOption(info, labelOf(pkg) ?: pkg, info.loadLabel(pm), cols, rows, icon, tintCache[pkg] ?: Color(WidgetTint.DEFAULT))
        }
        .groupBy { it.info.provider.packageName }

    // ווידג'ט שהמכשיר לא תומך בו (צעדים בלי חיישן) לא מוצע
    val builtins = BuiltinWidget.entries.filter { it.isSupported(context) }.groupBy { it.appPackage }

    return (builtins.keys + providers.keys).distinct().map { pkg ->
        WidgetGroup(
            pkg = pkg,
            label = labelOf(pkg) ?: tr("מסך הבית"),
            icon = iconOf(pkg),
            builtins = builtins[pkg].orEmpty(),
            providers = providers[pkg].orEmpty().sortedBy { it.widgetLabel.lowercase() },
        )
    }.sortedBy { it.label.lowercase() }
}
