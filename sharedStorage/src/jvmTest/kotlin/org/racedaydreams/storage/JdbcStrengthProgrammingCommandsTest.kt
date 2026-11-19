package org.racedaydreams.storage

class JdbcStrengthProgrammingCommandsTest : StrengthProgrammingCommandsContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
