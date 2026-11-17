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
    class ReviewedAppBackup
    internal constructor(
        internal val owner: LocalBackupOperations,
        internal val inspected: BackupEnvelopeJson.Checked<UserBackupDataJson.Snapshot>,
    ) {
        val version: Int
            get() = inspected.version

        val createdAt: Instant
            get() = inspected.createdAt
    }

    fun reviewApp(bytes: ByteArray): ReviewedAppBackup =
        ReviewedAppBackup(this, UserBackupDataJson.inspect(bytes))

    /** The host must first obtain explicit confirmation for this reviewed document. */
    fun restoreApp(reviewed: ReviewedAppBackup) {
        require(reviewed.owner === this) {
            "Review the backup in this app session before restoring."
        }
        operations.replace {
            SqliteApplicationBackupRestore(database, freshEpoch)
                .replaceValidated(reviewed.inspected.snapshot)
        }
    }

    private val operations = LocalDataOperations()

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
