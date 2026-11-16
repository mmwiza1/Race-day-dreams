package org.racedaydreams.storage

import kotlin.math.abs
import kotlinx.serialization.json.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.text

/**
 * Original store-v1 core data guards. This is only ONE part of parent-backup validation, never a
 * complete BackupEnvelopeJson validator or permission to replace data. Feature archives remain
 * independently validated and cross-checked before any transaction can be enabled.
 */
object CoreBackupDataJson {
    data class BestEffort(val result: RecordedResult, val source: SourceIdentity)

    data class Annotation(
        val provider: String,
        val recordId: String,
        val notes: String?,
        val rpe: Double?,
        val shoeId: String?,
    )

    data class Planned(val workout: PlannedWorkout, val revision: Long)

    class Snapshot
    internal constructor(
        val profile: UserProfile?,
        val draft: String?,
        val baseline: ManualBaselineInput?,
        val baselineAt: Instant?,
        shoes: List<Shoe>,
        activities: List<ActivityRecord>,
        results: List<RecordedResult>,
        bestEfforts: List<BestEffort>,
        annotations: List<Annotation>,
        settings: Map<String, String>,
        planned: List<Planned>,
        assessments: List<AssessmentResult>,
        trainingPlans: List<JsonObject>,
        adjustments: List<JsonObject>,
        sources: Map<String, SourceIdentity>,
    ) {
        val shoes = Immutable.copyList(shoes)
        val activities = Immutable.copyList(activities)
        val results = Immutable.copyList(results)
        val bestEfforts = Immutable.copyList(bestEfforts)
        val annotations = Immutable.copyList(annotations)
        val settings = Immutable.copyMap(settings)
        val planned = Immutable.copyList(planned)
        val assessments = Immutable.copyList(assessments)
        val trainingPlans = Immutable.copyList(trainingPlans)
        val adjustments = Immutable.copyList(adjustments)
        val activitySources = Immutable.copyMap(sources)
    }

    fun decode(data: JsonObject): Snapshot {
        require(data.requiredInteger("storeVersion") == 1) { "Unsupported backup store version." }
        fun child(key: String) = data.getValue(key).row()
        fun optionalObject(key: String) =
            if (data[key] == null || data[key] == JsonNull) null else child(key)
        val profile = optionalObject("profile")?.let { ProfileJson.read(it) }
        val draft = data.text("draft")?.also { StoredJson.objectValue(it) }
        val baseline = optionalObject("manualBaseline")?.let { BaselineJson.decode(it.toString()) }
        val baselineAt = data.text("manualBaselineRecordedAt")?.let { StoredInstant.parse(it) }
        val shoes = data.array("shoes", 100000).map { ShoeJson.decode(it.row().toString()) }
        val shoeIds = shoes.map { it.id() }.toSet()
        require(shoeIds.size == shoes.size && shoes.count { it.defaultShoe() } <= 1) {
            "Duplicate shoe or multiple default shoes."
        }
        val activities =
            data.array("activities", 100000).map { ActivityJson.decode(it.row().toString()) }
        val ids = mutableSetOf<String>()
        val sourceKeys = mutableSetOf<Pair<String, String>>()
        val activitySources = linkedMapOf<String, SourceIdentity>()
        for (a in activities) {
            require(isUserActivity(a)) { "Backup contains external health measurements." }
            require(
                ids.add(a.id()) && sourceKeys.add(a.source().provider() to a.source().recordId())
            ) {
                "Duplicate activity identity/source."
            }
            require(a.shoeId() == null || a.shoeId() in shoeIds) { "Activity shoe is missing." }
            activitySources[a.id()] = a.source()
        }
        val catalog = PerformanceDistance.CORE.associate { it.code() to it.metres() }.toMutableMap()
        fun register(distance: PerformanceDistance) {
            val previous = catalog.put(distance.code(), distance.metres())
            require(previous == null || abs(previous - distance.metres()) <= 0.000001) {
                "One distance code cannot refer to different distances."
            }
        }
        val resultIds = mutableSetOf<String>()
        val results =
            data.array("results", 100000).map {
                ResultJson.decode(it.row().toString()).also { r ->
                    require(r.source() != ResultSource.ROLLING) {
                        "Derived efforts require retained provenance."
                    }
                    require(resultIds.add(r.id())) { "Duplicate result identity." }
                    register(r.distance())
                }
            }
        val pairs = mutableSetOf<Pair<String, String>>()
        val bests =
            data.array("bestEfforts", 100000).map {
                val row = it.row()
                val r = ResultJson.decode(row.getValue("result").row().toString())
                require(r.source() == ResultSource.ROLLING && resultIds.add(r.id())) {
                    "Invalid archived effort provenance/identity."
                }
                require(pairs.add(r.activityId()!! to r.distance().code())) {
                    "Duplicate archived effort distance."
                }
                register(r.distance())
                val source = row.getValue("source").row()
                val identity =
                    SourceIdentity(
                        source.required("provider"),
                        source.required("recordId"),
                        source.text("originPackage"),
                    )
                val prior = activitySources.put(r.activityId()!!, identity)
                require(prior == null || prior == identity) {
                    "Archived efforts disagree about their activity source."
                }
                BestEffort(r, identity)
            }
        sourceKeys.clear()
        val annotations =
            data.array("annotations", 100000).map {
                val row = it.row()
                val provider = row.required("provider")
                val record = row.required("recordId")
                require(sourceKeys.add(provider to record)) { "Duplicate annotation source." }
                require("notes" in row) { "Annotation notes presence is required." }
                val rpe = row.number("rpe")
                val shoe = row.text("shoeId")
                require(rpe == null || rpe in 0.0..10.0) { "Invalid recorded effort." }
                require(shoe == null || shoe in shoeIds) { "Annotation shoe is missing." }
                Annotation(provider, record, row.text("notes"), rpe, shoe)
            }
        val settings =
            child("settings").entries.associate { (key, value) ->
                sqlRequired(key, "setting key")
                require(value is JsonPrimitive && value.isString) { "Setting values must be text." }
                key to value.content
            }
        ids.clear()
        val planned =
            data.array("plannedWorkouts", 100000).map {
                val row = it.row()
                val w = PlannedWorkoutJson.read(row.getValue("workout").row())
                val revision = row.exactLong("revision")
                require(ids.add(w.id()) && revision >= 1) {
                    "Duplicate workout or invalid revision."
                }
                Planned(w, revision)
            }
        fun opaque(key: String): List<JsonObject> {
            val seen = mutableSetOf<String>()
            return data.array(key, 100000).map { item ->
                val row = item.row()
                require(seen.add(row.required("id"))) { "Duplicate $key identity." }
                Json.parseToJsonElement(row.toString()).row()
            }
        }
        val assessments = opaque("assessments").map { BaselineJson.decodeAssessment(it.toString()) }
        return Snapshot(
            profile,
            draft,
            baseline,
            baselineAt,
            shoes,
            activities,
            results,
            bests,
            annotations,
            settings,
            planned,
            assessments,
            opaque("trainingPlans"),
            opaque("planAdjustments"),
            activitySources,
        )
    }

    fun validateRunningMirror(core: Snapshot, coaching: CoachingArchiveJson.Snapshot) {
        val current = coaching.currentVersionId ?: return
        require(
            core.planned.map { it.workout }.toSet() ==
                coaching.versions.getValue(current).plan().workouts().toSet()
        ) {
            "Backup current programme and reminder prescriptions disagree."
        }
    }

    fun validateRetention(core: Snapshot, retention: DetailRetentionArchiveJson.Snapshot) {
        for (r in retention.removals) {
            val source = core.activitySources[r.activityId()]
            require(
                source != null &&
                    source.provider() == r.provider() &&
                    source.recordId() == r.recordId()
            ) {
                "Removed detail disagrees with activity provenance."
            }
        }
    }
}
