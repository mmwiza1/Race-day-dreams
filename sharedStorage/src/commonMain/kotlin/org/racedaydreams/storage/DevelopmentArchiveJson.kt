package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.coaching.WorkoutDecision
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.requiredNumber
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.text

/**
 * Original schema-five evidence archive. This section alone cannot validate or restore a backup.
 */
object DevelopmentArchiveJson {
    enum class Kind {
        WEIGHT,
        HISTORICAL_PB,
        INTERRUPTION,
        SESSION_DECISION,
        STRENGTH_LINK,
        REVIEW_DECISION,
        WEEKLY_REVIEW,
    }

    sealed interface Evidence {
        data class Weight(val value: BodyWeight) : Evidence

        data class Historical(val value: HistoricalStrengthRecord) : Evidence

        data class Interruption(val value: WeeklyCoaching.Interruption) : Evidence

        data class SessionDecision(val value: WorkoutDecision) : Evidence

        data class StrengthLink(
            val plannedDate: LocalDate,
            val gymVersion: String?,
            val gymSession: String?,
        ) : Evidence

        data class ReviewDecision(val base: String, val choice: String, val result: String) :
            Evidence

        data class WeeklyReview(
            val base: String,
            val week: LocalDate,
            val planned: Int,
            val completed: Int,
            val kilometres: Double,
        ) : Evidence
    }

    class Record
    internal constructor(
        val kind: Kind,
        val id: String,
        val payload: JsonObject,
        val evidence: Evidence,
    )

    class Snapshot internal constructor(records: List<Record>) {
        val records: List<Record> = Immutable.copyList(records)
    }

    /** The enclosing root owns the validated syntax/size budget, including nested payloads. */
    fun decode(root: JsonObject): Snapshot {
        require(root.requiredInteger("version") == 1) { "Unsupported evidence format." }
        val ids = mutableSetOf<Pair<Kind, String>>()
        val records =
            root.array("records", 100000).map { item ->
                val row = item.row()
                val kind = Kind.valueOf(row.required("kind"))
                val id = row.required("id")
                require(ids.add(kind to id)) { "Duplicated evidence." }
                val payload = row.getValue("payload").row()
                require(id == payload.required("id")) { "Evidence identity differs." }
                // A private DOM copy protects retained legacy/unknown fields from caller mutation.
                val retained = Json.parseToJsonElement(payload.toString()).row()
                Record(kind, id, retained, evidence(kind, retained))
            }
        return Snapshot(records)
    }

    fun element(snapshot: Snapshot): JsonObject =
        buildJsonObject {
                put("version", 1)
                put(
                    "records",
                    JsonArray(
                        snapshot.records.map { record ->
                            buildJsonObject {
                                put("kind", record.kind.name)
                                put("id", record.id)
                                put("payload", record.payload)
                            }
                        }
                    ),
                )
            }
            .also { decode(it) }

    /**
     * Match original user-data exports; imported weights are retained locally, never deleted here.
     */
    fun userOwnedElement(snapshot: Snapshot): JsonObject =
        element(
            Snapshot(
                snapshot.records.filter {
                    val weight = it.evidence as? Evidence.Weight
                    weight == null || weight.value.provider() == "manual"
                }
            )
        )

    private fun evidence(kind: Kind, p: JsonObject): Evidence =
        when (kind) {
            Kind.WEIGHT ->
                Evidence.Weight(
                    BodyWeight(
                        p.required("id"),
                        p.requiredNumber("kg"),
                        StoredInstant.parse(p.required("at")),
                        StoredOffset.parse(p.required("offset")),
                        p.required("provider"),
                        p.required("sourceId"),
                        p.text("origin"),
                    )
                )
            Kind.HISTORICAL_PB ->
                Evidence.Historical(
                    HistoricalStrengthRecord(
                        p.required("id"),
                        StrengthEvidenceJson.readExercise(p.getValue("exercise").row()),
                        p.requiredNumber("kg"),
                        p.requiredInteger("reps"),
                        p.text("date")?.let { StoredDate.parse(p.required("date")) },
                        HistoricalStrengthRecord.DatePrecision.valueOf(p.required("precision")),
                        p.text("notes"),
                        if (p["seconds"] == null || p["seconds"] == JsonNull) null
                        else p.exactLong("seconds"),
                        p.number("metres"),
                    )
                )
            Kind.INTERRUPTION ->
                Evidence.Interruption(
                    WeeklyCoaching.Interruption(
                        StoredDate.parse(p.required("from")),
                        StoredDate.parse(p.required("returned")),
                        p.required("reason"),
                    )
                )
            Kind.SESSION_DECISION ->
                Evidence.SessionDecision(
                    WorkoutDecision(
                        p.required("id"),
                        p.required("workoutId"),
                        p.required("versionId"),
                        StoredDate.parse(p.required("originalDate")),
                        p.text("movedTo")?.let { StoredDate.parse(p.required("movedTo")) },
                        WorkoutDecision.Kind.valueOf(p.required("kind")),
                        StoredInstant.parse(p.required("at")),
                    )
                )
            Kind.STRENGTH_LINK -> {
                val version = p.text("gymVersion")
                val session = p.text("gymSession")
                require((version == null) == (session == null)) {
                    "Gym prescription identity is incomplete."
                }
                Evidence.StrengthLink(StoredDate.parse(p.required("plannedDate")), version, session)
            }
            Kind.REVIEW_DECISION ->
                Evidence.ReviewDecision(
                    p.required("base"),
                    p.required("choice"),
                    p.required("result"),
                )
            Kind.WEEKLY_REVIEW -> {
                val base = p.required("base")
                val week = StoredDate.parse(p.required("week"))
                val planned = p.requiredInteger("planned")
                val completed = p.requiredInteger("completed")
                val km = p.requiredNumber("km")
                require(planned >= 0 && completed >= 0 && km >= 0) { "Invalid weekly summary." }
                require(p["observations"] is JsonArray && p["channels"] is JsonArray) {
                    "Missing retained review context."
                }
                val choices =
                    p["choices"] as? JsonArray
                        ?: throw IllegalArgumentException("Missing review choices.")
                for (item in choices) {
                    val changes =
                        item.row()["replacements"] as? JsonArray
                            ?: throw IllegalArgumentException("Missing review replacements.")
                    for (change in changes) PlannedWorkoutJson.read(change.row())
                }
                // Deliberately do not invent current labels/channels/choice IDs for older
                // summaries.
                Evidence.WeeklyReview(base, week, planned, completed, km)
            }
        }
}
