# ROFOF — Priority 2: Shared Purchases

**Status:** First local-first vertical slice implemented; verification pending.  
**Branch:** `feat/rofof-shared-purchases`  
**Database:** Room schema version 5 (migration 4 → 5).

## Implemented source scope

- Purchase sessions with explicit ISO currency metadata and OPEN/SETTLED state.
- Participants are scoped to one session; participant names are unique within that session.
- Items store quantity at 1/1000 precision, unit price in integer minor units and a persisted line total.
- Line totals use integer arithmetic and deterministic half-up rounding; overflow is rejected.
- Per-item allocations are explicit per participant. The sum must equal the item total exactly; no equal-share assumption is made.
- Actual payments are stored separately from expected allocations. Positive amounts and unique idempotency keys are required.
- Participant settlement rows expose allocated, paid and remaining/credit amounts. Overpayments remain visible as negative balances.
- Closing a purchase requires every item to be fully allocated and every positive outstanding balance to be paid.
- An allocation can be explicitly added to My Expenses once. This creates a normal expense transaction and a unique source link in the same Room transaction, with account/category and currency validation.
- Shared purchase records are not automatically inserted into the personal ledger.

## Data integrity

- Room foreign keys protect session, participant, item, allocation, payment and source-link relationships.
- Unique indexes prevent duplicate participant names within a session, duplicate participant allocation per item, duplicate payment idempotency keys and duplicate personal-expense links.
- Multi-record workflows use Room transactions.
- Money is integer minor units; no floating-point arithmetic or implicit cross-currency conversion is used.
- Core features remain fully local. No backend, login or network dependency is introduced.

## UI

- Shared purchases are reachable from the main app navigation.
- Users can create a purchase, add participants and line items, allocate item totals, record payments, review balances, close a settled session and explicitly link an allocation to My Expenses.
- Personal-expense linking requires the user to choose a matching-currency account and an active expense category.

## Verification gate

The branch CI must pass on its exact latest commit:
1. JVM unit tests, including line-total rounding, overflow and allocation exactness.
2. Android lint.
3. Debug APK assembly.
4. Robolectric Room tests for participant balances, exact allocation, payment idempotency, settlement gating and explicit personal-expense linking.

A Robolectric upgrade-path test creates a physical SQLite file at user_version 4 with legacy tables, indexes and representative rows, then opens it through Room with MIGRATION_4_5. Room's generated schema validator and explicit assertions verify v5 tables/indexes, preserved legacy rows, foreign-key declarations, no foreign-key violations and unique-index enforcement. No merge, release APK or deployment is claimed until verification evidence is available.
