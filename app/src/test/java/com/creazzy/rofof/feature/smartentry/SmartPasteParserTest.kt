package com.creazzy.rofof.feature.smartentry

import org.junit.Assert.*
import org.junit.Test

class SmartPasteParserTest {
    @Test fun parsesBankMessageWithoutPostingAnything() {
        val result = SmartPasteParser.parse("تم خصم 150 جنيه من حسابك لدى ستاربكس")!!
        assertEquals("150", result.amountText)
        assertTrue(result.merchantText!!.contains("ستاربكس"))
        assertTrue(result.parsedEntry.requiresReview)
    }

    @Test fun rejectsUnstructuredTextAndMissingAmounts() {
        assertNull(SmartPasteParser.parse("مرحباً بك في تطبيق البنك"))
        assertNull(SmartPasteParser.parse("تم خصم مبلغ من حسابك"))
    }
}
