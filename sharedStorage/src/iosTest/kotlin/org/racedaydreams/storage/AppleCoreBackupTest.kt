package org.racedaydreams.storage

class AppleCoreBackupTest : CoreBackupRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
