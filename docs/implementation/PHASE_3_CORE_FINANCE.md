# ROFOF — Priority 1: Accounts, Categories and Transfers

**Status:** Implementation committed; latest-head CI and emulator verification pending.  
**Branch:** `feat/rofof-android-foundation`  
**Database:** Room schema version 4.

## Scope delivered

- Multiple local accounts with name, ISO currency code, currency exponent, opening balance and archive state.
- Live per-account balance projection:
  opening balance + income − expenses + incoming transfers − outgoing transfers.
- Transaction creation now selects an account and derives currency metadata from that account.
- Posting invariants reject archived/missing accounts, currency/exponent mismatch, missing/archived categories, category type mismatch, non-positive amounts and invalid transaction types.
- Same-currency account transfers stored in a dedicated `transfers` table, inserted inside a Room transaction.
- Transfers reject same-account, non-positive, archived/missing and cross-currency operations.
- Transfers affect the two account balances but are not written as income or expenses.
- Income and expense category management with a maximum of two levels and matching parent/child transaction types.
- Account archival is blocked while transactions or transfers reference the account; default cash account cannot be archived.
- Category archival is blocked while children or transactions remain.
- Dashboard account, transfer and category dialogs are connected to Room-backed state.
- Dashboard income/expense totals now use full-history, currency-and-exponent-scoped SQL aggregates rather than the recent 250-row list.
- Migration 3 → 4 creates the transfer ledger and supporting indexes.
- Instrumentation tests cover transfer balance effects, transfer exclusion from income/expense totals, cross-currency rejection, two-level category rules, safe account archival, full-history currency-scoped totals and transaction posting invariants.

## Financial integrity rules

- Monetary amounts are integer minor units.
- New account currency metadata must match Java's ISO currency metadata.
- Transfers require matching currency code and exponent; currency exchange is deliberately not simulated.
- No floating-point arithmetic is used for money.
- Transfers are independent ledger events, not income/expense transactions.
- Opening balances are included in per-account balances and the EGP dashboard net balance.
- Historical migration 2 → 3 remains intact; migration 3 → 4 adds transfers without rewriting existing transaction rows.

## Still pending in Priority 1

- Account rename/archive controls in the UI (domain operations are implemented).
- Category rename/archive controls in the UI (domain operations are implemented).
- Full migration-upgrade instrumentation from an on-disk v3 database.
- UX error feedback for invalid account/category/transfer submissions.
- UI tests for account selection and transfer/category dialogs.

## Verification gate

No successful latest-head CI result is claimed in this report. Required checks:
1. JVM unit tests.
2. Android lint.
3. Debug APK assembly.
4. Emulator-backed Room instrumentation tests.

No merge or deployment has been performed.
