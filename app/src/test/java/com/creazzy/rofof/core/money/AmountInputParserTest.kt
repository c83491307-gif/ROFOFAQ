package com.creazzy.rofof.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputParserTest {
    @Test fun parsesLatinDecimalInput() {
        assertEquals(1050L, AmountInputParser.toMinorUnits("10.50", 2))
    }

    @Test fun parsesArabicIndicDigitsAndDecimalMark() {
        assertEquals(1050L, AmountInputParser.toMinorUnits("١٠٫٥٠", 2))
    }

    @Test fun parsesEasternArabicIndicDigits() {
        assertEquals(1250L, AmountInputParser.toMinorUnits("۱۲٫۵۰", 2))
    }

    @Test fun parsesDecimalComma() {
        assertEquals(1050L, AmountInputParser.toMinorUnits("10,50", 2))
    }

    @Test fun removesArabicThousandsSeparatorAndSpaces() {
        assertEquals(123450L, AmountInputParser.toMinorUnits("١٬٢٣٤٫٥٠", 2))
    }

    @Test fun rejectsNegativeZeroMalformedAndExcessPrecision() {
        assertNull(AmountInputParser.toMinorUnits("-1.00", 2))
        assertNull(AmountInputParser.toMinorUnits("0", 2))
        assertNull(AmountInputParser.toMinorUnits("12..5", 2))
        assertNull(AmountInputParser.toMinorUnits("1.001", 2))
    }

    @Test fun rejectsOverflow() {
        assertNull(AmountInputParser.toMinorUnits("999999999999999999999999", 2))
    }
}
