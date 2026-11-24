package com.creazzy.rofof.feature.smartentry

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MathExpressionEvaluatorTest {
    @Test fun respectsOperatorPrecedenceAndParentheses() {
        assertEquals(BigDecimal("35"), MathExpressionEvaluator.evaluate("25+2*5"))
        assertEquals(BigDecimal("135"), MathExpressionEvaluator.evaluate("(25+20)*3"))
    }

    @Test fun supportsDivisionAndDecimalValues() {
        assertEquals(BigDecimal("12.5"), MathExpressionEvaluator.evaluate("25/2"))
        assertEquals(BigDecimal("2.75"), MathExpressionEvaluator.evaluate("1.25+1.5"))
    }

    @Test fun rejectsZeroDivisionMalformedAndNonPositiveAmounts() {
        assertNull(MathExpressionEvaluator.evaluate("5/0"))
        assertNull(MathExpressionEvaluator.evaluate("10+"))
        assertNull(MathExpressionEvaluator.evaluate("10-20"))
        assertNull(MathExpressionEvaluator.evaluate("1/3"))
    }

    @Test fun parserExtractsCalculatedAmountFromArabicQuickEntry() {
        val result = SmartEntryParser().parse("غداء ٢٥+٢٥")
        assertEquals("50", result.amountText)
    }
}
