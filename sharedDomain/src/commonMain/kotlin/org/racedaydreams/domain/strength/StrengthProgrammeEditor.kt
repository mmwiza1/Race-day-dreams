package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.javaUtf8
import org.racedaydreams.domain.time.*

/** Future-only explicit proposals. Activation/persistence remains a separate athlete command. */
object StrengthProgrammeEditor {
    @JvmStatic
    fun replaceFuture(
        previous: StrengthProgramme,
        candidate: StrengthProgramme,
        effectiveFrom: LocalDate,
        at: Instant?,
        completed: Set<String>,
        reason: String?,
    ): Proposal {
        require(candidate.id() == previous.id() && candidate.start() == previous.start()) {
            "Regeneration must retain programme identity and start."
        }
        val retained = mutableListOf<StrengthProgramme.Workout>()
        val occupied = mutableSetOf<LocalDate>()
        val old = mutableMapOf<String, StrengthProgramme.Workout>()
        for (workout in previous.workouts()) {
            old[workout.id()] = workout
            if (before(workout.date(), effectiveFrom) || workout.id() in completed) {
                require(
                    !before(workout.date(), candidate.start()) &&
                        !before(candidate.end(), workout.date())
                ) {
                    "New duration would discard retained prescription history."
                }
                retained.add(workout)
                occupied.add(workout.date())
            }
        }
        val protectedIds = retained.map { it.id() }.toSet()
        val reservedIds = protectedIds.toMutableSet()
        reservedIds.addAll(candidate.workouts().map { it.id() })
        val blocks = mutableListOf<StrengthProgramme.Block>()
        for (block in candidate.blocks()) {
            val sessions = mutableListOf<StrengthProgramme.Workout>()
            for (workout in block.workouts()) {
                if (before(workout.date(), effectiveFrom) || workout.date() in occupied) continue
                val existing = old[workout.id()]
                var workoutId = workout.id()
                val newIdentity = workoutId in protectedIds
                if (newIdentity) {
                    var salt = 0
                    do {
                        val seed =
                            "${previous.id()}\n${previous.revision()}\n${workout.id()}\n${workout.date()}\n${salt++}"
                        workoutId = "regenerated-${nameUuid(seed)}"
                    } while (!reservedIds.add(workoutId))
                }
                val d = workout.definition()
                sessions.add(
                    StrengthProgramme.Workout(
                        workoutId,
                        workout.date(),
                        workout.expectedMinutes(),
                        GymWorkoutDefinition(
                            if (newIdentity) workoutId else d.id(),
                            if (newIdentity || existing == null) 1
                            else existing.definition().revision() + 1,
                            at,
                            d.title(),
                            d.slots(),
                        ),
                        workout.notes(),
                    )
                )
            }
            for (workout in retained) if (
                !before(workout.date(), block.start()) && !before(block.end(), workout.date())
            )
                sessions.add(workout)
            sessions.sortBy { it.date().toEpochDay() }
            blocks.add(
                StrengthProgramme.Block(
                    block.id(),
                    block.phase(),
                    block.name(),
                    block.start(),
                    block.weeks(),
                    sessions,
                    block.explanation(),
                )
            )
        }
        val tradeoffs = candidate.tradeoffs().toMutableList()
        if (retained.isNotEmpty())
            tradeoffs.add(
                "Historical and completed sessions retain their exact prior prescriptions, even when availability changes."
            )
        val next =
            StrengthProgramme(
                previous.id(),
                previous.revision() + 1,
                previous.revision(),
                at,
                candidate.name(),
                candidate.origin(),
                candidate.templateId(),
                candidate.settings(),
                blocks,
                effectiveFrom,
                reason,
                tradeoffs,
            )
        return Proposal(previous, next, completed, emptyList())
    }

    data class Change(
        private val workoutId: String?,
        private val slotId: String?,
        private val fromExerciseId: String?,
        private val toExerciseId: String?,
        private val reason: String?,
    ) {
        init {
            GymWorkoutDefinition.requireId(workoutId, "workout")
            GymWorkoutDefinition.requireId(slotId, "slot")
            GymWorkoutDefinition.requireId(fromExerciseId, "original exercise")
            GymWorkoutDefinition.requireId(toExerciseId, "replacement exercise")
            StrengthProgramme.text(reason, 2000, false, "substitution reason")
            require(fromExerciseId != toExerciseId) { "Choose a different exercise." }
        }

        fun workoutId(): String = workoutId!!

        fun slotId(): String = slotId!!

        fun fromExerciseId(): String = fromExerciseId!!

        fun toExerciseId(): String = toExerciseId!!

        fun reason(): String = reason!!

        override fun toString(): String =
            "Change[workoutId=$workoutId, slotId=$slotId, fromExerciseId=$fromExerciseId, toExerciseId=$toExerciseId, reason=$reason]"
    }

    data class Proposal(
        private val previous: StrengthProgramme?,
        private val proposed: StrengthProgramme?,
        private var completedWorkoutIds: Set<String>?,
        private var substitutions: List<Change>?,
    ) {
        init {
            if (previous == null) throw NullPointerException()
            if (proposed == null) throw NullPointerException()
            completedWorkoutIds =
                Immutable.copySet(completedWorkoutIds ?: throw NullPointerException())
            substitutions = Immutable.copyList(substitutions ?: throw NullPointerException())
            require(
                previous.id() == proposed.id() &&
                    proposed.revision() == previous.revision() + 1 &&
                    !proposed.createdAt().isBefore(previous.createdAt()) &&
                    !before(proposed.effectiveFrom(), previous.effectiveFrom())
            ) {
                "Review a forward revision of this programme."
            }
            val after = proposed.workouts().associateBy { it.id() }
            val before = mutableMapOf<String, StrengthProgramme.Workout>()
            for (w in previous.workouts()) {
                before[w.id()] = w
                require(
                    !(before(w.date(), proposed.effectiveFrom()) ||
                        w.id() in completedWorkoutIds!!) || w == after[w.id()]
                ) {
                    "Historical and completed prescriptions cannot be rewritten."
                }
            }
            require(before.keys.containsAll(completedWorkoutIds!!)) {
                "Completed evidence references an unknown session."
            }
            for (workout in proposed.workouts()) {
                require(
                    before(workout.date(), proposed.effectiveFrom()) ||
                        workout.id() in completedWorkoutIds!! ||
                        workout.date().getDayOfWeek() in proposed.settings().days()
                ) {
                    "Future programming must respect available days."
                }
                require(
                    !before(workout.date(), proposed.effectiveFrom()) ||
                        workout == before[workout.id()]
                ) {
                    "New backdated prescriptions are not permitted."
                }
            }
            for (change in substitutions!!) {
                val old = before[change.workoutId()]
                val updated = after[change.workoutId()]
                require(old != null && updated != null) { "Substitution session is missing." }
                val from = slot(old, change.slotId())
                val to = slot(updated, change.slotId())
                require(
                    from.exercise().id() == change.fromExerciseId() &&
                        to.exercise().id() == change.toExerciseId() &&
                        to.target()?.referenceLoadKg() == null
                ) {
                    "Substitution provenance or cleared load reference is invalid."
                }
            }
        }

        fun previous(): StrengthProgramme = previous!!

        fun proposed(): StrengthProgramme = proposed!!

        fun completedWorkoutIds(): Set<String> = completedWorkoutIds!!

        fun substitutions(): List<Change> = substitutions!!

        override fun toString(): String =
            "Proposal[previous=$previous, proposed=$proposed, completedWorkoutIds=$completedWorkoutIds, substitutions=$substitutions]"
    }

    @JvmStatic
    fun substitute(
        programme: StrengthProgramme,
        workoutId: String?,
        slotId: String?,
        context: ExerciseSubstitutionEngine.Context,
        replacementId: String?,
        changedMeasurementTarget: GymWorkoutDefinition.Target?,
        effectiveFrom: LocalDate?,
        at: Instant?,
        completed: Set<String>?,
    ): Proposal {
        val original =
            programme.workouts().firstOrNull { it.id() == workoutId }
                ?: throw IllegalArgumentException("Session not found.")
        val from = slot(original, slotId)
        require(
            from.exercise() == context.original() &&
                context.goal() == programme.settings().goal() &&
                context.experience() == programme.settings().experience() &&
                context.availableEquipment() == programme.settings().equipment()
        ) {
            "Review alternatives using the programme's current context."
        }
        val suggestion =
            ExerciseSubstitutionEngine.rank(context, 50).firstOrNull {
                it.exercise().id() == replacementId
            } ?: throw IllegalArgumentException("Replacement is unavailable or excluded.")
        var target =
            if (suggestion.measurementChanged()) changedMeasurementTarget else from.target()
        require(!suggestion.measurementChanged() || target != null) {
            "Review a target for the new measurement."
        }
        target = target?.withoutLoadReference()
        val slots = original.definition().slots().toMutableList()
        slots[slots.indexOf(from)] =
            GymWorkoutDefinition.Slot(slotId, suggestion.exercise(), target, null)
        val changed =
            StrengthProgramme.Workout(
                original.id(),
                original.date(),
                StrengthWorkoutBudget.workoutMinutes(slots),
                GymWorkoutDefinition(
                    original.definition().id(),
                    original.definition().revision() + 1,
                    at,
                    original.definition().title(),
                    slots,
                ),
                original.notes(),
            )
        val blocks =
            programme.blocks().map { block ->
                StrengthProgramme.Block(
                    block.id(),
                    block.phase(),
                    block.name(),
                    block.start(),
                    block.weeks(),
                    block.workouts().map { if (it.id() == workoutId) changed else it },
                    block.explanation(),
                )
            }
        val reason = "Athlete chose a permanent programme substitution. ${suggestion.explanation()}"
        val next =
            StrengthProgramme(
                programme.id(),
                programme.revision() + 1,
                programme.revision(),
                at,
                programme.name(),
                programme.origin(),
                programme.templateId(),
                programme.settings(),
                blocks,
                effectiveFrom,
                reason,
                programme.tradeoffs(),
            )
        return Proposal(
            programme,
            next,
            completed,
            Immutable.list(Change(workoutId, slotId, from.exercise().id(), replacementId, reason)),
        )
    }

    private fun slot(w: StrengthProgramme.Workout, id: String?): GymWorkoutDefinition.Slot =
        w.definition().slots().firstOrNull { it.id() == id }
            ?: throw IllegalArgumentException("Preserved exercise slot is missing.")

    private fun before(left: LocalDate, right: LocalDate): Boolean =
        left.toEpochDay() < right.toEpochDay()

    // Keep Java nameUUIDFromBytes v3/variant bits and its original UTF-8 replacement semantics.
    private fun nameUuid(seed: String): String {
        val bytes = programmeRevisionDigest(javaUtf8(seed))
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x30).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val digits = "0123456789abcdef"
        return buildString {
            for (i in bytes.indices) {
                if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
                val part = bytes[i].toInt()
                append(digits[(part ushr 4) and 15])
                append(digits[part and 15])
            }
        }
    }
}

internal expect fun programmeRevisionDigest(bytes: ByteArray): ByteArray
