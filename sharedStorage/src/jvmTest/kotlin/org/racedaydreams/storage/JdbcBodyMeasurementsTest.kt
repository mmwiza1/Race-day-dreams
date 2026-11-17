package org.racedaydreams.storage
class JdbcBodyMeasurementsTest : BodyMeasurementsRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
