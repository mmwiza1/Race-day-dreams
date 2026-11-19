package org.racedaydreams.storage

class AppleStrengthProgrammingCommandsTest : StrengthProgrammingCommandsContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
