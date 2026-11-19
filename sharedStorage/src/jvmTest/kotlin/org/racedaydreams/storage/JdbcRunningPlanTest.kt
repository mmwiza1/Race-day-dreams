package org.racedaydreams.storage

class JdbcRunningPlanTest : RunningPlanRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
