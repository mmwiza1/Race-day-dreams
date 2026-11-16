package org.racedaydreams.storage

class JvmBackupStagingFileTest : BackupStagingFileContract() {
    override fun directory(): String = System.getProperty("java.io.tmpdir")
}
