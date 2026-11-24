package com.creazzy.rofof.data.backup

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackupArchiveTest {
    @Test fun roundTripsValidatedSqliteSnapshot() {
        val database = fakeSqliteDatabase(version = 8)
        val result = LocalBackupArchive.decode(LocalBackupArchive.encode(database))
        assertTrue(result.toString(), result is LocalBackupArchive.DecodeResult.Valid)
        val snapshot = (result as LocalBackupArchive.DecodeResult.Valid).snapshot
        assertEquals(8, snapshot.userVersion)
        assertTrue(database.contentEquals(snapshot.databaseBytes))
    }

    @Test fun rejectsNonSqliteInput() {
        val failure = runCatching { LocalBackupArchive.encode(ByteArray(128)) }.exceptionOrNull()
        assertTrue(failure != null)
    }

    @Test fun rejectsChecksumTampering() {
        val archive = LocalBackupArchive.encode(fakeSqliteDatabase(8))
        val decoded = LocalBackupArchive.decode(archive)
        assertTrue(decoded is LocalBackupArchive.DecodeResult.Valid)
        // A damaged ZIP must never be accepted as a restorable snapshot.
        val damaged = archive.copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x01).toByte() }
        assertTrue(LocalBackupArchive.decode(damaged) is LocalBackupArchive.DecodeResult.Invalid)
    }

    @Test fun rejectsUnexpectedZipEntries() {
        val bytes = ByteArrayOutputStream().use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("../outside")); zip.write(byteArrayOf(1)); zip.closeEntry()
            }
            out.toByteArray()
        }
        assertTrue(LocalBackupArchive.decode(bytes) is LocalBackupArchive.DecodeResult.Invalid)
    }

    private fun fakeSqliteDatabase(version: Int): ByteArray = ByteArray(128).also { bytes ->
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(bytes)
        bytes[60] = (version ushr 24).toByte()
        bytes[61] = (version ushr 16).toByte()
        bytes[62] = (version ushr 8).toByte()
        bytes[63] = version.toByte()
    }
}
