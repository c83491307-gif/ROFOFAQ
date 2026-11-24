package com.creazzy.rofof.data.importing

import com.creazzy.rofof.data.local.TransactionEntity
import java.math.BigDecimal
import java.time.Instant

/**
 * Strict parser for ROFOF's own CSV export format.
 * Parsing is side-effect free: callers must preview and explicitly confirm before persistence.
 */
object TransactionCsvImporter {
    private val expectedHeader = listOf(
        "id", "title", "type", "amount", "currency", "currency_exponent",
        "account_id", "category_id", "occurred_at_utc", "created_at_utc", "notes"
    )

    data class ImportResult(
        val transactions: List<TransactionEntity>,
        val errors: List<RowError>
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    data class RowError(val row: Int, val message: String)

    fun parse(csv: String): ImportResult {
        val rows = runCatching { readRows(csv.removePrefix("\uFEFF")) }
            .getOrElse { return ImportResult(emptyList(), listOf(RowError(1, it.message ?: "Malformed CSV"))) }
        if (rows.isEmpty()) return ImportResult(emptyList(), listOf(RowError(1, "CSV is empty")))
        if (rows.first() != expectedHeader) {
            return ImportResult(emptyList(), listOf(RowError(1, "Unsupported CSV header; expected ROFOF export columns")))
        }

        val transactions = mutableListOf<TransactionEntity>()
        val errors = mutableListOf<RowError>()
        val seenIds = mutableSetOf<String>()
        rows.drop(1).forEachIndexed { index, cells ->
            val rowNumber = index + 2
            if (cells.size != expectedHeader.size) {
                errors += RowError(rowNumber, "Expected ${expectedHeader.size} columns, found ${cells.size}")
                return@forEachIndexed
            }
            runCatching {
                val id = cells[0].trim()
                require(id.isNotEmpty()) { "Transaction id is required" }
                require(seenIds.add(id)) { "Duplicate transaction id in file" }
                val title = cells[1]
                require(title.isNotBlank()) { "Title is required" }
                val type = cells[2].trim().uppercase()
                require(type == "INCOME" || type == "EXPENSE") { "Type must be INCOME or EXPENSE" }
                val exponent = cells[5].toInt()
                require(exponent in 0..4) { "Currency exponent is outside supported range" }
                val amount = BigDecimal(cells[3]).movePointRight(exponent).longValueExact()
                require(amount > 0L) { "Amount must be positive" }
                val currency = cells[4].trim().uppercase()
                require(currency.matches(Regex("[A-Z]{3}"))) { "Currency must be a three-letter ISO code" }
                val accountId = cells[6].trim()
                val categoryId = cells[7].trim()
                require(accountId.isNotEmpty() && categoryId.isNotEmpty()) { "Account and category IDs are required" }
                val occurredAt = Instant.parse(cells[8]).toEpochMilli()
                val createdAt = Instant.parse(cells[9]).toEpochMilli()
                TransactionEntity(
                    id = id, title = title, transactionType = type, amountMinor = amount,
                    currencyCode = currency, currencyExponent = exponent, accountId = accountId,
                    categoryId = categoryId, occurredAtEpochMillis = occurredAt,
                    createdAtEpochMillis = createdAt, notes = cells[10].ifEmpty { null }
                )
            }.onSuccess { transactions += it }
                .onFailure { errors += RowError(rowNumber, it.message ?: "Invalid row") }
        }
        return ImportResult(if (errors.isEmpty()) transactions else emptyList(), errors)
    }

    /** RFC-4180 style comma-separated records, including escaped quotes and embedded newlines. */
    private fun readRows(input: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                quoted && ch == '"' && i + 1 < input.length && input[i + 1] == '"' -> {
                    cell.append('"'); i++
                }
                ch == '"' -> quoted = !quoted
                !quoted && ch == ',' -> { row += cell.toString(); cell.setLength(0) }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    row += cell.toString(); cell.setLength(0)
                    if (row.any { it.isNotEmpty() }) rows += row
                    row = mutableListOf()
                    if (ch == '\r' && i + 1 < input.length && input[i + 1] == '\n') i++
                }
                else -> cell.append(ch)
            }
            i++
        }
        require(!quoted) { "Unclosed quoted field" }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            if (row.any { it.isNotEmpty() }) rows += row
        }
        return rows
    }
}
