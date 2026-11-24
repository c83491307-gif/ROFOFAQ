# ROFOF

**ROFOF** is an offline-first Android application for daily spending, shared purchases, and personal finance management. Arabic is a first-class language, with English and RTL/LTR support.

## Product direction

The original project roadmap covers:
- Daily income and expense transactions.
- Multiple cash, card and savings accounts.
- Categories and subcategories, transfers, recurring templates, splits, receipt attachments and tags.
- Budgets, savings goals, loans and budget alerts.
- Monthly category reports, cash flow, net worth, search and spending forecasts.
- CSV import/export, with other financial formats added only after parser validation.
- Optional app lock and multi-user capabilities.
- Arabic-first UX, English, light/dark themes, and a future responsive web/PWA client.
- Optional receipt OCR and AI-assisted insights.

See [the full README requirements mapping](docs/discovery/README_REQUIREMENTS_APPLIED_PLAN.md) for the status of each feature.

## Approved architecture

The current delivery is **native Android first**, using Kotlin, Jetpack Compose and Room.

- Room/SQLite is the source of truth for core device-local data.
- Core workflows must work without an account, backend or network connection.
- Backend, cloud storage, banking sync and web/PWA are optional future extensions; none is required or enabled in the Android foundation.
- Monetary values use integer minor units and explicit ISO currency code/exponent. Floating-point values are not used for money.
- No cross-currency totals are combined without an explicit conversion.
- OCR and recurring rules create drafts; they do not silently post financial transactions.
- Shared purchase allocations enter personal expenses only after explicit user confirmation.

See [accepted architecture decisions](docs/architecture/DECISIONS.md).

## Android project

- Module: `app`
- Application ID: `com.creazzy.rofof`
- Minimum Android: API 26
- Compile/target SDK: API 35
- JDK: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3

## Build and verify

The GitHub Actions workflow runs JVM tests, Android lint, debug APK assembly and Room instrumentation tests on an Android emulator.

With Android SDK, JDK 17 and Gradle 8.9 installed:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
gradle connectedDebugAndroidTest
```

## Current implementation status

- [x] Local-first Android foundation and Room database.
- [x] Arabic/English dashboard and RTL/LTR switching.
- [x] Local income/expense ledger with Room persistence, account selection, categories, totals and confirmed deletion.
- [x] Default cash account and typed category records; legacy expense rows are migrated into the ledger while retaining their currency and category labels.
- [x] Multiple local accounts with opening balances and live per-account balances.
- [x] Same-currency atomic account transfers, excluded from income/expense totals.
- [x] Two-level income/expense categories and transaction category validation.
- [x] Account/category rename and safe archival UI with historical retention.
- [x] Shared purchase sessions, participants, item-level exact allocations, actual payments, settlement balances and explicit personal-expense source links (see Phase 4 verification report).
- [x] Monthly category budgets, manually maintained savings goals, and local budget-overrun notifications (Phases 5 and 10; see `docs/implementation/PHASE_10_BUDGET_OVERRUN_ALERTS.md`).
- [x] Monthly recurring expense rules and reviewable draft generation (Phase 6; drafts are not auto-posted).
- [x] Monthly category reports and transaction search.
- [ ] Cash-flow and forecast UI.
- [x] Safe local CSV export with UTF-8 BOM and formula-prefix neutralization.
- [ ] CSV import, validated backup/restore and app security.
- [ ] Optional OCR and future web/sync extensions.

The checked items above describe delivered source functionality, not a claim that the latest branch has passed CI. See the implementation reports for exact verification evidence, including [Priority 2 shared purchases](docs/implementation/PHASE_4_SHARED_PURCHASES.md).


## Latest local enhancements

- Voice-assisted entry with runtime microphone permission, Arabic/English locale selection, and local parsing of common spoken Arabic amount words. Offline recognition depends on the device speech service and downloaded language pack; offline mode is requested but cannot be guaranteed by the app.
- Explicit smart paste review for bank messages, four frequently used expense categories, recent-five amount suggestions scoped by currency, and save-success haptics.
- New offline operational engines for data quality, lineage, differential backup planning, recovery manifest checks, approvals, rules, budget suggestions, sensitive-operation guards, tax-document metadata, merchant aliases, localization checks, allowlisted plugins, seasonality, sensitivity and health checks.

See [new enhancements and V6.0 input implementation](docs/implementation/NEW_15_ENHANCEMENTS_AND_V6_INPUT.md) for implementation boundaries and verification status. These engines are foundations; the document lists persistence and product-integration gates that remain open.


## Build a debug APK with GitHub Actions

The repository includes a cloud build workflow. Open **Actions → ROFOF Debug APK**, run it manually or push to `feat/rofof-apk-integration`, then download the `ROFOF-Debug-APK` artifact after a successful run. The build uses JDK 17, Android SDK 35 and Gradle 8.9 provided by GitHub Actions; it does not require Android Studio or a local Gradle installation. The broader Android CI workflow also runs unit/Robolectric tests, lint and emulator instrumentation. See [APK build and branch integration](docs/implementation/APK_BUILD_AND_BRANCH_INTEGRATION.md).


Phase 7–9 implementation notes: [reports and search](docs/implementation/PHASE_7_REPORTS_AND_SEARCH.md), [CSV export](docs/implementation/PHASE_8_CSV_EXPORT.md), and [recurring draft approval](docs/implementation/PHASE_9_RECURRING_APPROVAL.md).
