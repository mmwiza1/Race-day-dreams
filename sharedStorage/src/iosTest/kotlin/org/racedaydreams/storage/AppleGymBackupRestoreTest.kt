package org.racedaydreams.storage

class AppleGymBackupRestoreTest : GymBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
