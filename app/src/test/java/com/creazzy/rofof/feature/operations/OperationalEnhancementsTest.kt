package com.creazzy.rofof.feature.operations

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class OperationalEnhancementsTest {
    private val hash = "a".repeat(64)

    @Test fun approvalRulesAndSensitiveActions() {
        val policy = ApprovalPolicy(10_000, 2, setOf("OWNER", "FINANCE"))
        assertEquals(2, FinancialApprovalEngine.evaluate(10_000, policy).remaining)
        assertEquals(1, FinancialApprovalEngine.evaluate(20_000, policy, 1).remaining)
        assertEquals(1, FinancialRuleEngine.evaluate(RuleContext("food", 500, "Cafe"), listOf(
            FinancialRule("r1", "food", 100, "caf", RuleAction.FLAG_FOR_APPROVAL)
        )).size)
        assertTrue(SensitiveOperationGuard.assess(SensitiveOperation(0, false, true), 1).reauthenticationRequired)
    }

    @Test fun dynamicBudgetsAndQualityChecks() {
        assertEquals(12_000L, DynamicBudgetEngine.suggest(listOf(12_000, 13_000, 11_000), 10_000, BudgetBounds(5_000, 20_000)))
        val row = QualityRecord("x", "Food", 100, "EGP", 2, 100)
        assertTrue(FinancialDataQuality.inspect(listOf(row, row)).any { it.code == "DUPLICATE_ID" })
    }

    @Test fun taxMerchantAndLineageValidation() {
        assertTrue(TaxDocumentIndex.validate(TaxDocument("t", "Invoice", 2026, LocalDate.of(2026, 12, 1), hash, "application/pdf")))
        val profile = MerchantProfile("Starbucks", setOf("ستاربكس"), "coffee")
        assertEquals(profile, MerchantIntelligence.match("ستاربكس", listOf(profile)))
        assertTrue(DataLineage.validate(LineageNode("net", "ledger", listOf("tx1"), "sum by month", 1)))
    }

    @Test fun backupAndRecoveryDetectChangesAndCorruption() {
        val old = BackupFileDigest("db/ledger", hash, 10)
        val changed = BackupFileDigest("db/ledger", "b".repeat(64), 11)
        assertEquals(1, DifferentialBackupPlanner.plan(listOf(old), listOf(changed)).changed.size)
        val entries = listOf(changed)
        val manifest = RecoveryManifest(entries, RecoveryLab.digest(entries))
        assertTrue(RecoveryLab.verify(manifest))
        assertFalse(RecoveryLab.verify(manifest.copy(digest = hash)))
    }

    @Test fun localizationPluginSeasonalitySensitivityAndHealth() {
        val report = LocalizationCoverage.compare(mapOf("save" to "Save {count}"), mapOf("save" to "حفظ"))
        assertTrue(report.placeholderMismatches.contains("save"))
        assertTrue(LocalPluginRegistry.validate(PluginDescriptor("rofof.export", "1.0.0", setOf("EXPORT_REPORT"), true)))
        assertFalse(LocalPluginRegistry.validate(PluginDescriptor("rofof.bad", "1.0.0", setOf("EXECUTE_SHELL"), true)))
        val season = SeasonalityAnalyzer.analyze(listOf(
            MonthlySpend(YearMonth.of(2026, 1), 100), MonthlySpend(YearMonth.of(2026, 2), 300)
        ))
        assertEquals(200L, season.averageMinor)
        assertEquals(200L, FinancialSensitivity.apply(SensitivityInput(100, 10_000)))
        assertFalse(ApplicationHealthMonitor.assess(listOf(HealthSample("db", 2_000, 0, 0))).healthy)
    }
}
