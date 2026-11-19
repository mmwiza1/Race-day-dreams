package org.racedaydreams.storage

class JdbcRetentionRestoreTest : RetentionRestoreRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
