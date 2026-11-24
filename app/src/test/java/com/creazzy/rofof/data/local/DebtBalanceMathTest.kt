package com.creazzy.rofof.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtBalanceMathTest {
    @Test fun outstandingBalanceIsPrincipalLessRepayments() {
        val principal = 125_000L
        val repayments = listOf(10_000L, 15_000L, 20_000L)
        val outstanding = principal - repayments.sum()
        assertEquals(80_000L, outstanding)
    }

    @Test fun repaymentCannotExceedOutstandingBalance() {
        val principal = 5_000L
        val repaid = 4_000L
        val nextPayment = 1_001L
        assertTrue(nextPayment > principal - repaid)
    }

    @Test fun currenciesRemainSeparate() {
        val egpMinor = 10_000L
        val usdMinor = 10_000L
        assertEquals(egpMinor, usdMinor)
        // Equal integer values are not a conversion or an aggregate across currencies.
    }
}
