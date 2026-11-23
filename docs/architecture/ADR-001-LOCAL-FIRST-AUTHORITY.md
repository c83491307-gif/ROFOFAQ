# ADR-001 — Local-first authority and optional synchronization

- **Status:** Accepted
- **Decision date:** 2026-11-23
- **Scope:** ROFOF Android v1.2.0
- **Decision owner:** Product owner (explicit approval in conversation)

## Context

The Android specification requires core workflows to operate offline, without an account or network, and identifies the local database as the source of truth. The Engineering Pack also describes a future server architecture. These requirements are reconciled by separating the first-release operating mode from a later opt-in synchronization capability.

## Decision

1. The first release is a native Android application using Kotlin, Jetpack Compose, Room, Coroutines and Flow.
2. Room is the authoritative store for all data created and edited on the device in the first release.
3. No account, backend, network permission, remote upload, or cloud dependency is required for core workflows.
4. Backend and synchronization are optional future capabilities. They are not implemented or activated by this decision.
5. Domain rules and use cases must not depend on Android UI, Room entities, HTTP, server DTOs, or a specific sync provider.
6. Persistence access is isolated behind repository interfaces. Room implementations live in the data layer.
7. Future synchronization must be explicitly enabled by the user, must define identity/workspace boundaries, conflict policy, deletion semantics, idempotency and audit behavior, and must never silently overwrite local financial records.
8. A future server may become authoritative only for explicitly synchronized collaborative workspaces under a separately approved ADR. Local-only personal data remains local unless the user explicitly exports or shares it.

## Consequences

### Positive
- The application remains useful in airplane mode and in low-connectivity environments.
- No backend credentials or operational infrastructure are required for the first release.
- Financial logic can be unit-tested without Android, SQLite or network dependencies.
- A later backend can implement the same repository-facing domain contracts through adapters.

### Constraints
- Multi-device collaboration is not part of the first-release promise.
- Local backup/export and restore are required because the device is the initial authority.
- Sync-ready identifiers and audit metadata may be stored locally, but they do not imply that sync exists.

## Explicit non-goals

- No Spring Boot service, PostgreSQL database, cloud account, remote analytics, remote OCR or background upload in the first-release foundation.
- No automatic cloud backup.
- No automatic posting of OCR or recurring-expense drafts.

## Acceptance checks

- Core purchase and personal-expense use cases can execute with network unavailable.
- Domain module has no dependency on Android, Room, HTTP or server libraries.
- Repository contracts can be implemented by Room without leaking Room entities.
- No sync worker or remote endpoint is required to create, read, update, export or restore local records.
- Tests verify that a local transaction is committed atomically and retries do not duplicate it.
