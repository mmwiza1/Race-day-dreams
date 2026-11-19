package org.racedaydreams.storage

class JdbcApplicationBackupRestoreTest : ApplicationBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
