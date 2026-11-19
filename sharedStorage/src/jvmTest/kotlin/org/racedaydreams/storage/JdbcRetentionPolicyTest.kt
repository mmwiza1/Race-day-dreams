package org.racedaydreams.storage

class JdbcRetentionPolicyTest : RetentionPolicyRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
