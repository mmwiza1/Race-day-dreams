package org.racedaydreams.coach.application

import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.ExerciseSearch
import org.racedaydreams.domain.strength.GymPlanEditor
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.GymProgrammeGenerator
import org.racedaydreams.domain.strength.StrengthAnalytics
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/**
 * Side-effect-free application operations for gym previews. The domain owns programme
 * generation/editing rules; accepting a preview is a separate persistence command.
 */
class GymProgrammeOperations(
    private val clock: Clock? = null,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun generate(
        current: GymProgramme?,
        settings: GymProgramme.Settings,
        running: List<PlannedWorkout>,
        actualStrength: List<StrengthSession>,
        favourites: Set<String>,
    ): GymProgramme {
        val actionClock = clockForAction()
        return GymProgrammeGenerator().generate(
            current?.id() ?: newId(), (current?.version() ?: 0) + 1, actionClock.instant(),
            "Runner chose gym preferences and approved the new block", settings,
            LocalDate.now(actionClock), running, actualStrength, favourites,
        )
    }

    fun move(plan: GymProgramme, sessionId: String, date: LocalDate, completed: Set<LocalDate>): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.move(plan, sessionId, date, LocalDate.now(actionClock), actionClock.instant(), completed)
    }

    fun reduce(plan: GymProgramme, sessionId: String, completed: Set<LocalDate>): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.reduce(plan, sessionId, LocalDate.now(actionClock), actionClock.instant(), completed)
    }

    fun skip(plan: GymProgramme, sessionId: String, completed: Set<LocalDate>): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.skip(plan, sessionId, LocalDate.now(actionClock), actionClock.instant(), completed)
    }

    fun swap(plan: GymProgramme, sessionId: String, from: String, to: String, completed: Set<LocalDate>): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.swap(plan, sessionId, from, to, LocalDate.now(actionClock), actionClock.instant(), completed)
    }

    // A retained coordinator must observe a device timezone change on the next action.
    // Explicit test clocks retain their supplied instant/zone without consulting the device.
    private fun clockForAction(): Clock = clock ?: Clock.systemDefaultZone()

    fun alternatives(plan: GymProgramme, from: String, query: String, exercises: List<StrengthExercise>): List<StrengthExercise> {
        val definition = ExerciseCatalog.definition(from)
        return ExerciseSearch.search(exercises, query, emptySet(), emptyList(), emptySet()).filter { candidate ->
            val alternative = ExerciseCatalog.definition(candidate.id())
            candidate.id() != from && alternative != null &&
                alternative.category() == definition?.category() &&
                candidate.measurement() == definition?.exercise()?.measurement() &&
                GymProgrammeGenerator.available(candidate.id(), plan.settings().equipment())
        }
    }

    fun conflicts(session: GymProgramme.Session, running: List<PlannedWorkout>, actualStrength: List<StrengthSession>): List<String> =
        GymProgrammeGenerator.conflicts(
            session.date(),
            session.exercises().any { ExerciseCatalog.definition(it.exerciseId())?.exercise()?.region() != StrengthExercise.Region.UPPER },
            running, StrengthAnalytics.hardLowerBodyDates(actualStrength),
        )
}
