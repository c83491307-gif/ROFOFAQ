# ROFOF roadmap completion — deduplicated execution register

**Working branch:** `feat/rofof-roadmap-completion`  
**Baseline:** `feat/rofof-shared-purchases` at the Phase 10 merge commit  
**Policy:** preserve delivered functionality; implement only missing scope; do not silently replace or duplicate existing features.

## Baseline verified from source

| Screenshot path | Existing source evidence | Deduplicated disposition |
|---|---|---|
| Backup and restore | No backup/restore implementation found in the inspected app source or Phase 0–10 delivery map | Implement validated, user-initiated backup/restore; never overwrite live DB before validation |
| CSV import and validation | Phase 8 has safe CSV export only; no importer found | Add import parser, preview/validation, duplicate handling and explicit commit; retain export |
| Database encryption | Room v7 database opens without an encryption helper or SQLCipher dependency in app/build.gradle.kts | Add an explicit local encryption design and migration/recovery plan; never invent a passphrase or silently encrypt existing user DB |
| PIN and biometric lock | Not present in inspected dependencies or implementation map | Add local app-lock flow and Android biometric integration |
| Room migrations and compatibility | Room v7 has migrations 1→2 through 6→7; no explicit on-disk upgrade test was found in the inspected implementation report | Preserve migrations; add real on-disk upgrade coverage for supported historical versions and integrity checks |
| Cash-flow reports | Phase 7 PR contains monthly category reporting/search, but that PR is still a separate unmerged draft; cash-flow view is not in cumulative Phase 10 branch | Integrate/extend reporting without rewriting monthly category reports |
| Multi-currency reports | Money stores currency code/exponent and rejects implicit conversion; no multi-currency report was found | Report each currency/exponent independently; no cross-currency sum without explicit FX rates |
| Financial forecasts | Not present in the cumulative branch | Add deterministic forecast estimates with assumptions and no mutation of ledger |
| Debt and loans | No debt/loan entities or workflow found | Add principal, rate/terms, due dates, repayments and balance invariants |
| Goals and savings | Phase 5 already has manually maintained savings goals, validation, archive retention and Room v6 migration | Preserve; only add missing goal analytics or integration if supported by tests |
| Performance and memory | No dedicated performance/memory test evidence found | Profile query/paging/recomposition and add regression checks; avoid speculative caching |
| Android UI tests | Compose/Robolectric UI tests exist for some account/category and dashboard flows | Extend coverage to the newly implemented workflows; do not duplicate existing tests |
| Data-loss/recovery tests | Migration and repository tests exist, but explicit backup restore/corruption/recovery tests are not evidenced | Add recovery and integrity tests around the new backup/restore and migration paths |
| RTL/LTR and localization | Arabic/English RTL/LTR dashboard and feature screens already exist | Audit all new screens/strings and add locale/direction regression coverage; do not rebuild existing localization |
| Release APK signing | Release build type exists, but no release signing configuration/workflow or signed artifact evidence found | Add CI signing only through GitHub Actions secrets; never commit keys or passwords |

## Non-negotiable invariants

1. Room/SQLite remains the local source of truth; core flows work offline.
2. Persist money as integer minor units with currency code and exponent.
3. Never combine currencies without an explicit, auditable conversion rate.
4. Never delete or replace the active database during restore until the candidate backup passes schema, integrity, and foreign-key validation.
5. Never encrypt an existing database in place without a recoverable migration path and user-confirmed recovery secret setup.
6. PIN secrets and encryption keys must not be stored as plaintext preferences or committed to source.
7. Financial forecasts are estimates and never create ledger transactions.
8. Debt repayments and imports must be atomic, idempotent where applicable, and tested for rollback.
9. No production release or publishing before exact-head CI, artifact verification, and signing checks pass.

## Execution order

1. Close the separate already-implemented Phase 7 reports/search and Phase 8 CSV export work into the cumulative delivery only after checking exact-head CI and resolving branch conflicts; do not reimplement them.
2. Add CSV import and preview/validation.
3. Add backup/restore with staged validation and recovery tests.
4. Add encryption and PIN/biometric lock with safe key lifecycle and explicit migration.
5. Add debt/loan management and cash-flow/multi-currency/forecast analytics.
6. Add Room historical upgrade tests, UI/recovery/performance/localization regression coverage.
7. Add secret-backed release signing and produce a signed release artifact only after all gates pass.

## Current execution state

- [x] Inspected repository default branch, Phase 10 PR and CI evidence.
- [x] Confirmed Phase 10 merged into `feat/rofof-shared-purchases`; it is not yet merged to `main`.
- [x] Created this isolated working branch from the Phase 10 cumulative branch.
- [ ] Remaining implementation and verification gates above are not yet complete.

## CI resilience note (2026-11-24)

The first run after correcting Kotlin CSV escape literals compiled the Kotlin sources successfully, then failed before test execution because Maven Central returned HTTP 429 while resolving Robolectric's `nativeruntime-dist-compat` artifact. This is an external dependency-rate-limit failure, not a test assertion failure. The Android CI workflow now retries unit tests and emulator instrumentation with bounded backoff, and retries lint/APK assembly to reuse the populated Gradle cache. A passing result is still required before merge or release.
