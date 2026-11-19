package org.racedaydreams.storage

class AppleCoreBackupRestoreTest : CoreBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
