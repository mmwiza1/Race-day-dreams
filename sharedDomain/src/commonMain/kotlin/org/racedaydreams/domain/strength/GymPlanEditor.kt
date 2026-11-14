package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.*

/** Runner-requested edits create a new immutable version, leaving regular availability intact. */
object GymPlanEditor {
    @JvmStatic
    fun move(
        original: GymProgramme,
        sessionId: String?,
        to: LocalDate?,
        today: LocalDate,
        now: Instant?,
        completed: Set<LocalDate>,
    ): GymProgramme {
        require(
            to != null &&
                to.toEpochDay() >= today.toEpochDay() &&
                to.toEpochDay() >= original.start().toEpochDay() &&
                to.toEpochDay() <= original.end().toEpochDay()
        ) {
            "Choose a future date inside this block."
        }
        require(original.sessions().none { it.id() != sessionId && it.date() == to }) {
            "Another strength session already uses this date."
        }
        return edit(
            original,
            sessionId,
            today,
            now,
            completed,
            "Runner moved one strength session; recurring days unchanged",
        ) {
            it.copy(date = to)
        }
    }

    @JvmStatic
    fun skip(
        p: GymProgramme,
        id: String?,
        today: LocalDate,
        now: Instant?,
        completed: Set<LocalDate>,
    ): GymProgramme =
        edit(
            p,
            id,
            today,
            now,
            completed,
            "Runner skipped one strength session; no catch-up volume",
        ) {
            null
        }

    @JvmStatic
    fun reduce(
        p: GymProgramme,
        id: String?,
        today: LocalDate,
        now: Instant?,
        completed: Set<LocalDate>,
    ): GymProgramme =
        edit(p, id, today, now, completed, "Runner reduced working sets for one session") { s ->
            s.copy(exercises = s.exercises().map { it.copy(sets = maxOf(1, it.sets() - 1)) })
        }

    @JvmStatic
    fun swap(
        p: GymProgramme,
        id: String?,
        exerciseId: String?,
        replacement: String?,
        today: LocalDate,
        now: Instant?,
        completed: Set<LocalDate>,
    ): GymProgramme {
        val from = ExerciseCatalog.definition(exerciseId)
        val to = ExerciseCatalog.definition(replacement)
        require(
            from != null &&
                to != null &&
                from.category() == to.category() &&
                from.exercise().region() == to.exercise().region() &&
                from.exercise().measurement() == to.exercise().measurement() &&
                GymProgrammeGenerator.available(replacement, p.settings().equipment())
        ) {
            "Choose an available exercise with matching region and measurement."
        }
        return edit(p, id, today, now, completed, "Runner chose an exercise substitution") { s ->
            var found = false
            val exercises =
                s.exercises().map { e ->
                    if (e.exerciseId() == exerciseId) {
                        found = true
                        e.copy(
                            exerciseId = replacement,
                            intent = "Runner chose this alternative. " + e.intent(),
                            referenceLoadKg = null,
                        )
                    } else e
                }
            require(found) { "Exercise is not in this session." }
            s.copy(exercises = exercises)
        }
    }

    @JvmStatic
    fun loadReference(
        p: GymProgramme,
        sessionId: String?,
        exerciseId: String?,
        kilograms: Double,
        today: LocalDate,
        now: Instant?,
        completed: Set<LocalDate>,
    ): GymProgramme =
        edit(
            p,
            sessionId,
            today,
            now,
            completed,
            "Runner accepted an evidence-based load reference",
        ) { s ->
            var found = false
            val exercises =
                s.exercises().map { e ->
                    if (e.exerciseId() == exerciseId) {
                        found = true
                        val definition = ExerciseCatalog.definition(exerciseId)
                        val reference = e.referenceLoadKg()
                        require(
                            definition != null &&
                                definition.exercise().measurement() ==
                                    StrengthExercise.MeasurementProfile.LOAD_REPS &&
                                !(reference != null && kilograms <= reference)
                        ) {
                            "Review a new, suitable load reference."
                        }
                        e.copy(referenceLoadKg = kilograms)
                    } else e
                }
            require(found) { "Exercise not prescribed." }
            s.copy(exercises = exercises)
        }

    private fun edit(
        p: GymProgramme,
        id: String?,
        today: LocalDate,
        now: Instant?,
        completed: Set<LocalDate>,
        reason: String,
        change: (GymProgramme.Session) -> GymProgramme.Session?,
    ): GymProgramme {
        var found = false
        val sessions = mutableListOf<GymProgramme.Session>()
        for (s in p.sessions()) {
            if (s.id() == id) {
                found = true
                require(s.date().toEpochDay() >= today.toEpochDay() && s.date() !in completed) {
                    "Completed or historical prescriptions cannot be rewritten."
                }
                change(s)?.let { sessions.add(it) }
            } else sessions.add(s)
        }
        require(found) { "Session not found." }
        return GymProgramme(
            p.id(),
            p.version() + 1,
            now,
            reason,
            p.settings(),
            p.start(),
            p.end(),
            sessions,
            p.tradeoffs(),
        )
    }
}
