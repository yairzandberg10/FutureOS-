package com.future.calculator.ui
import com.future.sharednav.systemui.StatusBarInset
import androidx.activity.compose.BackHandler

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.ScreenTopBar
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

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.rememberFutureSnackbarState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.calcButtonColor
import com.future.calculator.R
import com.future.calculator.logic.CalcHistoryEntry
import com.future.calculator.logic.CalcHistoryFormat
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
    var state by rememberSaveable(stateSaver = CalcStateSaver) { mutableStateOf(CalcState()) }
    val display = state.display
    val expressionLine = state.expressionLine
    val pendingOp = state.pendingOp
    val startFresh = state.startFresh
    val historyStore = rememberCalcHistoryStore()
    val history = historyStore.entries
    val snackbar = rememberFutureSnackbarState()
    var showMenu by rememberSaveable { mutableStateOf(false) }
    // ההיסטוריה כבר לא יושבת מתחת למקשים (הווירפריים לא משאיר לה מקום) - היא
    // מסך נפרד שנפתח מתפריט Options.
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var calcMode by rememberSaveable { mutableStateOf(CalculatorMode.STANDARD) }
    // נבחר לפני מחיקה של כל ההיסטוריה - מחיקה בלי אישור לא הפיכה.
    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
    fun toggleMode() {
        calcMode = if (calcMode == CalculatorMode.SCIENTIFIC) CalculatorMode.STANDARD else CalculatorMode.SCIENTIFIC
    }
    // מעלות או רדיאנים לפונקציות הטריגונומטריות במדעי.
    var degrees by rememberSaveable { mutableStateOf(true) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    // במצב רגיל החצים הם פעולות החשבון, ולכן הפוקוס ההתחלתי נמצא על "=" (OK) בלוח -
    // הוא מסומן בטבעת, כך שהמשתמש רואה איפה הוא. במצב מדעי הפוקוס עובר לרשת
    // הפונקציות והחצים זזים בה.
    val rootFocus = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(calcMode, showHistory, showMenu, confirmClearHistory) {
        // כשתפריט, היסטוריה או דיאלוג אישור פתוחים הפוקוס שייך להם; בסגירה הוא חוזר לכפתור.
        if (showHistory || showMenu || confirmClearHistory) return@LaunchedEffect
        runCatching { if (calcMode == CalculatorMode.STANDARD) rootFocus.requestFocus() else focusRequester.requestFocus() }
    }
    // * ו-# נצרכים ברמת המערכת (החזקה ארוכה = מרכז בקרה / מרכז התראות) ולחיצה
    // קצרה מגיעה רק כשידור - אין "* ארוך" או "# ארוך" באפליקציה. לכן * = נקודה
    // עשרונית (אין לה שום מקש אחר) ו-# = אחוז; C בתפריט Options, ו-BACK אחרי
    // תוצאה מנקה. המעבר למדעי - בתפריט ובסרגל התחתון. (המאזינים - אחרי inputDot/onPercent.)
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
        entry?.let {
            historyStore.add(it)
            // הווידג'ט במסך הבית מציג את התוצאה האחרונה.
            com.future.sharednav.widget.FutureContentWidget.refresh(context, com.future.calculator.CalculatorWidgetProvider::class.java)
        }
    }
    fun onClear() { state = CalculatorEngine.clear() }
    val sqrInput: () -> Unit = { state = CalculatorEngine.integerPower(state, 2) }
    val cubeInput: () -> Unit = { state = CalculatorEngine.integerPower(state, 3) }
    fun onPercent() { state = CalculatorEngine.percent(state) }
    fun applyUnaryFunction(fn: (Double) -> Double) { state = CalculatorEngine.unary(state, fn) }
    fun onFactorial() { state = CalculatorEngine.factorial(state) }
    fun onConstant(value: Double) { state = CalculatorEngine.constant(state, value) }
    fun onNegate() { state = CalculatorEngine.negate(state) }
    fun toRad(x: Double) = if (degrees) Math.toRadians(x) else x
    fun fromRad(x: Double) = if (degrees) Math.toDegrees(x) else x

    // * = נקודה עשרונית, # = אחוז - דרך השידור של FutureUI (ר' ההערה ליד המשתנים למעלה).
    com.future.sharednav.nav.onStarKeyPress { if (!showHistory && !showMenu) inputDot() }
    com.future.sharednav.nav.onPoundKeyPress { if (!showHistory && !showMenu) onPercent() }

    /** BACK במחשבון מוחק. רק כשאין מה למחוק הוא יוצא מהאפליקציה. */
    fun onBackKey() {
        when {
            state.isEmpty -> onBack()
            // אחרי "12 +" BACK מוחק רק את הפעולה, והמספר נשאר (כמו במחשבון כיס)
            state.startFresh && state.pendingOp != null -> state = state.copy(pendingValue = null, pendingOp = null, expressionLine = "")
            startFresh -> onClear()
            else -> onBackspace()
        }
    }
    BackHandler(onBack = ::onBackKey)

        // אין מסך מגע - ספרות מוקלדות ישירות דרך המקלדת הפיזית, אז השורה
        // העליונה כאן היא רק פעולות בלי מקש חומרה מקביל (במקום רשת ספרות
        // שממילא כפולה למקלדת הפיזית ותופסת מקום לשווא).
        // כל שורה: סימן הפונקציה + הפעולה. ארבע עמודות, נגללות לפי הפוקוס.
        fun trig(f: (Double) -> Double): () -> Unit = { applyUnaryFunction { x -> f(toRad(x)) } }
        fun arc(f: (Double) -> Double): () -> Unit = { applyUnaryFunction { x -> fromRad(f(x)) } }
        // נבנית מחדש רק כשמצב DEG/RAD משתנה (התווית שלו היא התלות היחידה)
        val scientificRows: List<List<Pair<String, () -> Unit>>> = remember(degrees) { listOf(
            listOf("sin" to trig(Math::sin), "cos" to trig(Math::cos), "tan" to trig(Math::tan), "xʸ" to { onOperator(CalcOp.POW) }),
            listOf("sin⁻¹" to arc(Math::asin), "cos⁻¹" to arc(Math::acos), "tan⁻¹" to arc(Math::atan), "ʸ√x" to { onOperator(CalcOp.ROOT) }),
            listOf("log" to { applyUnaryFunction(Math::log10) }, "ln" to { applyUnaryFunction(Math::log) }, "10ˣ" to { state = CalculatorEngine.tenPow(state) }, "eˣ" to { applyUnaryFunction(Math::exp) }),
            listOf("√" to { applyUnaryFunction(Math::sqrt) }, "∛" to { applyUnaryFunction(Math::cbrt) }, "x²" to sqrInput, "x³" to cubeInput),
            listOf("1/x" to { applyUnaryFunction { x -> 1.0 / x } }, "x!" to { onFactorial() }, "|x|" to { applyUnaryFunction(Math::abs) }, "±" to { onNegate() }),
            listOf("π" to { onConstant(Math.PI) }, "e" to { onConstant(Math.E) }, "mod" to { onOperator(CalcOp.MOD) }, (if (degrees) "DEG" else "RAD") to { degrees = !degrees }),
            listOf("÷" to { onOperator(CalcOp.DIV) }, "×" to { onOperator(CalcOp.MUL) }, "−" to { onOperator(CalcOp.SUB) }, "+" to { onOperator(CalcOp.ADD) }),
        ) }

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
            .focusable(enabled = calcMode == CalculatorMode.STANDARD)
            .onKeyEvent { event ->
                // שורות מסך ההיסטוריה מעבירות לכאן חצים שלא טופלו - בלי השער
                // הזה "למטה" ברשימה היה הופך לפעולת חיסור.
                if (showHistory) return@onKeyEvent false
                val native = event.nativeKeyEvent
                // KEYCODE_STAR/POUND מגיעים לכאן רק בלי FutureUI (אמולטור, מקלדת
                // חיצונית) - במכשיר הם מגיעים כשידור (ר' onStarKeyPress למעלה).
                if (native.keyCode == android.view.KeyEvent.KEYCODE_STAR) {
                    if (event.type == KeyEventType.KeyUp) inputDot()
                    return@onKeyEvent true
                }
                if (native.keyCode == android.view.KeyEvent.KEYCODE_POUND) {
                    if (event.type == KeyEventType.KeyUp) onPercent()
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
                        // ימינה בקצה הרשת חוזר ללשונית "רגיל" (הסרגל התחתון, RTL).
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
                title = if (calcMode == CalculatorMode.SCIENTIFIC) stringResource(R.string.calc_title_scientific) else stringResource(R.string.calc_title),
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
                            overflow = TextOverflow.StartEllipsis,
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
                        // בלי זה התוצאה נחתכת בקצה הימני ונעלמות הספרות הפחות משמעותיות
                        overflow = TextOverflow.StartEllipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val pendingSymbol = if (startFresh) pendingOp?.symbol else null
            if (calcMode == CalculatorMode.STANDARD) {
                CalcKeyLegend(theme)
                Spacer(Modifier.height(CalcPadGap))
                CalcDpad(theme = theme, pendingOp = if (startFresh) pendingOp else null, focusRequester = rootFocus)
                Spacer(Modifier.height(CalcPadGap))
            } else {
                // תאי ActionGrid של הדיזיין סיסטם: מילוי זכוכית, רדיוס כרטיס, טבעת
                // פוקוס 2dp בהדגשה. הפעולה שממתינה לאיבר השני דלוקה (20% הדגשה).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    scientificRows.forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                            row.forEachIndexed { colIndex, (glyph, action) ->
                                val isPending = glyph == pendingSymbol ||
                                    (glyph == "ʸ√x" && pendingSymbol == "ʸ√") ||
                                    (glyph == "xʸ" && pendingSymbol == "^")
                                val cellModifier = Modifier.weight(1f)
                                // הכיתוב תמיד משמאל לימין: בפריסת RTL "x!" ו-"1/x" היו מוצגים הפוכים
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    FutureActionCell(
                                        glyph = glyph,
                                        label = null,
                                        theme = theme,
                                        onClick = action,
                                        modifier = cellModifier,
                                        active = isPending,
                                        height = CalcKeyHeight,
                                        focusRequester = if (rowIndex == 0 && colIndex == 0) focusRequester else null,
                                    )
                                }
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
                        height = CalcKeyHeight,
                    )
                }
            }

            // סרגל תחתון בין רגיל למדעי. כמו בכל המערכת הוא לא מקבל פוקוס:
            // מחליפים לשונית בתפריט Options, או ימינה מקצה הרשת המדעית.
            FutureBottomNav(
                items = listOf(
                    FutureNavItem(stringResource(R.string.calc_mode_standard), FutureIcons.Calculate),
                    FutureNavItem(stringResource(R.string.calc_mode_scientific), FutureIcons.Functions),
                ),
                selectedIndex = if (calcMode == CalculatorMode.SCIENTIFIC) 1 else 0,
                theme = theme,
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
                onClear = {
                    showMenu = false
                    onClear()
                },
                onShowHistory = {
                    showMenu = false
                    showHistory = true
                },
                onCopyResult = {
                    showMenu = false
                    // שגיאה לא מועתקת (אחרת "שגיאה" היה נכנס ללוח); המשוב בא מהסנאקבר
                    if (state.isError) {
                        snackbar.show(context.getString(R.string.calc_nothing_to_copy))
                    } else {
                        context.getSystemService(ClipboardManager::class.java)
                            ?.setPrimaryClip(ClipData.newPlainText("result", display))
                        snackbar.show(context.getString(R.string.calc_copied))
                    }
                },
                onClearHistory = {
                    showMenu = false
                    confirmClearHistory = true
                }
            )
        }

        if (confirmClearHistory) {
            ConfirmDialog(
                message = stringResource(R.string.calc_confirm_clear_history),
                theme = theme,
                onCancel = { confirmClearHistory = false },
                onConfirm = {
                    confirmClearHistory = false
                    historyStore.clear()
                    // הווידג'ט במסך הבית קורא את ההיסטוריה - בלי רענון הוא מציג תוצאה ישנה
                    com.future.sharednav.widget.FutureContentWidget.refresh(context, com.future.calculator.CalculatorWidgetProvider::class.java)
                },
            )
        }

        FutureSnackbarHost(snackbar, theme)
    }
    }
}

/** 44dp - תא ברשת המדעית (שמונה שורות, נגללות לפי הפוקוס). */
private val CalcKeyHeight = 44.dp

// מידות לוח ה-D-pad מהווירפריים (620×950 פיקסלים ≈ חצי ב-dp על מסך 640×960):
// גלולה 469×324, מרכז "=" 168×150, 42 פיקסלים בין התצוגה ללוח ו-115 מתחתיו.
private val CalcPadWidth = 234.dp
private val CalcPadHeight = 162.dp
private val CalcPadCenterWidth = 84.dp
private val CalcPadCenterHeight = 75.dp
private val CalcPadGap = 16.dp
private val CalcPadOpSize = 48.dp

/**
 * לוח המחשבון הרגיל לפי הווירפריים: גלולה אחת בצבע מקש, "=" בעיגול ההדגשה
 * במרכז (OK), וארבע הפעולות סביבו במקום החץ שמפעיל אותן - למעלה חיבור, למטה
 * חיסור, שמאלה כפל, ימינה חילוק. הלוח לא מקבל פוקוס (אין בו מה לנווט): הוא
 * מפה של מקשי ה-D-pad הפיזיים. הפעולה שממתינה לאיבר השני נצבעת בהדגשה.
 * הפריסה כפויה LTR - החצים פיזיים, ו"שמאלה" חייב להופיע משמאל גם במסך RTL.
 */
@Composable
private fun CalcDpad(theme: FutureTheme, pendingOp: CalcOp?, focusRequester: FocusRequester) {
    // "=" (OK) הוא המקש הממוקד בתחילת המסך: הוא מחזיק את הפוקוס ומסומן בטבעת, כדי
    // שלא ייראה מסך בלי פוקוס גלוי. החצים והמקשים מגיעים אליו דרך ה-onKeyEvent של השורש.
    val centerSource = remember { MutableInteractionSource() }
    val centerFocused by centerSource.collectIsFocusedAsState()
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
                        .background(theme.readableAccentColor)
                        .border(2.dp, if (centerFocused) theme.textColor else Color.Transparent, FutureShapes.pill)
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = centerSource),
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
 * מפת המקשים שאין להם מקום בלוח: תאי ActionGrid של הדיזיין סיסטם, הפעולה
 * ומתחתיה המקש הפיזי שמפעיל אותה. זו מפה ולא מקשים - התאים לא מקבלים פוקוס,
 * כמו הלוח שמתחתיהם. קודם אלה היו חמש קופסאות צפופות בגופן 11sp, בצבע מקש
 * משני ובפינות 12dp - לא ActionGrid ולא כפתור.
 */
@Composable
private fun CalcKeyLegend(theme: FutureTheme) {
    val items = listOf(
        "." to "*",
        "%" to "#",
        "⌫" to stringResource(R.string.calc_key_back),
        "C" to stringResource(R.string.calc_key_menu),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        items.forEach { (function, key) ->
            FutureActionCell(
                glyph = function,
                label = key,
                theme = theme,
                onClick = {},
                modifier = Modifier.weight(1f),
                height = CalcLegendHeight,
                focusable = false,
            )
        }
    }
}

/** 56dp - תא במפת המקשים (סימן + שם המקש). */
private val CalcLegendHeight = 56.dp

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
        ScreenTopBar(title = stringResource(R.string.calc_history_title), textColor = theme.textColor, accentColor = theme.accentColor)
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.calc_empty), color = theme.subtleTextColor, fontSize = FutureTypography.body)
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
                // ביטוי ארוך נחתך בהתחלה (...) ולא דוחף את התוצאה מחוץ למסך
                Text(
                    entry.expression,
                    color = theme.textAlpha(50),
                    fontSize = FutureTypography.summary,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                    modifier = Modifier.weight(1f),
                )
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
    onClear: () -> Unit,
    onShowHistory: () -> Unit,
    onCopyResult: () -> Unit,
    onClearHistory: () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = stringResource(R.string.calc_title)) {
        FutureMenuRow(if (scientific) stringResource(R.string.calc_menu_standard) else stringResource(R.string.calc_menu_scientific), FutureIcons.Functions, theme, onToggleMode)
        FutureMenuRow(stringResource(R.string.calc_menu_clear), FutureIcons.Cancel, theme, onClear)
        FutureMenuRow(stringResource(R.string.calc_menu_history), FutureIcons.History, theme, onShowHistory)
        FutureMenuRow(stringResource(R.string.calc_menu_copy), FutureIcons.ContentCopy, theme, onCopyResult)
        FutureMenuRow(stringResource(R.string.calc_menu_clear_history), FutureIcons.Delete, theme, onClearHistory, destructive = true)
    }
}

/** המצב שורד סיבוב מסך ו-process death (ל-Activity אין configChanges). */
private val CalcStateSaver = listSaver<CalcState, Any>(
    save = {
        listOf(it.display, it.expressionLine, it.pendingValue?.toPlainString() ?: "", it.pendingOp?.name ?: "", it.startFresh, it.operandFromFunction)
    },
    restore = { l ->
        CalcState(
            display = l[0] as String,
            expressionLine = l[1] as String,
            pendingValue = (l[2] as String).takeIf { it.isNotEmpty() }?.let { java.math.BigDecimal(it) },
            pendingOp = (l[3] as String).takeIf { it.isNotEmpty() }?.let { CalcOp.valueOf(it) },
            startFresh = l[4] as Boolean,
            operandFromFunction = l[5] as Boolean,
        )
    },
)

/**
 * ההיסטוריה נשמרת בהעדפות (50 האחרונות) - קודם היא נעלמה בכל יציאה מהמחשבון.
 * שורה לכל חישוב: ביטוי ותוצאה מופרדים בתו שלא מופיע באף אחד מהם.
 */
private class CalcHistoryStore(private val prefs: android.content.SharedPreferences) {
    val entries = androidx.compose.runtime.mutableStateListOf<CalcHistoryEntry>().apply {
        addAll(CalcHistoryFormat.parse(prefs.getString(CalcHistoryFormat.KEY, "").orEmpty()))
    }

    fun add(entry: CalcHistoryEntry) {
        entries.add(0, entry)
        while (entries.size > CalcHistoryFormat.MAX) entries.removeAt(entries.lastIndex)
        save()
    }

    fun clear() {
        entries.clear()
        save()
    }

    private fun save() {
        prefs.edit().putString(CalcHistoryFormat.KEY, CalcHistoryFormat.serialize(entries)).apply()
    }
}

@Composable
private fun rememberCalcHistoryStore(): CalcHistoryStore {
    val context = LocalContext.current
    return remember { CalcHistoryStore(context.getSharedPreferences(CalcHistoryFormat.PREFS, android.content.Context.MODE_PRIVATE)) }
}
