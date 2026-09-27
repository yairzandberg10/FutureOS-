package com.future.calculator.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private fun type(vararg keys: String): CalcState {
        var s = CalcState()
        for (k in keys) {
            s = when (k) {
                "+" -> CalculatorEngine.operator(s, CalcOp.ADD)
                "-" -> CalculatorEngine.operator(s, CalcOp.SUB)
                "*" -> CalculatorEngine.operator(s, CalcOp.MUL)
                "/" -> CalculatorEngine.operator(s, CalcOp.DIV)
                "=" -> CalculatorEngine.equals(s).first
                "%" -> CalculatorEngine.percent(s)
                "." -> CalculatorEngine.inputDot(s)
                else -> k.fold(s) { acc, c -> CalculatorEngine.inputDigit(acc, c.toString()) }
            }
        }
        return s
    }

    @Test fun addition() = assertEquals("5", type("2", "+", "3", "=").display)

    @Test fun chainedOperatorsApplyLeftToRight() = assertEquals("20", type("2", "+", "3", "*", "4", "=").display)

    @Test fun secondOperatorReplacesFirst() = assertEquals("6", type("2", "+", "*", "3", "=").display)

    @Test fun decimals() = assertEquals("0.3", type("0", ".", "1", "+", "0", ".", "2", "=").display)

    @Test fun divisionByZeroIsError() = assertTrue(type("5", "/", "0", "=").isError)

    @Test fun operatorAfterErrorDoesNotContinueFromZero() {
        val s = CalculatorEngine.operator(type("5", "/", "0", "="), CalcOp.ADD)
        assertTrue(s.isError)
        assertNull(s.pendingOp)
    }

    @Test fun digitAfterErrorStartsFresh() = assertEquals("7", CalculatorEngine.inputDigit(type("5", "/", "0", "="), "7").display)

    @Test fun percentAlone() = assertEquals("0.5", type("50", "%").display)

    @Test fun percentOfFirstOperandWithAddition() = assertEquals("220", type("200", "+", "10", "%", "=").display)

    @Test fun percentWithSubtraction() = assertEquals("180", type("200", "-", "10", "%", "=").display)

    @Test fun percentWithMultiplication() = assertEquals("20", type("200", "*", "10", "%", "=").display)

    @Test fun digitAfterPercentStartsNewNumber() = assertEquals("3", type("50", "%", "3").display)

    @Test fun inputIsCappedAt15Digits() = assertEquals(15, type("1234567890123456789").display.length)

    @Test fun hugeResultsUseScientificNotation() {
        val s = CalculatorEngine.factorial(type("170"))
        assertTrue(s.display, s.display.length <= 20 && s.display.contains("E+306"))
    }

    @Test fun equalsReturnsHistoryEntry() {
        val (state, entry) = CalculatorEngine.equals(type("6", "*", "7"))
        assertEquals("42", state.display)
        assertEquals("6 × 7", entry?.expression)
    }

    @Test fun equalsWithoutOperationHasNoHistory() = assertNull(CalculatorEngine.equals(type("6")).second)

    @Test fun backspaceToZero() = assertEquals("0", CalculatorEngine.backspace(type("7")).display)
}
