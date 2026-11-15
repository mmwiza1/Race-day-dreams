package org.racedaydreams.storage

class AppleRunningPlanTest : RunningPlanRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
