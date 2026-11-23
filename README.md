# ROFOF

ROFOF is a local-first Android application for shared purchases and personal expense management.

## Architecture

- Kotlin, Jetpack Compose and Room.
- Room is the source of truth for core data stored on the device.
- Core workflows are designed to work without account or network access.
- Backend and synchronization are optional future extensions; they are not enabled in the local-first foundation.
- Monetary values use integer minor units and explicit ISO currency metadata. Floating-point values are not used for money.

See [accepted architecture decisions](docs/architecture/DECISIONS.md).

## Android project

- Android application module: `app`
- Namespace/application ID: `com.creazzy.rofof`
- Minimum Android: API 26
- Compile/target SDK: API 35
- JDK: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3

## Build and verify

The GitHub Actions workflow runs unit tests, Android lint, debug APK assembly and Room instrumentation tests on an Android emulator.

On a machine with Android SDK, JDK 17 and Gradle 8.9 installed:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
gradle connectedDebugAndroidTest
```

The Android UI now includes a bilingual Arabic/English expense dashboard, explicit RTL/LTR switching, and a Room-backed personal-expense flow. Shared purchase sessions, participants, allocations, contributions, settlements, reporting, backup/restore and the remaining v1.2.0 modules are still subsequent workstreams.
