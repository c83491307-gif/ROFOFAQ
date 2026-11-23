# ROFOF — README Application and Core Ledger

**Status:** Source changes committed; latest-head CI verification is still required.  
**Branch:** `feat/rofof-android-foundation`  
**Reference:** Original `main/README.md` product roadmap, adapted to the approved native Android local-first architecture.

## Applied from the original README

- Preserved the daily personal-finance purpose and the roadmap categories.
- Applied native Android with Kotlin, Jetpack Compose and Room instead of introducing the README's alternative Flutter/PWA/backend stacks.
- Kept Room as the device-local source of truth; no account, server, banking sync or internet dependency was added.
- Retained Arabic/English UI and RTL/LTR switching.
- Expanded the personal-expense-only screen into a local income/expense ledger.
- Added a typed transaction model, accounts and categories tables, and a Room repository boundary.
- Added a schema migration from version 2 to version 3.
- Migrates previously stored expenses into the new transaction ledger, retaining the transaction ID, amount, currency code/exponent, timestamps and original category labels.
- Preserves separate cash account records for each legacy currency, rather than assigning foreign-currency records to the EGP account.
- Dashboard now shows income, expenses and net balance, and allows adding income or expense transactions.
- Added explicit delete confirmation.
- Added DAO instrumentation tests for transaction ordering, persistence, deletion and currency-scoped totals.

## Data integrity choices

- Amounts remain integer minor units with explicit currency metadata.
- Income and expenses are separate transaction types.
- Aggregates are scoped to currency; the dashboard currently displays the EGP default account and should not be interpreted as a cross-currency consolidated balance.
- Legacy category labels are preserved during migration.
- Existing expense rows remain in the legacy table as a migration source; new UI writes to the canonical transactions table.
- Account/category management, transfers and account-aware balances are still pending.

## README roadmap status

Implemented in source:
- Android local-first foundation.
- Arabic/English and RTL/LTR dashboard.
- Income/expense transaction add, recent-list, totals and confirmed delete.
- Default local cash account and typed categories.
- Migration of prior expenses into the transaction ledger.

Still pending:
- User-managed multiple accounts, transfers and account balances.
- Category CRUD, subcategories, tags, splits, recurring templates and receipt attachments.
- Budgets, savings goals, loans and alerts.
- Reports, cash-flow charts, net worth and forecasting.
- Advanced search, CSV import/export and backup/restore.
- App lock, encryption hardening and optional multi-user capability.
- Optional OCR/AI workflows and future web/PWA/sync clients.

## Verification

The earlier foundation run passed JVM unit tests, lint and debug APK assembly, but its emulator-backed test step was cancelled. No completed CI run for the latest ledger commit is available yet. The new transaction instrumentation tests and the migration must pass on the latest head before this phase can be marked verified.

No merge or deployment has been performed.
