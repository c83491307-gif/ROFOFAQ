# Phase 9 — Recurring Draft Review and Approval

## Scope
Adds explicit review actions that convert a pending recurring draft into one ordinary ledger transaction, or skip it.

## Transactional invariants
- Approval runs in one Room transaction.
- Only a DRAFT can be approved; amount must be positive and type must be INCOME or EXPENSE.
- Referenced account and category must exist, be active, and match the stored currency/type.
- Deterministic transaction ID `recurring-draft:<draftId>` prevents duplicate posting.
- The transaction uses the scheduled due time and records the approval time as creation time.
- The draft becomes POSTED only after insert succeeds; failures roll back both changes.
- Skip is a guarded DRAFT→SKIPPED transition and cannot later be approved.

## Verification
Robolectric tests cover atomic posting, duplicate rejection, archived-account rejection and skip behavior. CI must pass on the exact branch head.

## Boundaries
Recurring rules continue to generate reviewable drafts; no automatic background posting is enabled.
