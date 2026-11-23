# ROFOF Phase 0 — Discovery Report

**Status:** Discovery baseline completed; implementation not started.  
**Repository:** `c83491307-gif/ROFOFAQ`  
**Inspection branch:** `feat/rofof-phase0-discovery`  
**Source branch:** `c83491307-gif-patch-1`  
**Source commit:** `888afc06eee3a10a6856c32daa32a55a0ba72ddc`  
**Specification:** ROFOF Android Complete Build Package v1.2.0

## 1. Source package verification

The uploaded Engineering Pack v1.0.0 contains nine entries, including the nested v1.2.0 specification archive. The nested archive contains 36 entries: 28 numbered specification documents, one package manifest, one concept image, and five database/reference resources.

- Outer archive size: 1,785,037 bytes.
- Outer archive SHA-256: `31b690dfc7730304cd58f151718d62ccded84832ca735962f13f40af10ac3139`.
- Nested specification archive size: 1,776,224 bytes.
- Nested archive SHA-256: `c0b41f594877146b8a8d00d34a9d429abda66da72b33d5f82e6f76440881d68c`.
- All 35 files declared in the nested manifest passed SHA-256 verification.
- Manifest counts agree with the archive: 28 specification documents, 14 reference tables, 410 seed records, one visual asset.
- SQLite `PRAGMA integrity_check`: `ok`.
- SQLite `PRAGMA foreign_key_check`: no violations.
- JSON, CSV and SQLite record counts agree for all 14 tables (410 total).
- SQLite `user_version=1`; reference dataset metadata version is `1.0.0`, distinct from product specification version `1.2.0`.

The visual asset is a concept image only, not proof of a running app. The archive explicitly states it is a specification/reference-data package, not an APK or compiled Android project.

## 2. Repository baseline

Both `main` and `c83491307-gif-patch-1` expose the same root text files: `README.md`, `.gitignore`, and `LICENSE`. The patch branch additionally contains the Engineering Pack ZIP. The commit adding that ZIP is `888afc06eee3a10a6856c32daa32a55a0ba72ddc`.

The README is a one-line product description. The current `.gitignore` contains Ada-only patterns and does not yet cover Android/Gradle, Kotlin, IntelliJ, local SDK configuration, signing material, or backend build output.

No Android source tree, Gradle build files, Room database implementation, backend source, API contracts, migrations, or test suite were present in the inspected branch tree. Therefore there is no existing app build or test baseline to report. This is a repository-discovery finding, not a claim about uninspected external files.

Repository collaborator permission for owner `c83491307-gif` was reported as `admin`. A dedicated discovery branch was created; no production branch was modified.

## 3. Product scope confirmed by the 28 documents

The 1.2.0 scope is broader than a daily-expense tracker. It includes:

1. Shared purchase sessions, item catalogue, quantity-aware pricing and allocations.
2. Participants, multiple payers, contributions, settlements and reconciled balances.
3. First-class independent My Expenses personal ledger.
4. Merchants, branches, location-aware store directory and price observations.
5. Pantry inventory, stock movements, expiry and replenishment alerts.
6. Budgets, goals, thresholds and spending forecasts.
7. Receipt capture and OCR as a reviewable draft; no automatic posting without confirmation.
8. Barcode/product lookup with manual fallback and duplicate/conflict handling.
9. Recurring lists, shared households and trips.
10. Reports, insights, exports and price history.
11. Optional sync/collaboration with explicit conflict, identity and security rules.
12. Accessibility, Arabic/English localization, data governance, privacy, performance, recovery and traceable tests.

Documents 01–15 are the preserved baseline. Documents 16–28 are additive requirements and remain in the complete v1.2.0 scope. Document 28 gives detailed acceptance rules for My Expenses and cross-ledger transfers.

## 4. Financial and data invariants

- Persist money as integer minor units plus ISO currency code and currency exponent; never persist money as binary floating point.
- Use explicit quantity precision and compatible physical dimensions; do not infer mass-to-volume conversions.
- Reconcile shared, personal and unallocated quantities to the purchased line.
- Separate obligations, actual contributions, settlements, refunds and credits.
- Preserve transaction-time snapshots when catalogue/category/merchant labels or prices change.
- Keep recurring rules and reminders separate from posted transactions.
- OCR results are drafts; barcode results are candidates; require user confirmation.
- A group allocation is not automatically a personal expense. An explicit transfer must be idempotent, linked to its source allocation and excluded from combined totals where the group amount is already counted.
- Multi-record financial operations must be atomic, auditable and retry-safe.
- Backup restore must use staging, checksums, SQLite integrity and foreign-key checks, recalculation, and atomic replacement only after validation.

## 5. Reference database assessment

The bundled SQLite database is a normalized starter/reference dataset, not the live application schema. Its 14 tables are: alert_rules, asset_device_templates, budget_guidance, categories, countries, currencies, document_types, habit_templates, inventory_products, maintenance_templates, metadata, tags, task_templates, and units.

It contains no live purchase sessions, purchase lines, participants, allocations, contributions, settlements, personal expenses, recurring expense occurrences, budgets, or audit ledger. Those transactional entities must be designed from the specifications; the 410 generic reference records must not be mistaken for user transactions or production records.

The SQL schema uses REAL for unit conversion factors and habit target values; these are reference quantities, not monetary fields. Money in the production domain must follow the fixed-point rules above.

## 6. Architecture decisions requiring resolution

### ADR-001 — Local-first authority versus server authority

The Android baseline (document 07) says core workflows work offline, require no account/network, and treats the local database as the source of truth. The Engineering Pack skill/target architecture instead describes PostgreSQL as the authoritative server-side source of truth and Room as local persistence/cache.

These are materially different authority models. Proposed safe interpretation for decision: keep core single-device operation fully local-first and make cloud identity/sync an explicit opt-in extension; when sync is enabled, define server authority and conflict/reconciliation rules only for synchronized shared state. This proposal is not yet an approved product decision and must be confirmed before backend/schema implementation.

### ADR-002 — Backend scope

The Engineering Pack adds Kotlin/Spring Boot, PostgreSQL, versioned REST APIs, authentication/authorization, background jobs, object storage and CI/CD. The baseline v1.2.0 Android specification does not require an account or network for core workflows and excludes several cloud behaviors from the first release. Decide whether backend is part of the first delivery or a separately gated optional deployment.

### ADR-003 — OCR and external services

OCR, maps, barcode lookups, remote lookups and sync must respect opt-in/privacy boundaries. OCR extraction must remain a draft requiring human verification. No external processing or upload should occur silently.

## 7. Immediate engineering gaps

- No native Android application source or Gradle project exists in the inspected branch.
- No production Room schema, migrations or DAOs exist.
- No financial domain engine or ledger exists.
- No application navigation, localization resources, or real UI exists.
- No backend/API/database migrations exist.
- No tests, CI workflows, build artifacts, or APK exist.
- Existing `.gitignore` is not aligned with the proposed stack.

## 8. Next implementation sequence

1. Approve ADR-001 and ADR-002 (local-only first release versus optional/required backend).
2. Produce a requirement traceability matrix linking every requirement to domain, entity, screen, migration/API, and test.
3. Design production transactional schema separately from the reference seed database.
4. Implement and test the financial/quantity engine before connecting UI.
5. Establish Android Gradle project, Room migrations, localization and CI.
6. Implement purchase/participant/allocation/payment flows and independent My Expenses.
7. Add the v1.2.0 extension modules in dependency order.
8. Verify backup/restore, offline recovery, accessibility, financial invariants and end-to-end workflows before release.

## 9. Evidence and limitations

This report is based on the uploaded archive and the inspected GitHub branch files/commit. No source code was changed, no APK was built, and no application tests were run. The discovery branch contains this report only at this stage. The two architecture decisions above remain open.
