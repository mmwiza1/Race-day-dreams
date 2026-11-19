package org.racedaydreams.storage

class AppleLegacyGymProgrammeTest : LegacyGymProgrammeRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
