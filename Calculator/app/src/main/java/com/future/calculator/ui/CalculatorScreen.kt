package com.future.calculator.ui
import com.future.sharednav.systemui.StatusBarInset
import androidx.activity.compose.BackHandler

import com.future.sharednav.icons.FutureIcons
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
import com.future.calculator.logic.CalcHistoryEntry
import com.future.calculator.logic.CalcOp
import com.future.calculator.logic.CalcState
import com.future.calculator.logic.CalculatorEngine

/** שני "סוגי מחשבון" זמינים - הקיים (רגיל, 4 פעולות) ומדעי חדש (טריגונומטריה/
 * לוגריתמים/חזקות) - נבחר בסרגל פלחים מתחת לכותרת. אותו state machine
 * (display/pendingValue/pendingOp) משרת את שניהם: פונקציות מדעיות חד-איבריות
 * (sin/cos/... ) מוחלות ישירות על הערך המוצג, בדיוק כמו במחשבון מדעי כיס אמיתי. */
private enum class CalculatorMode { STANDARD, SCIENTIFIC }

@Composable
fun CalculatorScreen(theme: FutureTheme, onBack: () -> Unit) {
    // כל הלוגיקה ב-CalculatorEngine (בלי ממשק, עם בדיקות); המסך מחזיק רק את המצב
    var state by remember { mutableStateOf(CalcState()) }
    val display = state.display
    val expressionLine = state.expressionLine
    val pendingOp = state.pendingOp
    val startFresh = state.startFresh
    val historyStore = rememberCalcHistoryStore()
    val history = historyStore.entries
    var showMenu by remember { mutableStateOf(false) }
    // ההיסטוריה כבר לא יושבת מתחת למקשים (הווירפריים לא משאיר לה מקום) - היא
    // מסך נפרד שנפתח מתפריט Options.
    var showHistory by remember { mutableStateOf(false) }
    // חלון הפונקציות המדעיות במצב הרגיל - נפתח ב-# ארוך.
    var showFunctions by remember { mutableStateOf(false) }
    var calcMode by remember { mutableStateOf(CalculatorMode.STANDARD) }
    val context = LocalContext.current
    // במצב רגיל אין אף מקש על המסך שמקבל פוקוס - החצים הם פעולות החשבון
    // עצמן - ולכן השורש הוא שמחזיק את הפוקוס ומקבל את המקשים. במצב מדעי
    // הפוקוס עובר לרשת הפונקציות והחצים זזים בה.
    val rootFocus = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(calcMode, showHistory, showFunctions) {
        if (showHistory || showFunctions) return@LaunchedEffect
        runCatching { if (calcMode == CalculatorMode.STANDARD) rootFocus.requestFocus() else focusRequester.requestFocus() }
    }
    // "*" קצר = C, "*" ארוך = נקודה עשרונית (ל-* ול-# יש כבר תפקיד, והנקודה
    // צריכה מקש פיזי כלשהו).
    var starHeld by remember { mutableStateOf(false) }
    // "#" קצר = אחוז, "#" ארוך (במצב רגיל) = חלון הפונקציות - אותו דפוס כמו "*".
    var poundHeld by remember { mutableStateOf(false) }
    // מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לאפליקציה -
    // התפריט נפתח באמת רק דרך השידור הגלובלי (ר' onOptionsKeyPress).
    com.future.sharednav.nav.onOptionsKeyPress { showMenu = true }

    fun inputDigit(digit: String) { state = CalculatorEngine.inputDigit(state, digit) }
    fun inputDot() { state = CalculatorEngine.inputDot(state) }
    fun onBackspace() { state = CalculatorEngine.backspace(state) }
    fun onOperator(op: CalcOp) { state = CalculatorEngine.operator(state, op) }
    fun onEquals() {
        val (next, entry) = CalculatorEngine.equals(state)
        state = next
        entry?.let(historyStore::add)
    }
    fun onClear() { state = CalculatorEngine.clear() }
    fun onPercent() { state = CalculatorEngine.percent(state) }
    fun applyUnaryFunction(fn: (Double) -> Double) { state = CalculatorEngine.unary(state, fn) }
    fun onFactorial() { state = CalculatorEngine.factorial(state) }
    fun onConstant(value: Double) { state = CalculatorEngine.constant(state, value) }

    /** BACK במחשבון מוחק. רק כשאין מה למחוק הוא יוצא מהאפליקציה. */
    fun onBackKey() {
        val empty = state.isEmpty
        when {
            empty -> onBack()
            startFresh -> onClear()
            else -> onBackspace()
        }
    }
    BackHandler(onBack = ::onBackKey)

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
            .focusRequester(rootFocus)
            .focusable(enabled = calcMode == CalculatorMode.STANDARD)
            .onKeyEvent { event ->
                // שורות מסך ההיסטוריה מעבירות לכאן חצים שלא טופלו - בלי השער
                // הזה "למטה" ברשימה היה הופך לפעולת חיסור.
                if (showHistory || showFunctions) return@onKeyEvent false
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
                if (native.keyCode == android.view.KeyEvent.KEYCODE_POUND) {
                    if (event.type == KeyEventType.KeyDown) {
                        if (native.repeatCount == 0) poundHeld = false
                        else if (!poundHeld) {
                            poundHeld = true
                            if (calcMode == CalculatorMode.STANDARD) showFunctions = true
                        }
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
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> { onEquals(); true }
                            else -> false
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
            ScreenTopBar(
                title = if (calcMode == CalculatorMode.SCIENTIFIC) "מחשבון מדעי" else "מחשבון",
                textColor = theme.textColor,
                accentColor = theme.accentColor,
            )
            // במצב רגיל התצוגה יושבת צמוד מעל לוח ה-D-pad, והרווח הפנוי נשאר למעלה.
            if (calcMode == CalculatorMode.STANDARD) Spacer(Modifier.weight(1f))

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

            // במדעי: C/⌫/% כבר על מקשים פיזיים (* / BACK / #), אז הרשת היא רק
            // הפונקציות, ארבע הפעולות ו-"=" - חמש שורות קומפקטיות במקום שבע,
            // בלי היסטוריה מתחת, כך שהכול נכנס למסך בלי גלילה.
            val scientificOps = listOf(
                listOf(
                    Triple("÷", false) { onOperator(CalcOp.DIV) },
                    Triple("×", false) { onOperator(CalcOp.MUL) },
                    Triple("−", false) { onOperator(CalcOp.SUB) },
                    Triple("+", false) { onOperator(CalcOp.ADD) },
                ),
            )
            if (calcMode == CalculatorMode.STANDARD) {
                CalcKeyLegend(theme)
                Spacer(Modifier.height(CalcPadGap))
                CalcDpad(theme = theme, pendingOp = if (startFresh) pendingOp else null)
                Spacer(Modifier.height(CalcPadBottom))
                return@Column
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingXs),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                (scientificRows + scientificOps).forEachIndexed { rowIndex, row ->
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
                CalcActionButton(
                    label = "=",
                    isAccent = true,
                    isMuted = false,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEquals() },
                )
            }
        }

        if (showFunctions) {
            CalcFunctionsDialog(
                rows = scientificRows,
                theme = theme,
                onDismiss = { showFunctions = false },
            )
        }

        if (showHistory) {
            CalcHistoryScreen(
                history = history,
                theme = theme,
                onRecall = { entry ->
                    state = CalculatorEngine.recall(entry.result)
                    showHistory = false
                },
                onClose = { showHistory = false },
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
                onShowHistory = {
                    showMenu = false
                    showHistory = true
                },
                onCopyResult = {
                    showMenu = false
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("result", display))
                },
                onClearHistory = {
                    showMenu = false
                    historyStore.clear()
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
            .focusable(interactionSource = interactionSource)
            .bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = FutureTypography.screenTitle, fontWeight = FutureTypography.weightMedium)
    }
}

/** 44dp - מקש ברשת המדעית (שש שורות על מסך של 480dp). */
private val CalcKeyHeight = 44.dp

// מידות לוח ה-D-pad מהווירפריים (620×950 פיקסלים ≈ חצי ב-dp על מסך 640×960):
// גלולה 469×324, מרכז "=" 168×150, 42 פיקסלים בין התצוגה ללוח ו-115 מתחתיו.
private val CalcPadWidth = 234.dp
private val CalcPadHeight = 162.dp
private val CalcPadCenterWidth = 84.dp
private val CalcPadCenterHeight = 75.dp
private val CalcPadGap = 16.dp
private val CalcPadBottom = 24.dp
private val CalcPadOpSize = 48.dp

/**
 * לוח המחשבון הרגיל לפי הווירפריים: גלולה אחת בצבע מקש, "=" בעיגול ההדגשה
 * במרכז (OK), וארבע הפעולות סביבו במקום החץ שמפעיל אותן - למעלה חיבור, למטה
 * חיסור, שמאלה כפל, ימינה חילוק. הלוח לא מקבל פוקוס (אין בו מה לנווט): הוא
 * מפה של מקשי ה-D-pad הפיזיים. הפעולה שממתינה לאיבר השני נצבעת בהדגשה.
 * הפריסה כפויה LTR - החצים פיזיים, ו"שמאלה" חייב להופיע משמאל גם במסך RTL.
 */
@Composable
private fun CalcDpad(theme: FutureTheme, pendingOp: CalcOp?) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(CalcPadWidth, CalcPadHeight)
                    .clip(FutureShapes.pill)
                    .background(theme.calcButtonColor)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(CalcPadCenterWidth, CalcPadCenterHeight)
                        .clip(FutureShapes.pill)
                        .background(theme.readableAccentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("=", color = theme.onReadableAccentColor, fontSize = FutureTypography.display, fontWeight = FutureTypography.weightMedium)
                }
                CalcPadOp(CalcOp.ADD, pendingOp, theme, Modifier.align(Alignment.TopCenter).padding(top = FutureDimens.spacingXs))
                CalcPadOp(CalcOp.SUB, pendingOp, theme, Modifier.align(Alignment.BottomCenter).padding(bottom = FutureDimens.spacingXs))
                CalcPadOp(CalcOp.MUL, pendingOp, theme, Modifier.align(Alignment.CenterStart).padding(start = FutureDimens.spacingMd))
                CalcPadOp(CalcOp.DIV, pendingOp, theme, Modifier.align(Alignment.CenterEnd).padding(end = FutureDimens.spacingMd))
            }
        }
    }
}

@Composable
private fun CalcPadOp(op: CalcOp, pendingOp: CalcOp?, theme: FutureTheme, modifier: Modifier) {
    val color by animateColorAsState(
        if (op == pendingOp) theme.readableAccentColor else theme.textColor,
        FutureMotion.focusColorSpec,
        label = "calcPadOp"
    )
    Box(modifier = modifier.size(CalcPadOpSize), contentAlignment = Alignment.Center) {
        Text(op.symbol, color = color, fontSize = FutureTypography.display, fontWeight = FutureTypography.weightLight)
    }
}

/**
 * שורת הפונקציות שאין להן מקום בלוח: כל תא מראה פונקציה ואת המקש הפיזי
 * שמפעיל אותה. זו מפה ולא מקשים - אין בה פוקוס, כמו הלוח שמתחתיה.
 */
@Composable
private fun CalcKeyLegend(theme: FutureTheme) {
    val items = listOf(
        "C" to "*",
        "." to "* ארוך",
        "%" to "#",
        "fx" to "# ארוך",
        "⌫" to "חזור",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
    ) {
        items.forEach { (function, key) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(FutureShapes.sm)
                    .background(theme.calcMutedButtonColor)
                    .padding(vertical = FutureDimens.spacingXs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(function, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FutureTypography.weightMedium, maxLines = 1)
                Text(key, color = theme.subtleTextColor, fontSize = FutureTypography.caption, maxLines = 1)
            }
        }
    }
}

/**
 * חלון הפונקציות המדעיות של המצב הרגיל (# ארוך): אותה רשת כמו במצב המדעי,
 * החצים זזים בה ו-OK מפעיל את הפונקציה על התצוגה וסוגר. BACK סוגר בלי כלום.
 */
@Composable
private fun CalcFunctionsDialog(
    rows: List<List<Triple<String, Boolean, () -> Unit>>>,
    theme: FutureTheme,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    com.future.sharednav.components.AppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FutureShapes.dialog)
                .background(theme.surfaceColor)
                .padding(FutureDimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            Text("פונקציות", color = theme.textAlpha(50), fontSize = FutureTypography.label)
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    row.forEachIndexed { colIndex, (label, _, action) ->
                        CalcActionButton(
                            label = label,
                            isAccent = false,
                            isMuted = true,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            focusRequester = if (rowIndex == 0 && colIndex == 0) first else null,
                            onClick = {
                                onDismiss()
                                action()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** מסך ההיסטוריה (מתפריט Options): OK על שורה מחזיר את התוצאה לתצוגה, BACK סוגר. */
@Composable
private fun CalcHistoryScreen(
    history: List<CalcHistoryEntry>,
    theme: FutureTheme,
    onRecall: (CalcHistoryEntry) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(top = StatusBarInset.TITLE_GAP_DP.dp)
            .then(if (history.isEmpty()) Modifier.focusRequester(firstRow).focusable() else Modifier)
    ) {
        ScreenTopBar(title = "היסטוריה", textColor = theme.textColor, accentColor = theme.accentColor)
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
                itemsIndexed(history, key = { index, _ -> index }) { index, entry ->
                    CalcHistoryRow(
                        entry,
                        theme = theme,
                        modifier = if (index == 0) Modifier.focusRequester(firstRow) else Modifier,
                        onClick = { onRecall(entry) },
                    )
                }
            }
        }
    }
}

/** שורת היסטוריה - שורת רשימה רגילה (FocusableItem): 14% הדגשה ומסגרת 1.5dp בפוקוס. */
@Composable
private fun CalcHistoryRow(entry: CalcHistoryEntry, theme: FutureTheme, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        idleBackgroundColor = theme.idleChipColor,
        modifier = modifier.fillMaxWidth(),
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
    onShowHistory: () -> Unit,
    onCopyResult: () -> Unit,
    onClearHistory: () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = "מחשבון") {
        FutureMenuRow(if (scientific) "מחשבון רגיל" else "מחשבון מדעי", FutureIcons.Functions, theme, onToggleMode)
        FutureMenuRow("היסטוריה", FutureIcons.History, theme, onShowHistory)
        FutureMenuRow("העתק תוצאה", FutureIcons.ContentCopy, theme, onCopyResult)
        FutureMenuRow("נקה היסטוריה", FutureIcons.Delete, theme, onClearHistory, destructive = true)
    }
}

/**
 * ההיסטוריה נשמרת בהעדפות (50 האחרונות) - קודם היא נעלמה בכל יציאה מהמחשבון.
 * שורה לכל חישוב: ביטוי ותוצאה מופרדים בתו שלא מופיע באף אחד מהם.
 */
private class CalcHistoryStore(private val prefs: android.content.SharedPreferences) {
    val entries = androidx.compose.runtime.mutableStateListOf<CalcHistoryEntry>().apply {
        prefs.getString(KEY, "").orEmpty().split('\n').filter { it.isNotEmpty() }.mapNotNullTo(this) { line ->
            val parts = line.split(SEP)
            if (parts.size == 2) CalcHistoryEntry(parts[0], parts[1]) else null
        }
    }

    fun add(entry: CalcHistoryEntry) {
        entries.add(0, entry)
        while (entries.size > MAX) entries.removeAt(entries.lastIndex)
        save()
    }

    fun clear() {
        entries.clear()
        save()
    }

    private fun save() {
        prefs.edit().putString(KEY, entries.joinToString("\n") { it.expression + SEP + it.result }).apply()
    }

    companion object {
        private const val KEY = "history"
        private const val SEP = "\u001F"
        private const val MAX = 50
    }
}

@Composable
private fun rememberCalcHistoryStore(): CalcHistoryStore {
    val context = LocalContext.current
    return remember { CalcHistoryStore(context.getSharedPreferences("calculator", android.content.Context.MODE_PRIVATE)) }
}
