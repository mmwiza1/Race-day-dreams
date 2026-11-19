package org.racedaydreams.storage

class AppleClimbingRepositoryTest : ClimbingRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
