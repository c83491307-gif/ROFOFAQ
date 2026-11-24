# ROFOF

**ROFOF** is an offline-first Android application for daily spending, shared purchases, and personal finance management. Arabic is a first-class language, with English and RTL/LTR support.

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Status](https://img.shields.io/badge/status-in%20development-yellow)]()

## Product direction

The product roadmap retains the original goals for practical personal-finance management:
- Daily income and expense transactions.
- Multiple cash, card and savings accounts.
- Categories and subcategories, transfers, recurring templates, splits, receipt attachments and tags.
- Budgets, savings goals, loans and budget alerts.
- Monthly category reports, cash flow, net worth, search and spending forecasts.
- CSV import/export, with other financial formats added only after parser validation.
- Optional app lock and multi-user capabilities.
- Arabic-first UX, English, light/dark themes, and a future responsive web/PWA client.
- Optional receipt OCR and AI-assisted insights.

The roadmap is a product direction, not a claim that every item is implemented. See [the requirements-to-delivery mapping](docs/discovery/README_REQUIREMENTS_APPLIED_PLAN.md) and the phase reports for scope and remaining work.

## Approved architecture

The current delivery is **native Android first**, using Kotlin, Jetpack Compose and Room. This is the accepted architecture for the current product; the earlier backend/Flutter/PWA sketches are not the implementation plan for this Android delivery.

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
- Namespace / release application ID: `com.creazzy.rofof`
- Debug application ID: `com.creazzy.rofof.debug`
- Minimum Android: API 26
- Compile/target SDK: API 35
- JDK: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21

## Build and verify

The repository provides GitHub Actions workflows for JVM/Robolectric tests, Android lint, debug APK assembly and Android emulator instrumentation. A focused workflow also builds and uploads the debug APK.

With Android SDK, JDK 17 and Gradle 8.9 installed:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
gradle connectedDebugAndroidTest
```

The repository currently does not include the Gradle Wrapper; CI installs Gradle 8.9 directly. See [APK build and branch integration](docs/implementation/APK_BUILD_AND_BRANCH_INTEGRATION.md). A configured workflow is not evidence of a passing run: verify the exact commit's Actions result before treating a build or test gate as passed.

## Current implementation status

The checked items below describe functionality present in the integration-branch source. They are not a claim that the latest branch has passed CI or that the complete product roadmap is finished.

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
- [x] Monthly recurring expense rules and reviewable draft generation; approval and skip are explicit user actions, not automatic posting (Phases 6 and 9).
- [x] Monthly category reports and case-insensitive transaction search (Phase 7; initial report scope is EGP-only).
- [x] Safe local CSV export of non-deleted ledger transactions with UTF-8 BOM and spreadsheet formula-prefix neutralization (Phase 8).
- [ ] Cash-flow and forecast UI.
- [ ] CSV import, validated backup/restore and app security.
- [ ] Optional OCR and future web/sync extensions.

See the implementation reports for exact feature boundaries: [Priority 2 shared purchases](docs/implementation/PHASE_4_SHARED_PURCHASES.md), [reports and search](docs/implementation/PHASE_7_REPORTS_AND_SEARCH.md), [CSV export](docs/implementation/PHASE_8_CSV_EXPORT.md), and [recurring draft approval](docs/implementation/PHASE_9_RECURRING_APPROVAL.md).

## Latest local enhancements

- Voice-assisted entry with runtime microphone permission, Arabic/English locale selection, and local parsing of common spoken Arabic amount words. Offline recognition depends on the device speech service and downloaded language pack; offline mode is requested but cannot be guaranteed by the app.
- Explicit smart paste review for bank messages, four frequently used expense categories, recent-five amount suggestions scoped by currency, and save-success haptics.
- New offline operational engines for data quality, lineage, differential backup planning, recovery manifest checks, approvals, rules, budget suggestions, sensitive-operation guards, tax-document metadata, merchant aliases, localization checks, allowlisted plugins, seasonality, sensitivity and health checks.

See [new enhancements and V6.0 input implementation](docs/implementation/NEW_15_ENHANCEMENTS_AND_V6_INPUT.md) for implementation boundaries and verification status. These engines are foundations; the document lists persistence and product-integration gates that remain open.

## Build a debug APK with GitHub Actions

Open **Actions → ROFOF Debug APK**, run the workflow manually or push to `feat/rofof-apk-integration`, then download the `ROFOF-Debug-APK` artifact only after a successful run. The build uses JDK 17, Android SDK 35 and Gradle 8.9 provided by GitHub Actions; it does not require Android Studio or a local Gradle installation. The broader Android CI workflow also runs unit/Robolectric tests, lint and emulator instrumentation. See [APK build and branch integration](docs/implementation/APK_BUILD_AND_BRANCH_INTEGRATION.md).

A debug APK is not release-signed and is not a Play Store release. No release signing, publishing or deployment is configured here.


## خارطة المنتج بالعربية

تظل الرؤية الأصلية للمنتج محفوظة ضمن خارطة الطريق، مع اعتماد تطبيق Android أصلي يعمل محلياً دون اتصال كأساس التنفيذ الحالي:

- **التتبع الأساسي:** تسجيل الدخل والمصروفات، حسابات نقدية وبطاقات وادخار متعددة، فئات رئيسية وفرعية، تحويلات، معاملات متكررة، تقسيم المعاملات، مرفقات الإيصالات، العلامات الاختيارية، والموقع الجغرافي الاختياري.
- **الميزانيات والأهداف:** ميزانيات حسب الفئة، أهداف ادخار، متابعة القروض والديون، وتنبيهات تجاوز الميزانية.
- **التحليلات والتقارير:** تقارير شهرية حسب الفئات، التدفق النقدي، صافي الثروة، رسوم قابلة للتخصيص، البحث والتصفية، وتقديرات الإنفاق المستقبلية.
- **الاستيراد والتصدير والمزامنة:** CSV أولاً مع معاينة والتحقق قبل الاعتماد؛ ويمكن دراسة Excel وOFX وQFX وQIF لاحقاً بعد التحقق من المحللات. التصدير البنكي والمزامنة البنكية إضافات اختيارية مستقبلية.
- **الأمان والمستخدمون:** قفل التطبيق ووسائل التحقق، مع دراسة تعدد المستخدمين وتحديد محاولات الدخول ضمن مراحل لاحقة.
- **المنصات وتجربة الاستخدام:** العربية أساسية مع الإنجليزية وRTL/LTR، المظهر الفاتح والداكن، وإمكانية عميل ويب/PWA مستقبلاً دون أن يكون شرطاً لتطبيق Android الحالي.
- **ميزات ذكية اختيارية:** قراءة الإيصالات OCR ومساعدة التحليلات؛ وتبقى النتائج مسودات قابلة للمراجعة ولا تنشئ معاملات مالية تلقائياً.

هذه قائمة أهداف المنتج وليست إقراراً بأن جميع البنود منفذة. راجع جدول المتطلبات وتقارير المراحل لمعرفة ما هو موجود فعلياً وما لا يزال قيد التنفيذ.
