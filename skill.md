# ROFOFAQ / ROFOF — Project Master Skill

> **Purpose:** Persistent operating manual and source-of-truth memory for every future ChatGPT/Codex/Claude session working on this repository. Read this file before planning, editing, testing, building, branching, or reviewing anything. This file consolidates recovered user instructions, architecture decisions, product requirements, branch history, implementation boundaries, and delivery rules. Update it whenever a user decision or verified implementation changes.

**Last consolidated:** 2026-11-25  
**Repository (exclusive):** https://github.com/c83491307-gif/ROFOFAQ  
**Product name:** ROFOF  
**Current default branch observed:** `main`  
**Documentation recovery branch:** `docs/rofof-project-memory-skill`  
**Operating mode:** preserve all prior work; branch-first; evidence-driven; no unauthorized merge, release, signing, or deployment.

---

## 0. Mandatory instructions for every new session

1. Work only in `c83491307-gif/ROFOFAQ`. Never substitute the similarly named Creazzy AI repository or another project.
2. Read this file, the README, `docs/architecture/DECISIONS.md`, `docs/discovery/README_REQUIREMENTS_APPLIED_PLAN.md`, `docs/discovery/REQUIREMENTS_TRACEABILITY_BASELINE.md`, and the relevant phase/workstream reports before changing code.
3. Fetch the latest remote refs and inspect **all relevant branches**. This project was developed across many branches and multiple conversations; do not assume a branch named `new-chat` contains the whole project or that one branch is the sole source of truth.
4. Preserve every existing commit, feature, migration, test, workflow, report, and user requirement. Never replace newer code with stale branch snapshots. Compare files and commits before cherry-picking or reconciling.
5. Keep work isolated on a dedicated branch. Do not merge, rebase, force-push, delete branches, publish a release, deploy, or change `main` unless the user explicitly authorizes that exact action.
6. Do not claim work is complete because a file, screen, workflow, helper, or test was added. Completion requires implementation, persistence, validation, UX states, accessibility where applicable, tests executed on the exact branch head, and evidence.
7. Do not mark CI, emulator, APK, signing, or release gates as passed without the actual successful run/artifact for the exact commit SHA.
8. Keep secrets out of source, logs, commits, chat, and artifacts. Never ask the user to paste signing passwords or keystore bytes into source. Use repository/environment secrets only when the user authorizes signing setup.
9. Do not run emulator tests if the user says not to. A release-signing workflow and a debug/emulator workflow are separate concerns.
10. At each stage boundary, report changed files, commit SHA, tests actually run, results, artifacts, known failures, and the next unfinished item. Maintain a durable status register here or in a linked progress report.
11. When a task says “continue”, resume from the latest verified state across branches, not from a guessed point and not by restarting or discarding work.
12. If historical context is missing, inspect Git history, branch trees, PRs, Actions, docs, and prior conversation records before asking the user to repeat requirements.

### Explicit user delivery preferences and constraints recovered

- The user repeatedly asked for the complete project to be implemented, tested, fixed, and documented while preserving earlier work.
- The user explicitly required the exact ROFOFAQ repository and corrected earlier repository confusion.
- User repeatedly prohibited merging or publishing until explicitly authorized and verification was complete.
- User requested workstreams 1–15 to be executed sequentially/hybrid, with actual source, tests, and documentation—not just plans.
- User requested Arabic/English professional Android UI with RTL/LTR support.
- User asked for APK build/release preparation. In the latest release-signing sequence, the user specifically asked to build the APK while leaving signing secrets for a later stage. Never block an authorized unsigned/debug APK build on signing secrets.
- Where emulator execution is explicitly excluded, do not run it or claim it passed.

---

## 1. Product identity and product vision

ROFOF is an offline-first Android application for personal finance, daily spending, shared purchases, and practical household financial organization. Arabic is a first-class language; English and correct RTL/LTR behavior are required.

The product vision includes:
- Personal income and expense ledger.
- Multiple cash, card, and savings accounts.
- Categories and subcategories, transfers, transaction splits, recurring templates, receipt attachments, and tags.
- Budgets, savings goals, debts/loans, and budget alerts.
- Shared purchase sessions, participants, item quantities, precise allocation, multi-payer contributions, balances, and settlements.
- Merchant/catalogue records, product identity, price history, optional location-aware merchant data.
- Search, monthly reports, cash-flow analysis, net worth, spending behavior, and forecasts.
- CSV import/export; other formats only after parser and round-trip validation.
- Backup/restore and data portability.
- Optional app lock, biometric authentication, and encrypted local data after safe key lifecycle and migration design.
- Optional receipt OCR and explainable financial assistance.
- Future optional web/PWA, sync, and multi-user capabilities.

**Roadmap is not implementation evidence.** Use source code, tests, and branch-head CI to distinguish shipped, partially integrated, and pending capabilities.

---

## 2. Accepted architecture — binding decisions

### 2.1 Native Android first; local-first

- Current delivery architecture is **native Android using Kotlin, Jetpack Compose, and Room/SQLite**.
- Room is the authoritative source of truth for core device-local state.
- Core workflows must work without login, backend, network, cloud storage, or banking connectivity.
- No backend, PostgreSQL, Go/FastAPI, Vue, Flutter, cloud service, or remote sync is required for the current Android delivery. Such proposals are superseded for this delivery by the accepted Android architecture.
- Backend, optional sync, cloud backup, banking integrations, and web/PWA may be considered later as explicit opt-in extensions behind stable repository/domain boundaries. They must not be silently enabled.
- No INTERNET permission or remote service should be introduced into offline core without a reviewed, approved requirement.
- Keep the initial single Gradle app module with clear package boundaries; avoid premature module proliferation.

### 2.2 Dependency direction

```
Compose UI
  -> ViewModel / UI state
  -> use cases
  -> domain repository interfaces
  -> Room-backed repository adapters
  -> Room / SQLite
```

Domain logic remains platform-independent. Room adapters implement repository contracts. A future sync adapter, if authorized, must be separate and must define identity, idempotency, versioning, authorization, conflict resolution, audit, encryption, and recovery.

### 2.3 Toolchain observed in repository documentation

- Android module: `app`
- Namespace / release application ID: `com.creazzy.rofof`
- Debug application ID: `com.creazzy.rofof.debug`
- Minimum Android API: 26
- Compile/target SDK: 35
- JDK: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
- Compose and Room
- Repository currently documents no Gradle Wrapper; CI installs Gradle 8.9 directly. Do not invent incomplete wrapper files or assume `./gradlew` exists. Verify the current tree before using build commands.

---

## 3. Financial and data invariants — never violate

1. **Money representation:** integer minor units plus ISO currency code and currency exponent. Never persist money as binary floating point.
2. Use checked integer arithmetic or safe wider intermediates; reject overflow.
3. Never combine different currencies or exponents in totals, comparisons, forecasts, budgets, or reports without an explicit, auditable conversion operation and rate.
4. **Transfers are not income or expenses.** Same-currency account transfers must be atomic and excluded from income/expense totals. Cross-currency transfers must be explicitly modeled or rejected; never silently convert.
5. **Shared purchase ledger is separate from personal ledger.** An allocation is not a personal expense until the user explicitly confirms a transfer/link. The link and expense insertion must be atomic and idempotent.
6. Never double-count shared purchase allocations in personal spending.
7. **Recurring rules create drafts.** No automatic posting. Draft approval is an explicit user action with guards and atomic posting.
8. **OCR, voice, smart paste, and AI suggestions create reviewable suggestions/drafts only.** They never post a transaction automatically.
9. Budget and forecast calculations are read-only projections; they do not create ledger entries or mutate account balances.
10. Preserve historical meaning. Renaming/archiving categories, accounts, merchants, or catalogue entries must not rewrite old transaction snapshots or erase history.
11. Archive is not delete. Destructive operations require confirmation, explicit guards, and recoverability.
12. Multi-record financial operations must be transactional: all commit or all roll back.
13. Use idempotency keys/stable identifiers for payment, import, recurring occurrence, sync event, and ledger-transfer retry paths.
14. Preserve source/provenance for posted amounts and audit-sensitive transformations.
15. Restore must validate a staged candidate (archive integrity, schema, Room integrity, foreign keys, domain reconciliation) before replacing the live database. Keep a rollback path.
16. Never encrypt or migrate an existing live database in place without a tested, recoverable migration and user-confirmed key recovery setup.
17. Do not store PINs, passwords, or encryption keys in plaintext preferences, source, logs, or test artifacts.
18. Offline workflows remain available when optional services are absent or disabled.

---

## 4. Product scope and requirements catalogue

The original requirement traceability baseline covers 28 requirement areas. Preserve the full intent, but mark each capability by real implementation evidence.

### Core personal finance
- Income/expense transactions with title, amount, currency, type, date/time, account, category, notes, and stable ID.
- Multiple accounts with opening balances and per-account balances.
- Category/subcategory management; rename and archive with history retention.
- Account management; protect default cash account from unsafe archival.
- Atomic transfers, transaction edit/delete with confirmation, and correct balance recomputation.
- Tags, receipt attachments, and optional location metadata as separately scoped features.
- Loans/debts and repayment lifecycle; repayments must be atomic and currency-safe.
- Search and filters; search results must be bounded/paginated where appropriate.

### Shared purchases and household accounting
- Purchase sessions with explicit currency and OPEN/SETTLED state.
- Participants scoped to a session.
- Items with quantity, unit, unit price, exact line total, and fixed-point quantity precision.
- Item-level participant allocations; allocated total must equal item total exactly.
- Equal or custom split only when explicitly selected; never assume an equal split.
- Actual payments are distinct from expected allocations.
- Participant balances show allocated, paid, remaining, or credit; overpayment remains visible.
- Multi-payer reconciliation and zero-sum/rounding invariants.
- Closing a session requires fully allocated items and all positive outstanding balances settled.
- Stable idempotency for payments and transfers.
- Explicit link of an allocation into personal expenses; matching currency/account/category required; single atomic transaction and unique source link.
- Purchase history cannot disappear through participant/item archival; prohibit editing/deleting records after financial history exists.

### Budgets, goals, alerts
- Budgets keyed by category, currency, exponent, and explicit half-open period `[start,end)`.
- Only expense transactions in the matching category/currency/period contribute.
- Exact-limit spending is not an overrun; strict condition is `spentMinor > budgetMinor`.
- Budget-overrun notifications are local, Arabic/English, permission-aware, duplicate-suppressed per budget/period, and rearmed after spending returns to or below limit.
- Notification permission is requested contextually after user action; denied permission must not block saving the transaction.
- Savings goals have target and separately maintained saved amount, optional deadline, and archive retention. Do not infer goal deposits from arbitrary transactions unless explicitly designed.
- Dynamic budget suggestions remain bounded suggestions; they never silently change budgets.

### Recurring transactions
- Recurrence rules generate unique scheduled drafts; uniqueness by rule and scheduled occurrence.
- Month-end dates clamp to the last valid day; catch-up is bounded.
- Rule advancement and draft creation are one Room transaction.
- Only DRAFT may be approved or skipped.
- Approval validates positive amount, type, active account/category, and currency compatibility.
- Deterministic transaction identity prevents duplicate posting.
- Draft becomes POSTED only after transaction insertion succeeds; rollback on failure.
- SKIPPED drafts cannot later be approved.
- No automatic background ledger posting.

### Reports, search, analytics
- Monthly category reports are read-only and currency/exponent scoped. Initial implemented report scope was EGP-only; do not imply all-currency reports without evidence.
- Search covers title/notes case-insensitively; current documented result cap is 200.
- Cash-flow calendar, net worth, forecast, scenario simulator, anomaly detection, subscription intelligence, behavioral insights, and health score require UI integration, persistence, assumptions, and tests before being called complete.
- Forecasts and scores must be explainable, bounded, and visibly estimates—not financial facts or ledger mutations.
- Currency groups remain separate.

### Import/export and backup
- CSV export is user-initiated through Android Storage Access Framework, read-only, UTF-8 with BOM, stable IDs, currency/exponent, UTC timestamps, quoted fields, doubled embedded quotes, and spreadsheet formula-prefix neutralization.
- CSV export currently covers non-deleted personal ledger income/expense transactions; it does not automatically include shared purchases, budgets, goals, transfers, or attachments.
- CSV import requires preview, validation, duplicate handling, clear error reporting, and explicit atomic commit.
- Parser/service/tests existing in source do not prove end-user import workflow complete.
- Backup archive foundation does not prove full restore is complete.
- Differential backup manifests must be path-safe and integrity checked (SHA-256).
- Restore stages and validates before replacing live data; corruption, partial failure, rollback, process death, low-storage, and historical migrations require tests.

### Security and privacy
- App lock/PIN/biometric integration and SQLCipher/database encryption remain distinct deliverables; dependency presence alone is not completion.
- Design key generation, Android Keystore use, backup/recovery, rotation, migration, lost-secret UX, and threat model before shipping encryption.
- Ask for permissions contextually and only when needed.
- Clipboard is read only after a user taps Paste; never poll clipboard on screen open.
- Logs and diagnostics must redact sensitive data.
- Data classification, consent, retention, export, deletion, and audit lifecycle must be explicit.
- No telemetry, cloud storage, remote AI, banking access, or network sync in offline core unless separately authorized.

### Catalogue, merchants, inventory, receipts
- Persistent catalogue, search, duplicate detection, price history, historical snapshots, and safe retention.
- Merchant and branch records; location privacy is opt-in and permission-aware.
- Pantry/inventory batches, movements, expiry, purchase-to-stock, and replenishment require quantity reconciliation and duplicate-event tests.
- Receipt OCR pipeline: capture, extraction, confidence, arithmetic reconciliation, user review, duplicate detection, and image lifecycle. Never auto-post.
- Barcode lookup must handle ambiguous matches, variants, offline/manual fallback, and duplicates.
- Product units/dimensions must be explicit; conversions only between compatible units.
- Price comparison requires comparable units, currency, package size, and timestamps.

### Localization, UI, accessibility
- Arabic is first-class, with English and full RTL/LTR support.
- Every screen, dialog, empty/loading/error state, notification, validation message, and accessibility label must be localized.
- Use correct locale-aware date, number, and currency formatting; preserve canonical storage values.
- Verify layout mirroring, navigation, charts, icons, alignment, long Arabic strings, and mixed Arabic/Latin numbers.
- Light/dark themes and semantic color tokens; do not rely on color alone for meaning.
- TalkBack semantics, large font scaling, contrast, touch targets, keyboard navigation where applicable.
- Professional native UI—not placeholder/demo screens. Motion must respect reduced-motion/accessibility expectations.
- User interaction specifications include navigation/back behavior, undo boundaries, clipboard actions, sheets/drawers, feedback, restoration after process death, and screen-to-workflow coverage.

---

## 5. Workstream registers recovered from project history

### 5.1 Hybrid 15 advanced financial workstreams

The cumulative workstream branch is `feat/rofof-hybrid-15-workstreams`, based on `feat/rofof-roadmap-completion`. A helper/engine alone is only a foundation; the acceptance gates below remain mandatory.

| # | Workstream | Existing foundation described in report | Remaining product acceptance |
|---|---|---|---|
| 1 | Financial anomaly detection | Currency/category median detector; minimum five comparable historical entries | Persisted history query, review UI, false-positive tests |
| 2 | Subscription intelligence | Stable amount + cadence candidate detector | Opt-in subscription list, reminders, recurring-link tests |
| 3 | Behavioral finance | Deterministic category-concentration ranking | Multi-period summaries and bilingual UI |
| 4 | Scenario simulator | Exact integer-minor-unit what-if calculator | Scenario editor, currency guards, boundary UI tests |
| 5 | Financial health score | Explainable bounded score with component factors | Explanation UI, calibration, accessibility review |
| 6 | Account reconciliation | Per-account/per-currency statement comparison | Import workflow, adjustment approval, Room integration |
| 7 | Bills center | Date-window and unpaid-item filtering | Bill persistence, reminders, recurrence, permission UX |
| 8 | Receipt intelligence | Draft-readiness validation for extracted fields | On-device OCR, image lifecycle, confirmation, duplicate detection |
| 9 | Net worth | Currency-separated asset/liability arithmetic | Asset/liability persistence, valuation history, dashboard |
| 10 | Cash-flow calendar | Date/currency-separated daily net calculator | Calendar UI, recurring/bill integration, uncertainty display |
| 11 | Offline sync conflicts | Divergent financial payloads are never auto-merged | Local-only default; opt-in transport, encryption, idempotency, recovery tests |
| 12 | Financial audit trail | SHA-256 append-chain primitive and tamper verification | Durable Room audit table, transactional hooks, retention/export, migration tests |
| 13 | Property-based financial testing | Seeded deterministic scenario/invariant tests | Generated transfer/ledger/import/restore invariants and reproducible failure seeds |
| 14 | Performance & battery lab | Large deterministic test baseline | Macrobenchmark, startup/scroll/DB/battery thresholds |
| 15 | Accessibility & compatibility lab | Compose semantics integration surface | Semantics, TalkBack, RTL/LTR, font scaling, API/device matrix |

**Known status from the branch report:** workstreams 1–12 had foundations only; workstreams 14–15 were incomplete. Never call all 15 complete unless the acceptance gates have been implemented and verified.

### 5.2 Fifteen additional operational enhancements

The V6.0 enhancement report lists these local deterministic foundations:

1. Multi-level approvals — amount thresholds, approval count, eligible roles.
2. Financial automation rules — deterministic allowlisted conditions/actions; no arbitrary code execution.
3. Dynamic budgets — median-based bounded suggestion with floor/ceiling.
4. Sensitive-operation guard — confirmation and re-authentication decisions.
5. Tax document center — metadata/year/MIME/SHA-256 validation only; not tax advice or filing.
6. Merchant intelligence — canonical name and alias normalization.
7. Financial data quality — required fields, positive amount, currency/exponent, timestamps, duplicate IDs.
8. Financial sensitivity — bounded basis-point scenario adjustment with overflow protection.
9. Seasonality — monthly average and peak-month calculation.
10. Data lineage — source IDs and transformation provenance.
11. Differential backups — path-safe changed/unchanged/deleted manifest planning.
12. Local plugin registry — capability allowlist only; no dynamic code loading or elevated execution.
13. Recovery lab — canonical SHA-256 manifest verification; changed/deleted detection; not full restore.
14. Localization coverage — missing/extra key and placeholder parity report.
15. Application health monitor — latency/error/queue-depth threshold assessment.

These are **engines/foundations**, not proof of full screens, durable persistence, automatic backup, production telemetry, tax filing, or plugin runtime.

### 5.3 V6.0 input experience

- Voice-assisted entry uses Android SpeechRecognizer and runtime microphone permission; locale follows Arabic/English.
- Offline preference is not a guarantee; actual offline speech depends on recognition service and downloaded language pack. Show recognizer errors honestly.
- Smart paste is user-initiated and requires review/confirmation; it never posts directly.
- Inline arithmetic uses bounded expression parsing, not arbitrary code execution.
- Frequent categories: four most-used active expense categories.
- Typical amount: average of five most recent non-deleted expenses for selected category and exact currency/exponent.
- Haptic success feedback fires only after repository insert succeeds.

---

## 6. Phase-by-phase implementation status and known boundaries

The following status is based on repository documentation, not a fresh full source audit. Recheck current source and Actions before changing these labels.

- **Phase 0 — Discovery:** discovery report and requirement traceability baseline exist. Initial discovery branch commit recorded as `d2661220f629806523e71eace65d98c766ebd1c2`; no app build was claimed at that point.
- **Phase 1 — Android foundation:** Kotlin/Compose/Room skeleton, Money type, local preferences, tests/workflow foundations. Early report explicitly said verification pending.
- **Phase 2 — Bilingual dashboard:** Arabic/English and RTL/LTR dashboard foundation.
- **Phase 3 — Core finance:** local ledger, accounts, categories, balances and transfers. Inspect current source and phase report for exact final scope.
- **Phase 4 — Shared purchases:** session/participants/items/exact allocations/payments/settlements/explicit personal expense linking. Room schema v5 was documented in the phase report; later cumulative roadmap says Room schema reached v13 with migrations 1→2 through 12→13. Always inspect actual current schema and migrations before editing.
- **Phase 5 — Budgets and goals:** Room v5→v6; monthly category budgets, manually maintained savings goals, period/category/currency filtering.
- **Phase 6 — Recurring drafts:** Room v6→v7; monthly recurring rules and due draft generation; no automatic posting.
- **Phase 7 — Reports/search:** monthly category report, initially EGP-only; case-insensitive title/notes search, max 200 results.
- **Phase 8 — CSV export:** user-initiated SAF export, UTF-8 BOM, stable IDs, currency/exponent, UTC timestamps, CSV quoting, formula neutralization.
- **Phase 9 — Recurring approval:** explicit approve/skip, one Room transaction, deterministic transaction ID, DRAFT-only guard, active account/category/currency validation.
- **Phase 10 — Budget-overrun alerts:** strict overrun, matching category/currency/exponent/period, contextual notification permission, duplicate suppression/rearm; event-driven, not a background scheduler.
- **Hybrid 15 workstreams:** see Section 5.1; partial foundations, outstanding UI/persistence/verification gates.
- **Additional 15 enhancements + V6.0:** see Section 5.2–5.3; foundations only where report says so.
- **APK integration:** cumulative reconciliation branch contains Phase 7–10 and enhancements. Older foundation/phase branches were not blindly merged because they could contain stale Room/dashboard/recurrence files.
- **Release signing:** separate signed-release workflow was merged to main in PR #11 on 2026-11-24. User later requested APK build while leaving signing secrets for a later phase; obey the latest instruction and do not invent configured secrets.

---

## 7. Branch map and cross-branch preservation

Repository branch inventory observed on 2026-11-25 (HEAD SHAs are point-in-time references; fetch current remote refs before acting):

| Branch | Observed HEAD | Known purpose / handling |
|---|---|---|
| `main` | `245e56d1f9706967bfed2537c938de8ccf680089` | Default branch; includes PR #11 signed Release workflow merge |
| `feat/rofof-phase0-discovery` | `35b101a35abe99d246152673f30d41621ffbf954` | Discovery and requirements baseline |
| `feat/rofof-android-foundation` | `98a666b33fa83a5f280cb5626902cd8dfb8f3f2f` | Android foundation |
| `feat/rofof-shared-purchases` | `f4b02c4c55a240fe711722a2e2f7072326f90c3f` | Shared purchases; session, allocations, payments, settlements |
| `feat/rofof-hybrid-15-workstreams` | `bc02a6df003ff9469fba7c6d19db3e2f280f01dc` | Hybrid 15 workstreams and additional enhancements |
| `feat/rofof-apk-integration` | `2ad1e9c54304491554c6a6158dcc98dfbc30d4d0` | Cumulative integration; Phase 7–10 and APK workflows |
| `feat/rofof-roadmap-completion` | `2e455ac4e09c4121cf93484d311efa7f31f8a517` | Cumulative roadmap and deduplication register |
| `feat/rofof-analytics-search` | `cb2f7f987ba3c0e561d753056bd5b178ec9a92c5` | Analytics/search |
| `feat/rofof-budget-overrun-alerts` | `799af0b9771cf590b49d3fdb779d96585511679c` | Budget-overrun alerts |
| `feat/rofof-csv-export` | `da4fb931052d1a44475544298c9e674ea869241c` | CSV export |
| `feat/rofof-recurring-approval` | `797d7fecc503047a44e0b6e5cdb29de7ab4a9381` | Recurring draft approval/skip |
| `feat/rofof-release-signing` | `fa7a5440b6f49a6b61707f87ecbe75893c1656ef` | Release signing workflow preparation |
| `feat/rofof-signed-release` | `c7fa78f3c592406064261bc03a26671a2b127a67` | Signed Release workflow branch merged via PR #11 |
| `build/rofof-unsigned-release` | `c6e4ab38bd1e68fbf7ca211b3232fce3bdb499f0` | Unsigned release build path |
| `work/rofof-dashboard-ui` | `f7058160f8e50290e5c64c9e5d72067fe7cb2e78` | Dashboard UI work |
| `c83491307-gif-patch-1` | `888afc06eee3a10a6856c32daa32a55a0ba72ddc` | Contains an engineering-pack ZIP reported around 1.78 MB; archive contents were not extracted/verified at time of discovery. Inspect safely before relying on it. |
| `docs/rofof-project-memory-skill` | created 2026-11-25 from main | This documentation-only branch contains this master skill; keep isolated until user authorizes any PR/merge. |

### Branch integration rules

- Do not assume the branch names encode freshness. Compare commit ancestry, tree content, file hashes, and test evidence.
- The cumulative hybrid branch was based on roadmap completion; APK integration was based on hybrid. Older foundation/phase branches may be stale.
- A previous integration report documented main divergence of four commits after merge base `a77e997ea409381213d68aabdf897e1f0c6f67b7`; it explicitly reconciled README architecture semantics and confirmed the focused debug workflow was byte-identical on main and integration at that time.
- The repository main branch was observed with commit `245e56d...`, a merge commit for PR #11 from `feat/rofof-signed-release`. Its parent commits were `305cff4c1b4739e93ad57f8d5d6b08825a7ef399` and `c7fa78f3c592406064261bc03a26671a2b127a67`.
- The signed-release workflow was described as manual, signature/integrity verified, and not invoking emulator execution. Verify current workflow contents and run status before relying on this.
- The main-only backend/Go/FastAPI/Vue/Flutter/PWA proposal commit was superseded for current delivery by the accepted native Android architecture. Preserve product ideas as optional future extensions, not active implementation requirements.
- Branch inventory is historical. Always refresh before new work and add newly discovered branches to this register.

---

## 8. Build, test, CI, APK, and release rules

### 8.1 Required verification gates

For a normal Android implementation change, run the applicable exact-head gates:
1. JVM/unit tests, including Robolectric Room repository tests.
2. Android lint.
3. Debug APK assembly.
4. Emulator instrumentation when allowed and relevant.
5. Migration/upgrade tests for schema changes.
6. UI/Compose, RTL/LTR, accessibility, and process-restoration tests where affected.
7. Artifact inspection: confirm APK exists, package/application ID, version, size, and expected contents.
8. Record run URL/ID, branch, commit SHA, job results, artifact IDs, and failure logs.

Documented local commands when the environment is correctly configured:
```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
gradle connectedDebugAndroidTest
```
Use `./gradlew` only if a real wrapper is present. Repository documentation says wrapper files were absent and CI installs Gradle 8.9.

### 8.2 Failure handling

- Inspect the exact failing job and first meaningful error; do not guess.
- Distinguish source/test assertion failures from infrastructure failures such as Maven Central HTTP 429, SDK provisioning, emulator boot, or runner resource exhaustion.
- Fix the smallest root cause, preserve unrelated work, rerun the same gates, and report each attempt.
- A retry/backoff can mitigate rate limits but does not turn an unexecuted test into a pass.
- Never hide a flaky emulator or skip a failing test to produce a green badge.
- Verify that test tasks actually executed; workflow configuration alone is not proof.

### 8.3 APK distinctions

- Debug APK is not release-signed and not a Play Store release.
- Unsigned release APK is not signed.
- Signed release APK requires secret-backed signing and verification of certificate/signature and file integrity.
- Do not claim an APK exists until the exact successful workflow artifact is present and inspected.
- User explicitly asked to leave signing secrets for a later stage in the latest APK-building sequence. Build the authorized APK without inventing or requiring secrets for a debug/unsigned path.
- No merge, publication, Play Store upload, or deployment unless separately authorized.

### 8.4 Signing secret hygiene

- Never commit keystore files, passwords, signing keys, or encoded secrets.
- Use GitHub Actions secrets/environment secrets only with explicit user authorization.
- Validate secret presence without printing values.
- Do not make a signing workflow auto-run on every push unless specifically authorized.
- Keep release signing separate from emulator tests when workflow design and user instruction require it.

---

## 9. Current known open work / do-not-overclaim list

The following were explicitly open in the repository documentation and must be rechecked against current source and CI:

- Complete user-facing CSV import preview, validation, duplicate handling, and atomic commit.
- Complete validated backup restore, staged candidate checks, rollback and recovery.
- Design and implement database encryption with safe key lifecycle and migration.
- Complete PIN/app lock and Android biometric workflow; biometric dependency alone is not sufficient.
- Add real on-disk historical Room migration upgrade tests across supported versions; preserve all migrations. A later report documents schema version 13 and migrations 1→2 through 12→13—verify exact current schema.
- Complete cash-flow and forecast UI; keep estimates distinct from actuals.
- Complete net-worth/asset/liability persistence and valuation history.
- Review and complete debt/loan lifecycle and repayment invariants.
- Complete UI/persistence/acceptance gates for advanced 15 workstreams.
- Complete performance/memory/battery benchmarks and accessibility/device compatibility matrix.
- Audit every screen and notification for Arabic/English, RTL/LTR, large fonts, TalkBack, contrast, and error/empty/loading states.
- Add corruption, low-storage, process-death, rollback, restore and recovery tests.
- Verify latest CI and APK artifact on exact current head.
- Review uninspected engineering-pack ZIP on `c83491307-gif-patch-1` only if needed; do not assume its contents.
- Check latest PR/Actions status and all branches before planning integration.

---

## 10. Recommended execution protocol for future tasks

### Step A — recover state
- Fetch repository metadata, all branches, recent commits, open/closed PRs, workflow runs, and latest artifacts.
- Read this file and all relevant phase reports.
- Compare relevant branch heads and identify the latest cumulative implementation.
- Build a branch graph and identify unique commits not yet represented in the cumulative branch.
- Check for in-progress Actions runs before starting duplicate work.

### Step B — make a plan
- Map each requested item to requirement ID, source file, domain rule, Room entity/migration, UI, tests, and documentation.
- Mark each item as implemented, partial, missing, blocked, or awaiting verification.
- Identify conflicts and dependencies across branches.
- Choose one dedicated working branch based on the latest safe cumulative baseline; do not overwrite another branch.
- Keep scope sequential when the user requests one task at a time.

### Step C — implement safely
- Make small, reviewable changes.
- Preserve stable APIs, IDs, migrations, old behavior, and all existing tests.
- Add migration tests before schema rollout.
- Keep domain logic deterministic and testable without Android where possible.
- Use Room transactions for multi-row financial operations.
- Ensure UI loading/empty/error/success/permission-denied states and accessibility semantics.
- Update documentation and traceability at the same time as code.

### Step D — verify
- Run unit/Robolectric tests, lint, debug assembly, and emulator only when authorized.
- Inspect APK artifact, not just workflow green status.
- Run migration, data integrity, currency, rounding, idempotency, rollback, and no-double-counting tests relevant to the change.
- Capture evidence: exact commands, environment, run ID/URL, commit SHA, test counts, artifacts, and failures.
- Repeat after fixes until the required gates pass or clearly report the blocker.

### Step E — checkpoint
- Commit only to the authorized working branch.
- Update this skill or a linked progress register with new decisions, branch, commit, status, and evidence.
- Do not merge/rebase/force-push/delete/publish without explicit approval.
- At each substantial pause, provide a concise checkpoint with what is done, what remains, and one actionable next step.

---

## 11. Definition of Done

A feature or phase is **DONE** only if all applicable criteria are satisfied:

- [ ] User requirement and acceptance criteria are traceable.
- [ ] Source implementation exists in the correct repository and branch.
- [ ] Domain rules and financial invariants are enforced.
- [ ] Room schema/migrations are safe and tested against real prior database versions when changed.
- [ ] UI is connected to real persistence; no demo-only or fake data path.
- [ ] Arabic/English and RTL/LTR work for the affected flow.
- [ ] Loading, empty, error, permission-denied, confirmation, and success states are handled.
- [ ] Accessibility semantics and large-font behavior are covered where relevant.
- [ ] Unit, repository/Room, UI/integration, and regression tests are added and actually executed.
- [ ] Lint and required build tasks pass on the exact branch HEAD.
- [ ] Emulator instrumentation passes when required and authorized.
- [ ] APK artifact is present and inspected when APK delivery is part of scope.
- [ ] Documentation and traceability report reflect actual behavior and limitations.
- [ ] No secrets, private data, or sensitive financial values leak into logs/artifacts.
- [ ] No unauthorized merge, release, signing, publication, or deployment occurred.

A green compile is not feature completion. A passing unit test alone is not release readiness. A workflow file is not evidence of a successful workflow.

---

## 12. Required handoff / session-start response

At the start of a new work session, report briefly:
1. Repository and current branch.
2. All relevant branch heads and any new branches.
3. Last verified commit and successful CI evidence.
4. Current implementation baseline.
5. Outstanding requirements and blockers.
6. Exact next action proposed.

At the end of each work segment, report:
- Branch and commit SHA.
- Files changed.
- Tests run and exact outcomes.
- CI run IDs/URLs and artifact links if verified.
- Failures and whether they are code or infrastructure failures.
- What remains unfinished.
- Explicit confirmation that no unauthorized merge or release was performed.

---

## 13. Source documents and evidence hierarchy

Read and maintain these repository documents:
- `README.md` — product overview and current implementation status.
- `docs/architecture/DECISIONS.md` — accepted architecture decisions.
- `docs/discovery/README_REQUIREMENTS_APPLIED_PLAN.md` — original product roadmap mapped to Android delivery and boundaries.
- `docs/discovery/REQUIREMENTS_TRACEABILITY_BASELINE.md` — 28 requirement areas, cross-cutting invariants, verification evidence template.
- `docs/discovery/PHASE_0_DISCOVERY_REPORT.md` — discovery findings.
- `docs/implementation/PHASE_1_ANDROID_FOUNDATION.md`
- `docs/implementation/PHASE_4_SHARED_PURCHASES.md`
- `docs/implementation/PHASE_5_BUDGETS_AND_GOALS.md`
- `docs/implementation/PHASE_6_RECURRING_DRAFTS.md`
- `docs/implementation/PHASE_7_REPORTS_AND_SEARCH.md`
- `docs/implementation/PHASE_8_CSV_EXPORT.md`
- `docs/implementation/PHASE_9_RECURRING_APPROVAL.md`
- `docs/implementation/PHASE_10_BUDGET_OVERRUN_ALERTS.md`
- `docs/implementation/HYBRID_15_WORKSTREAMS.md`
- `docs/implementation/NEW_15_ENHANCEMENTS_AND_V6_INPUT.md`
- `docs/implementation/ROADMAP_COMPLETION_DEDUPLICATION.md`
- `docs/implementation/APK_BUILD_AND_BRANCH_INTEGRATION.md`
- `docs/awareness-system.md`

**Evidence hierarchy:** explicit latest user instruction > accepted architecture decisions > current source + migration/test evidence > exact-head CI/artifact evidence > phase reports > older branch snapshots/old README checklists > assumptions. If sources conflict, preserve history, explain the conflict, and ask only when the user's decision is genuinely required.

---

## 14. Recovery completeness and provenance note

This skill was reconstructed from available prior conversation context, user instructions, GitHub branch inventory, README, architecture decisions, requirements traceability, and implementation reports on 2026-11-25. The work history spans multiple branches and separate conversations. It is intentionally explicit about partial foundations and unverified gates to avoid turning historical plans into false completion claims.

This document is the durable operating memory. Future sessions must append new user decisions and verified evidence rather than silently rewriting history or dropping older requirements.


---

# APPENDIX A — Full recovered source specifications and implementation reports

This appendix embeds the actual source documents available in the repository at consolidation time. These are included **inside this one skill.md** so a new chat/branch does not need to reconstruct requirements from scattered files. The standalone source documents remain in the repository as historical evidence; this file is the consolidated entry point.


## Source document: `docs/architecture/DECISIONS.md`

# ROFOF Architecture Decisions

See this file as the accepted architecture baseline for implementation.

- **ADR-001 — Local-first authority:** accepted. Room is authoritative for core device-local state. Core workflows work offline without account or network. Backend/sync are opt-in future extensions.
- **ADR-002 — Backend scope:** accepted. No backend, PostgreSQL, account, remote storage or network integration is required or introduced for v1 foundation.
- **ADR-003 — External processing:** accepted. OCR and remote lookups are optional; OCR creates reviewable drafts only.
- **ADR-004 — Money:** accepted. Integer minor units + ISO currency code + exponent; no floating-point money.
- **ADR-005 — Dependency direction:** accepted. UI → use cases → domain contracts; Room adapters implement repository interfaces; domain remains platform-independent.

## Local-first data flow

```
Compose UI
   ↓
ViewModel / UI state
   ↓
Use cases
   ↓
Domain repository interfaces
   ↓
Room-backed repository adapters
   ↓
Room / SQLite (local source of truth)
```

A future sync adapter may be added beside the local adapter behind the repository boundary. It must not be enabled implicitly and must define idempotency, versioning, conflict handling, audit and recovery before shipping.

## Initial Android module map

Single Gradle app module initially; package boundaries keep domain and data concerns separate without premature Gradle module proliferation.

- `app`: application and Activity entry points
- `core/money`: safe money value type
- `data/local`: Room entities, DAOs, migrations
- `data/repository`: local repository implementations
- `domain/model`, `domain/repository`, `domain/usecase`
- `feature/dashboard`, `feature/purchases`, `feature/expenses`, `feature/catalog`

## Not part of this phase

Backend, remote sync, login, cloud backup, payment execution, OCR auto-posting and remote product lookups.


---

## Source document: `docs/discovery/README_REQUIREMENTS_APPLIED_PLAN.md`

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


---

## Source document: `docs/discovery/REQUIREMENTS_TRACEABILITY_BASELINE.md`

# ROFOF v1.2.0 — Requirement Traceability Baseline

**Status:** Initial source-to-workstream map. Implementation links, test IDs and results are intentionally pending until code exists.

| Source | Requirement area | Planned domain/workstream | Minimum verification evidence |
|---|---|---|---|
| 00 Start Here | Scope precedence, invariants, delivery rules | Governance / release | Manifest, archive, schema and release-gate checks |
| 01 Product Vision | Purchase sessions, catalogue, quantity allocations, participants, payers, settlements, offline core | Purchases / catalogue / allocations / ledger | 5 kg allocation acceptance case; restart persistence; multi-payer reconciliation; airplane-mode workflow |
| 02 Governance | Configuration, change control, release gates, evidence | Governance | Decision log; approval gates; evidence completeness |
| 03 Product Direction | Parallel workstreams, shared contracts, conflict protocol | Engineering coordination | Contract review; dependency and conflict log |
| 04 Interaction Control | Navigation, back, undo/redo, clipboard, sheets/drawers, feedback | Android UX shell | Navigation, undo boundaries, state restoration and accessibility UI tests |
| 05 Memory/Catalogue | Persistent catalogue, search, duplicate detection, price history, retention, backup | Catalogue / data lifecycle | Search after restart; duplicate handling; historical snapshot preservation |
| 06 Engineering Quality | No demo shortcuts, implementation discipline, tests and reliability | Quality engineering | Requirement traceability and release gate |
| 07 Architecture | Kotlin/Compose/Room, money/quantity representation, offline-first and platform security | Android architecture / domain | Architecture tests; migration tests; offline core tests |
| 08 Information Architecture | Screen map and primary workflows | UX / navigation | Screen-to-workflow coverage and navigation tests |
| 09 Visual Design | Layout, typography, data presentation, motion, RTL, accessibility | Design system | Screenshot/UI review plus RTL and accessibility checks |
| 10 Interaction Specifications | Catalogue autocomplete, item entry, unit selection, allocations, payments, settlement, reminders, import/export | Android feature flows | UI/integration tests for each interaction |
| 11 Financial Logic | Pricing basis, quantity conservation, allocation, equal/custom splits, contributions, balances, settlement, duplicates | Financial domain engine | Fixed-point, rounding, quantity conservation, zero-sum and idempotency tests |
| 12 Color System | Semantic colors, modes, component tokens | Design system | Theme/token checks and contrast validation |
| 13 Templates/Scenarios | Purchase templates and mixed allocation/payer/settlement/unit scenarios | Purchase workflows | Scenario-based domain and integration tests |
| 14 Extensions/Import/Export | CSV/JSON/PDF, backup/restore, Android integration, notifications | Data portability / platform | Round-trip export/import; staged restore; permission-denied tests |
| 15 Master Build Prompt | Product authority, engineering method, acceptance and delivery | Governance / execution | Compliance review against source-of-truth and definition-of-done |
| 16 Location/Merchants | Merchant and branch records, location privacy, price observations | Merchant directory | Permission/opt-in, offline behavior, comparable-price tests |
| 17 Pantry/Inventory | Stock batches, movements, expiry, purchase-to-stock, replenishment | Inventory | Quantity reconciliation, expiry and duplicate-event tests |
| 18 Budgets/Goals | Budget arithmetic, goals, forecast, thresholds and alerts | Budgeting | Period-boundary, forecast-not-actual, threshold idempotency tests |
| 19 Receipt OCR | Capture, extraction pipeline, verification, arithmetic reconciliation | Receipt processing | OCR draft review; no auto-post; arithmetic mismatch tests |
| 20 Barcode Lookup | Barcode identity/variants, online/offline lookup, duplicate conflicts, labels | Product identity | Scan/manual fallback, ambiguous match and duplicate tests |
| 21 Recurring/Households/Trips | Recurrence, shared workspace boundaries, lists, trips and financial separation | Recurring / collaboration | Retry idempotency; occurrence lifecycle; access and ledger separation |
| 22 Reports/Insights/Price History | Reports, trends, comparable prices, explanations and exports | Analytics / reporting | Snapshot consistency, currency comparability and export reconciliation |
| 23 Sync/Collaboration/Security | Identity, events, conflicts, idempotency, authorization, encryption, sync lifecycle | Sync / security | Conflict matrix, authorization, duplicate delivery and privacy tests |
| 24 Test Strategy | Traceability, edge cases, triple verification and release evidence | QA / release engineering | Structural, semantic and artifact verification reports |
| 25 Accessibility/Localization | Arabic/English, RTL/LTR, dates/numbers/currency, semantics and inclusive UX | Localization / accessibility | Arabic and English UI tests; TalkBack, large-font and contrast checks |
| 26 Data Governance | Classification, local storage, consent, retention, deletion, export privacy, logs | Privacy / data governance | Data lifecycle tests; redaction checks; consent and deletion verification |
| 27 Performance/Reliability | Startup/search/write targets, background work, process death, restore and recovery | Performance / reliability | Benchmarks on documented devices; process-death, low-storage and restore tests |
| 28 My Expenses | Personal ledger, categories, recurrence, analysis, budgets, reports, exports and explicit group-share transfer | Personal finance ledger | CRUD/archive, period boundaries, currency precision, recurrence idempotency, transfer idempotency and cross-ledger no-double-count tests |

## Cross-cutting invariants

1. Money: integer minor units plus currency code/exponent; no binary floating-point persistence.
2. Quantity: explicit unit/dimension and compatible conversion only.
3. Atomicity: multi-record financial operations commit fully or roll back.
4. Idempotency: retrying payments, recurring occurrences, imports, sync events or share transfers must not create duplicates.
5. History: catalogue/category/merchant changes do not rewrite historical transaction snapshots.
6. Separation: shared purchase ledger and personal expense ledger remain independent unless an explicit, linked, idempotent transfer is confirmed.
7. Provenance: every posted amount has a source, owner/allocation and auditable history.
8. Offline: core workflows remain usable without network; optional external services are explicit and consent-based.
9. Restore: validate in staging and replace live data only after integrity, foreign-key and reconciliation checks.
10. Completion: a feature requires domain behavior, persistence, validation, UX states, accessibility and executed tests—not a screen or successful build alone.

## Traceability record template

For each requirement, add:
- Requirement ID and exact source section.
- User-visible behavior.
- Domain invariant.
- Android module/class and/or backend module/API.
- Database entity/migration.
- Unit, integration, database and UI test IDs.
- Actual command, environment, result and artifact/commit SHA.
- Open risk, decision and owner.

No test status is pre-populated as passed. No implementation class, migration, API, screen or test ID is claimed to exist until created and verified.


---

## Source document: `docs/awareness-system.md`

# ROFOF local awareness system

## Runtime boundaries
- Runs entirely in the Android process using bundled JSON assets, Kotlin and Room.
- No network client, AI SDK, internet permission, telemetry or remote rule source is used.
- Room v8 remains the source of truth. Seeder writes only missing category catalog rows.
- Analysis is a read-only preview. A user must apply/review the suggestion and press Save before any transaction is written.
- Currency comparisons are scoped to the selected account currency and exponent. Monetary values use minor-unit Long values and BigDecimal for ratios.

## Configuration
- `app/src/main/assets/awareness/categories.json`: stable category IDs and canonical names.
- `keywords_ar.json`: Arabic phrase-to-category mappings. Longest matching phrase wins.
- `rules.json`: deterministic conditions, priorities and bilingual actions.
- Supported operators: eq, neq, gt, gte, lt, lte, contains, exists, between.
- Rule conditions are AND-combined. Missing facts do not match ordinary comparisons.

## Room seeding
`DatabaseSeeder.seedFromAssets(context)` runs during ViewModel initialization. It is transactional and idempotent, preserves existing matching user categories, and does not create accounts, transactions, budgets or goals.

## Facts currently supplied to the rule engine
- categoryKnown, categoryId, transactionType
- localHour, isWeekend, isMonthEnd
- projected balanceMinor for the selected account
- budgetPercent including the proposed expense
- weeklySpendRatio, amountRatioToAverage and recurringSimilarityCount when sufficient same-category, same-currency history exists
- daysUntilRent when the local preference `awareness.rent_due_epoch_millis` is configured
- isFirstLaunch

Unsupported or unavailable facts are intentionally omitted rather than guessed. In particular, rent reminders require a due-date preference. Weekly, unusual-amount and recurring rules require enough historical records.

## Review and audit
The dashboard displays the proposed type, amount, category, and matched bilingual rule messages. Applying the review is a separate user action. The advisor never inserts a transaction. The latest matched rule IDs are stored locally under `awareness.last_review_rule_matches`; this is a compact local diagnostic, not a remote event log.

## Verification
Robolectric tests cover seeding idempotence, JSON validation, deterministic rule evaluation, Arabic entry resolution, unknown categories, proposed-expense budget thresholds, and the advisor's no-ledger-write guarantee. CI remains the authority for full build, lint and emulator status.


---

## Source document: `docs/implementation/PHASE_1_ANDROID_FOUNDATION.md`

# ROFOF Phase 1 — Android Foundation Implementation

**Status:** Foundation source added; automated verification pending.  
**Branch:** `feat/rofof-android-foundation`  
**Architecture:** Android local-first; Room is local source of truth; backend/sync deferred.

## Preserved work

This branch is based on the Phase 0 discovery commit and retains the discovery report and requirement traceability baseline. It does not merge or modify `main`.

## Implemented in this phase

- Accepted ADRs for local-first authority, backend deferral, external processing, exact money representation and dependency direction.
- Android Gradle application skeleton using Kotlin 2.0.21, Compose, AGP 8.7.3, Gradle 8.9 and JDK 17.
- Launcher Activity and manifest with RTL support and no INTERNET permission.
- Exact `Money` value type using integer minor units, currency code and exponent; checked addition/subtraction and currency compatibility validation.
- Initial Room database with a local preferences entity and DAO, plus in-memory instrumentation tests.
- JVM unit tests for exact addition, negative balances, currency mismatch, invalid currency metadata and overflow.
- GitHub Actions workflow for JVM tests, lint, debug APK assembly and emulator-backed Room instrumentation tests.
- Android/Gradle/IDE/signing-secret ignore rules.

## Verification status

No successful build or test result is claimed in this report. The first final-head run reached SDK setup and exposed an obsolete Android setup action version that requested the removed SDK package `tools`. The workflow has been updated to `android-actions/setup-android@v4`, whose current configuration no longer requests that package. A new final-head run must complete before this phase can be considered verified.

Required checks:
1. `testDebugUnitTest`
2. `lintDebug`
3. `assembleDebug`
4. `connectedDebugAndroidTest` (Room DAO insert/observe/delete)

## Explicit limitations

- The Room schema currently contains only local preferences. It is not the production financial schema.
- No purchase, participant, allocation, contribution, settlement or personal-expense workflows are implemented yet.
- The launcher is a foundation placeholder, not the final UI.
- No APK artifact is yet verified.
- No backend, network sync, login, remote storage or payment execution is included.

## Next implementation gate

Design the production transactional schema from the 28 v1.2.0 specification documents, then implement the money/quantity allocation engine and its invariant tests before adding financial UI.


---

## Source document: `docs/implementation/PHASE_4_SHARED_PURCHASES.md`

# ROFOF — Priority 2: Shared Purchases

**Status:** First local-first vertical slice implemented; verification pending.  
**Branch:** `feat/rofof-shared-purchases`  
**Database:** Room schema version 5 (migration 4 → 5).

## Implemented source scope

- Purchase sessions with explicit ISO currency metadata and OPEN/SETTLED state.
- Participants are scoped to one session; participant names are unique within that session.
- Items store quantity at 1/1000 precision, unit price in integer minor units and a persisted line total.
- Line totals use integer arithmetic and deterministic half-up rounding; overflow is rejected.
- Per-item allocations are explicit per participant. The sum must equal the item total exactly; no equal-share assumption is made.
- Actual payments are stored separately from expected allocations. Positive amounts and unique idempotency keys are required.
- Participant settlement rows expose allocated, paid and remaining/credit amounts. Overpayments remain visible as negative balances.
- Closing a purchase requires every item to be fully allocated and every positive outstanding balance to be paid.
- An allocation can be explicitly added to My Expenses once. This creates a normal expense transaction and a unique source link in the same Room transaction, with account/category and currency validation.
- Shared purchase records are not automatically inserted into the personal ledger.

## Data integrity

- Room foreign keys protect session, participant, item, allocation, payment and source-link relationships.
- Unique indexes prevent duplicate participant names within a session, duplicate participant allocation per item, duplicate payment idempotency keys and duplicate personal-expense links.
- Multi-record workflows use Room transactions.
- Money is integer minor units; no floating-point arithmetic or implicit cross-currency conversion is used.
- Core features remain fully local. No backend, login or network dependency is introduced.

## UI

- Shared purchases are reachable from the main app navigation.
- Users can create a purchase, add participants and line items, allocate item totals, record payments, review balances, close a settled session and explicitly link an allocation to My Expenses.
- Personal-expense linking requires the user to choose a matching-currency account and an active expense category.

## Verification gate

The branch CI must pass on its exact latest commit:
1. JVM unit tests, including line-total rounding, overflow and allocation exactness.
2. Android lint.
3. Debug APK assembly.
4. Emulator-backed Room tests for participant balances, exact allocation, payment idempotency, settlement gating and explicit personal-expense linking.

The v4 → v5 migration also requires an upgrade-path test against a real v4 database before this phase can be considered complete. No merge, release APK or deployment is claimed until verification evidence is available.


## Account and category management

- Account management now supports renaming and confirmed archival. The default cash account is not offered for archival.
- Category management now supports renaming and confirmed archival, retaining the archived entries in the management list while excluding them from active pickers.
- Archived accounts and categories remain persisted and discoverable through dedicated management flows; no financial rows are deleted by archival.
- Repository guards prevent archiving accounts with ledger or transfer activity, and categories with transactions or active children. Failures and successful changes are surfaced to the user in Arabic or English.
- Robolectric Compose UI tests cover account/category rename, archive confirmation, default-account protection, and archived-entry visibility. Repository tests cover persistence, activity guards, parent-child ordering, and active-vs-management queries.


## Purchase maintenance — next incremental slice

- Participants can be renamed while the purchase is open.
- Archiving a participant is allowed only before any allocations or payments exist; participants with financial history remain active/visible so settlement totals cannot disappear.
- Unallocated items can be edited with line totals recalculated by the fixed-point purchase math.
- Items can be deleted only before allocation; allocated item history is immutable.
- Robolectric repository tests cover participant rename/archive guards and item update/delete guards.
- UI controls for these operations are tracked as the next UI integration step; this repository slice does not claim the full interaction is complete.


---

## Source document: `docs/implementation/PHASE_5_BUDGETS_AND_GOALS.md`

# Phase 5 — Budgets and Financial Goals

## Scope

This phase adds persistent monthly category budgets and user-maintained financial savings goals without changing transaction, account, or shared-purchase records.

## Storage and migration

- Room schema advances from version 5 to version 6.
- Migration 5→6 creates `budgets` and `financial_goals` with explicit indexes.
- Budgets store a category, currency/exponent, integer minor-unit limit, and an explicit half-open period [start, end).
- A unique index prevents duplicate budgets for the same category, currency, and period start.
- Goals store a target and a separately maintained saved amount in integer minor units, optional deadline, archive state, and timestamps.

## Behavior

- The current-month budget view derives actual spending from ledger expense transactions matching category, currency, exponent, and the budget's period boundaries. Income, other categories, and out-of-period entries are excluded.
- Remaining amount and over-budget state are derived; budget values do not mutate ledger transactions.
- Budget limits can be edited or deleted.
- Financial goals can be created with an optional ISO date deadline, have their saved amount updated, and can be archived without deleting history.
- Currency exponent validation uses ISO currency metadata. Amount conversion uses `BigDecimal` and exact integer minor units; floating-point arithmetic is not used for persisted money.
- The feature is available from a dedicated bilingual Arabic/English screen, with RTL/LTR layout direction.

## Verification

Robolectric Room repository tests cover period/category/type filtering, duplicate budget prevention, invalid category and amount rejection, goal progress validation, archive retention, and version-6 table creation.

## Boundaries

Goal progress is manually maintained in this phase; it is not inferred from account balances or transaction descriptions. Budgets cover the current month in the UI; historical budget periods remain stored and are queryable by period start. No automatic transfers, savings movements, or transaction creation are performed by budget/goal actions.


---

## Source document: `docs/implementation/PHASE_6_RECURRING_DRAFTS.md`

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


---

## Source document: `docs/implementation/PHASE_7_REPORTS_AND_SEARCH.md`

# Phase 7 — Reports and Transaction Search

## Scope
Adds a local monthly expense-by-category report and case-insensitive search across transaction titles and notes. The report filters by one explicit currency code/exponent and a half-open month interval; it never combines currencies.

## Implementation
- Read-only aggregate query joins ledger transactions to category labels.
- Search matches title or notes, orders newest first and caps results at 200.
- Bilingual reports/search screen is reachable from Settings.
- No schema migration or ledger mutation is required.

## Verification
Robolectric Room tests verify income and out-of-period expenses are excluded and search matches title/notes case-insensitively. CI must pass tests, lint and debug APK assembly on the exact branch head.

## Boundaries
The initial monthly report is EGP-only. Date-range controls, charts and advanced filtering remain follow-up work.


---

## Source document: `docs/implementation/PHASE_8_CSV_EXPORT.md`

# Phase 8 — Transaction CSV Export

## Scope
Adds a user-initiated local export of all non-deleted ledger income/expense transactions through Android Storage Access Framework.

## Data contract
- UTF-8 with BOM for Arabic compatibility.
- Stable columns include transaction ID, title, type, decimal amount, currency/exponent, account/category IDs, UTC occurrence/creation timestamps and notes.
- Integer minor units are formatted using each transaction's stored currency exponent; currencies are never combined.
- CSV fields are quoted and embedded quotes doubled.
- Text cells beginning with spreadsheet formula prefixes (=, +, - or @ after leading whitespace) are prefixed with an apostrophe.
- Export is read-only and user-initiated; no storage permission or network upload is required.

## Verification
Unit tests cover Arabic text, UTF-8 BOM, decimal formatting, UTC timestamps, commas, embedded quotes, formula-prefix neutralization and empty exports.

## Limitations
This stage exports ledger transactions only. Shared purchase sessions, budgets, goals, transfers and attachments are separate export scopes.


---

## Source document: `docs/implementation/PHASE_9_RECURRING_APPROVAL.md`

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


---

## Source document: `docs/implementation/PHASE_10_BUDGET_OVERRUN_ALERTS.md`

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


---

## Source document: `docs/implementation/HYBRID_15_WORKSTREAMS.md`

# ROFOF — Hybrid 15-workstream execution

Branch: `feat/rofof-hybrid-15-workstreams`  
Base: `feat/rofof-roadmap-completion`  
Policy: preserve the cumulative branch; no merge, release, signing, or deployment.

## Delivery contract

A workstream is complete only when source, tests, UI/integration where applicable, documentation, and CI evidence exist. A domain helper or design note alone does not mark a product workflow complete.

| # | Workstream | Source delivered in this slice | Remaining acceptance gate |
|---|---|---|---|
| 1 | Financial anomaly detection | Currency/category-scoped median detector; minimum five comparable prior entries | Persisted history query, review UI, false-positive tests |
| 2 | Subscription intelligence | Stable amount + cadence candidate detector | Opt-in subscription list, reminder UX, recurring-link tests |
| 3 | Behavioral finance | Deterministic category-concentration ranking primitive | Multi-period summaries and bilingual UI |
| 4 | Scenario simulator | Exact integer-minor-unit what-if calculator | Scenario editor, currency guards, boundary UI tests |
| 5 | Financial health score | Explainable bounded score with component factors | Product explanation, calibration and accessibility review |
| 6 | Account reconciliation | Per-account/per-currency statement comparison | Import workflow, adjustment approval and Room integration |
| 7 | Bills center | Date-window and unpaid-item filtering | Bill persistence, reminders, recurrence and permission UX |
| 8 | Receipt intelligence | Validated draft-readiness gate for extracted fields | On-device OCR, image lifecycle, confirmation and duplicate detection |
| 9 | Net worth | Currency-separated asset/liability arithmetic | Asset/liability persistence, valuation history and dashboard |
| 10 | Cash-flow calendar | Date/currency-separated daily net calculator | Calendar UI, recurring/bill integration and uncertainty display |
| 11 | Offline sync conflicts | Divergent financial payloads never auto-merge | Local-only default; opt-in transport, encryption, idempotency and recovery tests |
| 12 | Financial audit trail | SHA-256 append-chain primitive and tamper verification | Durable Room audit table, transactional hooks, retention/export and migration tests |
| 13 | Property-based financial testing | Seeded deterministic scenario/invariant tests | Generated transfer/ledger/import/restore invariants and reproducible failure seeds |
| 14 | Performance & battery lab | Large deterministic test baseline | Macrobenchmark, startup/scroll/DB/battery thresholds |
| 15 | Accessibility & compatibility lab | Existing Compose semantics remain integration surface | Semantics checks, TalkBack, RTL/LTR, font scaling and API/device matrix |

## Safety invariants

- Room remains the local source of truth. No background or cloud sync is enabled by this work.
- Currency code and exponent are part of every monetary grouping key.
- Monetary calculations use integer minor units and checked/BigInteger intermediate arithmetic.
- Receipt extraction is draft-only; low-confidence or incomplete values cannot be posted.
- Divergent financial records are never resolved by last-write-wins.
- Audit-chain hashing is tamper-evident, not a claim of tamper-proof storage; durable append-only enforcement remains an integration gate.
- No workstream may silently change or rewrite existing ledger records.

## Verification

This branch is based on the latest cumulative roadmap branch head. Inspect the branch-specific GitHub Actions run before claiming compilation, unit tests, lint, emulator tests, or APK success. The 15-workstream program remains open until all acceptance gates above pass.


---

## Source document: `docs/implementation/NEW_15_ENHANCEMENTS_AND_V6_INPUT.md`

# ROFOF — New 15 enhancements + V6.0 input experience

Branch: \`feat/rofof-hybrid-15-workstreams\`  
Baseline: \`dc16aa40db84ee98cb10ee68005a98671ae4401e\`  
Delivery policy: keep all existing commits; no merge, release, APK signing, or deployment.

## Added implementation

| Requested order | Capability | Delivered source |
|---:|---|---|
| 7 | Financial data quality checks | \`FinancialDataQuality\`: required-field, positive-amount, currency/exponent, timestamp and duplicate-ID checks |
| 10 | Data lineage | \`DataLineage\`: validates source IDs and transformation provenance |
| 13 | Recovery lab | \`RecoveryLab\`: canonical SHA-256 manifest verification; differential planning detects changed/deleted entries |
| 2 | Financial automation rules | \`FinancialRuleEngine\`: deterministic allowlisted conditions/actions; no arbitrary code execution |
| 11 | Differential backups | \`DifferentialBackupPlanner\`: path-safe changed/unchanged/deleted manifest planning |
| 1 | Multi-level approvals | \`FinancialApprovalEngine\`: amount threshold, approval count and eligible role set |
| 3 | Dynamic budgets | \`DynamicBudgetEngine\`: median-based suggestion with bounded movement and hard floor/ceiling |
| 4 | Sensitive operations | \`SensitiveOperationGuard\`: confirmation and re-authentication decisions |
| 5 | Tax document center | \`TaxDocumentIndex\`: metadata, year, MIME and SHA-256 validation |
| 6 | Merchant intelligence | \`MerchantIntelligence\`: normalized canonical-name/alias lookup |
| 15 | Application health monitor | \`ApplicationHealthMonitor\`: latency, error and queue-depth threshold assessment |
| 14 | Localization coverage | \`LocalizationCoverage\`: missing/extra key and placeholder parity report |
| 12 | Plugin architecture | \`LocalPluginRegistry\`: capability allowlist; no dynamic code loading or elevated execution |
| 9 | Seasonality | \`SeasonalityAnalyzer\`: monthly average and peak-month calculation |
| 8 | Financial sensitivity | \`FinancialSensitivity\`: bounded basis-point scenario adjustment with overflow protection |

The engines are local deterministic foundations. They do not claim completed screens, durable persistence, automatic tax filing, background backup execution, production telemetry, or a dynamic plugin runtime. Those require separate reviewed integrations.

## V6.0 — input experience

- **Voice:** a Compose microphone action uses Android \`SpeechRecognizer\`, requests \`RECORD_AUDIO\`, and passes Arabic-Egyptian or English locale according to app language.
- **Offline caveat:** \`EXTRA_PREFER_OFFLINE\` is a recognizer preference, not a guarantee. Actual offline operation depends on the installed recognition service and downloaded language pack. The UI reports recognizer errors instead of claiming offline success.
- **Smart paste:** clipboard is read only after the user taps Paste. A bank-like message is parsed into a suggestion and requires explicit confirmation/review; it never posts a transaction directly.
- **Inline arithmetic:** the existing bounded \`MathExpressionEvaluator\` and \`SmartEntryParser\` remain the arithmetic implementation. Arithmetic expressions are accepted in the smart-command field, not silently evaluated as arbitrary code.
- **Frequent categories:** Room query returns the four most-used active expense categories.
- **Typical amount:** Room query averages the five most recent non-deleted expenses for the selected category and exact currency/exponent.
- **Haptics:** the success callback fires only after the repository insert succeeds; the Compose haptic is triggered from that callback.

## Changed files

- \`feature/operations/OperationalEnhancements.kt\`
- \`feature/smartentry/SmartPasteParser.kt\`
- \`feature/smartentry/SmartEntryAssistComponents.kt\`
- \`data/local/FrequentCategory.kt\`
- \`data/local/TransactionDao.kt\`
- \`feature/dashboard/ExpenseViewModel.kt\`
- \`feature/dashboard/DashboardScreen.kt\`
- \`app/src/main/AndroidManifest.xml\` (runtime microphone permission declaration)
- Unit and Room tests for operational engines, smart paste, and suggestion queries.

## Safety and data rules

- All money suggestions remain in integer minor units and are scoped by currency code and exponent.
- Clipboard access is user-initiated; clipboard text is not polled on screen open.
- Voice and OCR/paste output are suggestions only. User review is required before saving.
- No banking connection, network sync, remote AI, cloud storage, or automatic transaction posting was added.
- The plugin registry validates capability names only; it does not load or execute plugins.
- Recovery manifest verification is integrity checking, not a complete device restore implementation.
- Tax document support validates metadata only; it does not provide tax advice or submit filings.

## Verification status

New unit tests cover the operational engines and smart-paste parser. Room tests cover the recent-five average, currency separation, deleted/income exclusion, and top-four category ordering.

The existing GitHub Actions run \`45399097427\` was still in progress at the time of the initial check. Do not treat this change as build-verified until a branch-head run reports successful unit/Robolectric tests, lint, debug APK assembly, and emulator instrumentation. No merge or deployment is authorized.


---

## Source document: `docs/implementation/ROADMAP_COMPLETION_DEDUPLICATION.md`

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


---

## Source document: `docs/implementation/APK_BUILD_AND_BRANCH_INTEGRATION.md`

# ROFOF — APK build and branch integration

## Integration branch
- Base: `feat/rofof-hybrid-15-workstreams`
- Integration: `feat/rofof-apk-integration`
- Main remains unchanged.
- Existing Android source, Room schema/migrations, assets, tests and prior implementation history are retained.

## Branch reconciliation
The following implementation deltas were reconciled into this branch:
- Phase 7: monthly category report and transaction search UI, DAO queries, Room tests and documentation.
- Phase 8: full-ledger CSV export with UTF-8 BOM, stable IDs/currency metadata/UTC timestamps, CSV quoting and spreadsheet formula-prefix neutralization; dashboard export action and tests were wired to the safe exporter.
- Phase 9: explicit recurring draft approve/skip workflow, atomic repository, Room tests and documentation. The existing day-of-month scheduling was retained.
- Phase 10 budget alert source and tests were already byte-for-byte present in the cumulative branch.
- Existing CSV export, backup/debt foundations, local-first dashboard, shared purchases, budgets, recurring generation and V6 input enhancements were retained from the cumulative branch.
- Older foundation/phase branches were not blindly merged over newer source because they contain stale versions of Room, dashboard and recurring files. Their implemented source was checked against the cumulative tree before reconciliation.

## Cloud build
Two workflows are available:
- `.github/workflows/android-ci.yml`: unit/Robolectric tests, lint, debug APK assembly, emulator instrumentation and verification artifacts.
- `.github/workflows/build.yml`: focused debug APK build and dedicated `ROFOF-Debug-APK` artifact.

The focused build uses JDK 17, Android SDK 35 and Gradle 8.9 installed by GitHub Actions. It intentionally does not call `./gradlew`, because the repository does not currently contain `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` or `gradle-wrapper.properties`. No incomplete wrapper files are fabricated.

## Downloading the APK
1. Open the repository Actions tab.
2. Select **ROFOF Debug APK** and open the newest run for `feat/rofof-apk-integration`.
3. Wait for the run to complete successfully.
4. Download **ROFOF-Debug-APK** from Artifacts.
5. Extract the ZIP and install `app-debug.apk` on an Android device. The debug package uses the `.debug` application ID suffix and can coexist with a release package.

## Verification gate
Do not describe an APK as available until the workflow succeeds and the artifact exists. A successful debug build is not a release-signed APK and is not a Play Store release. No signing secrets, release signing, publishing or deployment are configured here.



---

# APPENDIX B — Recovered conversation instructions, chronology, and execution constraints

## B1. Historical chronology (2026-11-23 to 2026-11-25)

### 2026-11-23 — project discovery and original source pack
- User identified ROFOF Q / ROFOFAQ as the daily-expenses Android application and asked for repository inspection.
- Initial inspection of `main` found only `.gitignore`, `LICENSE`, and `README.md`; this did **not** represent the whole project. The user corrected the inspection because a separate branch contained `ROFOF_GitHub_Engineering_Pack_v1.0.0.zip` (reported around 1.78 MB) on `c83491307-gif-patch-1`.
- Historical discovery reported that the engineering pack contained 28 specification documents, 14 reference tables, 410 records, and 35/35 SHA-256 checks; SQLite `integrity_check` and foreign-key checks were reported clean. These are historical reported results, not a fresh revalidation in this consolidation task.
- User asked that the full original specification set, financial precision and rounding rules, independent My Expenses ledger, backup/restore, Arabic/English RTL/LTR, security, tests, and release-ready Android delivery be preserved.
- User explicitly chose Android local-first, Kotlin/Compose/Room/WorkManager, with architecture allowing optional future backend/sync. Discovery branch `feat/rofof-phase0-discovery` was created; discovery commit reported as `d2661220f629806523e71eace65d98c766ebd1c2`.

### 2026-11-24 — scope, architecture, cumulative development
- User required work exclusively in `c83491307-gif/ROFOFAQ`; never use Creazzy AI repository. Preserve all prior work, inspect every file and branch, balance coverage across components, and never claim a test passed without evidence.
- User approved continuing the cumulative native Android roadmap on `feat/rofof-roadmap-completion`.
- User required the 15 hybrid workstreams to be implemented in sequence, with source, tests, documentation, and preservation of all prior work; no merge or publish during execution.
- User then requested the additional 15 enhancements in this order: **7, 10, 13, 2, 11, 1, 3, 4, 5, 6, 15, 14, 12, 9, 8**. Keep this exact historical order in the traceability record; do not silently reorder it.
- User required CI/build setup including Gradle, Manifest, assets, `.gitignore`, and an `assembleDebug` workflow; later asked to consolidate commits/branches and prepare APK, while subsequent instructions narrowed merge/release authorization.
- User requested review of PR #9 and safe main integration after CI, but later instructions repeatedly said **do not merge or publish** while fixing CI, README, and APK evidence. Latest explicit action governs: no merge/publish unless newly authorized.
- User requested a full remaining-roadmap completion in sequence. Explicit release-readiness gates named by user: CI; CSV import/export; backup/restore; encryption; PIN/biometrics; debt/loans; reports/currencies; Room migration tests; performance/memory; RTL/LTR; signed APK preparation/installation; only then consider merge/release with authorization.
- User explicitly asked to prevent work loss caused by long conversations, slow responses, or branch/new-chat transitions. The project workflow must preserve context, checkpoint progress, and resume from actual repository state rather than redoing or forgetting work.
- User requested repair of `MissingPermission` in `NotificationHelper.kt:56` on the integration branch and rerun tests/lint/build.
- User requested following GitHub Actions runs **45447548639** and **45447548691** through Unit tests, Lint, Debug APK and Emulator; fix failures and rerun until success, without merge or publish.
- Earlier CI work also included run IDs **45399097427** and related workflow attempts; one documented failure was Maven Central HTTP 429 resolving a Robolectric artifact before tests ran. Treat it as an infrastructure/rate-limit failure, not a failing assertion and not a pass.
- User later asked to follow runs **45447548639** and **45447548691** through completion, including emulator, and fix all failures. This instruction was subsequently narrowed by a later request to build APK without running emulator; obey the latest task-specific emulator restriction.

### 2026-11-24 — APK, signing, and merge decisions
- PR #11 for the signed Release workflow was merged to `main` on 2026-11-24; main merge commit observed as `245e56d1f9706967bfed2537c938de8ccf680089`, with parents `305cff4c1b4739e93ad57f8d5d6b08825a7ef399` and `c7fa78f3c592406064261bc03a26671a2b127a67`.
- The user subsequently instructed: prepare signing secrets safely, then build Release APK and verify, without emulator. Later the user explicitly said **build the APK now and leave signing secrets for a later stage**. The latest instruction means do not block a debug/unsigned build on signing secrets and do not fabricate secrets.
- Later user asked to continue PR #11 merge safely and verify workflow on main without emulator; historical state says PR #11 is already merged. Do not repeat a merge action; verify current state instead.
- Latest user request before this consolidation: recover all instructions, directions, explanations across multiple conversations/branches and put **everything into one skill.md**. The user clarified that there is more than one branch. This overrides any interpretation that a short overview plus links is sufficient.

## B2. Explicit workflow and continuity contract

1. Never treat a new chat or branch as a clean slate. Start with this skill and repository state.
2. Inspect every branch, not only main or a branch called new-chat. Discover branch-specific unique commits, source, docs, tests, workflows, and artifacts.
3. Maintain one canonical consolidated `skill.md` with all project instructions. Add a dated append-only decision/history entry when new requirements arrive. Do not silently delete older requirements; mark superseded proposals as superseded with reason and date.
4. Preserve a branch/commit ledger and feature-to-branch map. When a feature exists only on a branch, state that explicitly; do not describe it as integrated into main.
5. Preserve exact execution order when the user provides one. Do not launch parallel work when user asks for sequential execution.
6. At each pause, record: last verified commit, branch, work completed, files touched, commands/tests actually run, Actions run IDs and outcomes, artifacts, blockers, and exact next action.
7. Continue from the latest verified checkpoint. Do not restart, discard, overwrite, or duplicate existing work.
8. Do not use a planning checklist as proof of implementation. Inspect actual source, migrations, tests, workflow logs, and APK contents.
9. Do not merge, publish, deploy, or sign unless the current user instruction explicitly authorizes that action. Historical requests to merge do not override a later explicit prohibition.
10. Do not run emulator when current request says without emulator; do not claim emulator success when skipped.

## B3. Repository/branch and CI evidence ledger

- Correct repository: `c83491307-gif/ROFOFAQ` only.
- Product: ROFOF / ROFOFAQ.
- Branches historically observed include discovery, Android foundation, shared purchases, roadmap completion, hybrid 15 workstreams, APK integration, analytics/search, budget alerts, CSV export, recurring approval, release signing, signed release, unsigned release build, dashboard UI, and patch/engineering-pack branch. See the branch table in Section 7; refresh the live branch list before every task.
- Main at the 2026-11-25 consolidation observation: `245e56d1f9706967bfed2537c938de8ccf680089`.
- Documentation branch created for this skill: `docs/rofof-project-memory-skill`; first commit `1e3e3b48794cc9cefae0cb60727900b049b592d1`. This file update is to remain branch-local unless user authorizes PR/merge.
- Historical Actions run IDs: `45399097427`, `45447548639`, `45447548691`. Do not assume their final states from user instructions alone; fetch their latest statuses/logs/artifacts.
- CI failure recorded: Maven Central HTTP 429 while resolving Robolectric dependency before test execution; bounded retry/backoff was added. Only final successful exact-head run proves success.
- CI failure recorded: `MissingPermission` in `NotificationHelper.kt:56`; inspect current source and logs to verify whether corrected.
- Required normal CI gates: unit/Robolectric, lint, assembleDebug, emulator instrumentation when authorized, artifact upload, and exact SHA evidence.
- Build workflow uses JDK 17, Android SDK 35, Gradle 8.9. Repository documentation says no Gradle wrapper is present; inspect actual tree before invoking `./gradlew`.

## B4. Source-pack provenance limitation

The original engineering pack is a ZIP stored on `c83491307-gif-patch-1`. Historical work reported its inventory and integrity results, but its binary contents were not extracted in the current recovery session. Therefore this file captures the 28-document requirement map and all available detailed repository reports, but does not claim to reproduce every paragraph/table/record of the ZIP byte-for-byte. If the ZIP is later materialized and inspected, append its full 28 source documents and reference tables into this same skill.md (or embed them as full text sections here); do not create a second competing master skill.
