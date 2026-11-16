package org.racedaydreams.storage
class JdbcGymExecutionCommandsTest : GymExecutionCommandsContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
