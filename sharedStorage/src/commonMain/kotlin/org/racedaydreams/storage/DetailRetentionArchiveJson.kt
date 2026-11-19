package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.storage.RemovedRecordingDetail
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string
import org.racedaydreams.storage.StoredJson.text

/**
 * Original retention v1 metadata inside a validated parent backup. This is not cleanup permission
 * or proof that raw detail is available. Activity/source and archived-detail matching belong to the
 * enclosing transactional restore, which must validate every section before replacing anything.
 */
object DetailRetentionArchiveJson {
    class Snapshot(
        preferences: DetailRetention.Preferences,
        removals: List<RemovedRecordingDetail>,
    ) {
        val preferences = preferences
        val removals = Immutable.copyList(removals)

        init {
            require(this.removals.size <= 100000) {
                "Too many detail receipts; nothing was truncated."
            }
            require(
                this.removals.map { it.sessionId() }.toSet().size == this.removals.size &&
                    this.removals.map { it.activityId() }.toSet().size == this.removals.size
            ) {
                "Duplicate removed-detail identity."
            }
        }
    }

    /** Missing older sections preserve existing-user protection, never new-install consent. */
    fun decode(json: JsonObject?): Snapshot {
        if (json == null) return Snapshot(DetailRetention.Preferences.existing(), emptyList())
        GymStoredJson.fields(
            json,
            "version",
            "policy",
            "choice",
            "chosenAt",
            "choiceId",
            "removals",
        )
        require(json.requiredInteger("version") == 1) { "Unsupported detail-retention format." }
        val preferences =
            DetailRetention.Preferences(
                DetailRetention.Policy.valueOf(json.string("policy")),
                DetailRetention.Choice.valueOf(json.string("choice")),
                json.text("chosenAt")?.let { StoredInstant.parse(it) },
                json.text("choiceId"),
            )
        val removals =
            (json["removals"] as? JsonArray)
                ?: throw IllegalArgumentException("Expected detail receipts.")
        require(removals.size <= 100000) { "Too many detail receipts; nothing was truncated." }
        return Snapshot(preferences, removals.map { removal(it.row()) })
    }

    /** Parent32MiB syntax/export limits apply; a second16MiB gym limit would truncate history. */
    fun element(snapshot: Snapshot): JsonObject = buildJsonObject {
        val p = snapshot.preferences
        put("version", 1)
        put("policy", p.policy().name)
        put("choice", p.choice().name)
        put("chosenAt", StoredJson.nullable(p.chosenAt()?.toString()))
        put("choiceId", StoredJson.nullable(p.choiceId()))
        put("removals", JsonArray(snapshot.removals.map { element(it) }))
    }

    fun removal(json: JsonObject): RemovedRecordingDetail {
        GymStoredJson.fields(
            json,
            "session",
            "activity",
            "provider",
            "record",
            "end",
            "removed",
            "policy",
            "start",
            "mono",
            "last",
            "points",
            "events",
            "pointsHash",
            "eventsHash",
        )
        return RemovedRecordingDetail(
            json.required("session"),
            json.required("activity"),
            json.required("provider"),
            json.required("record"),
            StoredInstant.parse(json.string("end")),
            StoredInstant.parse(json.string("removed")),
            DetailRetention.Policy.valueOf(json.string("policy")),
            StoredInstant.parse(json.string("start")),
            json.exactLong("mono"),
            json.exactLong("last"),
            json.exactLong("points"),
            json.exactLong("events"),
            json.string("pointsHash"),
            json.string("eventsHash"),
        )
    }

    fun element(row: RemovedRecordingDetail): JsonObject = buildJsonObject {
        put("session", row.sessionId())
        put("activity", row.activityId())
        put("provider", row.provider())
        put("record", row.recordId())
        put("end", row.endedAt().toString())
        put("removed", row.removedAt().toString())
        put("policy", row.policy().name)
        put("start", row.startedAt().toString())
        put("mono", row.startedMono())
        put("last", row.lastMono())
        put("points", row.points())
        put("events", row.events())
        put("pointsHash", row.pointsHash())
        put("eventsHash", row.eventsHash())
    }
}
