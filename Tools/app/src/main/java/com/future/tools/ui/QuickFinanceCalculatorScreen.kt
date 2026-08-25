package com.future.tools.ui

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
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import java.text.DecimalFormat
import kotlin.math.pow

private val moneyFormat = DecimalFormat("#,##0.##")

private enum class FinanceMode(val label: String) { DISCOUNT("הנחה"), TAX("מע\"מ"), LOAN("הלוואה") }

@Composable
fun QuickFinanceCalculatorScreen(theme: FutureTheme, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(FinanceMode.DISCOUNT) }

    // שדה ראשי: מחיר/סכום קרן. שדה משני: אחוז הנחה-מס, או ריבית שנתית להלוואה
    var primaryText by remember { mutableStateOf("0") }
    var secondaryText by remember { mutableStateOf("0") }
    var months by remember { mutableIntStateOf(12) }
    var activeField by remember { mutableStateOf(0) } // 0=ראשי, 1=משני
    var startFresh by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun resetInputs() {
        primaryText = "0"; secondaryText = "0"; activeField = 0; startFresh = true
    }

    val primary = primaryText.toDoubleOrNull() ?: 0.0
    val secondary = secondaryText.toDoubleOrNull() ?: 0.0

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    digitForKey(event.key)?.let { digit ->
                        if (activeField == 0) {
                            primaryText = if (startFresh || primaryText == "0") digit else primaryText + digit
                        } else {
                            secondaryText = if (startFresh || secondaryText == "0") digit else secondaryText + digit
                        }
                        startFresh = false
                        return@onKeyEvent true
                    }
                    if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_STAR) {
                        if (activeField == 0 && !primaryText.contains(".")) primaryText += "."
                        if (activeField == 1 && !secondaryText.contains(".")) secondaryText += "."
                        startFresh = false
                        return@onKeyEvent true
                    }
                    if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_POUND) {
                        activeField = 1 - activeField
                        startFresh = true
                        return@onKeyEvent true
                    }
                    when (event.key) {
                        Key.Backspace, Key.Delete -> {
                            if (activeField == 0) {
                                primaryText = if (primaryText.length <= 1) "0" else primaryText.dropLast(1)
                            } else {
                                secondaryText = if (secondaryText.length <= 1) "0" else secondaryText.dropLast(1)
                            }
                            true
                        }
                        Key.Tab -> { activeField = 1 - activeField; startFresh = true; true }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מחשבון פיננסי", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinanceMode.entries.forEachIndexed { index, m ->
                        FinanceChip(
                            m.label, isSelected = m == mode, theme = theme,
                            modifier = Modifier.weight(1f),
                            focusRequester = if (index == 0) focusRequester else null
                        ) {
                            mode = m
                            resetInputs()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FinanceField(
                    label = when (mode) {
                        FinanceMode.DISCOUNT -> "מחיר מקורי"
                        FinanceMode.TAX -> "סכום לפני מע\"מ"
                        FinanceMode.LOAN -> "סכום ההלוואה"
                    },
                    value = primaryText, isActive = activeField == 0, theme = theme
                )

                Spacer(modifier = Modifier.height(10.dp))

                FinanceField(
                    label = when (mode) {
                        FinanceMode.DISCOUNT -> "אחוז הנחה"
                        FinanceMode.TAX -> "אחוז מע\"מ"
                        FinanceMode.LOAN -> "ריבית שנתית (%)"
                    },
                    value = secondaryText, isActive = activeField == 1, theme = theme
                )

                if (mode == FinanceMode.LOAN) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("תקופה (חודשים)", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        ToolsStepperButton("-", theme = theme) { months = (months - 1).coerceAtLeast(1) }
                        Text("$months", color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        ToolsStepperButton("+", theme = theme) { months = (months + 1).coerceAtMost(480) }
                    }
                }

                Text(
                    "# להחלפת שדה קלט · הקלד ספרות מהמקלדת",
                    color = theme.textColor.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (mode) {
                        FinanceMode.DISCOUNT -> {
                            val discountAmount = primary * secondary / 100.0
                            FinanceResultRow("סכום ההנחה", moneyFormat.format(discountAmount), theme = theme)
                            FinanceResultRow("מחיר סופי", moneyFormat.format(primary - discountAmount), theme = theme, isAccent = true)
                        }
                        FinanceMode.TAX -> {
                            val taxAmount = primary * secondary / 100.0
                            FinanceResultRow("סכום המע\"מ", moneyFormat.format(taxAmount), theme = theme)
                            FinanceResultRow("מחיר כולל מע\"מ", moneyFormat.format(primary + taxAmount), theme = theme, isAccent = true)
                        }
                        FinanceMode.LOAN -> {
                            val monthlyRate = secondary / 100.0 / 12.0
                            val payment = if (monthlyRate == 0.0) {
                                primary / months
                            } else {
                                primary * monthlyRate * (1 + monthlyRate).pow(months) / ((1 + monthlyRate).pow(months) - 1)
                            }
                            val totalPaid = payment * months
                            FinanceResultRow("החזר חודשי", moneyFormat.format(payment), theme = theme, isAccent = true)
                            FinanceResultRow("סה\"כ ריבית", moneyFormat.format(totalPaid - primary), theme = theme)
                            FinanceResultRow("סה\"כ החזר", moneyFormat.format(totalPaid), theme = theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceField(label: String, value: String, isActive: Boolean, theme: FutureTheme) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(label, color = if (isActive) theme.accentColor else theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        Text(
            value,
            color = theme.textColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
    }
}

@Composable
private fun FinanceChip(
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
    val bgColor = when {
        isSelected -> theme.accentColor
        isFocused -> theme.textColor.copy(alpha = 0.18f)
        else -> theme.textColor.copy(alpha = 0.06f)
    }
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
        Text(label, color = if (isSelected) Color.Black else theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FinanceResultRow(label: String, value: String, theme: FutureTheme, isAccent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = theme.textColor.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(
            value,
            color = if (isAccent) theme.accentColor else theme.textColor,
            fontSize = if (isAccent) 20.sp else 15.sp,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Medium
        )
    }
}
