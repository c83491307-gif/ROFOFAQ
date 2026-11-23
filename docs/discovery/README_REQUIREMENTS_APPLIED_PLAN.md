# README Requirements — Applied Android Plan

## Source
This plan is derived from the repository's original `main/README.md` roadmap. It preserves its product intent while applying the user's later architecture decision: native Android first, Kotlin + Jetpack Compose + Room, offline-first, with backend/sync deferred and disabled.

## Architecture resolution
- Android native replaces the README's alternative Flutter/mobile proposal for the current delivery.
- Room/SQLite is the authoritative source for device-local data.
- No login, backend, banking sync, cloud storage or INTERNET permission is required for core flows.
- Money is integer minor units with ISO currency code and exponent. Currency totals are never combined across currencies without an explicit conversion operation.
- Web/PWA is a later client, not a prerequisite for Android delivery.

## Original README roadmap mapped to delivery

| README area | Applied Android implementation plan | Status |
|---|---|---|
| Quick transaction | Create/read/update/delete dated income and expense transactions with amount and category | Partial: add/read/delete expense exists; income/edit/date picker pending |
| Multiple accounts | Cash, card, savings and custom accounts with independent balances | Implemented: create local accounts, opening balances, selected-account transactions and live per-account balance projection |
| Categories/subcategories | Local editable two-level category tree | Implemented: income/expense categories and two-level create flow; rename/archive UI pending |
| Transfers | Atomic account-to-account transfer, excluded from income/expense totals | Implemented: same-currency transfer table, transactional insert, paired balance impact and currency mismatch rejection |
| Recurring/templates | Rules create reviewable drafts; no silent posting | Pending |
| Splits | Exact minor-unit split with deterministic visible remainder | Pending |
| Receipt images | User-selected local attachments, optional | Pending |
| Tags/location | Optional local metadata; location disabled unless explicitly enabled | Pending |
| Budgets/goals/loans | Local budgets, savings goals and debt/loan ledgers | Pending |
| Alerts/reports | Local threshold alerts, category/monthly charts, cash flow, net worth, forecasts | Pending |
| Search/import/export | Search and CSV import/export first; other formats only after parser validation | Pending |
| Banking sync / multi-user | Optional future extension only; never enabled implicitly | Deferred |
| Security | App PIN/biometric lock and encrypted local data, designed without weakening offline core | Pending |
| Web/PWA | Separate later client using explicit optional API boundary | Deferred |
| Dark/light and localization | Arabic/English, RTL/LTR, light/dark/system | Partial: Arabic/English and RTL/LTR dashboard exists |
| Receipt OCR / AI charts | Optional; OCR output remains a draft until human confirmation | Pending |

## Non-negotiable data rules
1. Shared purchase allocations are not personal expenses until the user explicitly confirms a transfer.
2. Transfers between accounts are not income or expenses.
3. Recurring rules and OCR create drafts, not posted transactions.
4. Every amount stores integer minor units plus currency code/exponent.
5. No implicit cross-currency netting.
6. Historical records retain the category/account labels needed to interpret past activity.
7. Destructive actions require confirmation; backup/restore must be validated before replacing local data.

## Current verified scope
The existing implementation has a Room-backed personal expense vertical slice and bilingual dashboard. It is not yet the complete README roadmap. No build/test success is claimed for the latest UI branch until CI completes on its exact head commit.

## Delivery order
1. Transaction ledger, accounts and categories.
2. Shared purchases, participants, exact allocations and settlement.
3. Budgets, goals, recurring drafts and reports.
4. Import/export, backup/restore and security hardening.
5. Optional OCR and optional web/sync adapters.
