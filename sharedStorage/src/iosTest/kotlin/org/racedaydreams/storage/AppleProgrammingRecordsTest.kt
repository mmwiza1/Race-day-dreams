package org.racedaydreams.storage

class AppleProgrammingRecordsTest : ProgrammingRecordsRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
