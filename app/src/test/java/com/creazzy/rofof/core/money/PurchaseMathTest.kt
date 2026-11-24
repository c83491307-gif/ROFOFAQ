package com.creazzy.rofof.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PurchaseMathTest {
    @Test fun quantityAndPriceProduceMinorUnits() {
        assertEquals(625L, PurchaseMath.lineTotalMinor(2_500L, 250L))
    }

    @Test fun fractionalQuantityUsesHalfUpRounding() {
        assertEquals(2L, PurchaseMath.lineTotalMinor(1_000L, 2L))
        assertEquals(1L, PurchaseMath.lineTotalMinor(500L, 1L))
    }

    @Test fun rejectsNonPositiveInputs() {
        assertThrows(IllegalArgumentException::class.java) {
            PurchaseMath.lineTotalMinor(0L, 100L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PurchaseMath.lineTotalMinor(1_000L, 0L)
        }
    }

    @Test fun allocationsMustMatchItemTotalExactly() {
        PurchaseMath.requireExactAllocation(1_001L, mapOf("a" to 500L, "b" to 501L))
        assertThrows(IllegalArgumentException::class.java) {
            PurchaseMath.requireExactAllocation(1_001L, mapOf("a" to 500L, "b" to 500L))
        }
    }

    @Test fun allocationsCannotBeNegativeOrEmpty() {
        assertThrows(IllegalArgumentException::class.java) {
            PurchaseMath.requireExactAllocation(100L, emptyMap())
        }
        assertThrows(IllegalArgumentException::class.java) {
            PurchaseMath.requireExactAllocation(100L, mapOf("a" to -1L, "b" to 101L))
        }
    }

    @Test fun largeMultiplicationDoesNotOverflowSilently() {
        assertThrows(ArithmeticException::class.java) {
            PurchaseMath.lineTotalMinor(Long.MAX_VALUE, Long.MAX_VALUE)
        }
    }
}
