package org.racedaydreams.storage

class JdbcGymBackupRestoreTest : GymBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
