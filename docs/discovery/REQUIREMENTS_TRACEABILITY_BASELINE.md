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
