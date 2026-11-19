package org.racedaydreams.storage

import platform.Foundation.NSTemporaryDirectory

class AppleCompleteBackupTest : CompleteBackupRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")

    override fun directory(): String = NSTemporaryDirectory()
}
