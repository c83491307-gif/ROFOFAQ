package com.creazzy.rofof.feature.smartentry

import java.text.Normalizer

/**
 * Offline Arabic/English quick-entry parser.
 *
 * It only produces a suggestion. Callers must show a review step and must
 * resolve the suggested category/account against the current Room data before
 * posting a ledger transaction.
 */
class SmartEntryParser(
    private val keywords: Map<String, CategorySuggestion> = DEFAULT_KEYWORDS
) {
    data class CategorySuggestion(
        val categoryNameArabic: String,
        val categoryNameEnglish: String,
        val transactionType: String
    )

    data class ParsedEntry(
        val amountText: String?,
        val category: CategorySuggestion?,
        val transactionType: String,
        val note: String,
        val requiresReview: Boolean = true
    )

    fun parse(input: String): ParsedEntry {
        val original = input.trim()
        val normalized = normalize(toAsciiDigits(original))
        val amount = AMOUNT.find(normalized)?.groupValues?.get(1)
            ?.replace(',', '.')
        val categoryMatch = keywords.entries
            .map { normalize(it.key) to it.value }
            .filter { (keyword, _) -> keyword.isNotBlank() && normalized.contains(keyword) }
            .maxByOrNull { it.first.length }
        val explicitIncome = INCOME_CUES.any { normalized.contains(normalize(it)) }
        val explicitExpense = EXPENSE_CUES.any { normalized.contains(normalize(it)) }
        val inferredType = when {
            explicitIncome && !explicitExpense -> "INCOME"
            else -> categoryMatch?.second?.transactionType ?: "EXPENSE"
        }
        val category = categoryMatch?.second?.takeIf { it.transactionType == inferredType }
        return ParsedEntry(
            amountText = amount,
            category = category,
            transactionType = inferredType,
            note = original
        )
    }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return decomposed
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace('ة', 'ه')
            .lowercase()
    }

    private fun toAsciiDigits(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            if (ch == '\u066C') return@forEach
            append(
                when (ch) {
                    in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                    in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                    '\u066B' -> '.'
                    else -> ch
                }
            )
        }
    }

    companion object {
        private val AMOUNT = Regex("""(?<![\p{L}\d])([0-9]+(?:[.,][0-9]{1,4})?)(?![\p{L}\d])""")

        private val INCOME_CUES = listOf("استلمت", "قبضت", "دخل لي", "وصلني", "تحويل وارد")
        private val EXPENSE_CUES = listOf("دفعت", "اشتريت", "صرف", "مصروف", "دفعت")

        val DEFAULT_KEYWORDS = buildMap {
            fun add(ar: String, en: String, type: String, vararg aliases: String) {
                val suggestion = CategorySuggestion(ar, en, type)
                (listOf(ar, en) + aliases).forEach { put(it, suggestion) }
            }
            add("طعام", "Food", "EXPENSE", "اكل", "أكل", "غداء", "فطار", "عشاء", "مطعم", "وجبة", "بيتزا", "برجر", "شاورما", "فول", "طعمية", "بقالة", "سوبرماركت")
            add("قهوة", "Coffee", "EXPENSE", "كوفي", "كافيه", "نسكافيه", "شاي", "ستاربكس")
            add("مواصلات", "Transport", "EXPENSE", "تاكسي", "اوبر", "أوبر", "كريم", "مترو", "اتوبيس", "أتوبيس", "ميكروباص")
            add("وقود", "Fuel", "EXPENSE", "بنزين", "سولار", "وقود")
            add("فواتير", "Bills", "EXPENSE", "كهرباء", "غاز", "مياه", "فاتورة", "نت", "انترنت", "إنترنت", "رصيد")
            add("إيجار", "Rent", "EXPENSE", "ايجار", "شقة")
            add("صحة", "Health", "EXPENSE", "دكتور", "صيدلية", "دواء", "تحاليل", "مستشفى", "عيادة")
            add("تعليم", "Education", "EXPENSE", "مدرسة", "جامعة", "كتب", "كورس", "دورة")
            add("تسوق", "Shopping", "EXPENSE", "هدوم", "ملابس", "جزمة", "حذاء", "شنطة", "امازون", "أمازون", "نون")
            add("ترفيه", "Entertainment", "EXPENSE", "سينما", "فيلم", "لعبة", "بلايستيشن", "نتفليكس", "شاهد", "خروجة", "فسحة")
            add("عائلة", "Family", "EXPENSE", "مصروف عائلة", "مصروف البيت")
            add("صدقة", "Charity", "EXPENSE", "زكاة", "تبرع")
            add("عمل", "Work", "EXPENSE", "شغل", "مكتب", "غداء عمل")
            add("راتب", "Salary", "INCOME", "مرتب", "راتب", "قبض")
            add("عمل حر", "Freelance", "INCOME", "فريلانس", "freelance")
            add("هدية", "Gift", "INCOME", "هدية استلمت")
        }
    }
}
