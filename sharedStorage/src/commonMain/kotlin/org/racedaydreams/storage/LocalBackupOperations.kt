package org.racedaydreams.storage

import org.racedaydreams.domain.time.Instant

/**
 * One instance per application database. Keep backup capture and data replacement under the same
 * operation lock.
 */
class LocalBackupOperations(
    private val database: SqlDatabase,
    private val now: () -> Instant,
    private val freshEpoch: () -> String,
) {
    sealed class ReviewedBackup(val version: Int, val createdAt: Instant) {
        abstract val includesRecordings: Boolean
        internal abstract val owner: LocalBackupOperations

        abstract fun close()
    }

    private class ReviewedAppBackup(
        override val owner: LocalBackupOperations,
        val inspected: BackupEnvelopeJson.Checked<UserBackupDataJson.Snapshot>,
    ) : ReviewedBackup(inspected.version, inspected.createdAt) {
        override val includesRecordings = false

        override fun close() = Unit
    }

    private class ReviewedCompleteBackup(
        override val owner: LocalBackupOperations,
        val inspected: InspectedBackupPackage,
    ) : ReviewedBackup(inspected.application.version, inspected.application.createdAt) {
        override val includesRecordings = true

        override fun close() = inspected.close()
    }

    fun reviewApp(bytes: ByteArray): ReviewedBackup =
        ReviewedAppBackup(this, UserBackupDataJson.inspect(bytes))

    fun reviewComplete(source: BackupArchiveSource, privateDirectory: String): ReviewedBackup =
        ReviewedCompleteBackup(this, InspectedBackupPackage.inspect(source, privateDirectory))

    /** The host must first obtain explicit confirmation for this reviewed document. */
    fun restoreReviewed(reviewed: ReviewedBackup) {
        require(reviewed.owner === this) {
            "Review the backup in this app session before restoring."
        }
        operations.replace {
            when (reviewed) {
                is ReviewedAppBackup ->
                    SqliteApplicationBackupRestore(database, freshEpoch)
                        .replaceValidated(reviewed.inspected.snapshot)
                is ReviewedCompleteBackup ->
                    SqliteCompleteBackupRestore(database, freshEpoch).replace(reviewed.inspected)
            }
        }
    }

    private val operations = LocalDataOperations()

    fun recordingInventory(): SqliteRecordingInventory.Inventory =
        operations.review { SqliteRecordingInventory(database).inspect() }

    fun writeApp(write: (ByteArray) -> Unit) {
        operations.backup { write(SqliteApplicationBackup(database).encode(now())) }
    }

    fun writeComplete(privateDirectory: String, write: (ByteArray) -> Unit) {
        operations.backup {
            val file = SqliteCompleteBackup(database).export(privateDirectory, now())
            try {
                val size = file.size
                var offset = 0L
                while (offset < size) {
                    val bytes = file.read(offset, BackupStagingFile.BLOCK_BYTES)
                    write(bytes)
                    offset += bytes.size
                }
            } catch (error: Throwable) {
                try {
                    file.close()
                } catch (close: Throwable) {
                    error.addSuppressed(close)
                }
                throw error
            }
            file.close()
        }
    }
}
