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
