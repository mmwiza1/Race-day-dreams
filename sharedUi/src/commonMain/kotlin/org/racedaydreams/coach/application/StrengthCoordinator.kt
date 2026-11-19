package org.racedaydreams.coach.application

import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.coaching.CoachingTransaction
import org.racedaydreams.domain.strength.ExerciseExclusionRepository
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.storage.SqliteCoachingContext
import org.racedaydreams.storage.SqliteLegacyGymProgrammes
import org.racedaydreams.storage.SqliteStrengthChoices

/** Strength sessions, programme versions and evidence-backed progression commands. */
class StrengthCoordinator(
    private val gym: SqliteLegacyGymProgrammes,
    private val choices: SqliteStrengthChoices,
    private val exclusions: ExerciseExclusionRepository,
    private val operations: CoachOperationRunner,
    private val coaching: CoachingRepository,
    private val development: SqliteCoachingContext,
    private val programmes: SharedGymProgrammeOperations,
    private val now: () -> Instant,
    private val today: () -> LocalDate,
) {
    fun previewGenerated(settings: GymProgramme.Settings): GymProgramme {
        val snapshot = operations.current
        val favourites =
            snapshot.exerciseChoices
                .filter { it.favourite() && !it.archived() }
                .map { it.exerciseId() }
                .toSet()
        return programmes.generate(
            snapshot.gymProgramme,
            settings,
            snapshot.plannedWorkouts,
            snapshot.strengthSessions,
            favourites,
            snapshot.exerciseExclusions.map { it.exerciseId() }.toSet(),
        )
    }

    fun previewMove(plan: GymProgramme, sessionId: String, date: LocalDate) =
        programmes.move(
            plan,
            sessionId,
            date,
            operations.current.strengthPlannedDates.values.toSet(),
        )

    fun previewFewerSets(plan: GymProgramme, sessionId: String) =
        programmes.reduce(plan, sessionId, operations.current.strengthPlannedDates.values.toSet())

    fun previewSkip(plan: GymProgramme, sessionId: String) =
        programmes.skip(plan, sessionId, operations.current.strengthPlannedDates.values.toSet())

    fun previewSwap(plan: GymProgramme, sessionId: String, from: String, to: String): GymProgramme {
        require(operations.current.exerciseExclusions.none { it.exerciseId() == to }) {
            "Restore this exercise before recommending it in a programme."
        }
        return programmes.swap(
            plan,
            sessionId,
            from,
            to,
            operations.current.strengthPlannedDates.values.toSet(),
        )
    }

    fun alternatives(plan: GymProgramme, from: String, query: String) =
        programmes.alternatives(plan, from, query, operations.current.exercises).filter { row ->
            operations.current.exerciseExclusions.none { it.exerciseId() == row.id() }
        }

    fun conflicts(session: GymProgramme.Session) =
        programmes.conflicts(
            session,
            operations.current.plannedWorkouts,
            operations.current.strengthSessions,
        )

    fun gymProgression(base: String, sessionId: String, exerciseId: String, accept: Boolean) =
        operations.submit("Strength progression choice recorded. Actual workouts are unchanged.") {
            val repository = gym
            val p =
                repository.current() ?: throw IllegalArgumentException("No current gym programme.")
            require(key(p) == base) { "The programme changed. Review the current suggestion." }
            val prescription =
                p.sessions()
                    .first { it.id() == sessionId }
                    .exercises()
                    .first { it.exerciseId() == exerciseId }
            val actual = coaching.strength()
            val advice =
                org.racedaydreams.domain.strength.StrengthProgressionAdvisor.review(
                    prescription,
                    actual,
                    now(),
                )
            val evidence =
                advice.reason() +
                    "\n" +
                    actual
                        .filter { s -> s.exercises().any { it.exercise().id() == exerciseId } }
                        .sortedByDescending { it.startedAt() }
                        .take(2)
                        .joinToString("\n") { s ->
                            s.id() +
                                ": " +
                                s.exercises()
                                    .filter { it.exercise().id() == exerciseId }
                                    .joinToString { it.sets().toString() }
                        }
            val proposed =
                if (accept) {
                    require(
                        advice.action() ==
                            org.racedaydreams.domain.strength.StrengthProgressionAdvisor.Action
                                .CONSIDER_INCREASE && advice.suggestedKilograms() != null
                    ) {
                        "Current evidence does not support that increase."
                    }
                    org.racedaydreams.domain.strength.GymPlanEditor.loadReference(
                        p,
                        sessionId,
                        exerciseId,
                        requireNotNull(advice.suggestedKilograms()),
                        today(),
                        now(),
                        development.strengthLinks().values.toSet(),
                    )
                } else null
            repository.decide(p, proposed, sessionId, exerciseId, evidence)
        }

    fun saveGym(plan: org.racedaydreams.domain.strength.GymProgramme) =
        operations.submit() { gym.save(plan) }

    fun disableGym() =
        operations.submit("Generated programming stopped. Your recorded workouts and PBs remain.") {
            gym.disable()
        }

    fun exerciseExclusion(
        id: String,
        reason: org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason?,
    ) =
        operations.submit(
            "Exercise preference saved. Existing targets, history and PBs are unchanged."
        ) {
            require(coaching.exercises().any { it.id() == id }) {
                "Exercise is no longer available."
            }
            if (reason == null) exclusions.restoreExercise(id)
            else
                exclusions.exclude(
                    org.racedaydreams.domain.strength.ExerciseExclusionRepository.Exclusion(
                        id,
                        reason,
                        now(),
                    )
                )
        }

    fun exerciseChoice(id: String, favourite: Boolean, archived: Boolean) =
        operations.submit { choices.save(SqliteStrengthChoices.Choice(id, favourite, archived)) }

    fun acknowledgePb(id: String) = operations.submit { choices.acknowledge(id) }

    fun saveStrength(
        session: StrengthSession,
        onSuccess: () -> Unit = {},
        plannedDate: LocalDate? = null,
    ) =
        operations.submit(
            "Strength workout saved. PB history is derived from retained sets.",
            onSuccess,
        ) {
            coaching.transaction(
                object : CoachingTransaction {
                    override fun run() {
                        coaching.saveStrength(session)
                        if (plannedDate != null && session.id() !in development.strengthLinks()) {
                            val plan = gym.current()
                            val prescription =
                                plan?.sessions()?.firstOrNull { it.date() == plannedDate }
                            if (plan != null && prescription != null)
                                development.linkGym(
                                    session.id(),
                                    plannedDate,
                                    key(plan),
                                    prescription.id(),
                                )
                            else development.linkStrength(session.id(), plannedDate)
                        }
                    }
                }
            )
        }

    fun deleteStrength(id: String) =
        operations.submit("Strength workout removed; PB history has been recalculated.") {
            coaching.transaction(
                object : CoachingTransaction {
                    override fun run() {
                        coaching.deleteStrength(id)
                        development.deleteStrengthLink(id)
                    }
                }
            )
        }

    fun saveExercise(exercise: StrengthExercise) =
        operations.submit(
            "Exercise saved. Recorded sessions retain their original exercise definition."
        ) {
            coaching.saveExercise(exercise)
        }

    fun saveHistorical(record: HistoricalStrengthRecord, done: () -> Unit = {}) =
        operations.submit(onSuccess = done) {
            val date = record.date()
            require(date == null || date.toEpochDay() <= today().toEpochDay()) {
                "A historical PB cannot be in the future."
            }
            development.saveHistorical(record)
        }

    fun deleteHistorical(id: String) = operations.submit { development.deleteHistorical(id) }

    private fun key(plan: GymProgramme) = "${plan.id()}:v${plan.version()}"
}
