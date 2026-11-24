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
