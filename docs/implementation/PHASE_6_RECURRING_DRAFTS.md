# Phase 6 — Recurring Rules and Reviewable Drafts

## Scope
Adds local monthly expense templates and a user-triggered due-occurrence generator. Generated occurrences are drafts only; this phase never posts them into the personal ledger automatically.

## Storage
- Room advances from version 6 to 7.
- Migration 6→7 creates `recurring_rules` and `recurring_drafts`.
- Draft uniqueness is enforced by (ruleId, scheduledForEpochMillis), making generation idempotent.
- Amounts remain integer minor units with explicit currency code and exponent.

## Behavior and safety
- The initial UI creates monthly expense rules against the default EGP cash account and General expense category.
- A separate action creates due drafts. Each generated draft snapshots title, amount, currency, account and category.
- Missed monthly occurrences are generated up to a bounded catch-up limit; month-end dates clamp to the target month's last day.
- Rule advancement and draft creation share one Room transaction.
- Generated drafts are visibly marked DRAFT and are not inserted into `transactions`.
- Custom cadence, editing/pausing, draft approval/posting, income rules, and notifications remain follow-up work.

## Verification
- Robolectric Room tests cover rule/draft persistence, unique occurrence enforcement, draft-only behavior (no ledger transaction is created), and guarded draft status transitions.
- The existing Room schema assertion now verifies version 7 and the new tables.
- CI must run unit/Robolectric tests, lint and debug APK assembly on the exact commit.
- A real on-disk version-6→7 upgrade-path test and automated recurrence boundary/catch-up tests remain required before migration and scheduler behavior are considered fully verified.
