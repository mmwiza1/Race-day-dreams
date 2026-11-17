package org.racedaydreams.storage

class JdbcCoachingRepositoryTest : CoachingRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
