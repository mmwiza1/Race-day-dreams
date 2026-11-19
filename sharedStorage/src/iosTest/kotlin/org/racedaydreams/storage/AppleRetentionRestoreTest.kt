package org.racedaydreams.storage

class AppleRetentionRestoreTest : RetentionRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
