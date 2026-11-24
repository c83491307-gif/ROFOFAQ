# ROFOF roadmap completion — deduplicated execution register

**Working branch:** `feat/rofof-apk-integration`  
**Baseline:** cumulative native Android integration branch  
**Policy:** preserve delivered functionality; implement only missing scope; do not silently replace or duplicate existing features.

## Current source baseline

| Area | Current source evidence | Remaining work / disposition |
|---|---|---|
| Backup and restore | `LocalBackupArchive` and archive tests are present | Complete the user-facing restore flow, staged schema/integrity/foreign-key validation, rollback and recovery tests. Never overwrite the live DB before candidate validation. |
| CSV import and validation | `TransactionCsvImporter`, import service and tests are present | Verify and complete user-facing preview, validation, duplicate handling and explicit atomic commit. Retain the existing safe CSV export. |
| Database encryption | Room database is local; no SQLCipher integration or safe key lifecycle is evidenced | Design explicit encryption, passphrase/key recovery and migration before changing existing user databases. |
| PIN and biometric lock | Biometric dependency is present; complete app-lock flow is not evidenced | Implement local app lock and Android biometric integration without storing secrets in plaintext. |
| Room migrations and compatibility | Room schema is version 13 with migrations 1→2 through 12→13 | Add real on-disk upgrade tests across supported historical versions and integrity checks. Preserve all existing migrations. |
| Cash-flow reports | Phase 7 provides monthly category reporting and transaction search; deterministic forecast engine source/tests exist | Complete cash-flow and forecast UI. Keep currencies separate and label estimates clearly. |
| Multi-currency reports | Money stores integer minor units, currency code and exponent | Report each currency/exponent independently; no cross-currency sum without explicit FX rates. |
| Financial forecasts | Deterministic forecast engine and tests are present | Integrate the engine into a user-facing forecast UI with assumptions; never mutate the ledger. |
| Debt and loans | Debt entities, DAO/repository and management screen are present | Review balances, repayment invariants, lifecycle and UI coverage before release. Do not rebuild existing foundations. |
| Goals and savings | Phase 5 includes manually maintained savings goals and Room persistence | Preserve; add only missing analytics or integration supported by tests. |
| Performance and memory | No dedicated performance/memory test evidence is recorded | Profile query/paging/recomposition and add regression checks; avoid speculative caching. |
| Android UI tests | Compose/Robolectric UI tests exist for dashboard/account/category flows | Extend coverage to import/restore, debt, reports and recurring workflows where user-facing flows exist. |
| Data-loss/recovery tests | Archive and repository tests exist; complete restore/recovery coverage is not evidenced | Add corruption, rollback, migration and staged restore tests. |
| RTL/LTR and localization | Arabic/English RTL/LTR dashboard and feature screens exist | Audit all new screens/strings and add locale/direction regression coverage. |
| Release APK signing | Release build type exists, but no release signing configuration/workflow or signed artifact evidence found | Add secret-backed signing only after all gates pass; never commit keys or passwords. |

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

1. Complete CSV import preview/validation/commit and backup/restore validation without duplicating the existing parser/archive foundations.
2. Add encryption and PIN/biometric lock with safe key lifecycle and explicit migration.
3. Complete debt/loan lifecycle and cash-flow/forecast analytics UI.
4. Add Room historical upgrade tests, UI/recovery/performance/localization regression coverage.
5. Add secret-backed release signing and produce a signed release artifact only after all gates pass.

## Current execution state

- [x] Reconciled Phase 7 monthly category reports/search into the cumulative integration source.
- [x] Reconciled Phase 8 safe ledger CSV export into the cumulative integration source.
- [x] Reconciled Phase 9 recurring draft approval/skip while preserving day-of-month scheduling.
- [x] Confirmed Phase 10 budget-overrun source/tests are already present in the cumulative branch.
- [ ] Remaining work and verification gates above are not yet complete.

## CI resilience note

A previous verification attempt compiled Kotlin sources, then failed before test execution because Maven Central returned HTTP 429 while resolving a Robolectric artifact. This is an external dependency-rate-limit failure, not a test assertion failure. The Android CI workflow now retries unit tests and emulator instrumentation with bounded backoff. A passing result is still required on the exact branch head before merge or release.

## Main divergence reconciliation — 2026-11-24

The four commits on `main` after merge base `a77e997ea409381213d68aabdf897e1f0c6f67b7` were reviewed individually. The integration branch remains the source of truth for the cumulative Android implementation; this reconciliation is documentation-only and does not import the older proposed architecture.

| Main commit | Change | Integration-branch disposition |
|---|---|---|
| `199fbe0e57f3f289bae98434fcb9640988221c61` | Added Arabic overview and original personal-finance feature roadmap to README | Preserved the product goals in the README's product direction and the Arabic roadmap section. Implementation checkboxes/status remain governed by source evidence, not the older all-pending checklist. |
| `86705e97b843b8f4d493f8069877f6de2518b0e9` | Added backend/Go/FastAPI/Vue/Flutter/PWA architecture and technology proposals | Intentionally superseded for current delivery by accepted native Android + Kotlin + Compose + Room + offline-first decisions in `docs/architecture/DECISIONS.md`. Backend, web/PWA and sync remain optional future extensions, not current implementation requirements. |
| `4ee798393e48d884ba99d08d49db216f8710050c` | Added focused debug APK workflow | Preserved; final workflow exists on both branches. |
| `1c1f70b19b7e012b9140c10930bb97eb6f059c3c` | Restricted automatic APK workflow pushes to the integration branch | Preserved exactly; `.github/workflows/build.yml` has identical blob SHA `24b519b862e5005c512ea03ef08fab1c978a3a7c` on `main` and `feat/rofof-apk-integration`. |

### Safe reconciliation decision

- No workflow copy or edit is needed: the final workflow content is byte-identical on both refs.
- The README conflict is semantic, not a source-code conflict. The integration README keeps the product roadmap, now explicitly retains its Arabic roadmap, and keeps the accepted Android architecture authoritative. It does not restore the superseded backend/Flutter proposal as the current architecture.
- The main README's original feature checklist marked almost every item pending. Those marks are not copied because current source already implements several items; current status remains mapped in `docs/discovery/README_REQUIREMENTS_APPLIED_PLAN.md` and the phase reports.
- No application source, database migration, build configuration, or existing test was replaced as part of this reconciliation.
- This records a branch-local semantic resolution; it does not change the merge base, rewrite history, or make the PR mergeable by itself. GitHub still needs to recalculate PR mergeability, and exact-head CI/artifact verification remains a separate gate.

### Main-only commits reviewed

- `199fbe0e57f3f289bae98434fcb9640988221c61`
- `86705e97b843b8f4d493f8069877f6de2518b0e9`
- `4ee798393e48d884ba99d08d49db216f8710050c`
- `1c1f70b19b7e012b9140c10930bb97eb6f059c3c`
