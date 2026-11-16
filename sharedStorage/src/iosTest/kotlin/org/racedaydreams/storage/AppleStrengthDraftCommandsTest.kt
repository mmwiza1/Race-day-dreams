package org.racedaydreams.storage

class AppleStrengthDraftCommandsTest : StrengthDraftCommandsContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
