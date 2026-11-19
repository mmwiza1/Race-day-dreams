package org.racedaydreams.storage

class AppleDevelopmentArchiveTest : DevelopmentArchiveRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")
}
