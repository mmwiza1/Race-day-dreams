package org.racedaydreams.storage

class AppleGymWorkoutLibraryTest : GymWorkoutLibraryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
