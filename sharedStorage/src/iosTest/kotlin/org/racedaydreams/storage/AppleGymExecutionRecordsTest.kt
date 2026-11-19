package org.racedaydreams.storage

class AppleGymExecutionRecordsTest : GymExecutionRecordsContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
