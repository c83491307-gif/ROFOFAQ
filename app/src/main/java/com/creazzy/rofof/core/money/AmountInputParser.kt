package com.creazzy.rofof.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Locale-tolerant, exact amount input conversion.
 * Accepts Latin, Arabic-Indic and Eastern Arabic-Indic digits.
 * A single comma or Arabic decimal separator is treated as the decimal mark.
 */
object AmountInputParser {
    fun normalize(raw: String): String? {
        val mapped = buildString(raw.length) {
            raw.forEach { char ->
                append(
                    when (char) {
                        in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
                        in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
                        '٫', ',' -> '.'
                        '٬', ' ', '\u00A0', '\u202F' -> ""
                        else -> char.toString()
                    }
                )
            }
        }.trim()
        if (mapped.isEmpty() || !mapped.matches(Regex("""\d+(\.\d+)?"""))) return null
        return mapped
    }

    fun toMinorUnits(raw: String, exponent: Int): Long? {
        if (exponent !in 0..6) return null
        val normalized = normalize(raw) ?: return null
        return runCatching {
            BigDecimal(normalized)
                .setScale(exponent, RoundingMode.UNNECESSARY)
                .movePointRight(exponent)
                .longValueExact()
                .takeIf { it > 0L }
        }.getOrNull()
    }
}
