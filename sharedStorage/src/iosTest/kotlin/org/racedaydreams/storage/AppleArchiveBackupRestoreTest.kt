package org.racedaydreams.storage

class AppleArchiveBackupRestoreTest : ArchiveBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
