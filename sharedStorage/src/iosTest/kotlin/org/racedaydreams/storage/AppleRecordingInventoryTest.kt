package org.racedaydreams.storage

class AppleRecordingInventoryTest : RecordingInventoryRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
