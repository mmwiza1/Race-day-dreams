package org.racedaydreams.storage

class JdbcGymAssociationCommandsTest : GymAssociationCommandsContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
