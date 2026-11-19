package org.racedaydreams.storage

/**
 * Internal all-or-nothing package1 restore, using BOTH fully inspected compatible inner documents.
 * Does not expose a destructive native UI or acquire platform recording/backup-operation locks.
 */
internal class SqliteCompleteBackupRestore(
    private val database: SqlDatabase,
    private val freshEpoch: () -> String,
) {
    fun replace(archive: InspectedBackupPackage) {
        // Fail before touching the database if the inspected private file was already closed.
        archive.recordingArchive().size
        database.transaction {
            SqliteApplicationBackupRestore(database, freshEpoch)
                .replaceValidated(archive.application.snapshot)
            SqliteRecordingArchiveRestore(database, freshEpoch).mergeComplete(archive)
            require(database.query("PRAGMA foreign_key_check").isEmpty()) {
                "Complete backup has missing references; previous data retained."
            }
        }
    }
}
