package com.future.futurelauncher.ui

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import com.future.futurelauncher.R
import com.future.futurelauncher.ui.theme.FutureTheme
import kotlinx.coroutines.delay

/**
 * מעטפת אחידה לכל תפריטי ה"אופציות" של הלאנצ'ר - זכוכית כהה/בהירה עקבית עם
 * שאר האפליקציה, במקום המראה הלבן והגנרי של AlertDialog הסטנדרטי של Material.
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
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            // המסך קבוע ב-640x960 (ר' CLAUDE.md) ותוכן דיאלוגים כמו אפשרויות
            // אפליקציה/ווידג'ט (שינוי גודל + שדות שינוי שם + הוספה לתיקייה) יכול
            // בקלות לעלות על הגובה הפנוי. בלי מגבלת גובה כאן, ה-Column החיצוני היה
            // פשוט תופס את הגובה שהתוכן דורש, מה שדוחף את הדיאלוג המרוכז אנכית
            // מעבר לגבולות המסך - הכותרת "בורחת" מעל החלק הנראה. BoxWithConstraints
            // נותן לנו את הגובה הפנוי האמיתי כדי להגביל את הדיאלוג אליו, עם רק אזור
            // התוכן האמצעי גולל (הכותרת והפוטר תמיד נשארים קבועים/נראים).
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val maxDialogHeight = maxHeight * 0.86f
                Column(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .heightIn(max = maxDialogHeight)
                        .clip(RoundedCornerShape(28.dp))
                        .background(theme.surfaceColor.copy(alpha = if (theme.isDarkMode) 0.92f else 0.97f))
                        .border(1.dp, theme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
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
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        trailingTitleContent?.invoke()
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        content = content
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    footer()
                }
            }
        }
    }
}

/** כפתור מלבני/גלולה בסגנון זכוכית עם טבעת פוקוס ברורה - להחלפת ה-Button/TextButton הגנריים של Material. */
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)

    val background = when {
        isDestructive -> Color(0xFFCF4A4A).copy(alpha = if (isFocused) 0.9f else 0.75f)
        isPrimary -> theme.accentColor.copy(alpha = if (isFocused) 0.95f else 0.8f)
        else -> theme.textColor.copy(alpha = if (isFocused) 0.18f else 0.1f)
    }
    val contentColor = when {
        isDestructive -> Color.White
        isPrimary -> if (theme.accentColor.luminance() > 0.5f) Color.Black else Color.White
        else -> theme.textColor
    }

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(shape)
            .background(background)
            .border(
                width = if (isFocused) 1.5.dp else 0.dp,
                color = if (isFocused) theme.textColor.copy(alpha = 0.6f) else Color.Transparent,
                shape = shape
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun Color.luminance(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)

/** שדה טקסט בסגנון זכוכית - להחלפת ה-OutlinedTextField הגנרי שצובע גבול/תווית בצבעי ברירת המחדל של Material. */
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    theme: FutureTheme,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp, color = theme.textColor.copy(alpha = 0.6f)) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = theme.textColor),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = theme.textColor,
            unfocusedTextColor = theme.textColor,
            focusedBorderColor = theme.accentColor,
            unfocusedBorderColor = theme.textColor.copy(alpha = 0.3f),
            focusedLabelColor = theme.accentColor,
            unfocusedLabelColor = theme.textColor.copy(alpha = 0.5f),
            cursorColor = theme.accentColor
        )
    )
}

/** שורת מתג הגדרה בסגנון זכוכית, עקבית עם שאר תפריטי ההגדרות ב-FutureOS. */
@Composable
private fun GlassSettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    theme: FutureTheme
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        if (isFocused) theme.accentColor.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.05f),
        label = "settingSwitchBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "settingSwitchScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(2.dp, theme.accentColor, shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        // המתג מוצג בלבד (הריבוע כולו כבר clickable דרך ה-Row למעלה) - לכן הוא
        // עטוף כך שלא יקבל פוקוס משלו וייצור עצירה כפולה לאותה פעולה בניווט שלט.
        Box(modifier = Modifier.focusable(false)) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = theme.accentColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = theme.textColor.copy(alpha = 0.3f)
                )
            )
        }
    }
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
                        color = if (isAppFocused) theme.accentColor.copy(alpha = 0.35f) else theme.textColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isAppFocused) theme.accentColor else Color.Transparent),
                        onClick = { onAppClick(app) }
                    ) {
                        Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = app.label, color = theme.textColor, fontSize = 9.sp, maxLines = 1, textAlign = TextAlign.Center)
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

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    GlassDialog(
        onDismissRequest = onDismiss,
        theme = theme,
        title = item.label,
        trailingTitleContent = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onRemove(); onDismiss() }
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.trash),
                    tint = Color(0xFFCF4A4A),
                    modifier = Modifier.size(20.dp)
                )
            }
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
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (item is LauncherItem.Widget) {
                Text(stringResource(R.string.resize_widget), color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.width, item.spanX), color = theme.textColor, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassButton("−", { if (item.spanX > 1) onResize(item.spanX - 1, item.spanY) }, theme, isPrimary = false, modifier = Modifier.size(40.dp, 34.dp))
                            GlassButton("+", { if (item.spanX < 4) onResize(item.spanX + 1, item.spanY) }, theme, isPrimary = false, modifier = Modifier.size(40.dp, 34.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.height, item.spanY), color = theme.textColor, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassButton("−", { if (item.spanY > 1) onResize(item.spanX, item.spanY - 1) }, theme, isPrimary = false, modifier = Modifier.size(40.dp, 34.dp))
                            GlassButton("+", { if (item.spanY < 4) onResize(item.spanX, item.spanY + 1) }, theme, isPrimary = false, modifier = Modifier.size(40.dp, 34.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = theme.textColor.copy(alpha = 0.12f))
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
                HorizontalDivider(color = theme.textColor.copy(alpha = 0.12f))
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

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

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
            HorizontalDivider(color = theme.textColor.copy(alpha = 0.12f))
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
            Text(stringResource(R.string.select_widget_title), color = theme.textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center, fontSize = 14.sp)
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

    LaunchedEffect(restrictToDefaultApps) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        apps = pm.queryIntentActivities(intent, 0)
            .filter { !restrictToDefaultApps || DefaultApps.isDefault(it.activityInfo.packageName) }
            .map { resolveInfo ->
                LauncherItem.App(
                    id = "app:${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}",
                    resolveInfo = resolveInfo,
                    label = resolveInfo.loadLabel(pm).toString()
                )
            }.sortedBy { it.label.lowercase() }
        if (apps.isNotEmpty()) {
            // Dialog() מריץ את החלון שלו במעטפת נפרדת - אם מבקשים פוקוס לפני
            // שהחלון בכלל נדבק, הבקשה נבלעת בשקט (בלי חריגה, בלי אפקט), בדיוק
            // כמו ב-EmptySlotOptionsDialog באותו קובץ.
            delay(100)
            firstItemFocusRequester.requestFocus()
        }
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
                            fontSize = 13.sp,
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
                        fontSize = 7.sp,
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
            Text(stringResource(R.string.launcher_settings_desc), color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(14.dp))

            if (confirmingReset) {
                Text(
                    stringResource(R.string.reset_layout_confirm),
                    color = Color(0xFFCF4A4A),
                    fontSize = 13.sp,
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
