package org.racedaydreams.storage

class JdbcGymToolsTest : GymToolsRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
