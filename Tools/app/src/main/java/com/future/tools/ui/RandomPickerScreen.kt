package com.future.tools.ui

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import kotlin.random.Random

@Composable
fun RandomPickerScreen(theme: FutureTheme, onBack: () -> Unit) {
    val options = remember { mutableStateListOf("פיצה", "סושי", "המבורגר") }
    var draft by remember { mutableStateOf("") }
    var chosenIndex by remember { mutableStateOf<Int?>(null) }

    fun pick() {
        if (options.isNotEmpty()) chosenIndex = Random.nextInt(options.size)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "בורר אקראי", theme = theme, onBack = onBack)

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
                        cursorBrush = SolidColor(theme.accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.textColor.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) {
                                Text("הוסף אפשרות...", color = theme.textColor.copy(alpha = 0.35f), fontSize = 15.sp)
                            }
                            inner()
                        }
                    )
                    RpAddButton(theme = theme) {
                        if (draft.isNotBlank()) {
                            options.add(draft.trim())
                            draft = ""
                            chosenIndex = null
                        }
                    }
                }

                if (options.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("הוסף לפחות אפשרות אחת", color = theme.textColor.copy(alpha = 0.35f), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(options) { index, option ->
                            RpOptionRow(
                                text = option,
                                isChosen = chosenIndex == index,
                                theme = theme,
                                onDelete = {
                                    options.removeAt(index)
                                    chosenIndex = null
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    RpPickButton(theme = theme, enabled = options.isNotEmpty()) { pick() }
                }
            }
        }
    }
}

@Composable
private fun RpAddButton(theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.1f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "הוסף", tint = if (isFocused) Color.Black else theme.accentColor)
    }
}

@Composable
private fun RpOptionRow(text: String, isChosen: Boolean, theme: FutureTheme, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isChosen) theme.accentColor.copy(alpha = 0.25f) else theme.textColor.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = if (isChosen) theme.accentColor else theme.textColor,
            fontSize = 15.sp,
            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        val deleteInteraction = remember { MutableInteractionSource() }
        val deleteFocused by deleteInteraction.collectIsFocusedAsState()
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (deleteFocused) Color(0xFFFF6B6B).copy(alpha = 0.3f) else Color.Transparent)
                .clickable(interactionSource = deleteInteraction, indication = null, onClick = onDelete)
                .focusable(interactionSource = deleteInteraction),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "מחק", tint = theme.textColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun RpPickButton(theme: FutureTheme, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor = if (!enabled) theme.textColor.copy(alpha = 0.08f) else if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("בחר אקראית", color = if (isFocused && enabled) Color.Black else theme.textColor.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
