package org.racedaydreams.storage

class JdbcGymExecutionRecordsTest : GymExecutionRecordsContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
