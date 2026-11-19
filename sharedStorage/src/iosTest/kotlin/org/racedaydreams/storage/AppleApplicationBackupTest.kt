package org.racedaydreams.storage

class AppleApplicationBackupTest : ApplicationBackupRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
