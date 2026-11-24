package com.creazzy.rofof.data.sync

import org.junit.Assert.*
import org.junit.Test

class ConflictAndAuditTest {
    private val digestA = "a".repeat(64)
    private val digestB = "b".repeat(64)
    private fun version(revision: Long, digest: String) = SyncVersion("device", revision, revision * 100L, digest)

    @Test fun divergentFinancialPayloadsRequireExplicitResolution() {
        assertTrue(FinancialSyncConflictResolver.resolve(version(1, digestA), version(2, digestB)) is SyncConflictDecision.RequireUserResolution)
    }

    @Test fun identicalPayloadsCanAdvanceMonotonicMetadata() {
        assertTrue(FinancialSyncConflictResolver.resolve(version(1, digestA), version(2, digestA)) is SyncConflictDecision.AcceptRemote)
    }

    @Test fun auditChainDetectsTamperingAndReordering() {
        val first = FinancialAuditChain.append(null, 1, 100, "user", "CREATE", "transaction", "tx-1", digestA)
        val second = FinancialAuditChain.append(first, 2, 200, "user", "UPDATE", "transaction", "tx-1", digestB)
        assertTrue(FinancialAuditChain.verify(listOf(first, second)))
        assertFalse(FinancialAuditChain.verify(listOf(first.copy(action = "DELETE"), second)))
        assertFalse(FinancialAuditChain.verify(listOf(second, first)))
    }

    @Test fun auditChainRejectsSequenceGapsAndBackdatedEvents() {
        val first = FinancialAuditChain.append(null, 1, 100, "user", "CREATE", "account", "a1", digestA)
        assertTrue(runCatching { FinancialAuditChain.append(first, 3, 200, "user", "UPDATE", "account", "a1", digestB) }.isFailure)
        assertTrue(runCatching { FinancialAuditChain.append(first, 2, 50, "user", "UPDATE", "account", "a1", digestB) }.isFailure)
    }
}
