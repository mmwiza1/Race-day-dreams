package org.racedaydreams.storage

import platform.Foundation.NSTemporaryDirectory

class AppleRecordingArchiveTest : RecordingArchiveRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")

    override fun directory(): String = NSTemporaryDirectory()
}
