package com.creazzy.rofof.data.export

import com.creazzy.rofof.data.local.TransactionEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCsvExporterTest {
    @Test fun exportsArabicTextDecimalAndUtcTimestampWithBom() {
        val csv = TransactionCsvExporter.export(listOf(
            TransactionEntity("tx-1", "مشتريات أسبوعية", "EXPENSE", 12345L, "EGP", 2,
                "cash", "food", 1735689600000L, 1735689600100L, "سوق، خضار")
        ))
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"مشتريات أسبوعية\""))
        assertTrue(csv.contains("\"123.45\""))
        assertTrue(csv.contains("\"سوق، خضار\""))
        assertTrue(csv.contains("\"2025-01-01T00:00:00Z\""))
    }

    @Test fun quotesCommasAndEscapesEmbeddedQuotes() {
        val csv = TransactionCsvExporter.export(listOf(
            TransactionEntity("id", "Lunch, \"team\"", "EXPENSE", 100L, "EGP", 2,
                "cash", "food", 0L, 0L, "line one, line two")
        ))
        assertTrue(csv.contains("\"Lunch, \"\"team\"\"\""))
        assertTrue(csv.contains("\"line one, line two\""))
    }

    @Test fun neutralizesSpreadsheetFormulaPrefixesInTextFields() {
        val csv = TransactionCsvExporter.export(listOf(
            TransactionEntity("id", "=HYPERLINK(\"https://example.invalid\")", "EXPENSE", 100L,
                "EGP", 2, "cash", "food", 0L, 0L, "  @SUM(A1:A2)")
        ))
        assertTrue(csv.contains("""'=HYPERLINK(""https://example.invalid"")"""))
        assertTrue(csv.contains("\"'  @SUM(A1:A2)\""))
    }

    @Test fun emptyExportStillContainsHeader() {
        val csv = TransactionCsvExporter.export(emptyList())
        assertTrue(csv.startsWith("\uFEFF\"id\""))
        assertTrue(csv.lines().size >= 2)
    }
}
