package org.racedaydreams.storage

class JdbcRecordingCleanupTest : RecordingCleanupRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")

    override fun directory(): String = System.getProperty("java.io.tmpdir")
}
