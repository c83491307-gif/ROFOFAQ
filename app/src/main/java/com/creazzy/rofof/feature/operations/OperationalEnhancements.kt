package com.creazzy.rofof.feature.operations

import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth

data class ApprovalPolicy(val thresholdMinor: Long, val requiredApprovals: Int, val allowedRoles: Set<String>) {
    init { require(thresholdMinor >= 0 && requiredApprovals in 1..5 && allowedRoles.isNotEmpty()) }
}
data class ApprovalDecision(val required: Int, val remaining: Int, val eligibleRoles: Set<String>)
object FinancialApprovalEngine {
    fun evaluate(amountMinor: Long, policy: ApprovalPolicy, approved: Int = 0): ApprovalDecision {
        require(amountMinor >= 0 && approved >= 0)
        val required = if (amountMinor >= policy.thresholdMinor) policy.requiredApprovals else 0
        return ApprovalDecision(required, (required - approved).coerceAtLeast(0), policy.allowedRoles)
    }
}

enum class RuleAction { FLAG_FOR_APPROVAL, WARN_BUDGET, SUGGEST_CATEGORY_REVIEW }
data class FinancialRule(val id: String, val categoryId: String?, val minimumMinor: Long, val merchantContains: String?, val action: RuleAction, val enabled: Boolean = true)
data class RuleContext(val categoryId: String, val amountMinor: Long, val merchant: String)
object FinancialRuleEngine {
    fun evaluate(context: RuleContext, rules: List<FinancialRule>) = rules.filter {
        it.enabled && context.amountMinor >= it.minimumMinor &&
            (it.categoryId == null || it.categoryId == context.categoryId) &&
            (it.merchantContains.isNullOrBlank() || context.merchant.contains(it.merchantContains, true))
    }.sortedBy { it.id }
}

data class BudgetBounds(val floorMinor: Long, val ceilingMinor: Long, val maxChangeBasisPoints: Int = 2_000) {
    init { require(floorMinor >= 0 && ceilingMinor >= floorMinor && maxChangeBasisPoints in 0..10_000) }
}
object DynamicBudgetEngine {
    fun suggest(history: List<Long>, currentMinor: Long, bounds: BudgetBounds): Long {
        require(history.all { it >= 0 } && currentMinor >= 0)
        if (history.isEmpty()) return currentMinor.coerceIn(bounds.floorMinor, bounds.ceilingMinor)
        val sorted = history.sorted()
        val median = BigInteger.valueOf(sorted[sorted.size / 2])
        val current = BigInteger.valueOf(currentMinor)
        val delta = current.multiply(BigInteger.valueOf(bounds.maxChangeBasisPoints.toLong())).divide(BigInteger.valueOf(10_000))
        val target = median.coerceIn(current.subtract(delta).max(BigInteger.ZERO), current.add(delta))
            .coerceIn(BigInteger.valueOf(bounds.floorMinor), BigInteger.valueOf(bounds.ceilingMinor))
        require(target.bitLength() < 63)
        return target.toLong()
    }
}

data class SensitiveOperation(val amountMinor: Long, val changesAccount: Boolean, val destructive: Boolean)
data class OperationGuardDecision(val confirmationRequired: Boolean, val reauthenticationRequired: Boolean)
object SensitiveOperationGuard {
    fun assess(operation: SensitiveOperation, reauthThresholdMinor: Long): OperationGuardDecision {
        require(operation.amountMinor >= 0 && reauthThresholdMinor >= 0)
        return OperationGuardDecision(operation.destructive || operation.changesAccount || operation.amountMinor > 0,
            operation.destructive || operation.amountMinor >= reauthThresholdMinor)
    }
}

data class TaxDocument(val id: String, val title: String, val taxYear: Int, val dueDate: LocalDate?, val sha256: String, val mimeType: String)
object TaxDocumentIndex {
    fun validate(row: TaxDocument) = row.id.isNotBlank() && row.title.isNotBlank() && row.taxYear in 1900..2200 &&
        (row.dueDate == null || row.dueDate.year in 1900..2200) &&
        row.sha256.matches(Regex("[a-fA-F0-9]{64}")) &&
        row.mimeType.matches(Regex("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+"))
}

data class MerchantProfile(val canonicalName: String, val aliases: Set<String>, val categoryId: String?)
object MerchantIntelligence {
    fun normalize(value: String) = value.trim().lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ")
    fun match(value: String, profiles: List<MerchantProfile>): MerchantProfile? {
        val key = normalize(value)
        if (key.isBlank()) return null
        return profiles.firstOrNull { normalize(it.canonicalName) == key || it.aliases.any { alias -> normalize(alias) == key } }
    }
}

data class QualityRecord(val id: String, val title: String, val amountMinor: Long, val currencyCode: String, val exponent: Int, val timestamp: Long)
data class QualityIssue(val recordId: String, val code: String)
object FinancialDataQuality {
    fun inspect(rows: List<QualityRecord>): List<QualityIssue> = buildList {
        val duplicates = rows.groupBy { it.id }.filterValues { it.size > 1 }.keys
        rows.forEach { row ->
            if (row.id.isBlank() || row.title.isBlank()) add(QualityIssue(row.id, "MISSING_REQUIRED_FIELD"))
            if (row.amountMinor <= 0) add(QualityIssue(row.id, "NON_POSITIVE_AMOUNT"))
            if (!row.currencyCode.matches(Regex("[A-Z]{3}")) || row.exponent !in 0..4) add(QualityIssue(row.id, "INVALID_CURRENCY"))
            if (row.timestamp <= 0) add(QualityIssue(row.id, "INVALID_TIMESTAMP"))
            if (row.id in duplicates) add(QualityIssue(row.id, "DUPLICATE_ID"))
        }
    }
}

data class LineageNode(val valueId: String, val source: String, val sourceIds: List<String>, val transformation: String, val createdAt: Long)
object DataLineage {
    fun validate(node: LineageNode) = node.valueId.isNotBlank() && node.source.isNotBlank() &&
        node.sourceIds.isNotEmpty() && node.sourceIds.all { it.isNotBlank() } &&
        node.transformation.isNotBlank() && node.createdAt > 0
}

data class BackupFileDigest(val path: String, val sha256: String, val sizeBytes: Long)
data class BackupPlan(val changed: List<BackupFileDigest>, val deletedPaths: List<String>, val unchangedCount: Int)
object DifferentialBackupPlanner {
    fun plan(previous: List<BackupFileDigest>, current: List<BackupFileDigest>): BackupPlan {
        (previous + current).forEach {
            require(it.path.isNotBlank() && !it.path.startsWith("/") && ".." !in it.path.split("/") &&
                it.sha256.matches(Regex("[a-fA-F0-9]{64}")) && it.sizeBytes >= 0)
        }
        require(previous.map { it.path }.distinct().size == previous.size && current.map { it.path }.distinct().size == current.size)
        val old = previous.associateBy { it.path }
        val unchanged = current.filter { row -> old[row.path]?.let { it.sha256.equals(row.sha256, true) && it.sizeBytes == row.sizeBytes } == true }
        return BackupPlan(current - unchanged.toSet(), (previous.map { it.path } - current.map { it.path }).sorted(), unchanged.size)
    }
}

data class RecoveryManifest(val entries: List<BackupFileDigest>, val digest: String)
object RecoveryLab {
    fun digest(entries: List<BackupFileDigest>): String {
        val canonical = entries.sortedBy { it.path }.joinToString("\n") { it.path + "|" + it.sha256.lowercase() + "|" + it.sizeBytes }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun verify(manifest: RecoveryManifest) = manifest.entries.map { it.path }.distinct().size == manifest.entries.size &&
        manifest.digest.matches(Regex("[a-fA-F0-9]{64}")) && digest(manifest.entries).equals(manifest.digest, true)
}

data class LocalizationReport(val missing: Set<String>, val extra: Set<String>, val placeholderMismatches: Set<String>)
object LocalizationCoverage {
    fun compare(reference: Map<String, String>, translation: Map<String, String>): LocalizationReport {
        val missing = reference.keys - translation.keys
        val extra = translation.keys - reference.keys
        val placeholders = Regex("\\{[A-Za-z0-9_]+}")
        val mismatches = (reference.keys intersect translation.keys).filterTo(mutableSetOf()) { key ->
            placeholders.findAll(reference.getValue(key)).map { it.value }.sorted().toList() !=
                placeholders.findAll(translation.getValue(key)).map { it.value }.sorted().toList()
        }
        return LocalizationReport(missing, extra, mismatches)
    }
}

data class PluginDescriptor(val id: String, val version: String, val capabilities: Set<String>, val enabled: Boolean)
object LocalPluginRegistry {
    private val allowed = setOf("READ_TRANSACTIONS", "READ_CATEGORIES", "SUGGEST_CATEGORY", "EXPORT_REPORT")
    fun validate(plugin: PluginDescriptor) = plugin.id.matches(Regex("[a-z][a-z0-9.-]{2,63}")) &&
        plugin.version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")) && plugin.capabilities.isNotEmpty() &&
        plugin.capabilities.all { it in allowed }
}

data class MonthlySpend(val month: YearMonth, val amountMinor: Long)
data class SeasonalityResult(val averageMinor: Long, val peakMonth: YearMonth?, val peakAmountMinor: Long)
object SeasonalityAnalyzer {
    fun analyze(rows: List<MonthlySpend>): SeasonalityResult {
        require(rows.all { it.amountMinor >= 0 })
        if (rows.isEmpty()) return SeasonalityResult(0, null, 0)
        val sum = rows.fold(BigInteger.ZERO) { acc, row -> acc + BigInteger.valueOf(row.amountMinor) }
        val peak = rows.maxBy { it.amountMinor }
        return SeasonalityResult(sum.divide(BigInteger.valueOf(rows.size.toLong())).toLong(), peak.month, peak.amountMinor)
    }
}

data class SensitivityInput(val baseMinor: Long, val changeBasisPoints: Int)
object FinancialSensitivity {
    fun apply(input: SensitivityInput): Long {
        require(input.baseMinor >= 0 && input.changeBasisPoints in -10_000..100_000)
        val result = BigInteger.valueOf(input.baseMinor).multiply(BigInteger.valueOf(10_000L + input.changeBasisPoints)).divide(BigInteger.valueOf(10_000))
        require(result.bitLength() < 63)
        return result.toLong()
    }
}

data class HealthSample(val component: String, val latencyMillis: Long, val errors: Int, val queueDepth: Int)
data class HealthStatus(val healthy: Boolean, val warnings: List<String>)
object ApplicationHealthMonitor {
    fun assess(rows: List<HealthSample>, latencyLimit: Long = 1_500, errorLimit: Int = 0, queueLimit: Int = 100): HealthStatus {
        require(latencyLimit >= 0 && errorLimit >= 0 && queueLimit >= 0)
        val warnings = buildList {
            rows.forEach { row ->
                if (row.component.isBlank() || row.latencyMillis < 0 || row.errors < 0 || row.queueDepth < 0) add("INVALID_SAMPLE")
                else {
                    if (row.latencyMillis > latencyLimit) add("SLOW:" + row.component)
                    if (row.errors > errorLimit) add("ERRORS:" + row.component)
                    if (row.queueDepth > queueLimit) add("QUEUE:" + row.component)
                }
            }
        }
        return HealthStatus(warnings.isEmpty(), warnings)
    }
}
