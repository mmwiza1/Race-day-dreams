package org.racedaydreams.storage

/** The existing Android schema, without destructive fallback or guessed recovery. */
object ApplicationSchema {
    const val VERSION = 20
    const val FILE_NAME = "racedaydreams.db"

    data class NewInstallation(val chosenAt: String, val choiceId: String) {
        init {
            org.racedaydreams.domain.storage.DetailRetention.Preferences.fresh(
                StoredInstant.parse(chosenAt),
                choiceId,
            )
        }
    }

    /** Call before publishing the connection to repositories. Fresh choices need real clock/ID. */
    fun open(database: SqlDatabase, freshChoice: () -> NewInstallation) {
        database.execute("PRAGMA foreign_keys = ON")
        check(database.query("PRAGMA foreign_keys").single().single() == SqlValue.Integer(1))
        database.transaction {
            val version =
                (database.query("PRAGMA user_version").single().single() as SqlValue.Integer).value
            require(version in 0L..VERSION.toLong()) {
                "Unsupported database version; files retained."
            }
            if (version == 0L) {
                require(
                    database
                        .query("SELECT name FROM sqlite_master WHERE name NOT LIKE 'sqlite_%'")
                        .isEmpty()
                ) {
                    "Unversioned existing database; files retained."
                }
            }
            for (next in (version.toInt() + 1)..VERSION) {
                for (statement in SchemaStatements.versions[next - 1]) database.execute(statement)
            }
            if (version == 0L) {
                val choice = freshChoice()
                database.execute(
                    "UPDATE detail_retention_preferences SET policy='LAST_24_MONTHS', choice='NEW_INSTALL_DEFAULT',chosen_at=?,choice_id=? WHERE id=1",
                    listOf(SqlValue.Text(choice.chosenAt), SqlValue.Text(choice.choiceId)),
                )
            }
            database.execute("PRAGMA user_version = $VERSION")
        }
    }
}
