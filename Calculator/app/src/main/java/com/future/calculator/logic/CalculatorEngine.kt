package com.future.calculator.logic

import java.math.BigDecimal
import java.math.MathContext

enum class CalcOp(val symbol: String) { ADD("+"), SUB("−"), MUL("×"), DIV("÷"), POW("^"), ROOT("ʸ√"), MOD("mod") }

data class CalcHistoryEntry(val expression: String, val result: String)

/**
 * מצב המחשבון - בלתי משתנה. כל פעולה מחזירה מצב חדש, כך שהלוגיקה נבדקת
 * בבדיקות יחידה בלי ממשק (ר' CalculatorEngineTest), והמסך רק מחזיק את המצב.
 */
data class CalcState(
    val display: String = "0",
    val expressionLine: String = "",
    val pendingValue: BigDecimal? = null,
    val pendingOp: CalcOp? = null,
    val startFresh: Boolean = true,
    /** התצוגה הגיעה מאחוז/פונקציה (√, x², n!...) על האופרנד הממתין - הפעולה הממתינה עוד לא חושבה. */
    val operandFromFunction: Boolean = false,
) {
    val isError: Boolean get() = display == CalculatorEngine.ERROR
    val isEmpty: Boolean get() = display == "0" && pendingOp == null && expressionLine.isEmpty()
}

object CalculatorEngine {
    const val ERROR = "שגיאה"

    /** תקרת ספרות קלט גולמי, כדי שהתוצאה תמיד תיכנס למסך הקבוע. */
    const val MAX_INPUT_DIGITS = 15

    /** דיוק אחיד לכל הפעולות - תוצאה לא מתפוצצת לעשרות ספרות. */
    val PRECISION = MathContext(12)

    /**
     * תצוגת מספר: רגילה כשהיא קצרה, וכתיב מדעי (1.23456789012E+30) כשהיא ארוכה
     * מ-16 תווים - 170! הוא 307 ספרות, הרבה מעבר למה שנכנס במסך.
     */
    fun format(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val plain = stripped.toPlainString()
        if (plain.length <= 16) return plain
        return stripped.round(PRECISION).stripTrailingZeros().toString()
    }

    fun value(s: CalcState): BigDecimal = try { BigDecimal(s.display) } catch (e: Exception) { BigDecimal.ZERO }

    fun inputDigit(s: CalcState, digit: String): CalcState {
        // אחרי שגיאה ספרה מתחילה מספר חדש, לא נצמדת ל"שגיאה"
        if (s.isError) return CalcState(display = digit, startFresh = false)
        val digitsOnly = s.display.count { it.isDigit() }
        if (!s.startFresh && s.display != "0" && digitsOnly >= MAX_INPUT_DIGITS) return s
        // מספר חדש אחרי ± (למשל 5 + ± 3) שומר על הסימן השלילי
        val display = when {
            s.startFresh && s.display.startsWith("-") -> "-$digit"
            s.startFresh || s.display == "0" -> digit
            else -> s.display + digit
        }
        return s.copy(display = display, startFresh = false, expressionLine = freshLine(s), operandFromFunction = false)
    }

    fun inputDot(s: CalcState): CalcState = when {
        s.isError || s.startFresh -> {
            val sign = if (!s.isError && s.display.startsWith("-")) "-" else ""
            s.copy(display = "$sign" + "0.", startFresh = false, expressionLine = freshLine(s), operandFromFunction = false)
        }
        !s.display.contains(".") -> s.copy(display = s.display + ".")
        else -> s
    }

    fun backspace(s: CalcState): CalcState {
        if (s.startFresh) return s
        val d = s.display
        return s.copy(display = if (d.length <= 1 || (d.length == 2 && d.startsWith("-"))) "0" else d.dropLast(1))
    }

    fun clear(): CalcState = CalcState()

    /**
     * חזקה: מעריך שלם (עד 1000 בערך) מחושב מדויק ב-BigDecimal, כדי ש-2^40 לא יאבד ספרות
     * דרך Double. מעריך שאינו שלם (או גדול מדי) נשאר על Math.pow.
     */
    private fun power(base: BigDecimal, exp: BigDecimal): BigDecimal {
        val n = exp.stripTrailingZeros()
        return if (n.scale() <= 0 && n.abs() <= BigDecimal(1000)) {
            base.pow(n.toInt(), MathContext.DECIMAL128)
        } else {
            BigDecimal(Math.pow(base.toDouble(), exp.toDouble()), PRECISION)
        }
    }

    private fun apply(pv: BigDecimal, op: CalcOp, cur: BigDecimal): String = try {
        val result = when (op) {
            // חיבור/חיסור/כפל מדויקים: הקלט עד 15 ספרות, והתוצאה מעוגלת רק בתצוגה ב-format
            // (עיגול ל-12 כאן איבד ספרות כבר ב-1234567890123 + 1)
            CalcOp.ADD -> pv.add(cur)
            CalcOp.SUB -> pv.subtract(cur)
            CalcOp.MUL -> pv.multiply(cur)
            CalcOp.DIV -> if (cur.signum() == 0) throw ArithmeticException("Division by zero") else pv.divide(cur, PRECISION)
            // חזקה לא-שלמה היא מחוץ ל-BigDecimal - דרך Double, כמו הפונקציות המדעיות
            CalcOp.POW -> power(pv, cur)
            // שורש y של x = x^(1/y). שורש אי-זוגי של מספר שלילי מוגדר (∛-8 = -2).
            CalcOp.ROOT -> {
                val x = pv.toDouble()
                val y = cur.toDouble()
                if (y == 0.0) throw ArithmeticException("root 0")
                // Math.abs: ב-Java סימן ה-% הוא סימן המחולק, אז -3 % 2.0 = -1.0 ולא 1.0
                val r = if (x < 0 && Math.abs(y % 2.0) == 1.0) -Math.pow(-x, 1.0 / y) else Math.pow(x, 1.0 / y)
                if (r.isNaN()) throw ArithmeticException("NaN")
                BigDecimal(r, PRECISION)
            }
            CalcOp.MOD -> if (cur.signum() == 0) throw ArithmeticException("mod 0") else pv.remainder(cur, PRECISION)
        }
        format(result)
    } catch (e: Exception) {
        ERROR
    }

    fun operator(s: CalcState, op: CalcOp): CalcState {
        // אחרי שגיאה פעולה לא ממשיכה בשקט מ-0 - צריך להקליד מספר חדש
        if (s.isError) return s
        // פעולה שנייה ברצף בלי מספר ביניהן רק מחליפה את הפעולה הממתינה - אלא אם התצוגה
        // הגיעה מאחוז/פונקציה, אז הפעולה הממתינה צריכה להתחשב קודם (200 + 10% + 5 = 225)
        if (s.startFresh && s.pendingOp != null && s.pendingValue != null && !s.operandFromFunction) {
            return s.copy(pendingOp = op, expressionLine = "${format(s.pendingValue)} ${op.symbol}")
        }
        val display = if (s.pendingValue != null && s.pendingOp != null) apply(s.pendingValue, s.pendingOp, value(s)) else s.display
        if (display == ERROR) return CalcState(display = ERROR)
        val pv = try { BigDecimal(display) } catch (e: Exception) { BigDecimal.ZERO }
        return CalcState(display = display, pendingValue = pv, pendingOp = op, startFresh = true, expressionLine = "${format(pv)} ${op.symbol}")
    }

    /** מחזיר את המצב החדש ואת שורת ההיסטוריה (אם חושב משהו). */
    fun equals(s: CalcState): Pair<CalcState, CalcHistoryEntry?> {
        val pv = s.pendingValue
        val op = s.pendingOp
        if (pv == null || op == null || s.isError) return s.copy(pendingValue = null, pendingOp = null, startFresh = true, expressionLine = "") to null
        val expression = "${format(pv)} ${op.symbol} ${format(value(s))}"
        val result = apply(pv, op, value(s))
        // הביטוי נשאר מעל התוצאה ("12 + 3 =") עד שמתחילים מספר חדש. תוצאת שגיאה לא נשמרת להיסטוריה.
        val entry = if (result == ERROR) null else CalcHistoryEntry(expression, result)
        return CalcState(display = result, startFresh = true, expressionLine = "$expression =") to entry
    }

    /** מספר חדש אחרי "=" מוחק את שורת הביטוי הקודם; באמצע פעולה היא נשארת. */
    private fun freshLine(s: CalcState): String = if (s.startFresh && s.pendingOp == null) "" else s.expressionLine

    /**
     * אחוזים כמו במחשבון כיס: עם חיבור/חיסור ממתין האחוז הוא מהמספר הראשון
     * (200 + 10% = 200 + 20), ובלי פעולה או עם כפל/חילוק - חלוקה ב-100.
     * התוצאה "סגורה": ספרה אחריה מתחילה מספר חדש ולא נצמדת אליה.
     */
    fun percent(s: CalcState): CalcState {
        if (s.isError) return s
        val cur = value(s)
        val pv = s.pendingValue
        val result = try {
            if (pv != null && (s.pendingOp == CalcOp.ADD || s.pendingOp == CalcOp.SUB)) {
                pv.multiply(cur).divide(BigDecimal(100), PRECISION)
            } else {
                cur.divide(BigDecimal(100), PRECISION)
            }
        } catch (e: Exception) {
            return s.copy(display = ERROR, startFresh = true, expressionLine = freshLine(s))
        }
        return s.copy(display = format(result), startFresh = true, expressionLine = freshLine(s), operandFromFunction = true)
    }

    fun unary(s: CalcState, fn: (Double) -> Double): CalcState {
        if (s.isError) return s
        val display = try {
            val result = fn(value(s).toDouble())
            if (result.isNaN() || result.isInfinite()) ERROR else format(BigDecimal(result, PRECISION))
        } catch (e: Exception) {
            ERROR
        }
        return s.copy(display = display, startFresh = true, expressionLine = freshLine(s), operandFromFunction = true)
    }

    /** x² / x³ בדיוק (בלי Double), כמו החזקה הרגילה. */
    fun integerPower(s: CalcState, exponent: Int): CalcState {
        if (s.isError) return s
        val display = try {
            format(value(s).pow(exponent))
        } catch (e: Exception) {
            ERROR
        }
        return s.copy(display = display, startFresh = true, expressionLine = freshLine(s), operandFromFunction = true)
    }

    /** 10ˣ: מעריך שלם נשאר מדויק (10^3 = 1000), אחרת דרך Math.pow. */
    fun tenPow(s: CalcState): CalcState {
        val x = value(s)
        val n = x.stripTrailingZeros()
        return if (n.scale() <= 0 && n.abs() <= BigDecimal(1000)) {
            s.copy(display = format(BigDecimal.TEN.pow(n.toInt(), MathContext.DECIMAL128)), startFresh = true, expressionLine = freshLine(s), operandFromFunction = true)
        } else {
            unary(s) { v -> Math.pow(10.0, v) }
        }
    }

    fun factorial(s: CalcState): CalcState {
        if (s.isError) return s
        val n = value(s).toDouble()
        val display = if (n < 0 || n != Math.floor(n) || n > 170) {
            ERROR
        } else {
            var result = BigDecimal.ONE
            for (i in 2..n.toInt()) result = result.multiply(BigDecimal(i))
            format(result)
        }
        return s.copy(display = display, startFresh = true, expressionLine = freshLine(s), operandFromFunction = true)
    }

    /** ± - הופך את הסימן של המספר שבתצוגה, בלי לסגור אותו (אפשר להמשיך להקליד). */
    fun negate(s: CalcState): CalcState {
        if (s.isError || s.display == "0") return s
        val d = s.display
        return s.copy(display = if (d.startsWith("-")) d.drop(1) else "-$d", expressionLine = freshLine(s))
    }

    fun constant(s: CalcState, value: Double): CalcState =
        s.copy(display = format(BigDecimal(value, PRECISION)), startFresh = true, expressionLine = freshLine(s), operandFromFunction = true)

    /** תוצאה מההיסטוריה חוזרת לתצוגה כמספר חדש. */
    fun recall(result: String): CalcState = CalcState(display = result, startFresh = true)
}
