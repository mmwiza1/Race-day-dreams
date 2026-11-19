package org.racedaydreams.storage

class AppleHealthKitRunningTest : HealthKitRunningRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
