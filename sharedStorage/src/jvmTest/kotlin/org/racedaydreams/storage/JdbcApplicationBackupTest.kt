package org.racedaydreams.storage

class JdbcApplicationBackupTest : ApplicationBackupRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
