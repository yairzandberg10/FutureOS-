package com.future.tasks.ui.screens
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Save

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureSectionHeader

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import com.future.tasks.data.Task
import com.future.tasks.data.TaskPriority

@Composable
fun TaskEditorScreen(
    task: Task?,
    theme: FutureTheme,
    onSave: (title: String, notes: String, priority: Int, isDone: Boolean) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    // ה-remember חייב להיות ממופתח (keyed) ב-id של המשימה: כשהמסך נפתח לפני
    // שהמשימה האמיתית זמינה (task==null זמנית), ה-state מאותחל לריק - בלי
    // key זה לא היה מתאפס נכון במעבר בין משימות שונות.
    val taskKey = task?.id ?: -1
    var title by remember(taskKey) { mutableStateOf(task?.title ?: "") }
    var notes by remember(taskKey) { mutableStateOf(task?.notes ?: "") }
    var priority by remember(taskKey) { mutableStateOf(task?.priority ?: TaskPriority.NORMAL) }
    var isDone by remember(taskKey) { mutableStateOf(task?.isDone ?: false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun saveIfNeeded() {
        if (title.isNotBlank() || notes.isNotBlank()) onSave(title, notes, priority, isDone) else onBack()
    }

    // מקש Back פיזי שומר (אלא אם המשימה ריקה לגמרי) במקום לזרוק שינויים בשקט.
    BackHandler { saveIfNeeded() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.escapeTextFieldFocusTrap().fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (task == null) "משימה חדשה" else "עריכת משימה",
                        fontSize = FutureTypography.title,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    TopBarIconButton(
                        if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        "בוצע",
                        theme.textColor,
                        theme.successColor,
                        { isDone = !isDone },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (task != null) {
                        TopBarIconButton(FutureIcons.Delete, "מחק", theme.textColor, theme.dangerColor) { showDeleteConfirm = true }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TopBarIconButton(Icons.Rounded.Save, "שמור", theme.textColor, theme.accentColor) {
                        onSave(title, notes, priority, isDone)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    EditorField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "כותרת המשימה",
                        theme = theme,
                        singleLine = true,
                    )
                    EditorField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = "פרטים נוספים",
                        theme = theme,
                        singleLine = false,
                        modifier = Modifier.weight(1f),
                    )

                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        FutureSectionHeader("עדיפות", theme, inset = false)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PriorityChip("נמוכה", TaskPriority.LOW, priority, theme.textColor.copy(alpha = 0.4f), theme) { priority = it }
                            PriorityChip("רגילה", TaskPriority.NORMAL, priority, theme.warningColor, theme) { priority = it }
                            PriorityChip("גבוהה", TaskPriority.HIGH, priority, theme.dangerColor, theme) { priority = it }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            message = "למחוק את המשימה \"${task?.title.orEmpty().ifEmpty { "ללא שם" }}\"?",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; onDelete() },
        )
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    theme: FutureTheme,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    FutureTextField(
        value = value,
        onValueChange = onValueChange,
        theme = theme,
        placeholder = placeholder,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
    )
}

/** בחירת עדיפות - הצ'יפ של הדיזיין סיסטם; הנבחר במילוי ההדגשה עם הדיו שלה (קודם
 * הצבע היה של העדיפות, אבל הטקסט נגזר מההדגשה - זוג שלא מבטיח ניגודיות). */
@Composable
private fun PriorityChip(label: String, value: Int, current: Int, @Suppress("UNUSED_PARAMETER") activeColor: Color, theme: FutureTheme, onSelect: (Int) -> Unit) {
    FutureChip(label, theme, selected = value == current, onClick = { onSelect(value) })
}
