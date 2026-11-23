# ROFOF Android Architecture — Local-first foundation

**Status:** Approved architecture baseline  
**Specification:** ROFOF Android Complete Build Package v1.2.0  
**Decision:** [ADR-001](ADR-001-LOCAL-FIRST-AUTHORITY.md)

## 1. Runtime shape

The Android app is a single-device, offline-capable application. Room is the first-release source of truth. Compose renders state exposed by ViewModels; ViewModels invoke use cases; use cases depend on domain contracts; repository implementations translate between domain models and Room records.

```text
Compose UI
   ↓ events / UiState
ViewModel
   ↓ commands
Domain use cases + policies
   ↓ repository interfaces
Data adapters
   ↓
Room database (local source of truth)

Optional future adapters (not in v1):
HTTP API / Sync engine / Remote identity
   ↓ explicit opt-in boundary only
```

Dependency direction points inward. Domain code must not import Android, Compose, Room, Retrofit/OkHttp, server DTOs or sync SDKs.

## 2. Proposed Gradle module boundaries

Start with a small modular structure; do not create a module for every screen.

| Module | Owns | Must not own |
|---|---|---|
| :app | Application startup, navigation host, dependency wiring, theme | Financial rules or SQL |
| :core:domain | Money/quantity values, entities, policies, use cases, repository contracts | Android/Room/network |
| :core:database | Room database, entities, DAOs, migrations, converters | UI and business decisions |
| :core:data | Repository implementations, mapping, transaction orchestration | Compose |
| :core:designsystem | Reusable Compose components, typography, tokens | Business state |
| :feature:home | Dashboard presentation | Persistence |
| :feature:purchases | Purchase session and allocation UI | Direct SQL |
| :feature:expenses | Independent personal ledger UI | Shared-purchase ownership |
| :feature:participants | Participant/contribution/settlement UI | Duplicate balance rules |
| :feature:reports | Read models and export presentation | Rewriting ledger history |

Feature modules may be introduced incrementally. The initial source scaffold can keep feature UI in :app until module extraction is justified, but the domain/data boundaries must remain real.

## 3. Domain contracts and caller flow

Example usage:

```kotlin
val result = createPersonalExpense(
    CreatePersonalExpenseCommand(
        amount = Money(minorUnits = 1250, currency = CurrencyCode("EGP"), exponent = 2),
        categoryId = categoryId,
        occurredAt = clock.instant(),
        note = "Groceries"
    )
)
```

The caller supplies a validated command and receives a domain result. It does not construct Room entities or execute SQL.

Core contracts (illustrative signatures; exact Kotlin declarations are implemented with the domain foundation):

- `PurchaseRepository`: observe sessions, create/update sessions, commit purchase operations atomically.
- `PersonalExpenseRepository`: observe and post independent personal expenses.
- `SettlementRepository`: record contributions and settlements with idempotency.
- `CatalogRepository`: query local catalogue and preserve transaction-time snapshots.
- `BackupRepository`: export/import validated local snapshots.
- `Clock` and ID generation: injected for deterministic tests.

Repository methods that change multiple financial records must expose one atomic use-case operation, not a sequence of UI-driven DAO calls.

## 4. Persistence ownership

Room owns durable storage and migrations. It does not own business policy. DAOs expose narrow persistence operations. Mapping between Room rows and domain values occurs in :core:data.

- Store money as signed 64-bit integer minor units plus ISO currency code and exponent.
- Store quantities as fixed-precision decimal text or a documented integer-scaled representation; never use binary floating point for financial values.
- Use stable UUID identifiers generated locally.
- Use UTC instants for event timestamps and preserve the relevant local date/time-zone context where reporting requires it.
- Use foreign keys, unique constraints and indexes to enforce structural invariants.
- Keep snapshots of names, units, prices and currency context on committed transaction lines.
- Use database transactions for posting, transfer and settlement operations.

## 5. Offline and future-sync boundary

First release:
- No account or login.
- No remote source of truth.
- No background network worker.
- No automatic transmission of user records.
- Export/backup is user-initiated and local-file based.

Future optional sync:
- Introduce a separate :core:sync adapter only after an approved sync ADR.
- Use stable IDs, schema versions, change sequence metadata and idempotency keys.
- Keep transport DTOs outside domain models.
- Define per-entity merge/conflict rules; financial events must not use last-write-wins.
- Require explicit user consent, visible sync status and recoverable retry behavior.
- Never sync sensitive attachments or OCR content without separate consent.

## 6. Financial ownership boundaries

- Purchase session owns shared purchase lines and participant allocations.
- Contributions represent actual payments; they are not the same as allocation obligations.
- Settlement records resolve balances; they do not rewrite historical purchase lines.
- My Expenses owns personal expense records independently.
- A shared allocation becomes a personal expense only through an explicit, confirmed, idempotent transfer that retains a source link.
- Combined reporting must avoid counting both the source allocation and its linked personal transfer twice.
- Recurring rules create drafts/occurrences; they do not post actual expenses automatically.

## 7. Failure handling

- Validate commands before persistence.
- Commit multi-row financial changes in a single Room transaction.
- Use idempotency keys for imports, transfer, recurring generation and payment recording.
- Surface storage-full, invalid-data and migration failures; never silently discard records.
- Backup restore uses staging, checksum validation, SQLite integrity and foreign-key checks, then atomic replacement.
- A failed operation leaves the previous committed state intact.

## 8. Architecture verification

The implementation phase must add:
- Domain unit tests with no Android runtime.
- Room DAO and migration tests.
- Repository transaction tests.
- Offline workflow integration tests.
- UI tests for Arabic RTL and English LTR.
- Architecture/dependency checks preventing domain-to-framework imports.

No test is considered passed until executed and its command/result is recorded.
