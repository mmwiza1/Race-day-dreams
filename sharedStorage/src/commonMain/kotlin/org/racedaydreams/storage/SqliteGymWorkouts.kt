package org.racedaydreams.storage

import org.racedaydreams.domain.strength.GymExecutionRepository.Preference
import org.racedaydreams.domain.strength.GymWorkoutDefinition

/** Saved library over the original tables. Reading a record does not approve its linked source. */
class SqliteGymWorkouts(private val database: SqlDatabase) {
    fun record(id: String, revision: Int): FrozenGymWorkoutRecordJson.Record? {
        val row =
            database
                .query(
                    "SELECT payload,source_kind,source_id FROM strength_workout_versions WHERE workout_id=? AND revision=?",
                    listOf(
                        SqlValue.Text(sqlRequired(id, "workout")),
                        SqlValue.Integer(revision.toLong()),
                    ),
                )
                .firstOrNull() ?: return null
        val value = FrozenGymWorkoutRecordJson.decode(row[0].sqlText())
        require(
            value.definition.id() == id &&
                value.definition.revision() == revision &&
                value.source?.kind()?.name == row[1].nullableSqlText() &&
                value.source?.id() == row[2].nullableSqlText()
        ) {
            "Saved workout identity differs from its retained record."
        }
        return value
    }

    fun workout(id: String, revision: Int): GymWorkoutDefinition? = record(id, revision)?.definition

    /** Selection is bounded and never loads actual sets or execution event histories. */
    fun favourites(limit: Int, offset: Int = 0): List<GymWorkoutDefinition> {
        require(limit in 1..50 && offset in 0..10000) { "Choose a bounded saved-workout page." }
        return database
            .query(
                "SELECT w.payload FROM strength_workout_preferences p JOIN strength_workout_versions w ON w.workout_id=p.workout_id AND w.revision=p.current_revision WHERE p.favourite=1 ORDER BY p.workout_id LIMIT ? OFFSET ?",
                listOf(SqlValue.Integer(limit.toLong()), SqlValue.Integer(offset.toLong())),
            )
            .map { FrozenGymWorkoutRecordJson.decode(it[0].sqlText()).definition }
    }

    fun preferences(): List<Preference> =
        database
            .query(
                "SELECT workout_id,current_revision,favourite FROM strength_workout_preferences ORDER BY workout_id"
            )
            .map { Preference(it[0].sqlText(), it[1].sqlLong().toInt(), it[2].sqlLong() == 1L) }

    fun savePreference(preference: Preference) =
        database.transaction {
            require(record(preference.workoutId(), preference.currentRevision()) != null) {
                "The saved workout revision is missing."
            }
            val values =
                listOf(
                    SqlValue.Integer(preference.currentRevision().toLong()),
                    SqlValue.Integer(if (preference.favourite()) 1 else 0),
                    SqlValue.Text(preference.workoutId()),
                )
            if (
                database
                    .query(
                        "SELECT 1 FROM strength_workout_preferences WHERE workout_id=?",
                        listOf(SqlValue.Text(preference.workoutId())),
                    )
                    .isEmpty()
            )
                database.execute(
                    "INSERT INTO strength_workout_preferences(current_revision,favourite,workout_id) VALUES(?,?,?)",
                    values,
                )
            else
                database.execute(
                    "UPDATE strength_workout_preferences SET current_revision=?,favourite=? WHERE workout_id=?",
                    values,
                )
        }
}
