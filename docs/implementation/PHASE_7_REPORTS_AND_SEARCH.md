# Phase 7 — Reports and Transaction Search

## Scope
Adds a local monthly expense-by-category report and case-insensitive text search across transaction titles and notes. The report filters by one explicit currency code/exponent and a half-open month interval; it never combines currencies.

## Implementation
- Read-only aggregate query joins ledger transactions to category labels.
- Search matches title or notes, orders newest first and caps results at 200.
- A bilingual reports/search destination is added to the navigation shell.
- No schema migration or ledger mutation is required.

## Verification
Robolectric Room tests verify that reports exclude income and out-of-period expenses and that search matches title/notes case-insensitively. CI must pass tests, lint and debug APK assembly on the exact branch head.

## Boundaries
This initial report is EGP-only. Cash-flow forecasting, date-range controls, account/type filters, charts, export and historical category-name snapshots remain follow-up work.
