package org.racedaydreams.storage

import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.time.Instant

/**
 * Application-owned lifecycle composition. Create ONE per application database and route all its
 * backup/restore/retention actions through it. Blocking IO belongs on the host's IO executor.
 * Native UI remains disabled until the real application uses this owner instead of direct kernels.
 * A scoped backup callback must finish copying before returning; its private source then closes.
 */
class SqliteDataLifecycle(
    private val database: SqlDatabase,
    private val privateDirectory: String,
    private val freshEpoch: () -> String,
    freshChoiceId: () -> String,
    physicalFileBytes: () -> Long? = { null },
) {
    private val operations = LocalDataOperations()
    private val policy =
        SqliteRetentionPolicy(database, freshChoiceId, operations::replacementInProgress)
    private val inventory = SqliteRecordingInventory(database, physicalFileBytes)

    fun retentionPreferences(): DetailRetention.Preferences =
        operations.review { SqliteRetentionPreferences(database).load() }

    fun inventory(): SqliteRecordingInventory.Inventory = operations.review { inventory.inspect() }

    /** Explicit informed choice only. This method never cleans up detailed evidence. */
    fun acknowledgeRetention(choice: DetailRetention.Policy, now: Instant) =
        operations.replace { policy.acknowledge(choice, now) }

    fun previewCleanup(choice: DetailRetention.Policy, now: Instant) =
        operations.review { policy.preview(choice, now) }

    fun approveCleanup(preview: DetailRetention.Preview, now: Instant) =
        operations.review { policy.approve(preview, now) }

    fun withApplicationBackup(at: Instant, consume: (ByteArray) -> Unit) =
        operations.backup { consume(SqliteApplicationBackup(database).encode(at)) }

    fun withRecordingArchive(consume: (BackupArchiveSource) -> Unit) =
        operations.backup {
            val file = SqliteRecordingArchive(database).export(privateDirectory)
            try {
                consume(file)
            } finally {
                file.close()
            }
        }

    fun withCompleteBackup(at: Instant, consume: (BackupArchiveSource) -> Unit) =
        operations.backup {
            val file = SqliteCompleteBackup(database).export(privateDirectory, at)
            try {
                consume(file)
            } finally {
                file.close()
            }
        }

    fun restoreApplication(bytes: ByteArray) =
        operations.replace { SqliteApplicationBackupRestore(database, freshEpoch).replace(bytes) }

    fun restoreRecordingArchive(source: BackupArchiveSource) =
        operations.replace {
            val checked = InspectedRecordingArchive.inspect(source, privateDirectory)
            try {
                SqliteRecordingArchiveRestore(database, freshEpoch).merge(checked)
            } finally {
                checked.close()
            }
        }

    fun restoreCompleteBackup(source: BackupArchiveSource) =
        operations.replace {
            val checked = InspectedBackupPackage.inspect(source, privateDirectory)
            try {
                SqliteCompleteBackupRestore(database, freshEpoch).replace(checked)
            } finally {
                checked.close()
            }
        }

    // Intentionally not exported to the Native UI. Host composition and final destructive-flow
    // acceptance remain required; an approval by itself must never remove training evidence.
    internal fun cleanOne(approval: DetailRetention.Approval, id: String, now: Instant) =
        SqliteRecordingDetailCleanup(database, operations).cleanOne(approval, id, now)
}
