package org.racedaydreams.storage

class JdbcCompleteBackupTest : CompleteBackupRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")

    override fun directory(): String = System.getProperty("java.io.tmpdir")
}
