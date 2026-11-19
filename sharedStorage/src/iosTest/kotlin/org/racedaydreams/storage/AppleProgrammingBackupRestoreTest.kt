package org.racedaydreams.storage

class AppleProgrammingBackupRestoreTest : ProgrammingBackupRestoreRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
