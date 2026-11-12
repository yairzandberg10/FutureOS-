package com.future.calculator.ui
import com.future.sharednav.i18n.tr
import com.future.sharednav.systemui.StatusBarInset
import androidx.activity.compose.BackHandler

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.components.FutureTabRow
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureActionCell
import com.future.sharednav.components.FutureBottomNav
import com.future.sharednav.components.FutureNavItem
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

private enum class CalcOp { ADD, SUB, MUL, DIV, POW, ROOT, MOD }

private fun CalcOp.symbol(): String = when (this) {
    CalcOp.ADD -> "+"
    CalcOp.SUB -> "−"
    CalcOp.MUL -> "×"
    CalcOp.DIV -> "÷"
    CalcOp.POW -> "^"
    CalcOp.ROOT -> "ʸ√"
    CalcOp.MOD -> "mod"
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

/**
 * תצוגת מספר: רגילה כשהיא קצרה, וכתיב מדעי (1.23456789012E+30) כשהיא ארוכה מ-16
 * תווים. קודם toPlainString הדפיס כל ספרה - 170! היה 307 ספרות, ו-10^-20 היה
 * עשרים אפסים, הרבה מעבר למה שנכנס במסך גם בגופן הקטן ביותר.
 */
private fun BigDecimal.toDisplay(): String {
    val stripped = stripTrailingZeros()
    val plain = stripped.toPlainString()
    if (plain.length <= 16) return plain
    return stripped.round(CALC_PRECISION).stripTrailingZeros().toString()
}

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
    // מעלות או רדיאנים לפונקציות הטריגונומטריות במדעי.
    var degrees by remember { mutableStateOf(true) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    // במצב רגיל אין אף מקש על המסך שמקבל פוקוס - החצים הם פעולות החשבון
    // עצמן - ולכן השורש הוא שמחזיק את הפוקוס ומקבל את המקשים. במצב מדעי
    // הפוקוס עובר לרשת הפונקציות והחצים זזים בה.
    val rootFocus = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(calcMode) {
        runCatching { if (calcMode == CalculatorMode.STANDARD) rootFocus.requestFocus() else focusRequester.requestFocus() }
    }
    // "*" קצר = C, "*" ארוך = נקודה עשרונית (ל-* ול-# יש כבר תפקיד, והנקודה
    // צריכה מקש פיזי כלשהו).
    var starHeld by remember { mutableStateOf(false) }
    // "#" קצר = אחוז, "#" ארוך = מעבר בין רגיל למדעי (הסרגל התחתון).
    var poundHeld by remember { mutableStateOf(false) }
    fun toggleMode() {
        calcMode = if (calcMode == CalculatorMode.SCIENTIFIC) CalculatorMode.STANDARD else CalculatorMode.SCIENTIFIC
    }
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
                    // שורש y של x: x^(1/y). שורש אי-זוגי של שלילי מוגדר (∛-8 = -2).
                    CalcOp.ROOT -> {
                        val x = pv.toDouble(); val y = cur.toDouble()
                        if (y == 0.0) throw ArithmeticException("root 0")
                        val r = if (x < 0 && y % 2.0 == 1.0) -Math.pow(-x, 1.0 / y) else Math.pow(x, 1.0 / y)
                        if (r.isNaN()) throw ArithmeticException("NaN")
                        BigDecimal(r, CALC_PRECISION)
                    }
                    CalcOp.MOD -> if (cur.signum() == 0) throw ArithmeticException("mod 0") else pv.remainder(cur, CALC_PRECISION)
                }
                result.toDisplay()
            } catch (e: Exception) {
                tr("שגיאה")
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
            expressionLine = "${pendingValue!!.toDisplay()} ${op.symbol()}"
            return
        }
        applyPending()
        pendingValue = currentValue()
        pendingOp = op
        startFresh = true
        expressionLine = "${pendingValue!!.toDisplay()} ${op.symbol()}"
    }

    fun onEquals() {
        val pv = pendingValue
        val op = pendingOp
        if (pv != null && op != null) {
            val expression = "${pv.toDisplay()} ${op.symbol()} ${currentValue().toDisplay()}"
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
            currentValue().divide(BigDecimal(100), MathContext(12)).toDisplay()
        } catch (e: Exception) {
            "0"
        }
        startFresh = true
    }

    // פונקציות מדעיות חד-איבריות (sin/cos/.../√ וכו') מוחלות מיד על הערך המוצג,
    // בדיוק כמו לחיצה על אותו כפתור במחשבון מדעי כיס אמיתי - לא צריך = כדי
    // לראות תוצאה. startFresh=true אחרי כל אחת כי התוצאה היא "מספר סגור" חדש,
    // בדיוק כמו אחרי =, לא המשך הקלדה של הספרות הקודמות.
    fun applyUnaryFunction(fn: (Double) -> Double) {
        display = try {
            val result = fn(currentValue().toDouble())
            if (result.isNaN() || result.isInfinite()) tr("שגיאה")
            else BigDecimal(result, CALC_PRECISION).toDisplay()
        } catch (e: Exception) {
            tr("שגיאה")
        }
        startFresh = true
    }

    fun onFactorial() {
        display = try {
            val n = currentValue().toDouble()
            if (n < 0 || n != Math.floor(n) || n > 170) {
                tr("שגיאה")
            } else {
                var result = BigDecimal.ONE
                for (i in 2..n.toInt()) result = result.multiply(BigDecimal(i))
                result.toDisplay()
            }
        } catch (e: Exception) {
            tr("שגיאה")
        }
        startFresh = true
    }

    fun onNegate() {
        display = when {
            display == "0" || display == "שגיאה" -> display
            display.startsWith("-") -> display.drop(1)
            else -> "-$display"
        }
    }

    fun toRad(x: Double) = if (degrees) Math.toRadians(x) else x
    fun fromRad(x: Double) = if (degrees) Math.toDegrees(x) else x

    fun onConstant(value: Double) {
        display = BigDecimal(value, CALC_PRECISION).toDisplay()
        startFresh = true
    }

    /** BACK במחשבון מוחק. רק כשאין מה למחוק הוא יוצא מהאפליקציה. */
    fun onBackKey() {
        val empty = display == "0" && pendingOp == null && expressionLine.isEmpty()
        when {
            empty -> onBack()
            startFresh -> onClear()
            else -> onBackspace()
        }
    }
    BackHandler(onBack = ::onBackKey)

    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
    // הפוקוס ההתחלתי צריך לנחות על כפתור אמיתי (למשל "C"), לא על ה-Box החיצוני
    // עצמו: Box עם .focusable() משלו "בולע" את הפוקוס לצמיתות ומונע ניווט D-pad
    // אל כפתורי הפעולה (+, -, ×, ÷, נקה) שמתחתיו - זה בדיוק מה שגרם ל"אין פוקוס
    // במחשבון". ה-onKeyEvent עדיין נשאר על ה-Box כדי שהקלדת ספרות/Backspace/=
    // מהמקלדת הפיזית תמשיך לעבוד מכל כפתור שכן ממוקד (bubble-up מהילד לאב).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .focusRequester(rootFocus)
            .focusable(enabled = calcMode == CalculatorMode.STANDARD)
            .onKeyEvent { event ->
                val native = event.nativeKeyEvent
                // "*": קצר = C (בשחרור), מוחזק = נקודה (בחזרה הראשונה של המקש).
                if (native.keyCode == android.view.KeyEvent.KEYCODE_STAR) {
                    if (event.type == KeyEventType.KeyDown) {
                        if (native.repeatCount == 0) starHeld = false
                        else if (!starHeld) { starHeld = true; inputDot() }
                    } else if (event.type == KeyEventType.KeyUp && !starHeld) {
                        onClear()
                    }
                    return@onKeyEvent true
                }
                // "#": קצר = אחוז (בשחרור), מוחזק = מעבר רגיל/מדעי.
                if (native.keyCode == android.view.KeyEvent.KEYCODE_POUND) {
                    if (event.type == KeyEventType.KeyDown) {
                        if (native.repeatCount == 0) poundHeld = false
                        else if (!poundHeld) { poundHeld = true; toggleMode() }
                    } else if (event.type == KeyEventType.KeyUp && !poundHeld) {
                        onPercent()
                    }
                    return@onKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                digitForKey(event.key)?.let {
                    inputDigit(it)
                    return@onKeyEvent true
                }
                when (event.key) {
                    Key.Backspace, Key.Delete -> { onBackspace(); true }
                    else -> if (calcMode == CalculatorMode.STANDARD) {
                        // במצב רגיל החצים הם הפעולות, ו-OK הוא "=".
                        when (event.key) {
                            Key.DirectionUp -> { onOperator(CalcOp.ADD); true }
                            Key.DirectionDown -> { onOperator(CalcOp.SUB); true }
                            Key.DirectionLeft -> { onOperator(CalcOp.MUL); true }
                            Key.DirectionRight -> { onOperator(CalcOp.DIV); true }
                            Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { onEquals(); true }
                            else -> false
                        }
                    } else {
                        // במצב מדעי OK מפעיל את המקש הממוקד (הוא מגיע לכאן רק אם אף
                        // מקש לא טיפל בו), ו-Enter של מקלדת חיצונית הוא "=".
                        // ימינה בקצה הרשת חוזר ללשונית "רגיל" (בסרגל התחתון, RTL).
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> { onEquals(); true }
                            Key.DirectionRight -> {
                                if (!focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right)) toggleMode()
                                true
                            }
                            else -> false
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
            ScreenTopBar(
                title = if (calcMode == CalculatorMode.SCIENTIFIC) tr("מחשבון מדעי") else tr("מחשבון"),
                textColor = theme.textColor,
                accentColor = theme.accentColor,
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

            // אין מסך מגע - ספרות מוקלדות ישירות דרך המקלדת הפיזית. במצב רגיל
            // הפעולות הן החצים/OK/*/#, ולכן במקום "כפתורים" שאי אפשר ללחוץ עליהם
            // מוצגת מפת מקשים: תאי ActionGrid של הדיזיין סיסטם, סימן הפעולה
            // ומתחתיו המקש הפיזי שמפעיל אותה. הפעולה הממתינה דלוקה (20% הדגשה).
            // הסדר בשורה מימין לשמאל תואם את מיקום החץ: ÷ מימין, × משמאל.
            val scientific = calcMode == CalculatorMode.SCIENTIFIC
            val pendingSymbol = if (pendingOp != null && startFresh) pendingOp?.symbol() else null
            if (!scientific) {
                val legend = listOf(
                    listOf("÷" to "→", "+" to "↑", "−" to "↓", "×" to "←"),
                    listOf("=" to "OK", "C" to "*", "%" to "#", "⌫" to tr("חזור")),
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    legend.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                            row.forEach { (glyph, key) ->
                                FutureActionCell(
                                    glyph = glyph,
                                    label = key,
                                    theme = theme,
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                    active = glyph == pendingSymbol,
                                    height = CalcLegendHeight,
                                    focusable = false,
                                )
                            }
                        }
                    }
                }
            } else {
                fun trig(f: (Double) -> Double): () -> Unit = { applyUnaryFunction { x -> f(toRad(x)) } }
                fun arc(f: (Double) -> Double): () -> Unit = { applyUnaryFunction { x -> fromRad(f(x)) } }
                val angleLabel = if (degrees) "DEG" else "RAD"
                val rows: List<List<Pair<String, () -> Unit>>> = listOf(
                    listOf("sin" to trig(Math::sin), "cos" to trig(Math::cos), "tan" to trig(Math::tan), "xʸ" to { onOperator(CalcOp.POW) }),
                    listOf("sin⁻¹" to arc(Math::asin), "cos⁻¹" to arc(Math::acos), "tan⁻¹" to arc(Math::atan), "ʸ√x" to { onOperator(CalcOp.ROOT) }),
                    listOf("log" to { applyUnaryFunction(Math::log10) }, "ln" to { applyUnaryFunction(Math::log) }, "10ˣ" to { applyUnaryFunction { x -> Math.pow(10.0, x) } }, "eˣ" to { applyUnaryFunction(Math::exp) }),
                    listOf("√" to { applyUnaryFunction(Math::sqrt) }, "∛" to { applyUnaryFunction(Math::cbrt) }, "x²" to { applyUnaryFunction { x -> x * x } }, "x³" to { applyUnaryFunction { x -> x * x * x } }),
                    listOf("1/x" to { applyUnaryFunction { x -> 1.0 / x } }, "x!" to { onFactorial() }, "|x|" to { applyUnaryFunction(Math::abs) }, "±" to { onNegate() }),
                    listOf("π" to { onConstant(Math.PI) }, "e" to { onConstant(Math.E) }, "mod" to { onOperator(CalcOp.MOD) }, angleLabel to { degrees = !degrees }),
                    listOf("÷" to { onOperator(CalcOp.DIV) }, "×" to { onOperator(CalcOp.MUL) }, "−" to { onOperator(CalcOp.SUB) }, "+" to { onOperator(CalcOp.ADD) }),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    rows.forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                            row.forEachIndexed { colIndex, (glyph, action) ->
                                val isPending = glyph == pendingSymbol ||
                                    (glyph == "ʸ√x" && pendingSymbol == "ʸ√") ||
                                    (glyph == "xʸ" && pendingSymbol == "^")
                                FutureActionCell(
                                    glyph = glyph,
                                    label = null,
                                    theme = theme,
                                    onClick = action,
                                    modifier = Modifier.weight(1f),
                                    active = isPending,
                                    height = CalcKeyHeightCompact,
                                    focusRequester = if (rowIndex == 0 && colIndex == 0) focusRequester else null,
                                )
                            }
                        }
                    }
                    FutureActionCell(
                        glyph = "=",
                        label = null,
                        theme = theme,
                        onClick = { onEquals() },
                        modifier = Modifier.fillMaxWidth(),
                        active = true,
                        height = CalcKeyHeightCompact,
                    )
                }
            }

            if (!scientific) {
                FutureSectionHeader(tr("היסטוריה"), theme)
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(tr("אין חישובים"), color = theme.subtleTextColor, fontSize = FutureTypography.body)
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

            // סרגל תחתון בין רגיל למדעי. הוא לא מקבל פוקוס (כמו בכל המערכת):
            // מחליפים לשונית בלחיצה ארוכה על #, או ימינה מקצה הרשת המדעית.
            FutureBottomNav(
                items = listOf(
                    FutureNavItem(tr("רגיל"), FutureIcons.Calculate),
                    FutureNavItem(tr("מדעי"), FutureIcons.Functions),
                ),
                selectedIndex = if (scientific) 1 else 0,
                theme = theme,
            )
        }

        if (showMenu) {
            CalculatorOptionsMenu(
                theme = theme,
                scientific = calcMode == CalculatorMode.SCIENTIFIC,
                onToggleMode = {
                    showMenu = false
                    calcMode = if (calcMode == CalculatorMode.SCIENTIFIC) CalculatorMode.STANDARD else CalculatorMode.SCIENTIFIC
                },
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

/** 56dp - תא במפת המקשים של המצב הרגיל (סימן + שם המקש). */
private val CalcLegendHeight = 56.dp

/** 44dp - מקש ברשת המדעית (שמונה שורות, נגללות לפי הפוקוס). */
private val CalcKeyHeightCompact = 44.dp

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
private fun CalculatorOptionsMenu(
    theme: FutureTheme,
    scientific: Boolean,
    onToggleMode: () -> Unit,
    onDismiss: () -> Unit,
    onCopyResult: () -> Unit,
    onClearHistory: () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = tr("מחשבון")) {
        FutureMenuRow(if (scientific) tr("מחשבון רגיל") else tr("מחשבון מדעי"), FutureIcons.Functions, theme, onToggleMode)
        FutureMenuRow(tr("העתק תוצאה"), FutureIcons.ContentCopy, theme, onCopyResult)
        FutureMenuRow(tr("נקה היסטוריה"), FutureIcons.Delete, theme, onClearHistory, destructive = true)
    }
}
