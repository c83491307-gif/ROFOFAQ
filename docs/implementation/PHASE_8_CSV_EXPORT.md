# Phase 8 — Transaction CSV Export

## Scope
Adds a user-initiated local export of all ledger income/expense transactions to a CSV file through Android's Storage Access Framework.

## Data contract
- UTF-8 with BOM for Arabic compatibility in spreadsheet applications.
- Stable columns: transaction ID, title, type, decimal amount, currency, currency exponent, account/category IDs, UTC occurrence/creation timestamps and notes.
- Integer minor units are converted using the stored currency exponent; currencies are never combined.
- CSV fields are quoted and embedded quotes doubled.
- Text cells beginning with spreadsheet formula prefixes (=, +, - or @ after leading whitespace) are prefixed with an apostrophe to mitigate CSV formula injection.
- Export is read-only and remains on-device. The user selects the destination; no storage permission or network upload is required.

## Verification
Unit tests cover Arabic UTF-8 BOM output, decimal formatting, UTC timestamps, quoting, embedded quotes, formula-prefix neutralization and empty datasets. CI must pass unit/Robolectric tests, lint and debug APK assembly on the exact commit.

## Limitations
This stage exports ledger transactions only. It does not yet export shared purchase sessions, budgets, goals, transfers, recurring rules or attachments. CSV import and full database backup/restore are separate stages.
