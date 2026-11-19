package org.racedaydreams.coach.application

import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.ExerciseSearch
import org.racedaydreams.domain.strength.GymPlanEditor
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.GymProgrammeGenerator
import org.racedaydreams.domain.strength.StrengthAnalytics
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/**
 * Side-effect-free application operations for gym previews. The domain owns programme
 * generation/editing rules; accepting a preview is a separate persistence command.
 */
class SharedGymProgrammeOperations(
    private val clockForAction: () -> GymProgrammeActionClock,
    private val newId: () -> String,
) {
    fun generate(
        current: GymProgramme?,
        settings: GymProgramme.Settings,
        running: List<PlannedWorkout>,
        actualStrength: List<StrengthSession>,
        favourites: Set<String>,
        excluded: Set<String> = emptySet(),
    ): GymProgramme {
        val actionClock = clockForAction()
        return GymProgrammeGenerator()
            .generate(
                current?.id() ?: newId(),
                (current?.version() ?: 0) + 1,
                actionClock.instant(),
                "Runner chose gym preferences and approved the new block",
                settings,
                actionClock.today(),
                running,
                actualStrength,
                favourites,
                excluded,
            )
    }

    fun move(
        plan: GymProgramme,
        sessionId: String,
        date: LocalDate,
        completed: Set<LocalDate>,
    ): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.move(
            plan,
            sessionId,
            date,
            actionClock.today(),
            actionClock.instant(),
            completed,
        )
    }

    fun reduce(plan: GymProgramme, sessionId: String, completed: Set<LocalDate>): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.reduce(
            plan,
            sessionId,
            actionClock.today(),
            actionClock.instant(),
            completed,
        )
    }

    fun skip(plan: GymProgramme, sessionId: String, completed: Set<LocalDate>): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.skip(
            plan,
            sessionId,
            actionClock.today(),
            actionClock.instant(),
            completed,
        )
    }

    fun swap(
        plan: GymProgramme,
        sessionId: String,
        from: String,
        to: String,
        completed: Set<LocalDate>,
    ): GymProgramme {
        val actionClock = clockForAction()
        return GymPlanEditor.swap(
            plan,
            sessionId,
            from,
            to,
            actionClock.today(),
            actionClock.instant(),
            completed,
        )
    }

    fun alternatives(
        plan: GymProgramme,
        from: String,
        query: String,
        exercises: List<StrengthExercise>,
    ): List<StrengthExercise> {
        val definition = ExerciseCatalog.definition(from)
        return ExerciseSearch.search(exercises, query, emptySet(), emptyList(), emptySet())
            .filter { candidate ->
                val alternative = ExerciseCatalog.definition(candidate.id())
                candidate.id() != from &&
                    alternative != null &&
                    alternative.category() == definition?.category() &&
                    candidate.measurement() == definition?.exercise()?.measurement() &&
                    GymProgrammeGenerator.available(candidate.id(), plan.settings().equipment())
            }
    }

    fun conflicts(
        session: GymProgramme.Session,
        running: List<PlannedWorkout>,
        actualStrength: List<StrengthSession>,
    ): List<String> =
        GymProgrammeGenerator.conflicts(
            session.date(),
            GymProgrammeGenerator.hasLowerBodyWork(session.exercises()),
            running,
            StrengthAnalytics.hardLowerBodyDates(actualStrength),
        )
}

/** One action uses one platform clock/zone; a later action must observe timezone changes. */
interface GymProgrammeActionClock {
    fun instant(): Instant

    fun today(): LocalDate
}
