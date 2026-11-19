package org.racedaydreams.storage
class AppleBodyMeasurementsTest : BodyMeasurementsRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
