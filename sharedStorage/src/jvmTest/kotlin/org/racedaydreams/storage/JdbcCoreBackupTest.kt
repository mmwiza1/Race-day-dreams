package org.racedaydreams.storage

class JdbcCoreBackupTest : CoreBackupRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
