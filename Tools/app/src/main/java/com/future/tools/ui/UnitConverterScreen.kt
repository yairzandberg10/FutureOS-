package com.future.tools.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import java.text.DecimalFormat

private data class UnitDef(val label: String, val toBase: (Double) -> Double, val fromBase: (Double) -> Double)

private val LENGTH_UNITS = listOf(
    UnitDef("מטר", { it }, { it }),
    UnitDef("ק\"מ", { it * 1000 }, { it / 1000 }),
    UnitDef("ס\"מ", { it * 0.01 }, { it / 0.01 }),
    UnitDef("מייל", { it * 1609.344 }, { it / 1609.344 }),
    UnitDef("רגל", { it * 0.3048 }, { it / 0.3048 })
)
private val WEIGHT_UNITS = listOf(
    UnitDef("ק\"ג", { it }, { it }),
    UnitDef("גרם", { it * 0.001 }, { it / 0.001 }),
    UnitDef("פאונד", { it * 0.453592 }, { it / 0.453592 }),
    UnitDef("אונקיה", { it * 0.0283495 }, { it / 0.0283495 })
)
private val TEMP_UNITS = listOf(
    UnitDef("צלזיוס", { it }, { it }),
    UnitDef("פרנהייט", { (it - 32) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32 }),
    UnitDef("קלווין", { it - 273.15 }, { it + 273.15 })
)
private val VOLUME_UNITS = listOf(
    UnitDef("ליטר", { it }, { it }),
    UnitDef("מ\"ל", { it * 0.001 }, { it / 0.001 }),
    UnitDef("גלון (US)", { it * 3.78541 }, { it / 3.78541 }),
    UnitDef("כוס", { it * 0.24 }, { it / 0.24 })
)

private enum class ConvCategory(val label: String, val units: List<UnitDef>) {
    LENGTH("אורך", LENGTH_UNITS),
    WEIGHT("משקל", WEIGHT_UNITS),
    TEMP("טמפ'", TEMP_UNITS),
    VOLUME("נפח", VOLUME_UNITS)
}

private val resultFormat = DecimalFormat("#,##0.####")

@Composable
fun UnitConverterScreen(theme: FutureTheme, onBack: () -> Unit) {
    var category by remember { mutableStateOf(ConvCategory.LENGTH) }
    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(1) }
    var inputText by remember { mutableStateOf("0") }
    var startFresh by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val inputValue = inputText.toDoubleOrNull() ?: 0.0
    val units = category.units
    val resultValue = try {
        units[toIndex].fromBase(units[fromIndex].toBase(inputValue))
    } catch (e: Exception) {
        0.0
    }

    fun setCategory(c: ConvCategory) {
        category = c
        fromIndex = 0
        toIndex = (1).coerceAtMost(c.units.size - 1)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // כמו במחשבון: הפוקוס לא יושב על ה-Box החיצוני (אחרת הוא "בולע" אותו
        // לצמיתות ומונע ניווט D-pad לצ'יפים/כפתורים) אלא על הצ'יפ הראשון בפועל.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    digitForKey(event.key)?.let {
                        inputText = if (startFresh || inputText == "0") it else inputText + it
                        startFresh = false
                        return@onKeyEvent true
                    }
                    if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_STAR) {
                        if (!inputText.contains(".")) inputText += "."
                        startFresh = false
                        return@onKeyEvent true
                    }
                    when (event.key) {
                        Key.Backspace, Key.Delete -> {
                            inputText = if (inputText.length <= 1) "0" else inputText.dropLast(1)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "ממיר יחידות", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConvCategory.entries.forEachIndexed { index, c ->
                        CategoryChip(
                            c.label,
                            isSelected = c == category,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            focusRequester = if (index == 0) focusRequester else null
                        ) { setCategory(c) }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    UnitChip(units[fromIndex].label, theme = theme) { fromIndex = (fromIndex + 1) % units.size }
                    Text(
                        inputText,
                        color = theme.textColor,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    SwapButton(theme = theme) {
                        val oldFrom = fromIndex
                        fromIndex = toIndex
                        toIndex = oldFrom
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    UnitChip(units[toIndex].label, theme = theme) { toIndex = (toIndex + 1) % units.size }
                    Text(
                        resultFormat.format(resultValue),
                        color = theme.accentColor,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                Text(
                    "הקלד ערך במקלדת · אישור על יחידה כדי להחליף",
                    color = theme.textColor.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val bgColor by animateColorAsState(
        when {
            isSelected -> theme.accentColor
            isFocused -> theme.textColor.copy(alpha = 0.18f)
            else -> theme.textColor.copy(alpha = 0.06f)
        },
        label = "categoryChipBg"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isSelected) Color.Black else theme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UnitChip(label: String, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.16f) else theme.textColor.copy(alpha = 0.06f), label = "unitChipBg")
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = theme.textColor.copy(alpha = 0.8f), fontSize = 13.sp)
    }
}

@Composable
private fun SwapButton(theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.35f) else theme.textColor.copy(alpha = 0.08f), label = "swapBg")
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.SwapVert, contentDescription = "החלף כיוון", tint = theme.accentColor, modifier = Modifier.size(18.dp))
    }
}
