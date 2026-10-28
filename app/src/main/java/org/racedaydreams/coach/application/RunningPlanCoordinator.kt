package org.racedaydreams.coach.application

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.data.Release08Store
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.coaching.AdjustmentProposal
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.coaching.OriginalTrainingPlanGenerator
import org.racedaydreams.domain.coaching.PlanPreservation
import org.racedaydreams.domain.coaching.PlanRequest
import org.racedaydreams.domain.coaching.PlanVersion
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.coaching.WorkoutDecision

/** Version-preserving running programme commands and explicit athlete decisions. */
class RunningPlanCoordinator(
    private val release: Release08Store,
    private val operations: CoachOperationRunner,
    private val activities: ActivityRepository,
    private val profiles: SqliteUserPreferencesRepository,
    private val coaching: CoachingRepository,
    private val development: DevelopmentStore,
) {
    fun generatePlan() =
        operations.submit {
            val profile =
                profiles.load().orElse(null)
                    ?: throw IllegalArgumentException("Save your goal and availability first.")
            val baseline = operations.current.baseline
            val schedule = release.get("config", "schedule")
            val unavailable =
                schedule?.getJSONArray("unavailable")?.let { a ->
                    (0 until a.length()).map { LocalDate.parse(a.getString(it)) }.toSet()
                } ?: emptySet()
            val candidate =
                OriginalTrainingPlanGenerator()
                    .generate(
                        PlanRequest(
                            profile,
                            baseline,
                            LocalDate.now(),
                            unavailable,
                            coaching.strength(),
                            schedule?.getInt("maximumMinutes"),
                            schedule?.getString("distribution") ?: "BALANCED",
                        )
                    )
            val versions = coaching.versions()
            val responses = coaching.responses().associateBy { it.activityId() }
            val oldCurrent = coaching.current()
            val completed =
                responses.values
                    .mapNotNull { response ->
                        versions
                            .firstOrNull { it.id() == response.planVersionId() }
                            ?.plan()
                            ?.workouts()
                            ?.firstOrNull { it.id() == response.plannedWorkoutId() }
                    }
                    .associateBy { it.id() }
                    .toMutableMap()
            // Legacy links without a version retain the current prescription rather than guessing
            // an older one. Restored response links also remain protected while HC awaits
            // re-import.
            activities.activities().forEach { actual ->
                if (responses[actual.id()] == null)
                    oldCurrent
                        ?.plan()
                        ?.workouts()
                        ?.firstOrNull { it.id() == actual.plannedWorkoutId() }
                        ?.let { completed.putIfAbsent(it.id(), it) }
            }
            val generated = PlanPreservation.retainCompleted(candidate, completed)
            val evidence =
                "Baseline window ${baseline.windowStart()} to ${baseline.windowEnd()}; confidence ${baseline.confidence()}; " +
                    "weekly km ${baseline.weeklyDistanceKm() ?: "unknown"}; longest km ${baseline.longestRunKm() ?: "unknown"}; " +
                    "consistent weeks ${baseline.consistentWeeks() ?: "unknown"}; sources ${baseline.provenance()}. " +
                    "History, manual inputs and assessment evidence remain distinct. No race prediction is inferred. Schedule preferences: ${schedule?.let{"${it.getInt("maximumMinutes")} minute usual budget; ${it.getString("distribution").lowercase().replace('_',' ')}; one-off unavailable dates: ${unavailable.sorted().joinToString{d->d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu",java.util.Locale.UK))}}"}?:"defaults"}."
            coaching.saveCurrent(
                PlanVersion(
                    UUID.randomUUID().toString(),
                    generated.id(),
                    (coaching
                        .versions()
                        .filter { it.planId() == generated.id() }
                        .maxOfOrNull { it.version() } ?: 0) + 1,
                    Instant.now(),
                    if (coaching.current() == null) "Runner requested initial programme"
                    else "Runner explicitly requested remaining programme rebuild",
                    profile,
                    evidence,
                    generated,
                )
            )
        }

    fun decide(proposal: AdjustmentProposal, optionId: String) =
        operations.submit("Your decision is saved. Other workouts and your goal are unchanged.") {
            val completed = activities.activities().mapNotNull { it.plannedWorkoutId() }.toSet()
            require(
                !activities.activities().any { it.plannedWorkoutId() == proposal.workoutId() }
            ) {
                "This workout has already been completed; refresh the plan."
            }
            coaching.decide(proposal, optionId, completed)
        }

    fun weeklyDecision(choice: String) =
        operations.submit {
            val review =
                operations.current.weeklyReview
                    ?: throw IllegalArgumentException("No review available.")
            development.decide(
                review,
                choice,
                coaching,
                coaching.responses().mapNotNull { it.plannedWorkoutId() }.toSet(),
            )
        }

    fun reportInterruption(from: LocalDate, returned: LocalDate, reason: String) =
        operations.submit {
            require(returned <= LocalDate.now()) { "Return date cannot be in the future." }
            development.saveInterruption(WeeklyCoaching.Interruption(from, returned, reason))
        }

    fun chooseEasy(workoutId: String, reason: String) =
        operations.submit(
            "Your selected easy alternative was saved. Other sessions are unchanged."
        ) {
            val current = coaching.current() ?: error("No current plan")
            val original = current.plan().workouts().first { it.id() == workoutId }
            require(original.date() >= LocalDate.now()) {
                "Only current or future sessions can be changed."
            }
            require(
                coaching.responses().none { it.plannedWorkoutId() == workoutId } &&
                    activities.activitySummaries().none { it.plannedWorkoutId() == workoutId }
            ) {
                "Completed sessions cannot be changed."
            }
            val easy =
                org.racedaydreams.domain.health.RecoveryCoaching.easyAlternative(original, reason)
            val p = current.plan()
            val updated =
                org.racedaydreams.domain.TrainingPlan(
                    p.id(),
                    p.startDate(),
                    p.endDate(),
                    p.phases(),
                    p.workouts().map { if (it.id() == workoutId) easy else it },
                )
            coaching.saveCurrent(
                PlanVersion(
                    UUID.randomUUID().toString(),
                    current.planId(),
                    current.version() + 1,
                    Instant.now(),
                    "Runner chose easy: $reason",
                    current.goalSnapshot(),
                    current.evidenceSnapshot(),
                    updated,
                )
            )
        }

    fun changeWorkout(workoutId: String, movedTo: LocalDate? = null) =
        operations.submit {
            val current = coaching.current() ?: throw IllegalArgumentException("No current plan.")
            val workout =
                current.plan().workouts().firstOrNull { it.id() == workoutId }
                    ?: throw IllegalArgumentException("Workout not found.")
            require(workout.category() != org.racedaydreams.domain.WorkoutCategory.RACE) {
                "Change your race date through goal setup."
            }
            require(
                coaching.responses().none { it.plannedWorkoutId() == workoutId } &&
                    activities.activities().none { it.plannedWorkoutId() == workoutId }
            ) {
                "Completed sessions cannot be changed."
            }
            if (movedTo != null) {
                require(movedTo >= LocalDate.now() && movedTo <= current.plan().endDate()) {
                    "Choose a current or future date within this programme. A one-off move does not change recurring days."
                }
                require(
                    current.plan().workouts().none { it.id() != workoutId && it.date() == movedTo }
                ) {
                    "A run is already planned on that date."
                }
                val monday =
                    movedTo.with(
                        java.time.temporal.TemporalAdjusters.previousOrSame(
                            java.time.DayOfWeek.MONDAY
                        )
                    )
                require(
                    current.plan().workouts().count {
                        it.id() != workoutId &&
                            it.date() >= monday &&
                            it.date() < monday.plusWeeks(1)
                    } < current.goalSnapshot().preferences().runsPerWeek()
                ) {
                    "This move would exceed your maximum runs/week."
                }
            }
            val workouts =
                current
                    .plan()
                    .workouts()
                    .mapNotNull { w ->
                        if (w.id() != workoutId) w
                        else if (movedTo == null) null
                        else
                            PlannedWorkout(
                                w.id(),
                                movedTo,
                                w.category(),
                                w.distanceKm(),
                                w.duration(),
                                w.segments(),
                                "Moved by the runner. " + w.note(),
                            )
                    }
                    .sortedBy { it.date() }
            val next =
                PlanVersion(
                    UUID.randomUUID().toString(),
                    current.planId(),
                    current.version() + 1,
                    Instant.now(),
                    if (movedTo == null) "Runner skipped a session" else "Runner moved a session",
                    current.goalSnapshot(),
                    current.evidenceSnapshot(),
                    org.racedaydreams.domain.TrainingPlan(
                        current.planId(),
                        current.plan().startDate(),
                        current.plan().endDate(),
                        current.plan().phases(),
                        workouts,
                    ),
                )
            coaching.transaction {
                coaching.saveCurrent(next)
                development.saveSessionDecision(
                    WorkoutDecision(
                        UUID.randomUUID().toString(),
                        workoutId,
                        current.id(),
                        workout.date(),
                        movedTo,
                        if (movedTo == null) WorkoutDecision.Kind.SKIPPED
                        else WorkoutDecision.Kind.MOVED,
                        Instant.now(),
                    )
                )
            }
        }
}
