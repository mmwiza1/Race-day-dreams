package org.racedaydreams.storage

import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository

/** Existing revisioned reminder mirror; immutable coaching history is held separately. */
class SqlitePlannedWorkouts(private val database: SqlDatabase) : PlannedWorkoutRepository {
    override fun plannedWorkouts(): List<PlannedWorkout> =
        database.query("SELECT payload FROM planned_workouts ORDER BY scheduled_date,id").map {
            PlannedWorkoutJson.decode(it.single().sqlText())
        }

    override fun plannedWorkoutRevision(id: String): Long =
        database
            .query("SELECT revision FROM planned_workouts WHERE id=?", listOf(SqlValue.Text(id)))
            .firstOrNull()
            ?.single()
            ?.sqlLong() ?: 0L

    override fun savePlannedWorkout(workout: PlannedWorkout) =
        database.transaction { put(workout, plannedWorkoutRevision(workout.id()) + 1) }

    internal fun put(workout: PlannedWorkout, revision: Long, replace: Boolean = true) {
        database.execute(
            (if (replace) "INSERT OR REPLACE" else "INSERT") +
                " INTO planned_workouts(id,scheduled_date,revision,payload) VALUES(?,?,?,?)",
            listOf(
                SqlValue.Text(sqlRequired(workout.id(), "workout ID")),
                SqlValue.Text(workout.date().toString()),
                SqlValue.Integer(revision),
                SqlValue.Text(PlannedWorkoutJson.encode(workout)),
            ),
        )
    }

    override fun deletePlannedWorkout(id: String) {
        database.execute(
            "DELETE FROM planned_workouts WHERE id=?",
            listOf(SqlValue.Text(sqlRequired(id, "workout ID"))),
        )
    }
}
