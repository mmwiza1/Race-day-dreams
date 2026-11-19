package org.racedaydreams.storage

class AppleRetentionPolicyTest : RetentionPolicyRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
