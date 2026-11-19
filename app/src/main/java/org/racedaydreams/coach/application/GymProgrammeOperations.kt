package org.racedaydreams.coach.application

import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/** Android clock/identity binding; the existing application preview operations are common. */
class GymProgrammeOperations(
    private val clock: Clock? = null,
    newId: () -> String = { UUID.randomUUID().toString() },
) {
    internal val shared =
        SharedGymProgrammeOperations(
            clockForAction = {
                // Resolve the device zone per action, not when a retained coordinator is created.
                val actionClock = clock ?: Clock.systemDefaultZone()
                object : GymProgrammeActionClock {
                    override fun instant() = actionClock.instant()

                    override fun today() = LocalDate.now(actionClock)
                }
            },
            newId = newId,
        )

    fun generate(
        current: GymProgramme?,
        settings: GymProgramme.Settings,
        running: List<PlannedWorkout>,
        actualStrength: List<StrengthSession>,
        favourites: Set<String>,
        excluded: Set<String> = emptySet(),
    ) = shared.generate(current, settings, running, actualStrength, favourites, excluded)

    fun move(plan: GymProgramme, sessionId: String, date: LocalDate, completed: Set<LocalDate>) =
        shared.move(plan, sessionId, date, completed)

    fun reduce(plan: GymProgramme, sessionId: String, completed: Set<LocalDate>) =
        shared.reduce(plan, sessionId, completed)

    fun skip(plan: GymProgramme, sessionId: String, completed: Set<LocalDate>) =
        shared.skip(plan, sessionId, completed)

    fun swap(
        plan: GymProgramme,
        sessionId: String,
        from: String,
        to: String,
        completed: Set<LocalDate>,
    ) = shared.swap(plan, sessionId, from, to, completed)

    fun alternatives(
        plan: GymProgramme,
        from: String,
        query: String,
        exercises: List<StrengthExercise>,
    ) = shared.alternatives(plan, from, query, exercises)

    fun conflicts(
        session: GymProgramme.Session,
        running: List<PlannedWorkout>,
        actualStrength: List<StrengthSession>,
    ) = shared.conflicts(session, running, actualStrength)
}
