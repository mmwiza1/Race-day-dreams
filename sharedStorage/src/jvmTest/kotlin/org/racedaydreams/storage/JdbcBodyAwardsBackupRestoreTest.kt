package org.racedaydreams.storage

class JdbcBodyAwardsBackupRestoreTest : BodyAwardsBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
