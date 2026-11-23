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
