package org.racedaydreams.storage

class JdbcHealthKitRunningTest : HealthKitRunningRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
