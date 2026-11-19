package org.racedaydreams.storage

class JdbcClimbingRepositoryTest : ClimbingRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
