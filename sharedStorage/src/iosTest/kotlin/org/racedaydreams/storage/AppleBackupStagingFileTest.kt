package org.racedaydreams.storage

import platform.Foundation.NSTemporaryDirectory

class AppleBackupStagingFileTest : BackupStagingFileContract() {
    override fun directory(): String = NSTemporaryDirectory()
}
