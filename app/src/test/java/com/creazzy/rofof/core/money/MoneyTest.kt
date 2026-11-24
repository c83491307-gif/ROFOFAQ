package com.creazzy.rofof.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    @Test fun additionUsesExactMinorUnits() {
        assertEquals(200L, Money(125, "EGP", 2).plus(Money(75, "EGP", 2)).minorUnits)
    }

    @Test fun subtractionAllowsNegativeBalances() {
        assertEquals(-150L, Money(100, "EGP", 2).minus(Money(250, "EGP", 2)).minorUnits)
    }

    @Test fun rejectsDifferentCurrencies() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(100, "EGP", 2).plus(Money(100, "USD", 2))
        }
    }

    @Test fun rejectsInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException::class.java) { Money(100, "egp", 2) }
    }

    @Test fun detectsOverflow() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE, "EGP", 2).plus(Money(1, "EGP", 2))
        }
    }
}
