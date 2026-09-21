package com.future.tasks.ui.screens

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
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
) {
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
        trailingIcon = Icons.Rounded.Add,
        trailingContentDescription = "משימה חדשה",
        onTrailingClick = onAddTask,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(searchQuery, onSearchChanged, theme)

            if (tasks.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Checklist,
                    title = if (searchQuery.isBlank()) "אין משימות" else "לא נמצאו משימות",
                    subtitle = if (searchQuery.isBlank()) "נווטו לכפתור ההוספה למעלה ולחצו OK כדי להוסיף אחת" else null,
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit, theme: FutureTheme) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.screenPadding, vertical = 8.dp)
            .clip(FutureShapes.md)
            .background(theme.textColor.copy(alpha = if (isFocused) 0.14f else 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = theme.textColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("חיפוש משימות", color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.body)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = TextStyle(color = theme.textColor, fontSize = FutureTypography.body),
                cursorBrush = SolidColor(theme.accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
            )
        }
    }
}

@Composable
private fun TaskRow(task: Task, theme: FutureTheme, focusRequester: FocusRequester, onClick: () -> Unit) {
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = FutureDimens.cardCornerRadius,
        contentPadding = 12.dp,
        focusRequester = focusRequester,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (task.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.isDone) theme.successColor else theme.textColor.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title.ifEmpty { "משימה ללא שם" },
                    color = if (task.isDone) theme.textColor.copy(alpha = 0.4f) else theme.textColor,
                    fontSize = FutureTypography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    maxLines = 1,
                )
                if (task.notes.isNotBlank()) {
                    Text(task.notes, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.label, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            PriorityDot(task.priority, theme)
        }
    }
}

@Composable
private fun PriorityDot(priority: Int, theme: FutureTheme) {
    val color = when (priority) {
        TaskPriority.HIGH -> theme.dangerColor
        TaskPriority.LOW -> theme.textColor.copy(alpha = 0.25f)
        else -> theme.warningColor
    }
    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
}
