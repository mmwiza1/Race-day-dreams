package org.racedaydreams.storage

class JdbcCoreBackupRestoreTest : CoreBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
