package org.racedaydreams.storage

class AppleGymAssociationCommandsTest : GymAssociationCommandsContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
