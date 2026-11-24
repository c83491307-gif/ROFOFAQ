# Phase 5 — Budgets and Financial Goals

## Scope

This phase adds persistent monthly category budgets and user-maintained financial savings goals without changing transaction, account, or shared-purchase records.

## Storage and migration

- Room schema advances from version 5 to version 6.
- Migration 5→6 creates `budgets` and `financial_goals` with explicit indexes.
- Budgets store a category, currency/exponent, integer minor-unit limit, and an explicit half-open period [start, end).
- A unique index prevents duplicate budgets for the same category, currency, and period start.
- Goals store a target and a separately maintained saved amount in integer minor units, optional deadline, archive state, and timestamps.

## Behavior

- The current-month budget view derives actual spending from ledger expense transactions matching category, currency, exponent, and the budget's period boundaries. Income, other categories, and out-of-period entries are excluded.
- Remaining amount and over-budget state are derived; budget values do not mutate ledger transactions.
- Budget limits can be edited or deleted.
- Financial goals can be created with an optional ISO date deadline, have their saved amount updated, and can be archived without deleting history.
- Currency exponent validation uses ISO currency metadata. Amount conversion uses `BigDecimal` and exact integer minor units; floating-point arithmetic is not used for persisted money.
- The feature is available from a dedicated bilingual Arabic/English screen, with RTL/LTR layout direction.

## Verification

Robolectric Room repository tests cover period/category/type filtering, duplicate budget prevention, invalid category and amount rejection, goal progress validation, archive retention, and version-6 table creation.

## Boundaries

Goal progress is manually maintained in this phase; it is not inferred from account balances or transaction descriptions. Budgets cover the current month in the UI; historical budget periods remain stored and are queryable by period start. No automatic transfers, savings movements, or transaction creation are performed by budget/goal actions.
