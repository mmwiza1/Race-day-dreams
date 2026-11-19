package org.racedaydreams.storage

class JdbcProgrammingRecordsTest : ProgrammingRecordsRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
