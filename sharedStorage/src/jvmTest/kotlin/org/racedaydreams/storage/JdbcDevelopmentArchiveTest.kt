package org.racedaydreams.storage

class JdbcDevelopmentArchiveTest : DevelopmentArchiveRepositoryContract() {
    override fun database(): SqlDatabase = JdbcDatabase("jdbc:sqlite::memory:")
}
