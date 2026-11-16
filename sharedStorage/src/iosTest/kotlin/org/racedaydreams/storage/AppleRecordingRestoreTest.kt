package org.racedaydreams.storage

class AppleRecordingRestoreTest : RecordingRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")

    override fun directory(): String = platform.Foundation.NSTemporaryDirectory()
}
