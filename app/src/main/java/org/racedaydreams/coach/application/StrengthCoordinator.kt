package org.racedaydreams.coach.application

import java.time.Instant
import java.time.LocalDate
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.StrengthUserStore
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/** Strength sessions, programme versions and evidence-backed progression commands. */
class StrengthCoordinator(
    private val gym: GymProgrammeStore,
    private val choices: StrengthUserStore,
    private val operations: CoachOperationRunner,
    private val coaching: CoachingRepository,
    private val development: DevelopmentStore,
    private val programmes: GymProgrammeOperations = GymProgrammeOperations(),
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

    fun previewSwap(plan: GymProgramme, sessionId: String, from: String, to: String) =
        programmes.swap(
            plan,
            sessionId,
            from,
            to,
            operations.current.strengthPlannedDates.values.toSet(),
        )

    fun alternatives(plan: GymProgramme, from: String, query: String) =
        programmes.alternatives(plan, from, query, operations.current.exercises)

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
            require(org.racedaydreams.data.GymProgrammeStore.key(p) == base) {
                "The programme changed. Review the current suggestion."
            }
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
                    Instant.now(),
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
                        advice.suggestedKilograms(),
                        LocalDate.now(),
                        Instant.now(),
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

    fun exerciseChoice(id: String, favourite: Boolean, archived: Boolean) =
        operations.submit {
            choices.save(org.racedaydreams.data.StrengthUserStore.Choice(id, favourite, archived))
        }

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
            coaching.transaction {
                coaching.saveStrength(session)
                if (plannedDate != null && session.id() !in development.strengthLinks()) {
                    val gym = gym.current()
                    val prescription = gym?.sessions()?.firstOrNull { it.date() == plannedDate }
                    if (gym != null && prescription != null)
                        development.linkGym(
                            session.id(),
                            plannedDate,
                            org.racedaydreams.data.GymProgrammeStore.key(gym),
                            prescription.id(),
                        )
                    else development.linkStrength(session.id(), plannedDate)
                }
            }
        }

    fun deleteStrength(id: String) =
        operations.submit("Strength workout removed; PB history has been recalculated.") {
            coaching.transaction {
                coaching.deleteStrength(id)
                development.deleteStrengthLink(id)
            }
        }

    fun saveExercise(exercise: StrengthExercise) =
        operations.submit(
            "Exercise saved. Recorded sessions retain their original exercise definition."
        ) {
            coaching.saveExercise(exercise)
        }

    fun saveHistorical(record: HistoricalStrengthRecord, done: () -> Unit = {}) =
        operations.submit(onSuccess = done) {
            require(record.date() == null || record.date() <= LocalDate.now()) {
                "A historical PB cannot be in the future."
            }
            development.saveHistorical(record)
        }

    fun deleteHistorical(id: String) = operations.submit { development.deleteHistorical(id) }
}
