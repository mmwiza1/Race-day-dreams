package org.racedaydreams.storage

class JdbcStrengthDraftCommandsTest : StrengthDraftCommandsContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
