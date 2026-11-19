package org.racedaydreams.storage

class AppleCoachingRepositoryTest : CoachingRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
