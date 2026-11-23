package com.creazzy.rofof.core.money

import java.math.BigInteger

object PurchaseMath {
    const val QUANTITY_SCALE = 1_000L

    /**
     * quantityMilli stores quantity with three decimal places.
     * Positive line totals are rounded half-up to the currency's minor unit.
     */
    fun lineTotalMinor(quantityMilli: Long, unitPriceMinor: Long): Long {
        require(quantityMilli > 0L) { "Quantity must be positive" }
        require(unitPriceMinor > 0L) { "Unit price must be positive" }
        val numerator = BigInteger.valueOf(quantityMilli)
            .multiply(BigInteger.valueOf(unitPriceMinor))
        val rounded = numerator.add(BigInteger.valueOf(QUANTITY_SCALE / 2))
            .divide(BigInteger.valueOf(QUANTITY_SCALE))
        if (rounded > LONG_MAX || rounded < LONG_MIN) {
            throw ArithmeticException("Line total exceeds the supported monetary range")
        }
        return rounded.toLong()
    }

    fun requireExactAllocation(totalMinor: Long, shares: Map<String, Long>) {
        require(totalMinor > 0L) { "Item total must be positive" }
        require(shares.isNotEmpty()) { "At least one participant allocation is required" }
        require(shares.keys.none { it.isBlank() }) { "Participant identifiers cannot be blank" }
        require(shares.values.all { it >= 0L }) { "Allocations cannot be negative" }
        val sum = shares.values.fold(BigInteger.ZERO) { acc, value ->
            acc.add(BigInteger.valueOf(value))
        }
        require(sum == BigInteger.valueOf(totalMinor)) {
            "Allocations must add up exactly to the item total"
        }
    }

    private val LONG_MAX: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    private val LONG_MIN: BigInteger = BigInteger.valueOf(Long.MIN_VALUE)
}
