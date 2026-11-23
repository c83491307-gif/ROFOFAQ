package com.creazzy.rofof.core.money

/** Exact monetary amount in integer minor units with explicit currency metadata. */
data class Money(
    val minorUnits: Long,
    val currencyCode: String,
    val currencyExponent: Int
) {
    init {
        require(currencyCode.matches(Regex("[A-Z]{3}"))) { "Currency code must be uppercase alpha-3." }
        require(currencyExponent in 0..6) { "Currency exponent must be between 0 and 6." }
    }

    fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = Math.subtractExact(minorUnits, other.minorUnits))
    }

    fun isZero(): Boolean = minorUnits == 0L

    private fun requireSameCurrency(other: Money) {
        require(currencyCode == other.currencyCode && currencyExponent == other.currencyExponent) {
            "Cannot combine amounts with different currency metadata."
        }
    }
}
