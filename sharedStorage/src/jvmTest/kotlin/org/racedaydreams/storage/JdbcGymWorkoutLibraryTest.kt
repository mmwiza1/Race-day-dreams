package org.racedaydreams.storage

class JdbcGymWorkoutLibraryTest : GymWorkoutLibraryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
