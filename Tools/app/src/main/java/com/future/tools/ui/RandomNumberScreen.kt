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
import kotlin.random.Random

@Composable
fun RandomNumberScreen(theme: FutureTheme, onBack: () -> Unit) {
    var minText by remember { mutableStateOf("1") }
    var maxText by remember { mutableStateOf("100") }
    var activeField by remember { mutableIntStateOf(0) } // 0=מינימום, 1=מקסימום
    var startFresh by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<Int?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val minValue = minText.toIntOrNull() ?: 0
    val maxValue = maxText.toIntOrNull() ?: 0
    val isValidRange = minValue <= maxValue

    fun roll() {
        if (isValidRange) result = Random.nextInt(minValue, maxValue + 1)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    digitForKey(event.key)?.let { digit ->
                        if (activeField == 0) {
                            minText = if (startFresh || minText == "0") digit else minText + digit
                        } else {
                            maxText = if (startFresh || maxText == "0") digit else maxText + digit
                        }
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
                                minText = if (minText.length <= 1) "0" else minText.dropLast(1)
                            } else {
                                maxText = if (maxText.length <= 1) "0" else maxText.dropLast(1)
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { roll(); true }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מספר אקראי", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RangeField(label = "מינימום", value = minText, isActive = activeField == 0, theme = theme, modifier = Modifier.weight(1f))
                    RangeField(label = "מקסימום", value = maxText, isActive = activeField == 1, theme = theme, modifier = Modifier.weight(1f))
                }

                Text(
                    "# למעבר בין שדות · הקלד מהמקלדת",
                    color = theme.textColor.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!isValidRange) {
                        Text("המינימום חייב להיות קטן או שווה למקסימום", color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 32.dp), textAlign = TextAlign.Center)
                    } else {
                        Text(result?.toString() ?: "?", color = theme.textColor, fontSize = 64.sp, fontWeight = FontWeight.Light)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    RollNumberButton(theme = theme, enabled = isValidRange, focusRequester = focusRequester) { roll() }
                }
            }
        }
    }
}

@Composable
private fun RangeField(label: String, value: String, isActive: Boolean, theme: FutureTheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = if (isActive) theme.accentColor else theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        Text(
            value,
            color = theme.textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.textColor.copy(alpha = if (isActive) 0.12f else 0.05f))
                .padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun RollNumberButton(theme: FutureTheme, enabled: Boolean, focusRequester: FocusRequester, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor = if (!enabled) theme.textColor.copy(alpha = 0.08f) else if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .focusRequester(focusRequester)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("הגרל", color = if (isFocused && enabled) Color.Black else theme.textColor.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
