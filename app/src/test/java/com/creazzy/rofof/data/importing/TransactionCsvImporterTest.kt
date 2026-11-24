package com.creazzy.rofof.data.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCsvImporterTest {
    private val header = "id,title,type,amount,currency,currency_exponent,account_id,category_id,occurred_at_utc,created_at_utc,notes"

    @Test fun parsesQuotedCommaAndEscapedQuote() {
        val csv = "\uFEFF$header\r\n1,\"Lunch, today\",EXPENSE,12.50,EGP,2,cash,food,2026-01-01T10:00:00Z,2026-01-01T10:01:00Z,\"said \"\"hello\"\"\""
        val result = TransactionCsvImporter.parse(csv)
        assertTrue(result.errors.toString(), result.isValid)
        assertEquals("Lunch, today", result.transactions.single().title)
        assertEquals("said \"hello\"", result.transactions.single().notes)
        assertEquals(1250L, result.transactions.single().amountMinor)
    }

    @Test fun rejectsDuplicateIdsAndDoesNotReturnPartialImport() {
        val row = "1,Taxi,EXPENSE,5.00,EGP,2,cash,transport,2026-01-01T10:00:00Z,2026-01-01T10:01:00Z,"
        val result = TransactionCsvImporter.parse("$header\n$row\n$row")
        assertFalse(result.isValid)
        assertTrue(result.transactions.isEmpty())
        assertEquals(1, result.errors.size)
    }

    @Test fun rejectsMalformedHeaderAndInvalidCurrency() {
        assertFalse(TransactionCsvImporter.parse("id,title\n1,test").isValid)
        val row = "1,Taxi,EXPENSE,5.00,EG,2,cash,transport,2026-01-01T10:00:00Z,2026-01-01T10:01:00Z,"
        assertFalse(TransactionCsvImporter.parse("$header\n$row").isValid)
    }

    @Test fun rejectsStrayQuoteAndCharactersAfterClosingQuote() {
        val stray = "1,Lu\"nch,EXPENSE,5.00,EGP,2,cash,food,2026-01-01T10:00:00Z,2026-01-01T10:01:00Z,"
        val trailing = "1,\"Lunch\"oops,EXPENSE,5.00,EGP,2,cash,food,2026-01-01T10:00:00Z,2026-01-01T10:01:00Z,"
        assertFalse(TransactionCsvImporter.parse("$header\n$stray").isValid)
        assertFalse(TransactionCsvImporter.parse("$header\n$trailing").isValid)
    }

    @Test fun rejectsFormulaLikeAmountAndInvalidTimestamp() {
        val formula = "1,Taxi,EXPENSE,=1+1,EGP,2,cash,transport,2026-01-01T10:00:00Z,2026-01-01T10:01:00Z,"
        assertFalse(TransactionCsvImporter.parse("$header\n$formula").isValid)
        val badDate = "1,Taxi,EXPENSE,5.00,EGP,2,cash,transport,not-a-date,2026-01-01T10:01:00Z,"
        assertFalse(TransactionCsvImporter.parse("$header\n$badDate").isValid)
    }
}
