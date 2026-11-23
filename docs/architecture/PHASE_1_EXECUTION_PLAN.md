# ROFOF Phase 1 — Architecture and Android foundation execution plan

**Status:** Started; implementation evidence pending  
**Approved mode:** Android local-first; backend/sync optional and deferred  
**Working branch:** `feat/rofof-phase0-discovery`

## Work sequence

### 1. Architecture contract
- [x] Approve local-first authority model.
- [x] Define domain/data/UI dependency direction.
- [x] Define initial financial entities and cross-ledger constraints.
- [ ] Review contracts against all 28 source specifications.
- [ ] Update requirement traceability with implementation file/test IDs.

### 2. Android project foundation
- [ ] Establish Gradle wrapper and pinned plugin/dependency versions.
- [ ] Configure Android app, Kotlin, Compose, Room, Coroutines and test dependencies.
- [ ] Add application entry point and a minimal real navigation shell.
- [ ] Add Arabic/English resources and RTL/LTR validation.
- [ ] Configure CI for structural checks, unit tests and Android debug assembly.

### 3. Domain foundation
- [ ] Implement Money and Currency value types with validation.
- [ ] Implement quantity/unit value types and deterministic arithmetic.
- [ ] Implement purchase, participant, allocation, contribution, settlement and personal-expense domain models.
- [ ] Implement allocation/rounding/balance policies.
- [ ] Implement explicit idempotent shared-allocation transfer.
- [ ] Add property-based or scenario tests for conservation and no-double-counting.

### 4. Persistence foundation
- [ ] Implement Room entities, DAOs, converters and schema export.
- [ ] Implement first migration and migration tests.
- [ ] Implement repository adapters and atomic transaction use cases.
- [ ] Verify restart persistence and process-death recovery.

### 5. Exit gates
Phase 1 is complete only when:
- A clean Android debug build succeeds in CI.
- Domain and database tests execute and pass.
- App launches on a supported emulator/device.
- Local purchase and personal-expense workflows persist across restart.
- Arabic RTL and English LTR render correctly.
- Offline core workflows pass with network unavailable.
- Financial invariants and transfer idempotency tests pass.
- No backend, login, sync or remote upload is required by the first-release app.

## Reporting discipline

For every completed item, record commit SHA, exact command, environment, test result and artifact link. A checked box requires source evidence. No build, test, install or APK result is presumed from documentation alone.
