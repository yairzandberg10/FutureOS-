package com.future.tools.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.future.tools.ui.theme.FutureTheme
import java.math.BigDecimal
import java.math.MathContext

private enum class CalcOp { ADD, SUB, MUL, DIV }

private fun CalcOp.symbol(): String = when (this) {
    CalcOp.ADD -> "+"
    CalcOp.SUB -> "−"
    CalcOp.MUL -> "×"
    CalcOp.DIV -> "÷"
}

private data class CalcHistoryEntry(val expression: String, val result: String)

@Composable
fun CalculatorScreen(theme: FutureTheme, onBack: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var expressionLine by remember { mutableStateOf("") }
    var pendingValue by remember { mutableStateOf<BigDecimal?>(null) }
    var pendingOp by remember { mutableStateOf<CalcOp?>(null) }
    var startFresh by remember { mutableStateOf(true) }
    val history = remember { mutableStateListOf<CalcHistoryEntry>() }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun currentValue(): BigDecimal = try { BigDecimal(display) } catch (e: Exception) { BigDecimal.ZERO }

    fun inputDigit(digit: String) {
        display = if (startFresh || display == "0") digit else display + digit
        startFresh = false
    }

    fun inputDot() {
        if (startFresh) {
            display = "0."
            startFresh = false
        } else if (!display.contains(".")) {
            display += "."
        }
    }

    fun onBackspace() {
        if (startFresh) return
        display = if (display.length <= 1 || (display.length == 2 && display.startsWith("-"))) "0" else display.dropLast(1)
    }

    fun applyPending() {
        val pv = pendingValue
        val op = pendingOp
        if (pv != null && op != null) {
            val cur = currentValue()
            display = try {
                val result = when (op) {
                    CalcOp.ADD -> pv.add(cur)
                    CalcOp.SUB -> pv.subtract(cur)
                    CalcOp.MUL -> pv.multiply(cur)
                    CalcOp.DIV -> if (cur.signum() == 0) throw ArithmeticException("Division by zero") else pv.divide(cur, MathContext(12))
                }
                result.stripTrailingZeros().toPlainString()
            } catch (e: Exception) {
                "שגיאה"
            }
        }
    }

    fun onOperator(op: CalcOp) {
        applyPending()
        pendingValue = currentValue()
        pendingOp = op
        startFresh = true
        expressionLine = "${pendingValue!!.stripTrailingZeros().toPlainString()} ${op.symbol()}"
    }

    fun onEquals() {
        val pv = pendingValue
        val op = pendingOp
        if (pv != null && op != null) {
            val expression = "${pv.stripTrailingZeros().toPlainString()} ${op.symbol()} ${currentValue().stripTrailingZeros().toPlainString()}"
            applyPending()
            history.add(0, CalcHistoryEntry(expression, display))
        }
        pendingValue = null
        pendingOp = null
        startFresh = true
        expressionLine = ""
    }

    fun onClear() {
        display = "0"
        pendingValue = null
        pendingOp = null
        startFresh = true
        expressionLine = ""
    }

    fun onPercent() {
        display = try {
            currentValue().divide(BigDecimal(100), MathContext(12)).stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            "0"
        }
        startFresh = false
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    // הפוקוס ההתחלתי צריך לנחות על כפתור אמיתי (למשל "C"), לא על ה-Box החיצוני
    // עצמו: Box עם .focusable() משלו "בולע" את הפוקוס לצמיתות ומונע ניווט D-pad
    // אל כפתורי הפעולה (+, -, ×, ÷, נקה) שמתחתיו - זה בדיוק מה שגרם ל"אין פוקוס
    // במחשבון". ה-onKeyEvent עדיין נשאר על ה-Box כדי שהקלדת ספרות/Backspace/=
    // מהמקלדת הפיזית תמשיך לעבוד מכל כפתור שכן ממוקד (bubble-up מהילד לאב).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                digitForKey(event.key)?.let {
                    inputDigit(it)
                    return@onKeyEvent true
                }
                when (event.nativeKeyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_STAR -> { inputDot(); true }
                    android.view.KeyEvent.KEYCODE_POUND -> { onEquals(); true }
                    else -> when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { onEquals(); true }
                        Key.Backspace, Key.Delete -> { onBackspace(); true }
                        Key.Menu, Key.Settings -> { showMenu = true; true }
                        else -> false
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ToolsHeader(
                title = "מחשבון",
                theme = theme,
                onBack = onBack,
                trailing = { ToolsIconButton(Icons.Rounded.MoreVert, "אפשרויות", theme = theme) { showMenu = true } }
            )

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                if (expressionLine.isNotEmpty()) {
                    Text(
                        expressionLine,
                        color = theme.textColor.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val displayFontSize = when {
                    display.length <= 9 -> 44.sp
                    display.length <= 12 -> 32.sp
                    display.length <= 16 -> 24.sp
                    else -> 18.sp
                }
                Text(
                    text = display,
                    color = theme.textColor,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // אין מסך מגע - ספרות מוקלדות ישירות דרך המקלדת הפיזית, אז השורה
            // העליונה כאן היא רק פעולות בלי מקש חומרה מקביל (במקום רשת ספרות
            // שממילא כפולה למקלדת הפיזית ותופסת מקום לשווא).
            val actionRows = listOf(
                listOf(Triple("C", true) { onClear() }, Triple("⌫", true) { onBackspace() }, Triple("%", true) { onPercent() }, Triple("÷", false) { onOperator(CalcOp.DIV) }),
                listOf(Triple("×", false) { onOperator(CalcOp.MUL) }, Triple("−", false) { onOperator(CalcOp.SUB) }, Triple("+", false) { onOperator(CalcOp.ADD) }, Triple("=", false) { onEquals() })
            )

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                actionRows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEachIndexed { colIndex, (label, isMuted, onClick) ->
                            CalcActionButton(
                                label = label,
                                isAccent = !isMuted,
                                isMuted = isMuted,
                                theme = theme,
                                modifier = Modifier.weight(1f),
                                focusRequester = if (rowIndex == 0 && colIndex == 0) focusRequester else null,
                                onClick = onClick
                            )
                        }
                    }
                }
            }

            Text(
                "היסטוריה",
                color = theme.textColor.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("עדיין אין חישובים", color = theme.textColor.copy(alpha = 0.35f), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(history) { entry ->
                        CalcHistoryRow(entry, theme = theme, onClick = {
                            display = entry.result
                            pendingValue = null
                            pendingOp = null
                            startFresh = true
                            expressionLine = ""
                        })
                    }
                }
            }
        }

        if (showMenu) {
            CalculatorOptionsMenu(
                theme = theme,
                onDismiss = { showMenu = false },
                onCopyResult = {
                    showMenu = false
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("result", display))
                },
                onClearHistory = {
                    showMenu = false
                    history.clear()
                }
            )
        }
    }
    }
}

@Composable
private fun CalcActionButton(
    label: String,
    isAccent: Boolean,
    isMuted: Boolean,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val baseColor = when {
        isAccent -> theme.accentColor
        isMuted -> Color(0xFF3A3A3C)
        else -> Color(0xFF2C2C2E)
    }
    val bgColor by animateColorAsState(
        if (isFocused) (if (isAccent) Color(0xFFFFB84D) else Color(0xFF5A5A5C)) else baseColor,
        label = "calcActionBg"
    )
    val textColor = if (isAccent) Color.Black else theme.textColor

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CalcHistoryRow(entry: CalcHistoryEntry, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.14f) else theme.textColor.copy(alpha = 0.05f),
        label = "calcHistoryRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(entry.expression, color = theme.textColor.copy(alpha = 0.5f), fontSize = 13.sp)
        Text(entry.result, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CalculatorOptionsMenu(theme: FutureTheme, onDismiss: () -> Unit, onCopyResult: () -> Unit, onClearHistory: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(vertical = 8.dp)
        ) {
            CalcMenuRow("העתק תוצאה", Icons.Rounded.ContentCopy, theme = theme, onClick = onCopyResult)
            CalcMenuRow("נקה היסטוריה", Icons.Rounded.DeleteSweep, theme = theme, onClick = onClearHistory, isDestructive = true)
        }
    }
}

@Composable
private fun CalcMenuRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, theme: FutureTheme, onClick: () -> Unit, isDestructive: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.12f) else Color.Transparent,
        label = "calcMenuRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isDestructive) Color(0xFFFF6B6B) else theme.accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = if (isDestructive) Color(0xFFFF6B6B) else theme.textColor, fontSize = 15.sp)
    }
}
