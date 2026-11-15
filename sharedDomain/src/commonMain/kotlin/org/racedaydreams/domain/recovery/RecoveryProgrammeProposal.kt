package org.racedaydreams.domain.recovery

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Existing preview-only future changes. Reviewed activation stays with the original repository. */
object RecoveryProgrammeProposal {
    @JvmStatic
    fun lighterWeek(
        previous: StrengthProgramme,
        review: TrainingRecovery.Review,
        now: Instant,
        offset: ZoneOffset,
        protectedIds: Set<String>,
    ): StrengthProgrammeEditor.Proposal {
        val today = CalendarDates.atOffset(now, offset)
        require(review.date()!! == today && review.lighterWeek()) {
            "Refresh supported recovery evidence before creating a lighter-week preview."
        }
        var changed = 0
        val blocks = mutableListOf<StrengthProgramme.Block>()
        for (block in previous.blocks()) {
            val workouts = mutableListOf<StrengthProgramme.Workout>()
            for (w in block.workouts()) {
                if (
                    w.date().toEpochDay() < today.toEpochDay() ||
                        w.date().toEpochDay() < previous.effectiveFrom().toEpochDay() ||
                        w.date().toEpochDay() > today.plusDays(6).toEpochDay() ||
                        w.id() in protectedIds
                ) {
                    workouts.add(w)
                    continue
                }
                val slots = mutableListOf<GymWorkoutDefinition.Slot>()
                var different = false
                for (slot in w.definition().slots()) {
                    var t = slot.target()
                    if (t != null && t.sets() > 1) {
                        t =
                            GymWorkoutDefinition.Target(
                                t.sets() - 1,
                                t.minimumReps(),
                                t.maximumReps(),
                                t.seconds(),
                                t.metres(),
                                t.effort(),
                                t.restSeconds(),
                                t.referenceLoadKg(),
                            )
                        different = true
                    }
                    slots.add(
                        GymWorkoutDefinition.Slot(
                            slot.id(),
                            slot.exercise(),
                            t,
                            slot.supersetGroup(),
                        )
                    )
                }
                if (!different) {
                    workouts.add(w)
                    continue
                }
                changed++
                workouts.add(
                    StrengthProgramme.Workout(
                        w.id(),
                        w.date(),
                        StrengthWorkoutBudget.workoutMinutes(slots),
                        GymWorkoutDefinition(
                            w.definition().id(),
                            nextConcreteRevision(w.definition().revision()),
                            now,
                            w.definition().title(),
                            slots,
                        ),
                        w.notes(),
                    )
                )
            }
            blocks.add(copy(block, workouts))
        }
        require(changed != 0) {
            "No eligible future working-set targets can be reduced. Keep your programme or review it manually."
        }
        return proposal(
            previous,
            blocks,
            today,
            now,
            protectedIds,
            "Athlete requested a lighter-week preview after repeated demanding recorded strength sessions. One fewer working set per targeted exercise for the next seven days, minimum one; loads, effort, tasks and later weeks are unchanged. Activation requires a separate review.",
        )
    }

    @JvmStatic
    fun alternatives(
        p: StrengthProgramme,
        workoutId: String,
        running: List<PlannedWorkout>,
        today: LocalDate,
        protectedIds: Set<String>,
    ): List<LocalDate> {
        val w = find(p, workoutId)
        if (
            w.date().toEpochDay() < today.toEpochDay() ||
                w.date().toEpochDay() < p.effectiveFrom().toEpochDay() ||
                workoutId in protectedIds
        )
            return emptyList()
        val block = p.blocks().first { b -> b.workouts().any { it.id() == workoutId } }
        val week = (w.date().toEpochDay() - block.start().toEpochDay()) / 7
        val first = block.start().plusWeeks(week)
        val occupied = p.workouts().filter { it.id() != workoutId }.map { it.date() }.toSet()
        val old = penalty(w.date(), running, today)
        val choices = mutableListOf<LocalDate>()
        for (i in 0..6) {
            val date = first.plusDays(i.toLong())
            if (
                date.toEpochDay() < today.toEpochDay() ||
                    date.toEpochDay() < p.effectiveFrom().toEpochDay() ||
                    date == w.date() ||
                    date in occupied ||
                    date.getDayOfWeek() !in p.settings().days()
            )
                continue
            if (penalty(date, running, today) < old) choices.add(date)
        }
        choices.sortWith(
            compareBy<LocalDate> { penalty(it, running, today) }.thenBy { it.toEpochDay() }
        )
        return Immutable.copyList(choices)
    }

    @JvmStatic
    fun move(
        previous: StrengthProgramme,
        workoutId: String,
        destination: LocalDate,
        running: List<PlannedWorkout>,
        today: LocalDate,
        now: Instant,
        protectedIds: Set<String>,
    ): StrengthProgrammeEditor.Proposal {
        require(destination in alternatives(previous, workoutId, running, today, protectedIds)) {
            "That date is no longer an eligible lower-conflict option. Refresh the schedule."
        }
        val blocks =
            previous.blocks().map { block ->
                copy(
                    block,
                    block
                        .workouts()
                        .map { w ->
                            if (w.id() == workoutId)
                                StrengthProgramme.Workout(
                                    w.id(),
                                    destination,
                                    w.expectedMinutes(),
                                    w.definition(),
                                    w.notes(),
                                )
                            else w
                        }
                        .sortedBy { it.date().toEpochDay() },
                )
            }
        return proposal(
            previous,
            blocks,
            today,
            now,
            protectedIds,
            "Athlete requested a lower-conflict gym date for $workoutId on $destination. Workout targets and running plans are unchanged; review and activate separately.",
        )
    }

    private fun penalty(date: LocalDate, running: List<PlannedWorkout>, today: LocalDate): Int {
        var total = 0
        for (run in running) if (
            TrainingRecovery.demanding(run) && run.date().toEpochDay() >= today.toEpochDay()
        ) {
            val gap = run.date().toEpochDay() - date.toEpochDay()
            total +=
                when {
                    gap == 0L -> 4
                    gap == 1L -> 3
                    gap == -1L -> 1
                    run.category() == WorkoutCategory.RACE && gap in 2L..6L -> 2
                    else -> 0
                }
        }
        return total
    }

    private fun find(p: StrengthProgramme, id: String): StrengthProgramme.Workout =
        p.workouts().firstOrNull { it.id() == id }
            ?: throw IllegalArgumentException("Programme workout is unavailable.")

    private fun copy(
        b: StrengthProgramme.Block,
        workouts: List<StrengthProgramme.Workout>,
    ): StrengthProgramme.Block =
        StrengthProgramme.Block(
            b.id(),
            b.phase(),
            b.name(),
            b.start(),
            b.weeks(),
            workouts,
            b.explanation(),
        )

    private fun proposal(
        old: StrengthProgramme,
        blocks: List<StrengthProgramme.Block>,
        today: LocalDate,
        now: Instant,
        protectedIds: Set<String>,
        reason: String,
    ): StrengthProgrammeEditor.Proposal {
        val effective = listOf(today, old.effectiveFrom(), old.start()).maxBy { it.toEpochDay() }
        require(
            effective.toEpochDay() <= old.end().toEpochDay() && !now.isBefore(old.createdAt())
        ) {
            "Review the programme dates and device clock before creating a proposal."
        }
        val next =
            StrengthProgramme(
                old.id(),
                nextConcreteRevision(old.revision()),
                old.revision(),
                now,
                old.name(),
                old.origin(),
                old.templateId(),
                old.settings(),
                blocks,
                effective,
                reason,
                old.tradeoffs(),
            )
        return StrengthProgrammeEditor.Proposal(old, next, protectedIds, emptyList())
    }
}
