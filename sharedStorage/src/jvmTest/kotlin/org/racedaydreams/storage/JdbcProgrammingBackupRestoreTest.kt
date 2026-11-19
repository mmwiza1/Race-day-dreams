package org.racedaydreams.storage

class JdbcProgrammingBackupRestoreTest : ProgrammingBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
