package org.racedaydreams.storage
class AppleHealthKitBodyTest : HealthKitBodyRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
