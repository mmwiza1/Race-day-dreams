package org.racedaydreams.storage

class AppleApplicationBackupRestoreTest : ApplicationBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
