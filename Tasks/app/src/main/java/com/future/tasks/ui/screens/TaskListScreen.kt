package com.future.tasks.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.rounded.CheckCircle
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.textAlpha

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.tasks.data.Task
import com.future.tasks.data.TaskPriority

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    searchQuery: String,
    theme: FutureTheme,
    onSearchChanged: (String) -> Unit,
    onTaskClick: (Task) -> Unit,
    onAddTask: () -> Unit,
    // המשימה שנפתחה לאחרונה - כשחוזרים "אחורה" מהעורך, הפוקוס צריך לשוב
    // אליה בדיוק, לא תמיד למשימה הראשונה ברשימה.
    lastSelectedTaskId: Int? = null,
    onToggleDone: (Task) -> Unit = {},
    onDelete: (Task) -> Unit = {},
) {
    // TK6: Options על משימה ממוקדת - סימון "בוצע" ומחיקה בלי להיכנס לעורך.
    var focusedTaskId by remember { mutableStateOf<Int?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Task?>(null) }
    com.future.sharednav.nav.onOptionsKeyPress { if (pendingDelete == null) menuOpen = true }
    val focusedTask = tasks.firstOrNull { it.id == focusedTaskId }
    if (menuOpen) {
        com.future.sharednav.components.FutureOptionsMenu(
            theme = theme,
            onDismissRequest = { menuOpen = false },
            header = focusedTask?.title?.ifEmpty { "משימה ללא שם" } ?: "משימות",
        ) {
            if (focusedTask != null) {
                com.future.sharednav.components.FutureMenuRow(
                    if (focusedTask.isDone) "סמן כלא בוצע" else "סמן כבוצע",
                    FutureIcons.Checklist,
                    theme,
                    { menuOpen = false; onToggleDone(focusedTask) },
                )
            }
            com.future.sharednav.components.FutureMenuRow("משימה חדשה", FutureIcons.Add, theme, { menuOpen = false; onAddTask() })
            if (focusedTask != null) {
                com.future.sharednav.components.FutureMenuRow("מחק", FutureIcons.Delete, theme, {
                    menuOpen = false
                    pendingDelete = focusedTask
                }, destructive = true)
            }
        }
    }
    pendingDelete?.let { task ->
        com.future.sharednav.components.ConfirmDialog(
            message = "למחוק את המשימה \"${task.title.ifEmpty { "ללא שם" }}\"?",
            theme = theme,
            onCancel = { pendingDelete = null },
            onConfirm = { pendingDelete = null; onDelete(task) },
        )
    }
    val rowFocusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val addFocusRequester = remember { FocusRequester() }
    LaunchedEffect(tasks.map { it.id }) {
        if (tasks.isEmpty()) {
            addFocusRequester.requestFocus()
        } else {
            val target = tasks.firstOrNull { it.id == lastSelectedTaskId } ?: tasks.first()
            rowFocusRequesters.getOrPut(target.id) { FocusRequester() }.requestFocus()
        }
    }

    ScreenScaffold(
        modifier = Modifier.escapeTextFieldFocusTrap(),
        backgroundColor = theme.backgroundColor,
        title = "משימות",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        trailingIcon = FutureIcons.Add,
        trailingContentDescription = "משימה חדשה",
        onTrailingClick = onAddTask,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(searchQuery, onSearchChanged, theme)

            if (tasks.isEmpty() && searchQuery.isNotBlank()) {
                // TK7: בחיפוש ההודעה מיד מתחת לשדה, ולא באמצע המסך - שם היא הוסתרה
                // מתחת לפס המקלדת.
                Text(
                    "לא נמצאו משימות",
                    color = theme.mutedTextColor,
                    fontSize = com.future.sharednav.theme.FutureTypography.summary,
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                )
            } else if (tasks.isEmpty()) {
                EmptyState(
                    icon = FutureIcons.Checklist,
                    title = if (searchQuery.isBlank()) "אין משימות" else "לא נמצאו משימות",
                    subtitle = if (searchQuery.isBlank()) "נווט לכפתור ההוספה שלמעלה ולחץ OK" else null,
                    textColor = theme.textColor,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            theme = theme,
                            focusRequester = rowFocusRequesters.getOrPut(task.id) { FocusRequester() },
                            onClick = { onTaskClick(task) },
                            modifier = Modifier.onFocusChanged { if (it.isFocused || it.hasFocus) focusedTaskId = task.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit, theme: FutureTheme) {
    FutureTextField(
        value = query,
        onValueChange = onQueryChanged,
        theme = theme,
        placeholder = "חיפוש משימות",
        leading = {
            Icon(FutureIcons.Search, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
    )
}

/** שורת משימה - שורת הרשימה של הדיזיין סיסטם; משימה שבוצעה בקו חוצה וב-40%. */
@Composable
private fun TaskRow(task: Task, theme: FutureTheme, focusRequester: FocusRequester, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // תזכורת עתידית מוצגת בשורה ("תזכורת: מחר 09:00"), לפני ההערות.
    val reminder = task.reminderAt?.takeIf { !task.isDone && it > System.currentTimeMillis() }
        ?.let { "תזכורת: " + com.future.tasks.data.TaskReminders.label(it) }
    FutureListItem(
        title = task.title.ifEmpty { "משימה ללא שם" },
        summary = listOfNotNull(reminder, task.notes.ifBlank { null }).joinToString(" · ").ifEmpty { null },
        modifier = modifier,
        theme = theme,
        onClick = onClick,
        focusRequester = focusRequester,
        titleColor = if (task.isDone) theme.subtleTextColor else theme.textColor,
        titleDecoration = if (task.isDone) TextDecoration.LineThrough else null,
        leading = {
            Icon(
                if (task.isDone) Icons.Rounded.CheckCircle else FutureIcons.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.isDone) theme.successColor else theme.subtleTextColor,
                modifier = Modifier.size(FutureDimens.iconSettingRow),
            )
        },
        trailing = { PriorityDot(task.priority, theme) },
    )
}

@Composable
private fun PriorityDot(priority: Int, theme: FutureTheme) {
    val color = when (priority) {
        TaskPriority.HIGH -> theme.dangerColor
        TaskPriority.LOW -> theme.textAlpha(30)
        else -> theme.warningColor
    }
    Box(modifier = Modifier.size(10.dp).clip(FutureShapes.pill).background(color))
}
