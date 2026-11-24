package com.creazzy.rofof.feature.smartentry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArabicAmountWordsTest {
    @Test fun parsesCommonEgyptianSpokenAmounts() {
        assertEquals("50", ArabicAmountWords.extract("قهوه خمسين جنيه")?.toPlainString())
        assertEquals("30", ArabicAmountWords.extract("تاكسي تلاتين")?.toPlainString())
        assertEquals("150", ArabicAmountWords.extract("مئه خمسين جنيه")?.toPlainString())
    }

    @Test fun ignoresTextWithoutAmountWords() {
        assertNull(ArabicAmountWords.extract("قهوه ستاربكس"))
    }
}
