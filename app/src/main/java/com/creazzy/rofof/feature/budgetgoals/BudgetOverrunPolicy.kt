package com.creazzy.rofof.feature.budgetgoals

object BudgetOverrunPolicy {
    fun isOverrun(spentMinor: Long, budgetMinor: Long): Boolean =
        budgetMinor > 0L && spentMinor > budgetMinor

    fun shouldNotify(spentMinor: Long, budgetMinor: Long, alreadyNotified: Boolean): Boolean =
        isOverrun(spentMinor, budgetMinor) && !alreadyNotified

    fun shouldRearm(spentMinor: Long, budgetMinor: Long): Boolean =
        spentMinor <= budgetMinor
}
