# ROFOF local awareness system

## Runtime boundaries
- Runs entirely in the Android process using bundled JSON assets, Kotlin and Room.
- No network client, AI SDK, internet permission, telemetry or remote rule source is used.
- Room v8 remains the source of truth. Seeder writes only missing category catalog rows.
- Analysis is a read-only preview. A user must apply/review the suggestion and press Save before any transaction is written.
- Currency comparisons are scoped to the selected account currency and exponent. Monetary values use minor-unit Long values and BigDecimal for ratios.

## Configuration
- `app/src/main/assets/awareness/categories.json`: stable category IDs and canonical names.
- `keywords_ar.json`: Arabic phrase-to-category mappings. Longest matching phrase wins.
- `rules.json`: deterministic conditions, priorities and bilingual actions.
- Supported operators: eq, neq, gt, gte, lt, lte, contains, exists, between.
- Rule conditions are AND-combined. Missing facts do not match ordinary comparisons.

## Room seeding
`DatabaseSeeder.seedFromAssets(context)` runs during ViewModel initialization. It is transactional and idempotent, preserves existing matching user categories, and does not create accounts, transactions, budgets or goals.

## Facts currently supplied to the rule engine
- categoryKnown, categoryId, transactionType
- localHour, isWeekend, isMonthEnd
- projected balanceMinor for the selected account
- budgetPercent including the proposed expense
- weeklySpendRatio, amountRatioToAverage and recurringSimilarityCount when sufficient same-category, same-currency history exists
- daysUntilRent when the local preference `awareness.rent_due_epoch_millis` is configured
- isFirstLaunch

Unsupported or unavailable facts are intentionally omitted rather than guessed. In particular, rent reminders require a due-date preference. Weekly, unusual-amount and recurring rules require enough historical records.

## Review and audit
The dashboard displays the proposed type, amount, category, and matched bilingual rule messages. Applying the review is a separate user action. The advisor never inserts a transaction. The latest matched rule IDs are stored locally under `awareness.last_review_rule_matches`; this is a compact local diagnostic, not a remote event log.

## Verification
Robolectric tests cover seeding idempotence, JSON validation, deterministic rule evaluation, Arabic entry resolution, unknown categories, proposed-expense budget thresholds, and the advisor's no-ledger-write guarantee. CI remains the authority for full build, lint and emulator status.
