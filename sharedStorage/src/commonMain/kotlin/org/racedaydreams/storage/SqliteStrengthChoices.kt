package org.racedaydreams.storage

import kotlinx.serialization.json.*

/**
 * Existing reversible favourites/archive choices and PB acknowledgement, separate from evidence.
 */
class SqliteStrengthChoices(private val database: SqlDatabase) {
    data class Choice(val exerciseId: String, val favourite: Boolean, val archived: Boolean)

    /** One bounded read in the shared transaction; no exclusions or PB records are changed. */
    fun archiveSnapshot(): StrengthChoicesArchiveJson.Snapshot =
        database.transaction {
            var minimumBytes = 0L
            for ((table, maximum, size) in
                listOf(
                    Triple("strength_user_state", 10000, "LENGTH(CAST(exercise_id AS BLOB))"),
                    Triple("pb_acknowledgements", 100000, "LENGTH(CAST(id AS BLOB))"),
                    Triple(
                        "strength_exclusions",
                        10000,
                        "LENGTH(CAST(exercise_id AS BLOB))+LENGTH(CAST(reason AS BLOB))+LENGTH(CAST(excluded_at AS BLOB))",
                    ),
                )) {
                val totals =
                    database.query("SELECT COUNT(*),COALESCE(SUM($size),0) FROM $table").single()
                require(totals[0].sqlLong() <= maximum) {
                    "Too many exercise choices; nothing was truncated."
                }
                val bytes = totals[1].sqlLong()
                require(bytes >= 0 && bytes <= BackupEnvelopeJson.MAX_BYTES - minimumBytes) {
                    "Exercise choices exceed the whole-backup budget; nothing was truncated."
                }
                minimumBytes += bytes
            }
            StrengthChoicesArchiveJson.decode(
                buildJsonObject {
                    put("version", 2)
                    put(
                        "choices",
                        buildJsonArray {
                            for (choice in choices()) add(
                                buildJsonObject {
                                    put("id", choice.exerciseId)
                                    put("favourite", choice.favourite)
                                    put("archived", choice.archived)
                                }
                            )
                        },
                    )
                    put("acknowledged", buildJsonArray { for (id in acknowledged()) add(id) })
                    put(
                        "exclusions",
                        buildJsonObject {
                            put("version", 1)
                            put(
                                "entries",
                                buildJsonArray {
                                    for (e in SqliteExerciseExclusions(database).exclusions()) add(
                                        buildJsonObject {
                                            put("exerciseId", e.exerciseId())
                                            put("reason", e.reason().name)
                                            put("excludedAt", e.excludedAt().toString())
                                        }
                                    )
                                },
                            )
                        },
                    )
                }
            )
        }

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
