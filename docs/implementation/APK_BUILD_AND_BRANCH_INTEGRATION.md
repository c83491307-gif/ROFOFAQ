# ROFOF — APK build and branch integration

## Integration branch
- Base: `feat/rofof-hybrid-15-workstreams`
- Integration: `feat/rofof-apk-integration`
- Main remains unchanged.
- Existing Android source, Room schema/migrations, assets, tests and prior implementation history are retained.

## Branch reconciliation
The following implementation deltas were reconciled into this branch:
- Phase 7: monthly category report and transaction search UI, DAO queries, Room tests and documentation.
- Phase 9: explicit recurring draft approve/skip workflow, atomic repository, Room tests and documentation. The existing day-of-month scheduling was retained.
- Phase 10 budget alert source and tests were already byte-for-byte present in the cumulative branch.
- Existing CSV export, backup/debt foundations, local-first dashboard, shared purchases, budgets, recurring generation and V6 input enhancements were retained from the cumulative branch.
- Older foundation/phase branches were not blindly merged over newer source because they contain stale versions of Room, dashboard and recurring files. Their implemented source was checked against the cumulative tree before reconciliation.

## Cloud build
Two workflows are available:
- `.github/workflows/android-ci.yml`: unit/Robolectric tests, lint, debug APK assembly, emulator instrumentation and verification artifacts.
- `.github/workflows/build.yml`: focused debug APK build and dedicated `ROFOF-Debug-APK` artifact.

The focused build uses JDK 17, Android SDK 35 and Gradle 8.9 installed by GitHub Actions. It intentionally does not call `./gradlew`, because the repository does not currently contain `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` or `gradle-wrapper.properties`. No incomplete wrapper files are fabricated.

## Downloading the APK
1. Open the repository Actions tab.
2. Select **ROFOF Debug APK** and open the newest run for `feat/rofof-apk-integration`.
3. Wait for the run to complete successfully.
4. Download **ROFOF-Debug-APK** from Artifacts.
5. Extract the ZIP and install `app-debug.apk` on an Android device. The debug package uses the `.debug` application ID suffix and can coexist with a release package.

## Verification gate
Do not describe an APK as available until the workflow succeeds and the artifact exists. A successful debug build is not a release-signed APK and is not a Play Store release. No signing secrets, release signing, publishing or deployment are configured here.
