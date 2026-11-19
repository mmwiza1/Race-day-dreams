package org.racedaydreams.storage

import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.Instant

/**
 * Running half of the existing coaching store; no fake strength methods or second adaptive engine.
 */
class SqliteRunningCoaching(
    private val database: SqlDatabase,
    private val now: () -> Instant,
    private val nextId: () -> String,
) {
    fun versions(): List<PlanVersion> =
        database.query("SELECT payload FROM coach_plan_versions ORDER BY rowid DESC").map {
            RunningCoachingJson.decodeVersion(it.single().sqlText())
        }

    fun current(): PlanVersion? =
        database
            .query(
                "SELECT v.payload FROM coach_current_plan c JOIN coach_plan_versions v ON c.version_id=v.id WHERE c.id=1"
            )
            .firstOrNull()
            ?.single()
            ?.sqlText()
            ?.let { RunningCoachingJson.decodeVersion(it) }

    fun responses(): List<RunExecution> =
        database.query("SELECT payload FROM run_responses ORDER BY activity_id").map {
            RunningCoachingJson.decodeResponse(it.single().sqlText())
        }

    fun proposals(): List<AdjustmentProposal> =
        database
            .query("SELECT payload FROM coach_proposals WHERE decision IS NULL ORDER BY rowid DESC")
            .map { RunningCoachingJson.decodeProposal(it.single().sqlText()) }

    fun proposalKnown(id: String): Boolean =
        database
            .query("SELECT 1 FROM coach_proposals WHERE id=?", listOf(SqlValue.Text(id)))
            .isNotEmpty()

    fun transaction(action: CoachingTransaction) = database.transaction { action.run() }

    fun saveCurrent(version: PlanVersion) =
        database.transaction {
            val proposed = version.plan().workouts().associateBy { it.id() }
            val history = mutableMapOf<String, PlanVersion>()
            for (row in
                database.query(
                    "SELECT payload FROM run_responses WHERE plan_version_id IS NOT NULL"
                )) {
                val response = RunningCoachingJson.decodeResponse(row.single().sqlText())
                val replacement = proposed[response.plannedWorkoutId()] ?: continue
                val original =
                    history.getOrPut(response.planVersionId()!!) {
                        val old =
                            database
                                .query(
                                    "SELECT payload FROM coach_plan_versions WHERE id=?",
                                    listOf(SqlValue.Text(response.planVersionId()!!)),
                                )
                                .firstOrNull()
                                ?: throw IllegalArgumentException(
                                    "Completed prescription version is missing."
                                )
                        RunningCoachingJson.decodeVersion(old.single().sqlText())
                    }
                val prescription =
                    original.plan().workouts().firstOrNull {
                        it.id() == response.plannedWorkoutId()
                    } ?: throw IllegalArgumentException("Completed prescription is missing.")
                require(PlannedWorkoutJson.samePrescription(prescription, replacement)) {
                    "A completed prescription cannot be overwritten. Its original targets must be retained."
                }
            }
            database.execute(
                "INSERT INTO coach_plan_versions(id,plan_id,version,payload) VALUES(?,?,?,?)",
                listOf(
                    SqlValue.Text(version.id()),
                    SqlValue.Text(version.planId()),
                    SqlValue.Integer(version.version().toLong()),
                    SqlValue.Text(RunningCoachingJson.encodeVersion(version)),
                ),
            )
            if (database.query("SELECT id FROM coach_current_plan WHERE id=1").isEmpty())
                database.execute(
                    "INSERT INTO coach_current_plan VALUES(1,?)",
                    listOf(SqlValue.Text(version.id())),
                )
            else
                database.execute(
                    "UPDATE coach_current_plan SET version_id=? WHERE id=1",
                    listOf(SqlValue.Text(version.id())),
                )
            val previous =
                database.query("SELECT id,revision,payload FROM planned_workouts").associateBy {
                    it[0].sqlText()
                }
            database.execute("DELETE FROM planned_workouts")
            val mirror = SqlitePlannedWorkouts(database)
            for (workout in version.plan().workouts()) {
                val prior = previous[workout.id()]
                val unchanged =
                    prior != null &&
                        PlannedWorkoutJson.samePrescription(
                            PlannedWorkoutJson.decode(prior[2].sqlText()),
                            workout,
                        )
                mirror.put(
                    workout,
                    (prior?.get(1)?.sqlLong() ?: 0L) + (if (unchanged) 0 else 1),
                    replace = false,
                )
            }
        }

    fun saveResponse(response: RunExecution) {
        if (response.planVersionId() != null) {
            val version =
                versions().firstOrNull { it.id() == response.planVersionId() }
                    ?: throw IllegalArgumentException("Plan version not found.")
            require(version.plan().workouts().any { it.id() == response.plannedWorkoutId() }) {
                "Workout is not in this plan version."
            }
        }
        val values =
            listOf(
                sqlText(response.plannedWorkoutId()),
                sqlText(response.planVersionId()),
                SqlValue.Text(RunningCoachingJson.encodeResponse(response)),
                SqlValue.Text(response.activityId()),
            )
        if (
            database
                .query(
                    "SELECT activity_id FROM run_responses WHERE activity_id=?",
                    listOf(SqlValue.Text(response.activityId())),
                )
                .isEmpty()
        )
            database.execute(
                "INSERT INTO run_responses(planned_workout_id,plan_version_id,payload,activity_id) VALUES(?,?,?,?)",
                values,
            )
        else
            database.execute(
                "UPDATE run_responses SET planned_workout_id=?,plan_version_id=?,payload=? WHERE activity_id=?",
                values,
            )
    }

    fun saveFeedback(activityId: String, versionId: String?, feedback: RunFeedback) {
        val values =
            listOf(
                sqlText(versionId),
                SqlValue.Text(RunningCoachingJson.encodeFeedback(feedback)),
                SqlValue.Text(activityId),
            )
        if (
            database
                .query(
                    "SELECT activity_id FROM run_feedback WHERE activity_id=?",
                    listOf(SqlValue.Text(activityId)),
                )
                .isEmpty()
        )
            database.execute(
                "INSERT INTO run_feedback(plan_version_id,payload,activity_id) VALUES(?,?,?)",
                values,
            )
        else
            database.execute(
                "UPDATE run_feedback SET plan_version_id=?,payload=? WHERE activity_id=?",
                values,
            )
    }

    fun saveProposal(proposal: AdjustmentProposal) {
        database.execute(
            "INSERT INTO coach_proposals(id,base_version_id,payload) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(proposal.id()),
                SqlValue.Text(proposal.baseVersionId()),
                SqlValue.Text(RunningCoachingJson.encodeProposal(proposal)),
            ),
        )
    }

    fun decide(proposal: AdjustmentProposal, optionId: String, completed: Set<String>) =
        database.transaction {
            val current = current()
            require(current != null && current.id() == proposal.baseVersionId()) {
                "The plan has changed. Review the current recommendation."
            }
            val decision =
                database
                    .query(
                        "SELECT decision FROM coach_proposals WHERE id=?",
                        listOf(SqlValue.Text(proposal.id())),
                    )
                    .firstOrNull()
            require(decision != null && decision.single() == SqlValue.Null) {
                "This proposal has already been resolved."
            }
            val option =
                proposal.options().firstOrNull { it.id() == optionId }
                    ?: throw IllegalArgumentException("Choose a proposal option.")
            var result = current.id()
            if (option.action() != AdjustmentProposal.Action.KEEP) {
                val changed = AdaptiveCoaching.apply(current.plan(), proposal, option, completed)
                result = nextId()
                saveCurrent(
                    PlanVersion(
                        result,
                        current.planId(),
                        current.version() + 1,
                        now(),
                        "Runner chose: ${option.label()}",
                        current.goalSnapshot(),
                        current.evidenceSnapshot(),
                        changed,
                    )
                )
            }
            database.execute(
                "UPDATE coach_proposals SET decision=?,result_version_id=? WHERE id=?",
                listOf(SqlValue.Text(optionId), SqlValue.Text(result), SqlValue.Text(proposal.id())),
            )
        }
}
