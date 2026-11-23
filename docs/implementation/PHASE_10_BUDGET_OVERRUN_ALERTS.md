# Phase 10 — Budget overrun alerts

## Delivered

- Added a Room projection that evaluates a budget only for the expense category, ISO currency code/exponent, and half-open period containing the changed transaction.
- Added a local Android notification channel and Arabic/English notification copy.
- Added a contextual permission control in the Budgets & Goals screen. Android 13+ requests POST_NOTIFICATIONS only after the user taps Enable alerts; if app notifications are disabled in system settings, the control opens the app notification settings.
- Triggered reevaluation after adding, editing, and deleting ledger transactions, and after creating or changing a budget. Editing a transaction refreshes both the old and new category/period so the alert state can be rearmed correctly.
- Suppressed duplicate notifications per budget and period using app-local preferences. If spending returns to or below the limit, the alert is rearmed for a later overrun.
- Kept all monetary comparisons in integer minor units. Exact-limit spending is not an overrun; the condition is strictly spentMinor > amountMinor.
- No Room schema migration, cloud dependency, storage permission, merge, or deployment was introduced.

## Behavior and boundaries

- Only EXPENSE transactions contribute.
- Matching requires category ID, currency code, currency exponent, and transaction occurrence within the budget's [periodStart, periodEnd) interval.
- Notification delivery requires Android notification permission and app-level notifications to be enabled. If permission is denied, the expense remains saved and no notification is marked as delivered; a later qualifying expense can retry.
- The feature is event-driven by dashboard ledger add/edit/delete and budget create/update operations. It is not a background scheduler and does not guarantee delivery while the app is not running or for writes performed by independent workflows that do not invoke this notifier.
- Existing budgets, goals, recurring records, and transaction history are retained.

## Tests added

- Room query isolates category, currency, and period boundaries.
- Strict overrun threshold: equal-to-limit is not an overrun; one minor unit above is.
- Duplicate suppression and rearming after spending returns to the limit.
- Non-positive limits never create alerts.

## Verification gate

Run the repository Android workflow and require all of the following on this exact branch head:

1. testDebugUnitTest (including Robolectric Room tests)
2. lintDebug
3. assembleDebug
4. Upload of verification artifacts

Do not mark the phase complete until the workflow reports success. This branch is independent and remains unmerged and undeployed.
