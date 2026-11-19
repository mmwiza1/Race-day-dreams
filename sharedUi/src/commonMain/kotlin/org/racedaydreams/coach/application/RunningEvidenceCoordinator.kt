package org.racedaydreams.coach.application

import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.ResultRepository
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.activity.ShoeRepository
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.coaching.CoachingTransaction
import org.racedaydreams.domain.coaching.RunExecution
import org.racedaydreams.domain.coaching.RunScoreEngine
import org.racedaydreams.domain.time.*
import org.racedaydreams.storage.SqliteRunningSupportArchives

/** Actual running evidence and its explicit preserved-prescription associations. */
class RunningEvidenceCoordinator(
    private val release: SqliteRunningSupportArchives,
    private val operations: CoachOperationRunner,
    private val activities: ActivityRepository,
    private val results: ResultRepository,
    private val shoes: ShoeRepository,
    private val coaching: CoachingRepository,
    private val now: () -> Instant,
    private val today: () -> LocalDate,
    private val newId: () -> String,
    private val localTiming: (LocalDate, String, Duration) -> Timing,
) {
    /** Resolved by the real platform timezone, including an offset change during the run. */
    data class Timing(
        val start: Instant,
        val end: Instant,
        val startOffset: ZoneOffset,
        val endOffset: ZoneOffset,
    )

    fun saveResult(result: RecordedResult, onSuccess: () -> Unit = {}) =
        operations.submit("Result saved with its stated source.", onSuccess) {
            results.saveResult(result)
        }

    fun deleteResult(id: String) = operations.submit("Result removed.") { results.deleteResult(id) }

    fun saveShoe(shoe: Shoe, onSuccess: () -> Unit = {}) =
        operations.submit("Shoe saved.", onSuccess) { shoes.saveShoe(shoe) }

    fun assignShoe(activityId: String, shoeId: String?) =
        operations.submit("Shoe assignment updated.") { activities.assignShoe(activityId, shoeId) }

    fun saveNotes(id: String, notes: String, rpe: Double?) =
        operations.submit("Run notes saved.") { activities.saveActivityNotes(id, notes, rpe) }

    fun shoeDiscomfort(activityId: String, noted: Boolean) =
        operations.submit("Shoe observation saved. No cause is inferred.") {
            require(
                activities.activitySummaries().any { it.id() == activityId && it.shoeId() != null }
            ) {
                "Assign a shoe before adding an observation."
            }
            val ids = operations.current.shoeDiscomfort.toMutableSet()
            if (noted) ids.add(activityId) else ids.remove(activityId)
            release.saveShoeDiscomfort(ids)
        }

    fun linkRun(activityId: String, versionId: String, workoutId: String, reps: Int? = null) =
        operations.submit("Run linked to its preserved prescription.") {
            require(coaching.responses().none { it.activityId() == activityId }) {
                "This run is already linked; review its source before changing it."
            }
            val activity =
                activities.activities().firstOrNull { it.id() == activityId }
                    ?: throw IllegalArgumentException("Run no longer available.")
            val version =
                coaching.versions().firstOrNull { it.id() == versionId }
                    ?: throw IllegalArgumentException("Plan version no longer available.")
            val workout =
                version.plan().workouts().firstOrNull { it.id() == workoutId }
                    ?: throw IllegalArgumentException("Workout not found.")
            require(workout.date().toEpochDay() <= today().toEpochDay()) {
                "A future workout cannot be marked completed."
            }
            require(
                activities.activities().none {
                    it.id() != activityId && it.plannedWorkoutId() == workoutId
                }
            ) {
                "Another run is linked to this workout. Review it before changing this link."
            }
            val response =
                RunExecution(
                    activity.id(),
                    workout.id(),
                    version.id(),
                    reps,
                    null,
                    "Runner explicitly linked activity to prescription",
                )
            coaching.transaction(
                object : CoachingTransaction {
                    override fun run() {
                        coaching.saveResponse(response)
                        release.saveRunAssociation(activityId, versionId, workoutId, now())
                        coaching.saveFeedback(
                            activity.id(),
                            version.id(),
                            RunScoreEngine.analyse(workout, activity, response),
                        )
                    }
                }
            )
        }

    fun completeRun(
        versionId: String,
        workoutId: String,
        distanceKm: Double,
        duration: Duration,
        date: LocalDate,
        time: String,
        rpe: Double?,
        notes: String,
        onSuccess: (String) -> Unit,
    ) {
        val id = newId()
        operations.submit(onSuccess = { onSuccess(id) }) {
            require(distanceKm.isFinite() && distanceKm > 0 && distanceKm <= 300) {
                "Enter an actual distance between 0 and 300 km."
            }
            require(
                !duration.isZero() && !duration.isNegative() && duration <= Durations.ofHours(48)
            ) {
                "Enter a positive actual duration of up to 48 hours."
            }
            val timing = localTiming(date, time, duration)
            require(!timing.end.isAfter(now())) {
                "A completed run must end before the current time."
            }
            val version =
                coaching.versions().firstOrNull { it.id() == versionId }
                    ?: throw IllegalArgumentException("Plan version not found.")
            val workout =
                version.plan().workouts().firstOrNull { it.id() == workoutId }
                    ?: throw IllegalArgumentException("Workout not found.")
            require(workout.date().toEpochDay() <= today().toEpochDay()) {
                "A future prescription cannot be marked completed."
            }
            require(activities.activities().none { it.plannedWorkoutId() == workoutId }) {
                "This workout already has a recorded run."
            }
            val activity =
                ActivityRecord(
                    id,
                    SourceIdentity("manual", id, null),
                    timing.start,
                    timing.end,
                    timing.startOffset,
                    timing.endOffset,
                    distanceKm * 1000,
                    null,
                    null,
                    null,
                    null,
                    null,
                    rpe,
                    notes,
                    emptyList(),
                    emptyList(),
                    workoutId,
                    shoes.shoes().firstOrNull { it.defaultShoe() }?.id(),
                )
            val response =
                RunExecution(
                    id,
                    workoutId,
                    versionId,
                    null,
                    null,
                    "Manually recorded actual run; no splits or reps invented",
                )
            coaching.transaction(
                object : CoachingTransaction {
                    override fun run() {
                        activities.upsertManualActivity(activity)
                        coaching.saveResponse(response)
                        coaching.saveFeedback(
                            id,
                            versionId,
                            RunScoreEngine.analyse(workout, activity, response),
                        )
                    }
                }
            )
        }
    }
}
