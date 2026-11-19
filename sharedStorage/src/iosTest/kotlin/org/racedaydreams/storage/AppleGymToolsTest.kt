package org.racedaydreams.storage

class AppleGymToolsTest : GymToolsRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
