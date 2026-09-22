package com.future.calculator.ui
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.FutureTabRow
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.calcButtonColor
import com.future.sharednav.theme.calcButtonFocusedColor
import com.future.sharednav.theme.calcMutedButtonColor
import java.math.BigDecimal
import java.math.MathContext

private enum class CalcOp { ADD, SUB, MUL, DIV, POW }

private fun CalcOp.symbol(): String = when (this) {
    CalcOp.ADD -> "+"
    CalcOp.SUB -> "−"
    CalcOp.MUL -> "×"
    CalcOp.DIV -> "÷"
    CalcOp.POW -> "^"
}

/** שני "סוגי מחשבון" זמינים - הקיים (רגיל, 4 פעולות) ומדעי חדש (טריגונומטריה/
 * לוגריתמים/חזקות) - נבחר בסרגל פלחים מתחת לכותרת. אותו state machine
 * (display/pendingValue/pendingOp) משרת את שניהם: פונקציות מדעיות חד-איבריות
 * (sin/cos/... ) מוחלות ישירות על הערך המוצג, בדיוק כמו במחשבון מדעי כיס אמיתי. */
private enum class CalculatorMode { STANDARD, SCIENTIFIC }

private data class CalcHistoryEntry(val expression: String, val result: String)

/** תקרת ספרות קלט גולמי, כדי שהתוצאה תמיד תיכנס למסך הקבוע (ר' inputDigit). */
private const val MAX_INPUT_DIGITS = 15

/** דיוק אחיד לכל הפעולות (לא רק חילוק) - כך תוצאת כפל/חיבור/חיסור של שני
 * מספרים ארוכים לא תתפוצץ למחרוזת בת עשרות תווים שגם גודל הגופן המינימלי
 * לא יכיל בלי חיתוך שקט על המסך הקבוע של 640px. */
private val CALC_PRECISION = MathContext(12)

@Composable
fun CalculatorScreen(theme: FutureTheme, onBack: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var expressionLine by remember { mutableStateOf("") }
    var pendingValue by remember { mutableStateOf<BigDecimal?>(null) }
    var pendingOp by remember { mutableStateOf<CalcOp?>(null) }
    var startFresh by remember { mutableStateOf(true) }
    val history = remember { mutableStateListOf<CalcHistoryEntry>() }
    var showMenu by remember { mutableStateOf(false) }
    var calcMode by remember { mutableStateOf(CalculatorMode.STANDARD) }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לאפליקציה -
    // התפריט נפתח באמת רק דרך השידור הגלובלי (ר' onOptionsKeyPress).
    com.future.sharednav.nav.onOptionsKeyPress { showMenu = true }

    fun currentValue(): BigDecimal = try { BigDecimal(display) } catch (e: Exception) { BigDecimal.ZERO }

    fun inputDigit(digit: String) {
        // מגבלת אורך קלט: בלי תקרה, המשתמש יכול להקליד ספרות ללא סוף וליצור
        // תוצאה שאף גודל גופן לא יציג במסך הקבוע של 640px בלי חיתוך שקט
        // (בדיוק הבאג הישן מ-Tools/CalculatorScreen.kt). מחשבונים פיזיים
        // תמיד חוסמים קלט מעבר למספר ספרות נתון - כך גם כאן.
        val digitsOnly = display.count { it.isDigit() }
        if (!startFresh && display != "0" && digitsOnly >= MAX_INPUT_DIGITS) return
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
                    CalcOp.ADD -> pv.add(cur, CALC_PRECISION)
                    CalcOp.SUB -> pv.subtract(cur, CALC_PRECISION)
                    CalcOp.MUL -> pv.multiply(cur, CALC_PRECISION)
                    CalcOp.DIV -> if (cur.signum() == 0) throw ArithmeticException("Division by zero") else pv.divide(cur, CALC_PRECISION)
                    // חזקה לא-שלמה (חיובית/שברית) היא מחוץ ליכולות BigDecimal הבסיסיות -
                    // ממירים ל-Double כמו בכל פונקציה מדעית אחרת כאן (ר' applyUnaryFunction).
                    CalcOp.POW -> BigDecimal(Math.pow(pv.toDouble(), cur.toDouble()), CALC_PRECISION)
                }
                result.stripTrailingZeros().toPlainString()
            } catch (e: Exception) {
                "שגיאה"
            }
        }
    }

    fun onOperator(op: CalcOp) {
        // אם כבר יש פעולה ממתינה והמשתמש לא הקליד ספרה חדשה מאז (למשל לחץ
        // "+" ואז מיד "×" בלי להקליד מספר), רק מחליפים את הפעולה הממתינה
        // ולא מפעילים אותה מחדש על אותו ערך - אחרת "5 +" ואז "+" שוב היה
        // מחשב 5+5=10 מוקדם מדי, בניגוד להתנהגות מחשבון רגילה.
        if (startFresh && pendingOp != null && pendingValue != null) {
            pendingOp = op
            expressionLine = "${pendingValue!!.stripTrailingZeros().toPlainString()} ${op.symbol()}"
            return
        }
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

    // פונקציות מדעיות חד-איבריות (sin/cos/.../√ וכו') מוחלות מיד על הערך המוצג,
    // בדיוק כמו לחיצה על אותו כפתור במחשבון מדעי כיס אמיתי - לא צריך = כדי
    // לראות תוצאה. startFresh=true אחרי כל אחת כי התוצאה היא "מספר סגור" חדש,
    // בדיוק כמו אחרי =, לא המשך הקלדה של הספרות הקודמות.
    fun applyUnaryFunction(fn: (Double) -> Double) {
        display = try {
            val result = fn(currentValue().toDouble())
            if (result.isNaN() || result.isInfinite()) "שגיאה"
            else BigDecimal(result, CALC_PRECISION).stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            "שגיאה"
        }
        startFresh = true
    }

    fun onFactorial() {
        display = try {
            val n = currentValue().toDouble()
            if (n < 0 || n != Math.floor(n) || n > 170) {
                "שגיאה"
            } else {
                var result = BigDecimal.ONE
                for (i in 2..n.toInt()) result = result.multiply(BigDecimal(i))
                result.toPlainString()
            }
        } catch (e: Exception) {
            "שגיאה"
        }
        startFresh = true
    }

    fun onConstant(value: Double) {
        display = BigDecimal(value, CALC_PRECISION).stripTrailingZeros().toPlainString()
        startFresh = true
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
                        else -> false
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = "מחשבון",
                textColor = theme.textColor,
                accentColor = theme.accentColor,
                onBack = onBack,
                trailingIcon = Icons.Rounded.MoreVert,
                trailingContentDescription = "אפשרויות",
                onTrailingClick = { showMenu = true },
            )

            FutureTabRow(
                items = listOf("רגיל", "מדעי"),
                selectedIndex = calcMode.ordinal,
                theme = theme,
                onSelect = { calcMode = CalculatorMode.entries[it] },
            )

            // הביטוי והתוצאה חייבים להישאר קריאים משמאל-לימין (ספרות + סימני
            // פעולה) גם כשכל שאר המסך ב-RTL: בלי לכפות כאן LTR מקומי, מחרוזת
            // בלי תו-חוזק חזק (כמו "5 ×") הייתה מקבלת את כיוון הפריסה הסביבתי
            // (RTL) ומוצגת הפוך ("× 5") - הבאג הקלאסי של מחשבון ב-RTL.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm)) {
                    if (expressionLine.isNotEmpty()) {
                        Text(
                            expressionLine,
                            color = theme.textAlpha(50),
                            fontSize = FutureTypography.bodyLarge,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // מדרגות נוספות מעבר ל-16 תווים כרשת ביטחון: גם עם התקרה על
                    // אורך הקלט הגולמי ודיוק אחיד לכל הפעולות (MAX_INPUT_DIGITS /
                    // CALC_PRECISION למעלה), עדיף גופן שממשיך להצטמצם על פני חיתוך
                    // שקט של התוצאה בקצה המסך הקבוע (הבאג הישן מ-Tools).
                    val displayFontSize = when {
                        display.length <= 9 -> FutureTypography.hero
                        display.length <= 12 -> FutureTypography.display
                        display.length <= 16 -> FutureTypography.headline
                        display.length <= 20 -> FutureTypography.title
                        else -> FutureTypography.body
                    }
                    Text(
                        text = display,
                        color = theme.textColor,
                        fontSize = displayFontSize,
                        fontWeight = FutureTypography.weightLight,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // אין מסך מגע - ספרות מוקלדות ישירות דרך המקלדת הפיזית, אז השורה
            // העליונה כאן היא רק פעולות בלי מקש חומרה מקביל (במקום רשת ספרות
            // שממילא כפולה למקלדת הפיזית ותופסת מקום לשווא).
            val scientificRows = listOf(
                listOf(
                    Triple("sin", true) { applyUnaryFunction { x -> Math.sin(Math.toRadians(x)) } },
                    Triple("cos", true) { applyUnaryFunction { x -> Math.cos(Math.toRadians(x)) } },
                    Triple("tan", true) { applyUnaryFunction { x -> Math.tan(Math.toRadians(x)) } },
                    Triple("xʸ", true) { onOperator(CalcOp.POW) },
                ),
                listOf(
                    Triple("log", true) { applyUnaryFunction { x -> Math.log10(x) } },
                    Triple("ln", true) { applyUnaryFunction { x -> Math.log(x) } },
                    Triple("√", true) { applyUnaryFunction { x -> Math.sqrt(x) } },
                    Triple("x²", true) { applyUnaryFunction { x -> x * x } },
                ),
                listOf(
                    Triple("1/x", true) { applyUnaryFunction { x -> 1.0 / x } },
                    Triple("x!", true) { onFactorial() },
                    Triple("π", true) { onConstant(Math.PI) },
                    Triple("e", true) { onConstant(Math.E) },
                ),
            )
            val standardRows = listOf(
                listOf(Triple("C", true) { onClear() }, Triple("⌫", true) { onBackspace() }, Triple("%", true) { onPercent() }, Triple("÷", false) { onOperator(CalcOp.DIV) }),
                listOf(Triple("×", false) { onOperator(CalcOp.MUL) }, Triple("−", false) { onOperator(CalcOp.SUB) }, Triple("+", false) { onOperator(CalcOp.ADD) }, Triple("=", false) { onEquals() })
            )
            val actionRows = if (calcMode == CalculatorMode.SCIENTIFIC) scientificRows + standardRows else standardRows

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingXs),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                actionRows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)
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

            FutureSectionHeader("היסטוריה", theme)
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("אין חישובים", color = theme.subtleTextColor, fontSize = FutureTypography.body)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs)
                ) {
                    itemsIndexed(history, key = { index, _ -> index }) { _, entry ->
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

/**
 * מקש במחשבון. הצבעים הם של הדיזיין סיסטם (guidelines/colors-app.html):
 * מקש רגיל / מקש משני / מקש ממוקד. מקש פעולה נצבע בהדגשה *המתוקנת* - קודם
 * הוא נצבע בהדגשה הגולמית, ובפוקוס בכתום קבוע (#FFB84D) שהניח שההדגשה
 * כתומה. עכשיו מקש פעולה ממוקד מסומן כמו כל פקד: מסגרת 2dp בצבע הטקסט.
 * רדיוס 8dp - הדרגה של מקשים ופריטי רשת.
 */
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
    val accent = theme.readableAccentColor
    val baseColor = when {
        isAccent -> accent
        isMuted -> theme.calcMutedButtonColor
        else -> theme.calcButtonColor
    }
    val bgColor by animateColorAsState(
        if (isFocused && !isAccent) theme.calcButtonFocusedColor else baseColor,
        FutureMotion.focusColorSpec,
        label = "calcActionBg"
    )
    val ring by animateColorAsState(
        if (isFocused && isAccent) theme.textColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "calcActionRing"
    )
    val textColor = if (isAccent) theme.onReadableAccentColor else theme.textColor

    Box(
        modifier = modifier
            .height(CalcKeyHeight)
            .clip(FutureShapes.sm)
            .background(bgColor)
            .border(FutureDimens.focusBorderControl, ring, FutureShapes.sm)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = FutureTypography.screenTitle, fontWeight = FutureTypography.weightMedium)
    }
}

/** 52dp - גובה מקש במחשבון. */
private val CalcKeyHeight = 52.dp

/** שורת היסטוריה - שורת רשימה רגילה (FocusableItem): 14% הדגשה ומסגרת 1.5dp בפוקוס. */
@Composable
private fun CalcHistoryRow(entry: CalcHistoryEntry, theme: FutureTheme, onClick: () -> Unit) {
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        idleBackgroundColor = theme.idleChipColor,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 0.dp,
    ) {
        // הביטוי והתוצאה נקראים משמאל לימין גם בתוך מסך RTL.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(entry.expression, color = theme.textAlpha(50), fontSize = FutureTypography.summary)
                Text(entry.result, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FutureTypography.weightMedium)
            }
        }
    }
}

@Composable
private fun CalculatorOptionsMenu(theme: FutureTheme, onDismiss: () -> Unit, onCopyResult: () -> Unit, onClearHistory: () -> Unit) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = "מחשבון") {
        FutureMenuRow("העתק תוצאה", Icons.Rounded.ContentCopy, theme, onCopyResult)
        FutureMenuRow("נקה היסטוריה", Icons.Rounded.DeleteSweep, theme, onClearHistory, destructive = true)
    }
}
