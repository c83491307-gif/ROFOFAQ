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
CI must run Room/Robolectric unit tests, lint and debug APK assembly on the exact commit. A version-6→7 migration test and recurrence boundary tests remain required before this migration is considered fully verified.
