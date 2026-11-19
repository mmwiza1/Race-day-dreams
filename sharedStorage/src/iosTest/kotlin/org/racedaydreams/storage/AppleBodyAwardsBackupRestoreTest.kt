package org.racedaydreams.storage

class AppleBodyAwardsBackupRestoreTest : BodyAwardsBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
