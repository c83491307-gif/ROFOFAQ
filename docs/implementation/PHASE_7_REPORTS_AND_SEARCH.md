# Phase 7 — Reports and Transaction Search

## Scope
Adds a local monthly expense-by-category report and case-insensitive search across transaction titles and notes. The report filters by one explicit currency code/exponent and a half-open month interval; it never combines currencies.

## Implementation
- Read-only aggregate query joins ledger transactions to category labels.
- Search matches title or notes, orders newest first and caps results at 200.
- Bilingual reports/search screen is reachable from Settings.
- No schema migration or ledger mutation is required.

## Verification
Robolectric Room tests verify income and out-of-period expenses are excluded and search matches title/notes case-insensitively. CI must pass tests, lint and debug APK assembly on the exact branch head.

## Boundaries
The initial monthly report is EGP-only. Date-range controls, charts and advanced filtering remain follow-up work.
