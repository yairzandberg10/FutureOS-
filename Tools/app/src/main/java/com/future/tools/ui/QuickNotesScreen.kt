package com.future.tools.ui
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
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "רשימה מהירה", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = TextStyle(color = theme.textColor, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.textColor.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) {
                                Text("הוסף פריט חדש...", color = theme.textColor.copy(alpha = 0.35f), fontSize = 15.sp)
                            }
                            inner()
                        }
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
                        Text("הרשימה ריקה", color = theme.textColor.copy(alpha = 0.35f), fontSize = 13.sp)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.1f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "הוסף", tint = if (isFocused) Color.Black else theme.accentColor)
    }
}

@Composable
private fun NoteRow(item: NoteItem, theme: FutureTheme, onToggle: () -> Unit, onDelete: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isFocused) theme.textColor.copy(alpha = 0.14f) else theme.textColor.copy(alpha = 0.05f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (item.done) theme.accentColor else theme.textColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.done) Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            item.text,
            color = if (item.done) theme.textColor.copy(alpha = 0.4f) else theme.textColor,
            fontSize = 15.sp,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        val deleteInteraction = remember { MutableInteractionSource() }
        val deleteFocused by deleteInteraction.collectIsFocusedAsState()
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (deleteFocused) theme.dangerColor.copy(alpha = 0.3f) else Color.Transparent)
                .clickable(interactionSource = deleteInteraction, indication = null, onClick = onDelete)
                .focusable(interactionSource = deleteInteraction).bringIntoViewOnFocus(),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "מחק", tint = theme.textColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
        }
    }
}
