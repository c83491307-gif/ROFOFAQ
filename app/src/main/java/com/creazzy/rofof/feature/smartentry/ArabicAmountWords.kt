package com.creazzy.rofof.feature.smartentry

import java.math.BigDecimal

/** Conservative Arabic colloquial number-word support for common quick-entry amounts. */
object ArabicAmountWords {
    private val values = mapOf(
        "صفر" to 0, "واحد" to 1, "واحده" to 1, "واحدا" to 1,
        "اتنين" to 2, "اثنين" to 2, "اثنان" to 2,
        "تلاته" to 3, "ثلاثه" to 3, "ثلاث" to 3,
        "اربعه" to 4, "اربع" to 4, "خمسه" to 5, "خمس" to 5,
        "سته" to 6, "ست" to 6, "سبعه" to 7, "سبع" to 7,
        "تمانيه" to 8, "ثمانيه" to 8, "ثمان" to 8,
        "تسعه" to 9, "تسع" to 9, "عشره" to 10, "عشر" to 10,
        "عشرين" to 20, "ثلاثين" to 30, "تلاتين" to 30, "اربعين" to 40,
        "خمسين" to 50, "ستين" to 60, "سبعين" to 70, "تمانين" to 80,
        "ثمانين" to 80, "تسعين" to 90, "ميه" to 100, "مئه" to 100, "مائه" to 100
    )

    fun extract(normalizedText: String): BigDecimal? {
        val tokens = normalizedText.split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
        val numeric = tokens.mapNotNull(values::get)
        if (numeric.isEmpty()) return null
        val total = numeric.sum()
        if (total <= 0 || total > 1_000_000) return null
        return BigDecimal.valueOf(total.toLong())
    }
}
