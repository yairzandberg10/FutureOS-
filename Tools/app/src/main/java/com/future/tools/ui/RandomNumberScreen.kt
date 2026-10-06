package com.future.tools.ui
import com.future.sharednav.nav.onPoundKeyPress
import com.future.sharednav.components.FutureButton
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.readableAccentColor
import androidx.compose.foundation.border
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
import kotlin.random.Random
import com.future.sharednav.focus.animateFocusColor
import com.future.sharednav.focus.focusScale

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

    fun switchField() {
        activeField = 1 - activeField
        startFresh = true
    }

    fun deleteDigit() {
        if (activeField == 0) {
            minText = if (minText.length <= 1) "0" else minText.dropLast(1)
        } else {
            maxText = if (maxText.length <= 1) "0" else maxText.dropLast(1)
        }
    }

    // # נצרך ב-FutureUI ומגיע רק כשידור - בלי זה אי אפשר היה להגיע לשדה המקסימום.
    onPoundKeyPress { switchField() }

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
                    // רק בלי FutureUI (אמולטור, מקלדת חיצונית) - במכשיר דרך השידור למעלה.
                    if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_POUND) {
                        switchField()
                        return@onKeyEvent true
                    }
                    when (event.key) {
                        Key.Backspace, Key.Delete -> { deleteDigit(); true }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { roll(); true }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // אין במכשיר מקש מחיקה: BACK מוחק ספרה, ויוצא רק כשהשדה הפעיל ריק.
                ToolsHeader(title = "מספר אקראי", theme = theme, onBack = {
                    if ((if (activeField == 0) minText else maxText) != "0") deleteDigit() else onBack()
                })

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RangeField(label = "מינימום", value = minText, isActive = activeField == 0, theme = theme, modifier = Modifier.weight(1f))
                    RangeField(label = "מקסימום", value = maxText, isActive = activeField == 1, theme = theme, modifier = Modifier.weight(1f))
                }

                Text(
                    "# מעבר בין שדות · BACK מוחק ספרה",
                    color = theme.subtleTextColor,
                    fontSize = FutureTypography.caption,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!isValidRange) {
                        Text("המינימום חייב להיות קטן או שווה למקסימום", color = theme.dangerColor, fontSize = FutureTypography.summary, modifier = Modifier.padding(horizontal = 32.dp), textAlign = TextAlign.Center)
                    } else {
                        Text(result?.toString() ?: "?", color = theme.textColor, fontSize = FutureTypography.hero, fontWeight = FutureTypography.weightLight, fontFamily = FutureTypography.monoFamily)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    RollNumberButton(theme = theme, enabled = isValidRange, focusRequester = focusRequester) { roll() }
                }
            }
        }
    }
}

/** שדה מספרי שמקבל ספרות מהמקלדת. השדה הפעיל מסומן כמו שדה ממוקד: מסגרת 2dp בהדגשה. */
@Composable
private fun RangeField(label: String, value: String, isActive: Boolean, theme: FutureTheme, modifier: Modifier = Modifier) {
    val accent = theme.readableAccentColor
    Column(modifier = modifier) {
        Text(label, color = animateFocusColor(isActive, accent, theme.mutedTextColor).value, fontSize = FutureTypography.label, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        Text(
            value,
            color = theme.textColor,
            fontSize = FutureTypography.display,
            fontWeight = FutureTypography.weightLight,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .focusScale(isActive, focusedScale = 1.04f)
                .clip(FutureShapes.textField)
                .background(theme.idleFieldColor)
                .border(FutureDimens.focusBorderControl, animateFocusColor(isActive, accent, Color.Transparent).value, FutureShapes.textField)
                .padding(vertical = FutureDimens.spacingSm)
        )
    }
}

@Composable
private fun RollNumberButton(theme: FutureTheme, enabled: Boolean, focusRequester: FocusRequester, onClick: () -> Unit) {
    FutureButton("הגרל", theme, onClick, fillMaxWidth = true, focusRequester = focusRequester, enabled = enabled)
}
