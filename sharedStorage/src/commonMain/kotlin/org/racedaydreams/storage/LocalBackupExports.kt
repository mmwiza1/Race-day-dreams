package org.racedaydreams.storage

import org.racedaydreams.domain.time.Instant

/**
 * One instance per application database. Keep capture and copying under the same operation lock.
 */
class LocalBackupExports(private val database: SqlDatabase, private val now: () -> Instant) {
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
