package com.future.tools.ui
import com.future.sharednav.components.FutureButton
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.onAccentColor
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
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
        Box(modifier = Modifier.escapeTextFieldFocusTrap().fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "בורר אקראי", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FutureTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        theme = theme,
                        placeholder = "הוסף אפשרות",
                        modifier = Modifier.weight(1f),
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
                        Text("הוסף לפחות אפשרות אחת", color = theme.subtleTextColor, fontSize = FutureTypography.summary)
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
    ToolsIconButton(Icons.Rounded.Add, "הוסף", theme, onClick = onClick)
}

/**
 * אפשרות ברשימה. האפשרות שנבחרה בהגרלה מסומנת כ"נבחר" של הדיזיין סיסטם -
 * מילוי מלא בהדגשה עם הדיו שמעליה (states.html: "Selected always beats
 * focused"). קודם זה היה 25% הדגשה, דרגה שאינה בסולם.
 */
@Composable
private fun RpOptionRow(text: String, isChosen: Boolean, theme: FutureTheme, onDelete: () -> Unit) {
    val type = rememberFutureType()
    val accent = theme.readableAccentColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FutureShapes.sm)
            .background(if (isChosen) accent else theme.idleChipColor)
            .padding(start = FutureDimens.spacingMd, end = FutureDimens.spacingXs, top = FutureDimens.spacingXs, bottom = FutureDimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = if (isChosen) theme.onReadableAccentColor else theme.textColor,
            fontSize = type.title,
            fontWeight = if (isChosen) FutureTypography.weightBold else FutureTypography.weightMedium,
            modifier = Modifier.weight(1f)
        )
        ToolsIconButton(Icons.Rounded.Close, "מחק", theme, tint = if (isChosen) theme.onReadableAccentColor else theme.textColor, onClick = onDelete)
    }
}

@Composable
private fun RpPickButton(theme: FutureTheme, enabled: Boolean, onClick: () -> Unit) {
    FutureButton("בחר אקראית", theme, onClick, fillMaxWidth = true, enabled = enabled)
}
