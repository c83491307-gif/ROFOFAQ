package com.creazzy.rofof.feature.smartentry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartEntryParserTest {
    private val parser = SmartEntryParser()

    @Test fun parsesArabicDigitsAndCoffeeCategory() {
        val result = parser.parse("قهوة ٥٠")
        assertEquals("50", result.amountText)
        assertEquals("Coffee", result.category?.categoryNameEnglish)
        assertEquals("EXPENSE", result.transactionType)
        assertTrue(result.requiresReview)
    }

    @Test fun matchesLongestPhraseForWorkLunch() {
        val result = parser.parse("غداء عمل ١٢٠")
        assertEquals("Work", result.category?.categoryNameEnglish)
    }

    @Test fun recognizesExplicitIncomeCue() {
        val result = parser.parse("استلمت راتب 15000")
        assertEquals("INCOME", result.transactionType)
        assertEquals("Salary", result.category?.categoryNameEnglish)
    }

    @Test fun unknownCategoryRemainsUnclassified() {
        val result = parser.parse("مصاريف غير معروفة 75")
        assertEquals("75", result.amountText)
        assertNull(result.category)
    }

    @Test fun doesNotInventAnAmount() {
        val result = parser.parse("قهوة")
        assertNull(result.amountText)
        assertNotNull(result.category)
    }

    @Test fun removesArabicThousandsSeparatorWithoutChangingValue() {
        assertEquals("1200", parser.parse("تاكسي ١٬٢٠٠").amountText)
    }

    @Test fun supportsPersianAndArabicIndicDigits() {
        assertEquals("125.50", parser.parse("تاكسي ۱۲۵٫۵۰").amountText)
        assertEquals("25", parser.parse("تاكسي ٢٥").amountText)
    }
}
