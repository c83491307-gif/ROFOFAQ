# ROFOF Phase 2 — Bilingual Android Dashboard and Local Expenses

**Status:** Source implementation committed on an isolated work branch; Android CI verification pending.  
**Branch:** `work/rofof-dashboard-ui`  
**Architecture:** Android local-first; Room remains the local source of truth.

## Delivered

- Replaced the launcher placeholder with a Material 3 expense dashboard.
- Added Arabic and English UI copy with an in-app language switch.
- Applies RTL for Arabic and LTR for English through Compose layout direction.
- Persists the selected language in the existing Room local-preferences table.
- Added a Room `expenses` table and DAO with newest-first observation, insertion, deletion and total-since query.
- Added a repository contract and Room-backed implementation.
- Added an AndroidViewModel that exposes persisted expense records and handles local writes.
- Added an add-expense dialog and recent-expenses list; deletion removes the selected persisted record.
- Added EGP amount parsing using `BigDecimal` and integer minor units; UI formatting also uses `BigDecimal`, not floating-point arithmetic.
- Added a Room migration from schema version 1 to version 2, preserving existing local preferences.
- Added instrumentation tests for expense persistence, ordering, summation and deletion.

## Data behavior

- The dashboard total is calculated from records observed from Room; no sample/demo expenses are inserted.
- New entries are stored as integer piastres with currency code `EGP` and exponent `2`.
- A failed/invalid amount is not written.
- Data remains local to the device. No account, backend, network permission, cloud sync or remote service is introduced.

## Verification

The previous foundation CI run had already passed JVM unit tests, Android lint and debug APK assembly; its emulator-backed Room tests were still running when the dashboard work began. This UI branch must be built and tested as the new final head. Do not claim the new UI compiles or the instrumentation tests pass until a complete CI run reports success.

## Scope still outstanding

This phase is a real personal-expense vertical slice, not the complete ROFOF product. It does not yet implement shared purchase sessions, participants, allocation/remainder calculations, contributions, settlements, transfer-to-personal-expenses confirmation, budgets, inventory, OCR drafts, reports, export or backup/restore. Those workflows must be added against the approved specifications and protected by invariant tests.
