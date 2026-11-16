package org.racedaydreams.storage

class JdbcLegacyGymProgrammeTest : LegacyGymProgrammeRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
