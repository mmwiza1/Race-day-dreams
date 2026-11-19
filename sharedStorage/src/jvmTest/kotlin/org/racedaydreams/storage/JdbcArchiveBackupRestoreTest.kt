package org.racedaydreams.storage

class JdbcArchiveBackupRestoreTest : ArchiveBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
