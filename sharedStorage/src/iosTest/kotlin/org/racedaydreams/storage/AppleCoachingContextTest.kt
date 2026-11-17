package org.racedaydreams.storage

class AppleCoachingContextTest : CoachingContextRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
