package org.racedaydreams.storage

class JdbcRecordingInventoryTest : RecordingInventoryRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
