package com.future.futurelauncher.ui
import com.future.sharednav.components.AppDialog
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureFormField
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.secondaryTextColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.future.futurelauncher.DefaultApps
import com.future.futurelauncher.DeveloperApps
import com.future.futurelauncher.R
import com.future.sharednav.theme.FutureTheme

/**
 * מעטפת אחידה לכל תפריטי ה"אופציות" של הלאנצ'ר - זכוכית כהה/בהירה עקבית עם
 * שאר האפליקציה, במקום המראה הלבן והגנרי של AlertDialog הסטנדרטי של Material.
 */
/**
 * המעטפת של כל דיאלוג בלאנצ'ר - המשטח של הדיזיין סיסטם: אטום, רדיוס 20dp,
 * ריפוד 20dp, כותרת 15sp מודגשת. קודם זה היה "זכוכית" שקופה-למחצה ברדיוס
 * 28dp (הרדיוס של התראה צפה) עם מסגרת ב-30% מצבע ההדגשה - שלושה דברים שאין
 * לדיאלוג בעיצוב ("There is no blur", "a dialog is separated by the scrim").
 */
@Composable
private fun GlassDialog(
    onDismissRequest: () -> Unit,
    theme: FutureTheme,
    title: String,
    widthFraction: Float = 0.85f,
    trailingTitleContent: (@Composable () -> Unit)? = null,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AppDialog(onDismissRequest = onDismissRequest, widthFraction = widthFraction) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FutureShapes.dialog)
                    .background(theme.surfaceColor)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = theme.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = FutureTypography.dialog,
                        modifier = Modifier.weight(1f)
                    )
                    trailingTitleContent?.invoke()
                }
                Spacer(modifier = Modifier.height(FutureDimens.spacingLg))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    content = content
                )
                Spacer(modifier = Modifier.height(FutureDimens.spacingLg))
                footer()
            }
        }
    }
}

/** כפתור מלבני/גלולה בסגנון זכוכית עם טבעת פוקוס ברורה - להחלפת ה-Button/TextButton הגנריים של Material. */
/** כפתור - FutureButton של הדיזיין סיסטם (ראשי / משני / הרסני). */
@Composable
private fun GlassButton(
    text: String,
    onClick: () -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    // Dialog() מריץ את החלון שלו במעטפת נפרדת - אם מבקשים פוקוס לפני שהחלון
    // בכלל נדבק, הבקשה נבלעת בשקט. onGloballyPositioned מבטיח שהבקשה תקרה
    // ברגע שהכפתור באמת נמדד/מוצג.
    var hasRequestedFocus by remember { mutableStateOf(false) }
    FutureButton(
        text = text,
        theme = theme,
        onClick = onClick,
        modifier = modifier.then(
            if (focusRequester != null) Modifier.onGloballyPositioned {
                if (!hasRequestedFocus) {
                    hasRequestedFocus = true
                    runCatching { focusRequester.requestFocus() }
                }
            } else Modifier
        ),
        variant = when {
            isDestructive -> FutureButtonVariant.Destructive
            isPrimary -> FutureButtonVariant.Primary
            else -> FutureButtonVariant.Secondary
        },
        focusRequester = focusRequester,
    )
}


/** שדה טקסט בסגנון זכוכית - להחלפת ה-OutlinedTextField הגנרי שצובע גבול/תווית בצבעי ברירת המחדל של Material. */
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier
) {
    FutureFormField(label = label, value = value, onValueChange = onValueChange, theme = theme, modifier = modifier)
}

/** שורת מתג הגדרה בסגנון זכוכית, עקבית עם שאר תפריטי ההגדרות ב-FutureOS. */
/** שורת הגדרה עם מתג - FutureSettingItem ו-FutureSwitch של הדיזיין סיסטם. */
@Composable
private fun GlassSettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    theme: FutureTheme
) {
    FutureSettingItem(
        title = title,
        summary = description,
        theme = theme,
        onClick = { onCheckedChange(!checked) },
        trailing = { FutureSwitch(checked = checked, theme = theme) },
    )
}

@Composable
fun FolderDialog(
    folder: LauncherItem.Folder,
    pm: PackageManager,
    onAppClick: (LauncherItem.App) -> Unit,
    onDismiss: () -> Unit,
    theme: FutureTheme = FutureTheme()
) {
    GlassDialog(
        onDismissRequest = onDismiss,
        theme = theme,
        title = folder.label,
        widthFraction = 0.9f,
        footer = {},
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(180.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(folder.apps) { _, app ->
                var isAppFocused by remember { mutableStateOf(false) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    val icon = remember(app.resolveInfo.activityInfo.packageName) {
                        app.resolveInfo.loadIcon(pm).toBitmap().asImageBitmap()
                    }
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .onFocusChanged { isAppFocused = it.isFocused },
                        shape = RoundedCornerShape(percent = 28),
                        color = if (isAppFocused) theme.readableAccentColor.copy(alpha = 0.14f) else theme.idleFieldColor,
                        border = androidx.compose.foundation.BorderStroke(FutureDimens.focusBorderItem, if (isAppFocused) theme.readableAccentColor else Color.Transparent),
                        onClick = { onAppClick(app) }
                    ) {
                        Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = app.label, color = theme.textColor, fontSize = FutureTypography.caption, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AppOptionsDialog(
    item: LauncherItem,
    onAddToFolder: (String) -> Unit,
    onRename: (String) -> Unit,
    onResize: (Int, Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    theme: FutureTheme = FutureTheme()
) {
    var folderName by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf(item.customLabel ?: item.label) }
    val focusRequester = remember { FocusRequester() }

    GlassDialog(
        onDismissRequest = onDismiss,
        theme = theme,
        title = item.label,
        trailingTitleContent = {
            TopBarIconButton(Icons.Rounded.Delete, stringResource(R.string.trash), theme.dangerColor, theme.accentColor, { onRemove(); onDismiss() })
        },
        footer = {
            GlassButton(text = stringResource(R.string.close), onClick = onDismiss, theme = theme, isPrimary = false, modifier = Modifier.fillMaxWidth())
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.options_for, item.label),
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = FutureTypography.label
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (item is LauncherItem.Widget) {
                Text(stringResource(R.string.resize_widget), color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = FutureTypography.body)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.width, item.spanX), color = theme.textColor, fontSize = FutureTypography.label)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TopBarIconButton(Icons.Rounded.Remove, "הקטן", theme.textColor, theme.accentColor, { if (item.spanX > 1) onResize(item.spanX - 1, item.spanY) })
                            TopBarIconButton(Icons.Rounded.Add, "הגדל", theme.textColor, theme.accentColor, { if (item.spanX < 4) onResize(item.spanX + 1, item.spanY) })
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.height, item.spanY), color = theme.textColor, fontSize = FutureTypography.label)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TopBarIconButton(Icons.Rounded.Remove, "הקטן", theme.textColor, theme.accentColor, { if (item.spanY > 1) onResize(item.spanX, item.spanY - 1) })
                            TopBarIconButton(Icons.Rounded.Add, "הגדל", theme.textColor, theme.accentColor, { if (item.spanY < 4) onResize(item.spanX, item.spanY + 1) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FutureDivider(theme = theme, inset = false)
                Spacer(modifier = Modifier.height(10.dp))
            }

            GlassTextField(
                value = customName,
                onValueChange = { customName = it },
                label = stringResource(R.string.rename),
                theme = theme,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            GlassButton(
                text = stringResource(R.string.save_new_name),
                onClick = { onRename(customName) },
                theme = theme,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            if (item is LauncherItem.App) {
                Spacer(modifier = Modifier.height(10.dp))
                FutureDivider(theme = theme, inset = false)
                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = stringResource(R.string.folder_name),
                    theme = theme,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                GlassButton(
                    text = stringResource(R.string.add_to_folder),
                    onClick = { if (folderName.isNotBlank()) onAddToFolder(folderName) },
                    theme = theme,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun EmptySlotOptionsDialog(
    onAddApp: () -> Unit,
    onAddFolder: (String) -> Unit,
    onDismiss: () -> Unit,
    theme: FutureTheme = FutureTheme()
) {
    var folderName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    GlassDialog(
        onDismissRequest = onDismiss,
        theme = theme,
        title = stringResource(R.string.options),
        footer = {
            GlassButton(text = stringResource(R.string.cancel), onClick = onDismiss, theme = theme, isPrimary = false, modifier = Modifier.fillMaxWidth())
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassButton(
                text = stringResource(R.string.add_app),
                onClick = onAddApp,
                theme = theme,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(12.dp))
            FutureDivider(theme = theme, inset = false)
            Spacer(modifier = Modifier.height(12.dp))

            GlassTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = stringResource(R.string.folder_name),
                theme = theme,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            GlassButton(
                text = stringResource(R.string.add_folder_btn),
                onClick = { if (folderName.isNotBlank()) onAddFolder(folderName) },
                theme = theme,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WidgetsDialog(onSelectWidget: () -> Unit, onDismiss: () -> Unit, theme: FutureTheme = FutureTheme()) {
    GlassDialog(
        onDismissRequest = onDismiss,
        theme = theme,
        title = stringResource(R.string.widgets),
        footer = {
            GlassButton(text = stringResource(R.string.cancel), onClick = onDismiss, theme = theme, isPrimary = false, modifier = Modifier.fillMaxWidth())
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.select_widget_title), color = theme.secondaryTextColor, textAlign = TextAlign.Center, fontSize = FutureTypography.body)
            Spacer(modifier = Modifier.height(12.dp))
            GlassButton(
                text = stringResource(R.string.select_widget_button),
                onClick = { onDismiss(); onSelectWidget() },
                theme = theme
            )
        }
    }
}

/** קוד סודי שמוקלד במקשי המספרים בתוך בורר האפליקציות - פותח לצמיתות את כל
 *  האפליקציות המותקנות (לא רק ברירת המחדל של FutureOS), במקום מתג הגדרות גלוי. */
private val UNLOCK_ALL_APPS_CODE = listOf(5, 3, 5, 7)

private fun digitForAppListKey(key: Key): Int? = when (key) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

@Composable
fun AppListDialog(
    pm: PackageManager,
    onAppClick: (LauncherItem.App) -> Unit,
    onDismiss: () -> Unit,
    theme: FutureTheme = FutureTheme(),
    restrictToDefaultApps: Boolean = false,
    onUnlockCode: () -> Unit = {}
) {
    var apps by remember { mutableStateOf<List<LauncherItem.App>>(emptyList()) }
    var typedDigits by remember { mutableStateOf(emptyList<Int>()) }
    // הפוקוס חייב לנחות על הפריט הראשון בפועל (לא על ה-Box העוטף) - אחרת
    // אין שום סמן פוקוס גלוי על המסך, ומקשי חצים לא זזים לשום מקום כי אין
    // עוגן להתחיל ממנו. onKeyEvent על ה-Box עדיין תופס אירועים שעולים
    // (bubble) מהפריט הממוקד, בלי שה-Box עצמו יצטרך להיות focusable.
    val firstItemFocusRequester = remember { FocusRequester() }
    // Dialog() מריץ את החלון שלו במעטפת נפרדת - אם מבקשים פוקוס לפני שהחלון
    // בכלל נדבק, הבקשה נבלעת בשקט. onGloballyPositioned (למטה, על פריט האפליקציה
    // הראשון) מבטיח שהבקשה תקרה ברגע שהפריט באמת נמדד/מוצג, במקום delay() קבוע ושביר.
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(restrictToDefaultApps) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        // אותה בדיקה כמו SystemInteractor.isDeveloperModeEnabled ב-Settings - ציבורית,
        // בלי הרשאה מיוחדת. גם קוד "פתח את כל האפליקציות" (5357) לא חושף טרמינל/
        // לאנצ'ר/SystemUI - אלה שני מנגנוני הגבלה נפרדים.
        val devModeEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        apps = pm.queryIntentActivities(intent, 0)
            .filter { !restrictToDefaultApps || DefaultApps.isDefault(it.activityInfo.packageName) }
            .filter { devModeEnabled || !DeveloperApps.isDeveloperOnly(it.activityInfo.packageName) }
            .map { resolveInfo ->
                LauncherItem.App(
                    id = "app:${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}",
                    resolveInfo = resolveInfo,
                    label = resolveInfo.loadLabel(pm).toString()
                )
            }.sortedBy { it.label.lowercase() }
    }

    GlassDialog(
        onDismissRequest = onDismiss,
        theme = theme,
        title = stringResource(R.string.select_app_title),
        widthFraction = 0.9f,
        footer = {
            GlassButton(text = stringResource(R.string.cancel), onClick = onDismiss, theme = theme, isPrimary = false, modifier = Modifier.fillMaxWidth())
        }
    ) {
        Box(
            modifier = Modifier
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val digit = digitForAppListKey(keyEvent.key) ?: run { typedDigits = emptyList(); return@onKeyEvent false }
                    val next = (typedDigits + digit).takeLast(UNLOCK_ALL_APPS_CODE.size)
                    typedDigits = next
                    if (next == UNLOCK_ALL_APPS_CODE) {
                        typedDigits = emptyList()
                        onUnlockCode()
                    }
                    false
                }
        ) {
            Column {
                if (restrictToDefaultApps && apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.restrict_default_apps_desc),
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = FutureTypography.summary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(280.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(apps) { index, app ->
                var isAppFocused by remember { mutableStateOf(false) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(2.dp)
                ) {
                    val icon = remember(app.resolveInfo.activityInfo.packageName) {
                        app.resolveInfo.loadIcon(pm).toBitmap().asImageBitmap()
                    }
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                            .then(
                                if (index == 0) Modifier.onGloballyPositioned {
                                    if (!hasRequestedInitialFocus) {
                                        hasRequestedInitialFocus = true
                                        firstItemFocusRequester.requestFocus()
                                    }
                                } else Modifier
                            )
                            .onFocusChanged { isAppFocused = it.isFocused },
                        shape = RoundedCornerShape(percent = 28),
                        color = if (isAppFocused) theme.accentColor.copy(alpha = 0.4f) else theme.textColor.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isAppFocused) theme.accentColor else Color.Transparent),
                        onClick = { onAppClick(app) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.label,
                        color = if (isAppFocused) theme.accentColor else theme.textColor,
                        fontSize = FutureTypography.caption,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isAppFocused) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
                }
            }
        }
    }
}

@Composable
fun LauncherSettingsDialog(
    onResetLayout: () -> Unit,
    onDismiss: () -> Unit,
    theme: FutureTheme = FutureTheme()
) {
    var confirmingReset by remember { mutableStateOf(false) }

    GlassDialog(
        onDismissRequest = onDismiss,
        theme = theme,
        title = stringResource(R.string.launcher_settings_title),
        footer = {
            GlassButton(text = stringResource(R.string.close), onClick = onDismiss, theme = theme, isPrimary = false, modifier = Modifier.fillMaxWidth())
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.launcher_settings_desc), color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary)
            Spacer(modifier = Modifier.height(14.dp))

            if (confirmingReset) {
                Text(
                    stringResource(R.string.reset_layout_confirm),
                    color = theme.dangerColor,
                    fontSize = FutureTypography.summary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(stringResource(R.string.reset_layout_confirm_button), { onResetLayout(); onDismiss() }, theme, isDestructive = true)
                    GlassButton(stringResource(R.string.cancel), { confirmingReset = false }, theme, isPrimary = false)
                }
            } else {
                GlassButton(
                    text = stringResource(R.string.reset_layout),
                    onClick = { confirmingReset = true },
                    theme = theme,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
