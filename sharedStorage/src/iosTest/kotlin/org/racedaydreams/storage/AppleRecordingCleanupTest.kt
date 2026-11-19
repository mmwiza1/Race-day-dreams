package org.racedaydreams.storage

class AppleRecordingCleanupTest : RecordingCleanupRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")

    override fun directory(): String = platform.Foundation.NSTemporaryDirectory()
}
