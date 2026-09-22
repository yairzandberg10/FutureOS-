package com.future.tools.ui
import com.future.sharednav.components.FutureButton
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.components.FutureTabItem
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.mutedTextColor

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class CoinDiceMode(val label: String) { COIN("מטבע"), DICE("קובייה") }

@Composable
fun CoinDiceScreen(theme: FutureTheme, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(CoinDiceMode.COIN) }
    var diceCount by remember { mutableIntStateOf(1) }
    var coinResult by remember { mutableStateOf<Boolean?>(null) } // true=עץ, false=פלי
    var diceResults by remember { mutableStateOf<List<Int>>(emptyList()) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun roll() {
        scope.launch {
            rotation.snapTo(0f)
            rotation.animateTo(360f * 3, animationSpec = tween(FlipMillis))
        }
        when (mode) {
            CoinDiceMode.COIN -> coinResult = Random.nextBoolean()
            CoinDiceMode.DICE -> diceResults = (1..diceCount).map { Random.nextInt(1, 7) }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מטבע וקובייה", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoinDiceMode.entries.forEach { m ->
                        ModeChip(m.label, isSelected = m == mode, theme = theme, modifier = Modifier.weight(1f)) { mode = m }
                    }
                }

                if (mode == CoinDiceMode.DICE) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("מספר קוביות", color = theme.mutedTextColor, fontSize = FutureTypography.summary, modifier = Modifier.weight(1f))
                        ToolsStepperButton("-", theme = theme) { diceCount = (diceCount - 1).coerceAtLeast(1) }
                        Text("$diceCount", color = theme.textColor, fontSize = FutureTypography.title, fontWeight = FontWeight.Medium)
                        ToolsStepperButton("+", theme = theme) { diceCount = (diceCount + 1).coerceAtMost(6) }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (mode) {
                        CoinDiceMode.COIN -> CoinFace(result = coinResult, rotationDeg = rotation.value, theme = theme)
                        CoinDiceMode.DICE -> DiceRow(results = diceResults, rotationDeg = rotation.value, theme = theme)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                    RollButton(theme = theme) { roll() }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, isSelected: Boolean, theme: FutureTheme, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FutureTabItem(label, isSelected, theme, onClick, modifier)
}

@Composable
private fun CoinFace(result: Boolean?, rotationDeg: Float, theme: FutureTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .rotate(rotationDeg)
                .clip(CircleShape)
                .background(theme.textAlpha(12)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (result) {
                    true -> "עץ"
                    false -> "פלי"
                    null -> "?"
                },
                color = theme.textColor, fontSize = FutureTypography.display, fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            when (result) {
                true -> "יצא עץ"
                false -> "יצא פלי"
                null -> "לחץ על \"הטל\" כדי להתחיל"
            },
            color = theme.mutedTextColor, fontSize = FutureTypography.body
        )
    }
}

@Composable
private fun DiceRow(results: List<Int>, rotationDeg: Float, theme: FutureTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (results.isEmpty()) {
                DiceFace(value = null, rotationDeg = rotationDeg, theme = theme)
            } else {
                results.forEach { value -> DiceFace(value = value, rotationDeg = rotationDeg, theme = theme) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (results.isNotEmpty()) {
            Text("סה\"כ: ${results.sum()}", color = theme.textColor, fontSize = FutureTypography.title, fontWeight = FontWeight.Bold)
        } else {
            Text("לחץ על \"הטל\" כדי להתחיל", color = theme.mutedTextColor, fontSize = FutureTypography.body)
        }
    }
}

@Composable
private fun DiceFace(value: Int?, rotationDeg: Float, theme: FutureTheme) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .rotate(rotationDeg)
            .clip(FutureShapes.md)
            .background(theme.textColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(value?.toString() ?: "?", color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RollButton(theme: FutureTheme, onClick: () -> Unit) {
    FutureButton("הטל", theme, onClick, fillMaxWidth = true)
}

/** סיבוב ההטלה - אנימציה של התוכן (המטבע מסתובב), לא מעבר של ממשק, ולכן מחוץ לסקאלת התנועה. */
private const val FlipMillis = 500
