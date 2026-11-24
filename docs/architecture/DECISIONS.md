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
