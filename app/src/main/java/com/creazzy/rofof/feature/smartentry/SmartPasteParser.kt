package com.creazzy.rofof.feature.smartentry

import java.text.Normalizer

data class SmartPasteSuggestion(
    val amountText: String,
    val merchantText: String?,
    val parsedEntry: SmartEntryParser.ParsedEntry,
    val sourcePreview: String
)

/** Pure parser. Clipboard access belongs to an explicit user-initiated paste action in the UI. */
object SmartPasteParser {
    private val amountPattern = Regex("""(?<![\p{L}\d])([0-9٠-٩۰-۹]+(?:[.,٫][0-9٠-٩۰-۹]{1,4})?)(?![\p{L}\d])""")
    private val bankCues = listOf("تم خصم", "تم سحب", "purchase", "charged", "debited", "خصم", "سحب")

    fun parse(text: String, parser: SmartEntryParser = SmartEntryParser()): SmartPasteSuggestion? {
        val clean = text.trim().take(2_000)
        if (clean.isBlank()) return null
        val amount = amountPattern.find(clean)?.groupValues?.get(1) ?: return null
        val normalized = Normalizer.normalize(clean, Normalizer.Form.NFKC).lowercase()
        if (bankCues.none { normalized.contains(it) }) return null
        val merchant = clean.replace(amount, " ")
            .replace(Regex("(?i)تم خصم|تم سحب|من حسابك|من حساب|لدى|شراء|خصم|سحب|جنيه|جنيهات|egp|ج\\\\.م"), " ")
            .replace(Regex("\\s+"), " ").trim().takeIf { it.length in 2..80 }
        val parsed = parser.parse(clean)
        return SmartPasteSuggestion(amount, merchant, parsed.copy(amountText = amount), clean.take(180))
    }
}
