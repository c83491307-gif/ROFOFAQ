# README Requirements — Applied Android Plan

## Source and authority

This plan preserves the original product intent from the repository's initial README while applying the accepted architecture decision: native Android first, Kotlin + Jetpack Compose + Room, offline-first, with backend/sync deferred and disabled.

The current integration-branch implementation status is summarized here. Source presence is not equivalent to a successful latest-head CI run; see the phase reports and GitHub Actions for verification evidence.

## Architecture resolution

- Native Android is the current client; the original Flutter/mobile and backend sketches are not the current implementation plan.
- Room/SQLite is authoritative for device-local state.
- Core flows do not require login, backend, banking sync, cloud storage or network access.
- Money is integer minor units with ISO currency code and exponent. Currency totals are never combined across currencies without an explicit conversion operation.
- Web/PWA is a later optional client, not a prerequisite for Android delivery.
- OCR and recurring rules produce reviewable drafts, not silently posted transactions.

## Original README roadmap mapped to current integration-branch source

| README area | Current source delivery | Status / boundary |
|---|---|---|
| Quick transaction | Room-backed income/expense ledger, account/category selection, totals and confirmed deletion | Implemented core flow; advanced edit/date/receipt workflows remain follow-up work |
| Multiple accounts | Local accounts, opening balances, selected-account transactions and live balances | Implemented |
| Categories/subcategories | Income/expense categories, two-level category flow, rename/archive and historical retention | Implemented |
| Transfers | Atomic same-currency account transfers, excluded from income/expense totals | Implemented; cross-currency transfer is rejected |
| Recurring/templates | Rules generate reviewable drafts; Phase 9 provides guarded atomic approve/skip actions | Implemented; no automatic posting |
| Splits/shared purchases | Shared purchase sessions, participants, exact allocations, payments, settlement and explicit personal-expense source links | Implemented; see Phase 4 report |
| Receipt images / tags / location | Optional local attachments and metadata | Not part of the current delivered scope |
| Budgets/goals | Local category budgets, manually maintained savings goals and budget-overrun notifications | Implemented; no bank sync |
| Loans/debts | Local debt entities, repository and management screen | Implemented source foundation; review the debt phase/source tests before release claims |
| Alerts/reports | Local budget alerts and monthly category report | Implemented; initial monthly category report is EGP-only; cash-flow/forecast UI remains follow-up |
| Search | Case-insensitive transaction title/notes search | Implemented; result set is capped at 200 |
| CSV export | User-initiated SAF export, UTF-8 BOM, currency metadata, UTC timestamps, CSV quoting and formula-prefix neutralization | Implemented for non-deleted ledger transactions only |
| CSV import | Parser/import service and tests are present | End-user preview, validation and explicit commit integration must be verified before marking the complete import workflow done |
| Backup/restore | Local backup archive foundation and tests are present | Full validated restore/recovery workflow remains a gate; never overwrite the live database before candidate validation |
| Banking sync / multi-user | Optional future extension only | Deferred; never enabled implicitly |
| Security | Biometric dependency exists; app-lock and database encryption are not yet delivered as a complete security workflow | Pending |
| Web/PWA | Separate later client using an explicit optional API boundary | Deferred |
| Dark/light and localization | Arabic/English and RTL/LTR dashboard and feature screens | Implemented for current screens; audit new screens before release |
| Receipt OCR / AI charts | Optional future feature | Deferred; OCR must remain reviewable and must not auto-post financial transactions |

## Non-negotiable data rules

1. Shared purchase allocations are not personal expenses until the user explicitly confirms a transfer.
2. Transfers between accounts are not income or expenses.
3. Recurring rules and OCR create drafts, not posted transactions.
4. Every amount stores integer minor units plus currency code/exponent.
5. No implicit cross-currency netting.
6. Historical records retain the category/account labels needed to interpret past activity.
7. Destructive restore operations require staged validation, integrity checks and a recoverable rollback path.

## Verification evidence

- [Phase 7 — Reports and Transaction Search](../implementation/PHASE_7_REPORTS_AND_SEARCH.md)
- [Phase 8 — Transaction CSV Export](../implementation/PHASE_8_CSV_EXPORT.md)
- [Phase 9 — Recurring Draft Review and Approval](../implementation/PHASE_9_RECURRING_APPROVAL.md)
- [Architecture decisions](../architecture/DECISIONS.md)
- [APK build and branch integration](../implementation/APK_BUILD_AND_BRANCH_INTEGRATION.md)

These reports define the feature boundaries. A successful test/build on the exact current branch head is still required before release or merge approval.
