# Phase 9 — Recurring Draft Review and Approval

## Scope
Adds an explicit review action that converts one pending recurring draft into one ordinary ledger transaction, plus a skip action.

## Transactional invariants
- Approval executes in one Room database transaction.
- The draft must still be in DRAFT status.
- Amount must be positive and transaction type must be INCOME or EXPENSE.
- The referenced account must exist, be active and match the stored currency code/exponent.
- The category must exist, be active and match the transaction type.
- The ledger transaction ID is deterministic: recurring-draft:<draftId>. A second approval cannot create a duplicate.
- The transaction occurrence time is the scheduled due time; creation time is the approval time.
- Draft status changes to POSTED only after the transaction insert succeeds. Any failure rolls back both operations.
- Skipping is a guarded DRAFT→SKIPPED transition and cannot later be approved.

## Verification
Robolectric tests cover atomic posting, status transition, repeated approval rejection, archived-account rejection and skip behavior. CI must pass tests, lint and debug APK assembly on the exact commit.

## Boundaries
This stage handles approval of generated drafts. It does not create automatic background posting, notifications, recurring rule editing, or recurring income UI.
