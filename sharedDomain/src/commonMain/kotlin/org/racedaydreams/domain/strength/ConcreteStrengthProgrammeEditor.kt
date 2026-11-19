package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.Instant

/** Edits an unaccepted concrete preview; every other dated prescription is retained exactly. */
object ConcreteStrengthProgrammeEditor {
    @JvmStatic
    fun replaceWorkout(
        draft: ConcreteStrengthProgrammeDraft,
        replacement: StrengthProgramme.Workout,
    ): ConcreteStrengthProgrammeDraft {
        val old = draft.programme()
        var found = false
        val blocks =
            old.blocks().map { block ->
                val rows =
                    block.workouts().map { workout ->
                        if (workout.id() == replacement.id()) {
                            require(workout.date() == replacement.date()) {
                                "Review schedule changes through regeneration."
                            }
                            found = true
                            replacement
                        } else workout
                    }
                block.copy(workouts = rows)
            }
        require(found) { "This workout is no longer in the preview." }
        return draft.withProgramme(old.copy(blocks = blocks), draft.substitutions())
    }

    /** A retained accepted intent, including a replacement already chosen in this draft. */
    @JvmStatic
    fun retainedIntent(
        draft: ConcreteStrengthProgrammeDraft,
        base: StrengthProgramme,
        workoutId: String?,
        slotId: String?,
    ): Boolean {
        val previous = findSlot(base, workoutId, slotId) ?: return false
        val current = findSlot(draft.programme(), workoutId, slotId) ?: return false
        return previous.exercise().id() == current.exercise().id() ||
            draft.substitutions().any {
                it.workoutId() == workoutId &&
                    it.slotId() == slotId &&
                    it.fromExerciseId() == previous.exercise().id() &&
                    it.toExerciseId() == current.exercise().id()
            }
    }

    /** One athlete choice updates one draft; changed tasks require their own dated targets. */
    @JvmStatic
    fun substitute(
        draft: ConcreteStrengthProgrammeDraft,
        base: StrengthProgramme,
        workoutId: String?,
        slotId: String?,
        context: ExerciseSubstitutionEngine.Context,
        replacementId: String?,
        changedTarget: GymWorkoutDefinition.Target?,
        protectedIds: Set<String>,
        allFutureOccurrences: Boolean,
        at: Instant?,
    ): ConcreteStrengthProgrammeDraft {
        draft.validateAgainst(base)
        val programme = draft.programme()
        val selected =
            programme.workouts().firstOrNull { it.id() == workoutId }
                ?: throw IllegalArgumentException("Choose a dated workout.")
        val selectedSlot = findSlot(programme, workoutId, slotId)
        require(
            selectedSlot != null &&
                selectedSlot.exercise() == context.original() &&
                retainedIntent(draft, base, workoutId, slotId) &&
                selected.date().toEpochDay() >= programme.effectiveFrom().toEpochDay() &&
                workoutId !in protectedIds &&
                context.goal() == programme.settings().goal() &&
                context.experience() == programme.settings().experience() &&
                context.availableEquipment() == programme.settings().equipment()
        ) {
            "Review a retained, uncompleted future exercise using this programme's context."
        }
        val suggestion =
            ExerciseSubstitutionEngine.rank(context, 50).firstOrNull {
                it.exercise().id() == replacementId
            } ?: throw IllegalArgumentException("Replacement is unavailable or excluded.")
        require(
            !suggestion.measurementChanged() || changedTarget != null && !allFutureOccurrences
        ) {
            "Review a fresh measurement target for each dated workout."
        }
        val changes = draft.substitutions().toMutableList()
        var count = 0
        val blocks =
            programme.blocks().map { block ->
                val workouts =
                    block.workouts().map { workout ->
                        val slots = workout.definition().slots().toMutableList()
                        var changed = false
                        if (
                            workout.date().toEpochDay() >= programme.effectiveFrom().toEpochDay() &&
                                workout.id() !in protectedIds
                        ) {
                            for (index in slots.indices) {
                                val slot = slots[index]
                                if (
                                    !(workout.id() == workoutId && slot.id() == slotId) &&
                                        !(allFutureOccurrences &&
                                            slot.exercise().id() == context.original().id() &&
                                            retainedIntent(draft, base, workout.id(), slot.id()))
                                )
                                    continue
                                val previous = findSlot(base, workout.id(), slot.id())!!
                                // A chain of pending swaps must still retain the accepted base's
                                // intent.
                                val baseContext =
                                    ExerciseSubstitutionEngine.Context(
                                        previous.exercise(),
                                        context.goal(),
                                        context.experience(),
                                        context.availableEquipment(),
                                        context.excludedIds(),
                                        context.favouriteIds(),
                                        context.historyIds(),
                                        ExerciseSubstitutionEngine.Reason.ALTERNATIVES,
                                    )
                                require(
                                    ExerciseSubstitutionEngine.rank(baseContext, 50).any {
                                        it.exercise().id() == replacementId
                                    } || previous.exercise().id() == replacementId
                                ) {
                                    "The alternative must retain the original programme intent."
                                }
                                val target =
                                    (if (suggestion.measurementChanged()) changedTarget
                                    else slot.target())
                                        ?: throw IllegalArgumentException(
                                            "Review a target before swapping."
                                        )
                                slots[index] =
                                    GymWorkoutDefinition.Slot(
                                        slot.id(),
                                        suggestion.exercise(),
                                        target.withoutLoadReference(),
                                        null,
                                    )
                                changes.removeAll {
                                    it.workoutId() == workout.id() && it.slotId() == slot.id()
                                }
                                if (previous.exercise().id() != replacementId) {
                                    changes.add(
                                        StrengthProgrammeEditor.Change(
                                            workout.id(),
                                            slot.id(),
                                            previous.exercise().id(),
                                            replacementId,
                                            "Athlete chose a programme substitution. ${suggestion.explanation()}",
                                        )
                                    )
                                }
                                changed = true
                                count++
                            }
                        }
                        if (changed) {
                            val definition = workout.definition()
                            StrengthProgramme.Workout(
                                workout.id(),
                                workout.date(),
                                StrengthWorkoutBudget.workoutMinutes(slots),
                                GymWorkoutDefinition(
                                    definition.id(),
                                    nextConcreteRevision(definition.revision()),
                                    at,
                                    definition.title(),
                                    slots,
                                ),
                                workout.notes(),
                            )
                        } else workout
                    }
                block.copy(workouts = workouts)
            }
        require(count != 0) { "No uncompleted future occurrence remains." }
        val proposed =
            programme.copy(
                createdAt = at,
                blocks = blocks,
                reason =
                    "Athlete chose ${suggestion.exercise().name()} for $count future prescription(s). ${suggestion.explanation()}",
            )
        StrengthProgrammeEditor.Proposal(base, proposed, protectedIds, changes)
        return draft.withProgramme(proposed, changes)
    }

    private fun findSlot(
        programme: StrengthProgramme,
        workoutId: String?,
        slotId: String?,
    ): GymWorkoutDefinition.Slot? =
        programme
            .workouts()
            .firstOrNull { it.id() == workoutId }
            ?.definition()
            ?.slots()
            ?.firstOrNull { it.id() == slotId }
}
