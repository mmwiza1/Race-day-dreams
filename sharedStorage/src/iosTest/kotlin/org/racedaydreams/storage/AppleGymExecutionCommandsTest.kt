package org.racedaydreams.storage
class AppleGymExecutionCommandsTest : GymExecutionCommandsContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
