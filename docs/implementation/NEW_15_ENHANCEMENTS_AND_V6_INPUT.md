# ROFOF — New 15 enhancements + V6.0 input experience

Branch: \`feat/rofof-hybrid-15-workstreams\`  
Baseline: \`dc16aa40db84ee98cb10ee68005a98671ae4401e\`  
Delivery policy: keep all existing commits; no merge, release, APK signing, or deployment.

## Added implementation

| Requested order | Capability | Delivered source |
|---:|---|---|
| 7 | Financial data quality checks | \`FinancialDataQuality\`: required-field, positive-amount, currency/exponent, timestamp and duplicate-ID checks |
| 10 | Data lineage | \`DataLineage\`: validates source IDs and transformation provenance |
| 13 | Recovery lab | \`RecoveryLab\`: canonical SHA-256 manifest verification; differential planning detects changed/deleted entries |
| 2 | Financial automation rules | \`FinancialRuleEngine\`: deterministic allowlisted conditions/actions; no arbitrary code execution |
| 11 | Differential backups | \`DifferentialBackupPlanner\`: path-safe changed/unchanged/deleted manifest planning |
| 1 | Multi-level approvals | \`FinancialApprovalEngine\`: amount threshold, approval count and eligible role set |
| 3 | Dynamic budgets | \`DynamicBudgetEngine\`: median-based suggestion with bounded movement and hard floor/ceiling |
| 4 | Sensitive operations | \`SensitiveOperationGuard\`: confirmation and re-authentication decisions |
| 5 | Tax document center | \`TaxDocumentIndex\`: metadata, year, MIME and SHA-256 validation |
| 6 | Merchant intelligence | \`MerchantIntelligence\`: normalized canonical-name/alias lookup |
| 15 | Application health monitor | \`ApplicationHealthMonitor\`: latency, error and queue-depth threshold assessment |
| 14 | Localization coverage | \`LocalizationCoverage\`: missing/extra key and placeholder parity report |
| 12 | Plugin architecture | \`LocalPluginRegistry\`: capability allowlist; no dynamic code loading or elevated execution |
| 9 | Seasonality | \`SeasonalityAnalyzer\`: monthly average and peak-month calculation |
| 8 | Financial sensitivity | \`FinancialSensitivity\`: bounded basis-point scenario adjustment with overflow protection |

The engines are local deterministic foundations. They do not claim completed screens, durable persistence, automatic tax filing, background backup execution, production telemetry, or a dynamic plugin runtime. Those require separate reviewed integrations.

## V6.0 — input experience

- **Voice:** a Compose microphone action uses Android \`SpeechRecognizer\`, requests \`RECORD_AUDIO\`, and passes Arabic-Egyptian or English locale according to app language.
- **Offline caveat:** \`EXTRA_PREFER_OFFLINE\` is a recognizer preference, not a guarantee. Actual offline operation depends on the installed recognition service and downloaded language pack. The UI reports recognizer errors instead of claiming offline success.
- **Smart paste:** clipboard is read only after the user taps Paste. A bank-like message is parsed into a suggestion and requires explicit confirmation/review; it never posts a transaction directly.
- **Inline arithmetic:** the existing bounded \`MathExpressionEvaluator\` and \`SmartEntryParser\` remain the arithmetic implementation. Arithmetic expressions are accepted in the smart-command field, not silently evaluated as arbitrary code.
- **Frequent categories:** Room query returns the four most-used active expense categories.
- **Typical amount:** Room query averages the five most recent non-deleted expenses for the selected category and exact currency/exponent.
- **Haptics:** the success callback fires only after the repository insert succeeds; the Compose haptic is triggered from that callback.

## Changed files

- \`feature/operations/OperationalEnhancements.kt\`
- \`feature/smartentry/SmartPasteParser.kt\`
- \`feature/smartentry/SmartEntryAssistComponents.kt\`
- \`data/local/FrequentCategory.kt\`
- \`data/local/TransactionDao.kt\`
- \`feature/dashboard/ExpenseViewModel.kt\`
- \`feature/dashboard/DashboardScreen.kt\`
- \`app/src/main/AndroidManifest.xml\` (runtime microphone permission declaration)
- Unit and Room tests for operational engines, smart paste, and suggestion queries.

## Safety and data rules

- All money suggestions remain in integer minor units and are scoped by currency code and exponent.
- Clipboard access is user-initiated; clipboard text is not polled on screen open.
- Voice and OCR/paste output are suggestions only. User review is required before saving.
- No banking connection, network sync, remote AI, cloud storage, or automatic transaction posting was added.
- The plugin registry validates capability names only; it does not load or execute plugins.
- Recovery manifest verification is integrity checking, not a complete device restore implementation.
- Tax document support validates metadata only; it does not provide tax advice or submit filings.

## Verification status

New unit tests cover the operational engines and smart-paste parser. Room tests cover the recent-five average, currency separation, deleted/income exclusion, and top-four category ordering.

The existing GitHub Actions run \`45399097427\` was still in progress at the time of the initial check. Do not treat this change as build-verified until a branch-head run reports successful unit/Robolectric tests, lint, debug APK assembly, and emulator instrumentation. No merge or deployment is authorized.
