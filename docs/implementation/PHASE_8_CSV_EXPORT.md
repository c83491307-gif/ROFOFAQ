# Phase 8 — Transaction CSV Export

## Scope
Adds a user-initiated local export of all non-deleted ledger income/expense transactions through Android Storage Access Framework.

## Data contract
- UTF-8 with BOM for Arabic compatibility.
- Stable columns include transaction ID, title, type, decimal amount, currency/exponent, account/category IDs, UTC occurrence/creation timestamps and notes.
- Integer minor units are formatted using each transaction's stored currency exponent; currencies are never combined.
- CSV fields are quoted and embedded quotes doubled.
- Text cells beginning with spreadsheet formula prefixes (=, +, - or @ after leading whitespace) are prefixed with an apostrophe.
- Export is read-only and user-initiated; no storage permission or network upload is required.

## Verification
Unit tests cover Arabic text, UTF-8 BOM, decimal formatting, UTC timestamps, commas, embedded quotes, formula-prefix neutralization and empty exports.

## Limitations
This stage exports ledger transactions only. Shared purchase sessions, budgets, goals, transfers and attachments are separate export scopes.
