package com.future.tools.ui
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "quick_notes"
private const val KEY_ITEMS = "items"

private data class NoteItem(val text: String, val done: Boolean)

private fun loadItems(context: android.content.Context): List<NoteItem> {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            NoteItem(obj.getString("text"), obj.getBoolean("done"))
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveItems(context: android.content.Context, items: List<NoteItem>) {
    val array = JSONArray()
    items.forEach { item ->
        array.put(JSONObject().apply {
            put("text", item.text)
            put("done", item.done)
        })
    }
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .edit().putString(KEY_ITEMS, array.toString()).apply()
}

@Composable
fun QuickNotesScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<NoteItem>().apply { addAll(loadItems(context)) } }
    var draft by remember { mutableStateOf("") }

    fun persist() = saveItems(context, items)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.escapeTextFieldFocusTrap().fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "רשימה מהירה", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FutureTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        theme = theme,
                        placeholder = "הוסף פריט",
                        modifier = Modifier.weight(1f),
                    )
                    NoteAddButton(theme = theme) {
                        if (draft.isNotBlank()) {
                            items.add(0, NoteItem(draft.trim(), false))
                            draft = ""
                            persist()
                        }
                    }
                }

                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("הרשימה ריקה", color = theme.subtleTextColor, fontSize = FutureTypography.summary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(items) { index, item ->
                            NoteRow(
                                item = item,
                                theme = theme,
                                onToggle = {
                                    items[index] = item.copy(done = !item.done)
                                    persist()
                                },
                                onDelete = {
                                    items.removeAt(index)
                                    persist()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteAddButton(theme: FutureTheme, onClick: () -> Unit) {
    ToolsIconButton(Icons.Rounded.Add, "הוסף", theme, onClick = onClick)
}

/** פריט ברשימה: OK מסמן/מבטל, תיבת הסימון בתחילת השורה, מחיקה בכפתור אייקון בסופה. */
@Composable
private fun NoteRow(item: NoteItem, theme: FutureTheme, onToggle: () -> Unit, onDelete: () -> Unit) {
    FutureListItem(
        title = item.text,
        theme = theme,
        onClick = onToggle,
        titleColor = if (item.done) theme.subtleTextColor else theme.textColor,
        titleDecoration = if (item.done) TextDecoration.LineThrough else null,
        leading = { FutureCheckbox(item.done, theme) },
        trailing = { ToolsIconButton(Icons.Rounded.Close, "מחק", theme, onClick = onDelete) },
    )
}
