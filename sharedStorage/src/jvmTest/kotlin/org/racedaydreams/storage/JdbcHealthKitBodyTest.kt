package org.racedaydreams.storage
class JdbcHealthKitBodyTest : HealthKitBodyRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
