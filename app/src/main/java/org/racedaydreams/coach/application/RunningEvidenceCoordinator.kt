package org.racedaydreams.coach.application

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.data.Release08Store
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.ResultRepository
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.activity.ShoeRepository
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.coaching.RunExecution
import org.racedaydreams.domain.coaching.RunScoreEngine

/** Actual running evidence and its explicit preserved-prescription associations. */
class RunningEvidenceCoordinator(
    private val release: Release08Store,
    private val operations: CoachOperationRunner,
    private val activities: ActivityRepository,
    private val results: ResultRepository,
    private val shoes: ShoeRepository,
    private val coaching: CoachingRepository,
) {
    fun saveResult(result: RecordedResult, onSuccess: () -> Unit = {}) = operations.submit("Result saved with its stated source.", onSuccess) { results.saveResult(result) }

    fun deleteResult(id: String) = operations.submit("Result removed.") { results.deleteResult(id) }

    fun saveShoe(shoe: Shoe, onSuccess: () -> Unit = {}) = operations.submit("Shoe saved.", onSuccess) { shoes.saveShoe(shoe) }

    fun assignShoe(activityId: String, shoeId: String?) = operations.submit("Shoe assignment updated.") { activities.assignShoe(activityId, shoeId) }

    fun saveNotes(id: String, notes: String, rpe: Double?) = operations.submit("Run notes saved.") { activities.saveActivityNotes(id, notes, rpe) }

    fun shoeDiscomfort(activityId:String,noted:Boolean) = operations.submit("Shoe observation saved. No cause is inferred.") {
        require(activities.activitySummaries().any{it.id()==activityId&&it.shoeId()!=null}){"Assign a shoe before adding an observation."}
        val ids=operations.current.shoeDiscomfort.toMutableSet();if(noted)ids.add(activityId)else ids.remove(activityId)
        release.save("config","shoe-feedback",org.json.JSONObject().put("version",1).put("type","shoe_feedback").put("activities",org.json.JSONArray(ids.sorted())))
    }

    fun linkRun(activityId: String, versionId: String, workoutId: String, reps: Int? = null) = operations.submit("Run linked to its preserved prescription.") {
        require(coaching.responses().none{it.activityId()==activityId}) { "This run is already linked; review its source before changing it." }
        val activity = activities.activities().firstOrNull { it.id() == activityId } ?: throw IllegalArgumentException("Run no longer available.")
        val version = coaching.versions().firstOrNull { it.id() == versionId } ?: throw IllegalArgumentException("Plan version no longer available.")
        val workout = version.plan().workouts().firstOrNull { it.id() == workoutId } ?: throw IllegalArgumentException("Workout not found.")
        require(workout.date() <= LocalDate.now()) { "A future workout cannot be marked completed." }
        require(activities.activities().none { it.id() != activityId && it.plannedWorkoutId() == workoutId }) { "Another run is linked to this workout. Review it before changing this link." }
        val response = RunExecution(activity.id(), workout.id(), version.id(), reps, null, "Runner explicitly linked activity to prescription")
        coaching.transaction {
            coaching.saveResponse(response)
            release.save("association",activityId,org.json.JSONObject().put("version",1).put("activity",activityId).put("planVersion",versionId).put("workout",workoutId).put("at",Instant.now().toString()))
            coaching.saveFeedback(activity.id(), version.id(), RunScoreEngine.analyse(workout, activity, response))
        }
    }

    fun completeRun(versionId: String, workoutId: String, distanceKm: Double, duration: Duration,
                    date: LocalDate, time: LocalTime, rpe: Double?, notes: String, onSuccess: (String) -> Unit) {
        val id = UUID.randomUUID().toString()
        operations.submit(onSuccess = { onSuccess(id) }) {
            require(distanceKm.isFinite() && distanceKm > 0 && distanceKm <= 300) { "Enter an actual distance between 0 and 300 km." }
            require(!duration.isZero && !duration.isNegative && duration <= Duration.ofHours(48)) { "Enter a positive actual duration of up to 48 hours." }
            val start = date.atTime(time).atZone(ZoneId.systemDefault())
            require(!start.toInstant().plus(duration).isAfter(Instant.now())) { "A completed run must end before the current time." }
            val version = coaching.versions().firstOrNull { it.id() == versionId } ?: throw IllegalArgumentException("Plan version not found.")
            val workout = version.plan().workouts().firstOrNull { it.id() == workoutId } ?: throw IllegalArgumentException("Workout not found.")
            require(workout.date() <= LocalDate.now()) { "A future prescription cannot be marked completed." }
            require(activities.activities().none { it.plannedWorkoutId() == workoutId }) { "This workout already has a recorded run." }
            val activity = ActivityRecord(id, SourceIdentity("manual", id, null), start.toInstant(), start.toInstant().plus(duration), start.offset,
                start.plus(duration).offset, distanceKm * 1000, null, null, null, null, null, rpe, notes, emptyList(), emptyList(), workoutId,
                shoes.shoes().firstOrNull { it.defaultShoe() }?.id())
            val response = RunExecution(id, workoutId, versionId, null, null, "Manually recorded actual run; no splits or reps invented")
            coaching.transaction {
                activities.upsertManualActivity(activity)
                coaching.saveResponse(response)
                coaching.saveFeedback(id, versionId, RunScoreEngine.analyse(workout, activity, response))
            }
        }
    }

}
