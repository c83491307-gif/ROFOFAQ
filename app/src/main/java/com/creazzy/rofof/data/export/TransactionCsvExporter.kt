package com.creazzy.rofof.data.export

import com.creazzy.rofof.data.local.TransactionEntity
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

object TransactionCsvExporter {
    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT

    fun export(transactions: List<TransactionEntity>): String = buildString {
        append('\uFEFF')
        appendLine(listOf("id", "title", "type", "amount", "currency", "currency_exponent", "account_id", "category_id", "occurred_at_utc", "created_at_utc", "notes").joinToString(",") { cell(it) })
        transactions.forEach { transaction ->
            val amount = BigDecimal.valueOf(transaction.amountMinor, transaction.currencyExponent).toPlainString()
            val values = listOf(
                transaction.id, transaction.title, transaction.transactionType, amount,
                transaction.currencyCode, transaction.currencyExponent.toString(),
                transaction.accountId, transaction.categoryId,
                timestampFormatter.format(Instant.ofEpochMilli(transaction.occurredAtEpochMillis)),
                timestampFormatter.format(Instant.ofEpochMilli(transaction.createdAtEpochMillis)),
                transaction.notes.orEmpty()
            )
            appendLine(values.joinToString(",") { cell(it) })
        }
    }

    private fun cell(raw: String): String {
        val first = raw.trimStart().firstOrNull()
        val safe = if (first == '=' || first == '+' || first == '-' || first == '@') "'$raw" else raw
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
