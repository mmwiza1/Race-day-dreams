package org.racedaydreams.storage

class JdbcCoachingContextTest : CoachingContextRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
