package com.creazzy.rofof.feature.budgetgoals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetOverrunPolicyTest {
    @Test fun reachingBudgetExactlyDoesNotCountAsOverrun() {
        assertFalse(BudgetOverrunPolicy.isOverrun(10_000L, 10_000L))
    }

    @Test fun anyAmountAboveBudgetCountsAsOverrun() {
        assertTrue(BudgetOverrunPolicy.isOverrun(10_001L, 10_000L))
    }

    @Test fun duplicateNotificationIsSuppressedUntilSpendingReturnsToLimit() {
        assertTrue(BudgetOverrunPolicy.shouldNotify(10_001L, 10_000L, alreadyNotified = false))
        assertFalse(BudgetOverrunPolicy.shouldNotify(10_002L, 10_000L, alreadyNotified = true))
        assertTrue(BudgetOverrunPolicy.shouldRearm(10_000L, 10_000L))
        assertTrue(BudgetOverrunPolicy.shouldNotify(10_001L, 10_000L, alreadyNotified = false))
    }

    @Test fun nonPositiveBudgetCannotGenerateAnAlert() {
        assertFalse(BudgetOverrunPolicy.isOverrun(1L, 0L))
        assertFalse(BudgetOverrunPolicy.isOverrun(1L, -1L))
    }
}
