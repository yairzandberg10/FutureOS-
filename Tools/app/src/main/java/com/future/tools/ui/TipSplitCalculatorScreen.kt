package com.future.tools.ui
import com.future.sharednav.components.FutureTabItem
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.mutedTextColor

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import java.text.DecimalFormat

private val moneyFormat = DecimalFormat("#,##0.##")
private val TIP_PRESETS = listOf(10, 15, 18, 20)

@Composable
fun TipSplitCalculatorScreen(theme: FutureTheme, onBack: () -> Unit) {
    var billText by remember { mutableStateOf("0") }
    var startFresh by remember { mutableStateOf(true) }
    var tipPercent by remember { mutableIntStateOf(15) }
    var people by remember { mutableIntStateOf(1) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val bill = billText.toDoubleOrNull() ?: 0.0
    val tipAmount = bill * tipPercent / 100.0
    val total = bill + tipAmount
    val perPerson = total / people

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    digitForKey(event.key)?.let {
                        billText = if (startFresh || billText == "0") it else billText + it
                        startFresh = false
                        return@onKeyEvent true
                    }
                    if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_STAR) {
                        if (!billText.contains(".")) billText += "."
                        startFresh = false
                        return@onKeyEvent true
                    }
                    when (event.key) {
                        Key.Backspace, Key.Delete -> {
                            billText = if (billText.length <= 1) "0" else billText.dropLast(1)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "טיפים ופיצול חשבון", theme = theme, onBack = onBack)

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text("סכום החשבון", color = theme.mutedTextColor, fontSize = FutureTypography.label)
                    Text(
                        billText,
                        color = theme.textColor,
                        fontSize = FutureTypography.display,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("אחוז טיפ", color = theme.mutedTextColor, fontSize = FutureTypography.label, modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TIP_PRESETS.forEachIndexed { index, preset ->
                        TipChip(
                            "$preset%", isSelected = tipPercent == preset, theme = theme,
                            modifier = Modifier.weight(1f),
                            focusRequester = if (index == 0) focusRequester else null
                        ) { tipPercent = preset }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("מספר סועדים", color = theme.mutedTextColor, fontSize = FutureTypography.label, modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ToolsStepperButton("-", theme = theme) { people = (people - 1).coerceAtLeast(1) }
                    Text("$people", color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.Medium)
                    ToolsStepperButton("+", theme = theme) { people = (people + 1).coerceAtMost(99) }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultRow("סכום הטיפ", moneyFormat.format(tipAmount), theme = theme)
                    ResultRow("סה\"כ לתשלום", moneyFormat.format(total), theme = theme, isEmphasized = true)
                    ResultRow("לתשלום לכל סועד", moneyFormat.format(perPerson), theme = theme, isAccent = true)
                }
            }
        }
    }
}

@Composable
private fun TipChip(
    label: String,
    isSelected: Boolean,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    FutureTabItem(label, isSelected, theme, onClick, modifier, focusRequester)
}

@Composable
private fun ResultRow(label: String, value: String, theme: FutureTheme, isEmphasized: Boolean = false, isAccent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = theme.mutedTextColor, fontSize = FutureTypography.body)
        Text(
            value,
            color = theme.textColor,
            fontSize = if (isEmphasized || isAccent) FutureTypography.screenTitle else FutureTypography.dialog,
            fontWeight = if (isEmphasized || isAccent) FontWeight.Bold else FontWeight.Medium
        )
    }
}
