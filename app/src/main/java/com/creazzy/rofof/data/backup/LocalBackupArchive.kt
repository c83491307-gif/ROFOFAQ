package com.creazzy.rofof.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Deterministic, bounded archive codec for a SQLite snapshot.
 * The caller must checkpoint WAL and obtain a consistent snapshot before encoding.
 * Decoding is validation only; it never replaces the live database.
 */
object LocalBackupArchive {
    private const val DATABASE_ENTRY = "database.sqlite"
    private const val MANIFEST_ENTRY = "manifest.txt"
    private const val FORMAT_VERSION = 1
    private const val MAX_DATABASE_BYTES = 256 * 1024 * 1024
    private const val SQLITE_HEADER = "SQLite format 3\u0000"

    data class Snapshot(val databaseBytes: ByteArray, val userVersion: Int)

    sealed interface DecodeResult {
        data class Valid(val snapshot: Snapshot) : DecodeResult
        data class Invalid(val reason: String) : DecodeResult
    }

    fun encode(databaseBytes: ByteArray): ByteArray {
        require(databaseBytes.size in 100..MAX_DATABASE_BYTES) { "Database size is outside supported limits" }
        val version = readUserVersion(databaseBytes)
        require(version in 1..7) { "Unsupported Room schema version" }
        val digest = sha256(databaseBytes)
        val manifest = "format=$FORMAT_VERSION\nroom_version=$version\nsha256=$digest\n"
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifest.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
                zip.write(databaseBytes)
                zip.closeEntry()
            }
            output.toByteArray()
        }
    }

    fun decode(archiveBytes: ByteArray): DecodeResult = runCatching {
        require(archiveBytes.isNotEmpty() && archiveBytes.size <= MAX_DATABASE_BYTES + 1024 * 1024) {
            "Backup archive size is outside supported limits"
        }
        var manifest: String? = null
        var database: ByteArray? = null
        ZipInputStream(ByteArrayInputStream(archiveBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name in setOf(MANIFEST_ENTRY, DATABASE_ENTRY)) {
                    "Unexpected backup entry"
                }
                when (entry.name) {
                    MANIFEST_ENTRY -> {
                        require(manifest == null) { "Duplicate manifest entry" }
                        val bytes = zip.readBounded(4096)
                        manifest = bytes.toString(Charsets.UTF_8)
                    }
                    DATABASE_ENTRY -> {
                        require(database == null) { "Duplicate database entry" }
                        database = zip.readBounded(MAX_DATABASE_BYTES)
                    }
                }
                zip.closeEntry()
            }
        }
        val metadata = requireNotNull(manifest) { "Backup manifest is missing" }
            .lineSequence().filter { it.contains('=') }.associate {
                it.substringBefore('=') to it.substringAfter('=')
            }
        require(metadata["format"] == FORMAT_VERSION.toString()) { "Unsupported backup format" }
        val bytes = requireNotNull(database) { "Database snapshot is missing" }
        require(bytes.size >= 100 && bytes.copyOfRange(0, SQLITE_HEADER.length).toString(Charsets.US_ASCII) == SQLITE_HEADER) {
            "Backup does not contain a valid SQLite database header"
        }
        val version = readUserVersion(bytes)
        require(version in 1..7 && metadata["room_version"] == version.toString()) {
            "Room schema version mismatch or unsupported version"
        }
        require(metadata["sha256"] == sha256(bytes)) { "Backup checksum mismatch" }
        DecodeResult.Valid(Snapshot(bytes, version))
    }.getOrElse { DecodeResult.Invalid(it.message ?: "Invalid backup archive") }

    private fun readUserVersion(bytes: ByteArray): Int {
        require(bytes.size >= 100 && bytes.copyOfRange(0, SQLITE_HEADER.length).toString(Charsets.US_ASCII) == SQLITE_HEADER) {
            "Not a SQLite database"
        }
        return (bytes[60].toInt() and 0xff shl 24) or
            (bytes[61].toInt() and 0xff shl 16) or
            (bytes[62].toInt() and 0xff shl 8) or
            (bytes[63].toInt() and 0xff)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun ZipInputStream.readBounded(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "Backup entry exceeds size limit" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
