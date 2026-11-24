package com.creazzy.rofof.data.sync

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class SyncVersion(val deviceId: String, val revision: Long, val updatedAtEpochMillis: Long, val payloadDigest: String) {
    init { require(deviceId.isNotBlank() && revision >= 0L && updatedAtEpochMillis >= 0L && payloadDigest.matches(Regex("[a-fA-F0-9]{64}"))) }
}
sealed interface SyncConflictDecision {
    data class AcceptRemote(val remote: SyncVersion) : SyncConflictDecision
    data class KeepLocal(val local: SyncVersion) : SyncConflictDecision
    data class RequireUserResolution(val local: SyncVersion, val remote: SyncVersion) : SyncConflictDecision
}
object FinancialSyncConflictResolver {
    /** Financial records are never last-write-wins merged: divergent payloads require explicit review. */
    fun resolve(local: SyncVersion, remote: SyncVersion): SyncConflictDecision = when {
        local.payloadDigest.equals(remote.payloadDigest, ignoreCase = true) ->
            if (compare(remote, local) > 0) SyncConflictDecision.AcceptRemote(remote) else SyncConflictDecision.KeepLocal(local)
        else -> SyncConflictDecision.RequireUserResolution(local, remote)
    }
    private fun compare(a: SyncVersion, b: SyncVersion): Int =
        compareValuesBy(a, b, SyncVersion::revision, SyncVersion::updatedAtEpochMillis, SyncVersion::deviceId)
}

data class AuditEvent(val sequence: Long, val occurredAtEpochMillis: Long, val actor: String, val action: String, val entityType: String, val entityId: String, val payloadDigest: String, val previousHash: String, val hash: String)
object FinancialAuditChain {
    val GENESIS = "0".repeat(64)
    fun append(previous: AuditEvent?, sequence: Long, occurredAtEpochMillis: Long, actor: String, action: String, entityType: String, entityId: String, payloadDigest: String): AuditEvent {
        require(sequence == (previous?.sequence ?: 0L) + 1L)
        require(occurredAtEpochMillis >= (previous?.occurredAtEpochMillis ?: 0L))
        require(listOf(actor, action, entityType, entityId).all { it.isNotBlank() })
        require(payloadDigest.matches(Regex("[a-fA-F0-9]{64}")))
        val prior = previous?.hash ?: GENESIS
        val hash = digest(listOf(sequence, occurredAtEpochMillis, actor, action, entityType, entityId, payloadDigest.lowercase(), prior).joinToString("|"))
        return AuditEvent(sequence, occurredAtEpochMillis, actor, action, entityType, entityId, payloadDigest.lowercase(), prior, hash)
    }
    fun verify(events: List<AuditEvent>): Boolean {
        var previous: AuditEvent? = null
        return events.all { event ->
            val expected = runCatching { append(previous, event.sequence, event.occurredAtEpochMillis, event.actor, event.action, event.entityType, event.entityId, event.payloadDigest) }.getOrNull()
            val valid = expected != null && expected.hash == event.hash && event.previousHash == (previous?.hash ?: GENESIS)
            if (valid) previous = event
            valid
        }
    }
    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
