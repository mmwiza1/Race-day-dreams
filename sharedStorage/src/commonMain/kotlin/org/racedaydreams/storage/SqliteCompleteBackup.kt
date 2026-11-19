package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.time.Instant

/**
 * Original complete package1, with parent13 and raw3 captured in ONE existing SQLite transaction.
 * Finish/recover an unfinished run first, as Android already requires. Ordinary app/raw exports
 * retain their independent rules. Returns only sealed private scratch; caller owns copy and close.
 */
class SqliteCompleteBackup(private val database: SqlDatabase) {
    fun export(privateDirectory: String, createdAt: Instant): BackupStagingFile {
        var raw: BackupStagingFile? = null
        var output: BackupStagingFile? = null
        try {
            val app =
                database.transaction {
                    require(
                        database
                            .query(
                                "SELECT COUNT(*) FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE')"
                            )
                            .single()
                            .single()
                            .sqlLong() == 0L
                    ) {
                        "Finish or recover the current run before complete backup."
                    }
                    val bytes = SqliteApplicationBackup(database).encode(createdAt)
                    raw = SqliteRecordingArchive(database).export(privateDirectory)
                    bytes
                }
            val appSource =
                object : BackupArchiveSource {
                    override val size = app.size.toLong()

                    override fun read(offset: Long, maximum: Int) =
                        app.copyOfRange(offset.toInt(), minOf(app.size, offset.toInt() + maximum))
                }
            val inputs =
                listOf(
                    BackupZipWriter.Source("app.json", appSource),
                    BackupZipWriter.Source("recordings.zip", requireNotNull(raw)),
                )
            val receipts = inputs.map { CompleteBackupManifest.Receipt(it.name, measure(it.bytes)) }
            val manifestBytes =
                buildJsonObject {
                        put("format", "rdd-complete-backup")
                        put("version", 1)
                        put(
                            "files",
                            buildJsonObject {
                                for (receipt in receipts) put(
                                    receipt.name,
                                    buildJsonObject {
                                        put("bytes", receipt.evidence.bytes)
                                        put("sha256", receipt.evidence.sha256)
                                    },
                                )
                            },
                        )
                    }
                    .toString()
                    .encodeToByteArray()
            CompleteBackupManifest.decode(manifestBytes).verify(receipts)
            val manifest =
                object : BackupArchiveSource {
                    override val size = manifestBytes.size.toLong()

                    override fun read(offset: Long, maximum: Int) =
                        manifestBytes.copyOfRange(
                            offset.toInt(),
                            minOf(manifestBytes.size, offset.toInt() + maximum),
                        )
                }
            val file = BackupStagingFile.create(privateDirectory, BackupStagingFile.MAX_BYTES)
            output = file
            BackupZipWriter.write(
                BackupZipArchive.Kind.COMPLETE,
                listOf(BackupZipWriter.Source("manifest.json", manifest)) + inputs,
                file::append,
            )
            file.seal()
            raw?.close()
            return file
        } catch (error: Throwable) {
            for (file in listOfNotNull(output, raw)) try {
                file.close()
            } catch (cleanup: Throwable) {
                error.addSuppressed(cleanup)
            }
            throw error
        } finally {
            raw?.close()
        }
    }

    private fun measure(source: BackupArchiveSource): CompleteBackupManifest.Entry {
        val size = source.size
        val digest = archiveSha256()
        try {
            var offset = 0L
            while (offset < size) {
                val maximum = minOf(32768L, size - offset).toInt()
                val bytes = source.read(offset, maximum)
                require(bytes.isNotEmpty() && bytes.size <= maximum) {
                    "Incomplete complete-backup evidence."
                }
                digest.update(bytes, 0, bytes.size)
                offset += bytes.size
            }
            require(source.size == size) { "Staged complete-backup source changed." }
            return CompleteBackupManifest.Entry(
                size,
                digest.finish().joinToString("") { it.toUByte().toString(16).padStart(2, '0') },
            )
        } finally {
            digest.close()
        }
    }
}
