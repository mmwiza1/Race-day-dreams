package org.racedaydreams.storage

import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.Instant

/**
 * Full existing coaching boundary over ONE database. Running decisions, actual edits and custom
 * definitions delegate to their established kernels; no duplicate adaptive/execution engine. Host
 * owns IO dispatch, operation coordination and database lifetime.
 */
class SqliteCoachingRepository(
    private val database: SqlDatabase,
    now: () -> Instant,
    nextId: () -> String,
    fingerprints: LegacyStrengthFingerprint,
) : CoachingRepository {
    private val running = SqliteRunningCoaching(database, now, nextId)
    private val executions = SqliteGymExecutions(database, now, fingerprints)
    private val drafts = SqliteStrengthDrafts(database)

    override fun current(): PlanVersion? = running.current()

    override fun versions(): List<PlanVersion> = running.versions()

    override fun responses(): List<RunExecution> = running.responses()

    override fun proposals(): List<AdjustmentProposal> = running.proposals()

    override fun proposalKnown(id: String): Boolean = running.proposalKnown(id)

    override fun saveCurrent(version: PlanVersion) = running.saveCurrent(version)

    override fun saveResponse(response: RunExecution) = running.saveResponse(response)

    override fun saveFeedback(activityId: String, versionId: String?, feedback: RunFeedback) =
        running.saveFeedback(activityId, versionId, feedback)

    override fun saveProposal(proposal: AdjustmentProposal) = running.saveProposal(proposal)

    override fun decide(
        proposal: AdjustmentProposal,
        optionId: String,
        completedWorkoutIds: Set<String>,
    ) = running.decide(proposal, optionId, completedWorkoutIds)

    override fun transaction(action: CoachingTransaction) = running.transaction(action)

    override fun exercises(): List<StrengthExercise> {
        val all = linkedMapOf<String, StrengthExercise>()
        for (exercise in StrengthExercise.starters()) all[exercise.id()] = exercise
        for (row in database.query("SELECT payload FROM strength_exercises ORDER BY id")) {
            val exercise = StrengthEvidenceJson.decodeExercise(row.single().sqlText())
            all[exercise.id()] = exercise
        }
        return Immutable.copyList(all.values)
    }

    override fun strength(): List<StrengthSession> =
        Immutable.copyList(
            database
                .query("SELECT payload FROM strength_sessions ORDER BY started_at DESC,id")
                .map { StrengthEvidenceJson.decodeSession(it.single().sqlText()) }
        )

    override fun saveExercise(exercise: StrengthExercise) =
        database.transaction {
            if (!drafts.updateLoggingDefinition(exercise)) {
                val values =
                    listOf(
                        SqlValue.Text(StrengthEvidenceJson.encodeExercise(exercise)),
                        SqlValue.Text(exercise.id()),
                    )
                database.execute("UPDATE strength_exercises SET payload=? WHERE id=?", values)
                if (database.query("SELECT changes()").single().single().sqlLong() == 0L) {
                    database.execute(
                        "INSERT INTO strength_exercises(payload,id) VALUES(?,?)",
                        values,
                    )
                }
            }
        }

    override fun saveStrength(session: StrengthSession) = executions.saveActual(session)

    override fun deleteStrength(id: String) = executions.deleteActual(id)
}
