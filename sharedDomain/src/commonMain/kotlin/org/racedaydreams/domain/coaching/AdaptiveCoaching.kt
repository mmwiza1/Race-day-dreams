package org.racedaydreams.domain.coaching

import kotlin.math.abs
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** One shared proposal engine over raw evidence; Run Score is deliberately not an input. */
object AdaptiveCoaching {
    fun review(
        version: PlanVersion,
        actual: List<ActivityRecord>,
        responses: List<RunExecution>,
        strength: List<StrengthSession>,
        today: LocalDate,
        now: Instant,
    ): AdjustmentProposal? {
        val prescriptions = HashMap<String, PlannedWorkout>()
        version.plan().workouts().forEach { prescriptions[it.id()] = it }
        val completed = mutableSetOf<String>()
        var highEffort = 0
        var lowAndHard = false
        val evidence = mutableListOf<String>()
        val evidenceIds = mutableListOf<String>()
        for (run in actual) {
            val response = responses.firstOrNull { it.activityId() == run.id() }
            val linked =
                if (response == null) run.plannedWorkoutId() else response.plannedWorkoutId()
            if (linked != null) completed.add(linked)
            if (
                run.end()
                    .isBefore(
                        Instants.ofEpochSecond(
                            checkedAdd(now.getEpochSecond(), -10 * 86400L),
                            now.getNano().toLong(),
                        )
                    ) || run.end().isAfter(now)
            )
                continue
            val planned = prescriptions[linked] ?: continue
            val rpe = run.rpe() ?: continue
            val expected =
                if (isQuality(planned.category()))
                    (planned.segments().mapNotNull { it.rpe() }.maxOrNull() ?: 6.0) + 1
                else 4.0
            val poorStepPace =
                response != null &&
                    response.steps().any { s ->
                        val slow = s.slowPace()
                        s.kind() == WorkoutSegment.Kind.RUN &&
                            s.completed() &&
                            s.reliablePace() &&
                            s.metres() > 0 &&
                            slow != null &&
                            s.activeMillis() / s.metres() > slow * 1.10
                    }
            if (rpe > expected || poorStepPace && rpe >= 8) {
                highEffort++
                evidenceIds.add("${run.id()}:$rpe")
            }
            val plannedKm = planned.distanceKm()
            val metres = run.distanceMetres()
            if (
                plannedKm != null &&
                    plannedKm > 0 &&
                    metres != null &&
                    metres < plannedKm * 750 &&
                    rpe >= 8
            ) {
                lowAndHard = true
                evidenceIds.add("${run.id()}:$metres")
            }
        }
        val repeated = highEffort >= 2
        val hardDates = StrengthAnalytics.hardLowerBodyDates(strength)
        val next =
            version.plan().workouts().firstOrNull { w ->
                !coachingBefore(w.date(), today) &&
                    coachingBefore(w.date(), today.plusDays(7)) &&
                    w.id() !in completed &&
                    w.category() != WorkoutCategory.RACE &&
                    isDemanding(w.category()) &&
                    (repeated || lowAndHard || w.date().minusDays(1) in hardDates)
            } ?: return null
        val gym = next.date().minusDays(1) in hardDates
        if (!gym && !repeated && !lowAndHard) return null
        if (repeated)
            evidence.add(
                "At least two recent linked sessions had unusually high reported effort or corroborated difficult structured execution."
            )
        if (lowAndHard)
            evidence.add(
                "A recent linked run combined less than 75% of planned distance with RPE 8 or higher."
            )
        if (gym) {
            evidence.add(
                "Recorded hard lower-body work falls the day before this demanding session."
            )
            strength
                .filter {
                    CalendarDates.atOffset(it.startedAt(), it.offset()) == next.date().minusDays(1)
                }
                .forEach { evidenceIds.add("${it.id()}:$it") }
        }
        val options = mutableListOf<AdjustmentProposal.Option>()
        val easy =
            PlannedWorkout(
                next.id(),
                next.date(),
                WorkoutCategory.EASY_RUN,
                next.distanceKm(),
                next.duration(),
                Immutable.list(
                    WorkoutSegment(
                        WorkoutSegment.Kind.RUN,
                        next.distanceKm(),
                        next.duration(),
                        null,
                        null,
                        null,
                        3.0,
                        "Conversational effort; shorten or take walk breaks if needed.",
                    )
                ),
                "Runner-selected easier alternative. Original prescription remains in plan history.",
            )
        options.add(
            AdjustmentProposal.Option(
                "easy",
                "Run easy instead (recommended)",
                AdjustmentProposal.Action.REPLACE,
                easy,
            )
        )
        if (gym) {
            var day = next.date().plusDays(1)
            while (coachingBefore(day, next.date().plusDays(4))) {
                val candidate = day
                day = day.plusDays(1)
                if (
                    candidate.getDayOfWeek() !in
                        version.goalSnapshot().preferences().runningDays() ||
                        coachingAfter(candidate, version.plan().endDate())
                )
                    continue
                if (
                    version.plan().workouts().any {
                        it.date() == candidate ||
                            isDemanding(it.category()) &&
                                abs(it.date().toEpochDay() - candidate.toEpochDay()) < 2
                    }
                )
                    continue
                if (candidate.minusDays(1) in hardDates) continue
                val monday = coachingMonday(candidate)
                val count =
                    version.plan().workouts().count {
                        it.id() != next.id() &&
                            !coachingBefore(it.date(), monday) &&
                            coachingBefore(it.date(), monday.plusWeeks(1))
                    }
                if (count >= version.goalSnapshot().preferences().runsPerWeek()) continue
                options.add(
                    0,
                    AdjustmentProposal.Option(
                        "move",
                        "Move demanding session to ${formatGymProgrammeDate(candidate)} (recommended)",
                        AdjustmentProposal.Action.REPLACE,
                        PlannedWorkout(
                            next.id(),
                            candidate,
                            next.category(),
                            next.distanceKm(),
                            next.duration(),
                            next.segments(),
                            "Runner accepted a move after recorded lower-body work. ${next.note()}",
                        ),
                    ),
                )
                break
            }
        }
        options.add(
            AdjustmentProposal.Option(
                "keep",
                "Keep original workout",
                AdjustmentProposal.Action.KEEP,
                null,
            )
        )
        options.add(
            AdjustmentProposal.Option("rest", "Rest instead", AdjustmentProposal.Action.REST, null)
        )
        val id = StrengthProgrammeEditor.nameUuid(version.id() + next.id() + evidenceIds)
        return AdjustmentProposal(
            id,
            version.id(),
            next.id(),
            now,
            evidence.joinToString(" ") +
                " Consider reducing or moving one demanding session; no other workouts need changing from this evidence.",
            evidence,
            "Only this workout is affected. No mileage is added elsewhere. Your race goal is unchanged.",
            "Moderate — coaching heuristics, not a medical assessment",
            options,
        )
    }

    fun isQuality(category: WorkoutCategory): Boolean = WorkoutCategories.isQuality(category)

    fun isDemanding(category: WorkoutCategory): Boolean = WorkoutCategories.isDemanding(category)

    fun apply(
        plan: TrainingPlan,
        proposal: AdjustmentProposal,
        option: AdjustmentProposal.Option,
        completed: Set<String>,
    ): TrainingPlan {
        require(option in proposal.options()) { "Choose an option from this proposal." }
        require(proposal.workoutId() !in completed) { "Completed prescriptions cannot be changed." }
        val workouts = mutableListOf<PlannedWorkout>()
        var found = false
        for (w in plan.workouts()) {
            if (w.id() != proposal.workoutId()) workouts.add(w)
            else {
                found = true
                when (option.action()) {
                    AdjustmentProposal.Action.KEEP -> workouts.add(w)
                    AdjustmentProposal.Action.REPLACE ->
                        workouts.add(requireNotNull(option.replacement()))
                    AdjustmentProposal.Action.REST -> Unit
                }
            }
        }
        require(found) { "This proposal no longer matches the current plan." }
        workouts.sortBy { it.date().toEpochDay() }
        return TrainingPlan(plan.id(), plan.startDate(), plan.endDate(), plan.phases(), workouts)
    }
}
