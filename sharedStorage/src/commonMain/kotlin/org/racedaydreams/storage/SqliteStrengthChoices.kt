package org.racedaydreams.storage

/**
 * Existing reversible favourites/archive choices and PB acknowledgement, separate from evidence.
 */
class SqliteStrengthChoices(private val database: SqlDatabase) {
    data class Choice(val exerciseId: String, val favourite: Boolean, val archived: Boolean)

    fun choices(): List<Choice> =
        database
            .query(
                "SELECT exercise_id,favourite,archived FROM strength_user_state ORDER BY exercise_id"
            )
            .map { Choice(it[0].sqlText(), it[1].sqlLong() == 1L, it[2].sqlLong() == 1L) }

    fun save(choice: Choice) {
        require(choice.exerciseId.isNotEmpty() && choice.exerciseId.length <= 200) {
            "Invalid exercise ID."
        }
        database.transaction {
            val values =
                listOf(
                    SqlValue.Integer(if (choice.favourite) 1 else 0),
                    SqlValue.Integer(if (choice.archived) 1 else 0),
                    SqlValue.Text(choice.exerciseId),
                )
            if (
                database
                    .query(
                        "SELECT exercise_id FROM strength_user_state WHERE exercise_id=?",
                        listOf(SqlValue.Text(choice.exerciseId)),
                    )
                    .isEmpty()
            )
                database.execute(
                    "INSERT INTO strength_user_state(favourite,archived,exercise_id) VALUES(?,?,?)",
                    values,
                )
            else
                database.execute(
                    "UPDATE strength_user_state SET favourite=?,archived=? WHERE exercise_id=?",
                    values,
                )
        }
    }

    fun acknowledged(): Set<String> =
        database
            .query("SELECT id FROM pb_acknowledgements ORDER BY id")
            .map { it.single().sqlText() }
            .toSet()

    fun acknowledge(id: String) {
        require(id.isNotEmpty() && id.length <= 500) { "Invalid PB ID." }
        database.execute(
            "INSERT OR IGNORE INTO pb_acknowledgements(id) VALUES(?)",
            listOf(SqlValue.Text(id)),
        )
    }
}
