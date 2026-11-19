package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.GymRestTimer
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.bool
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version
import org.racedaydreams.storage.GymStoredJson.whole
import org.racedaydreams.storage.GymStoredJson.write

/** Existing v1 advisory archive. Execution ownership must be validated before any restore. */
object GymToolsPayloadJson {
    data class Snapshot(val preferences: GymRestTimer.Preferences, val timers: List<GymRestTimer>)

    fun encode(snapshot: Snapshot): String = write(element(snapshot))

    /** Parent backups retain their own whole-file budget rather than a second standalone cap. */
    fun element(snapshot: Snapshot): JsonObject = buildJsonObject {
        put("version", 1)
        val p = snapshot.preferences
        put(
            "preferences",
            buildJsonObject {
                put("defaultSeconds", p.defaultSeconds())
                put("suggestAfterSet", p.suggestAfterSet())
                put("notifications", p.notifications())
                put("vibration", p.vibration())
                put("audio", p.audio())
            },
        )
        put(
            "timers",
            buildJsonArray {
                for (t in snapshot.timers) add(
                    buildJsonObject {
                        put("execution", t.executionId())
                        put("trigger", t.triggerId())
                        put("phase", t.phase().name)
                        put("epoch", t.epoch())
                        put("observed", t.observedMillis())
                        put("remaining", t.remainingMillis())
                        put("duration", t.durationMillis())
                    }
                )
            },
        )
    }

    /** Null is the old-backup default; a timer without a retained execution is never accepted. */
    fun decode(payload: String?, knownExecutions: Set<String>): Snapshot {
        if (payload == null) return Snapshot(GymRestTimer.Preferences.defaults(), emptyList())
        return decode(read(payload), knownExecutions)
    }

    /** Call only after parent syntax/size validation; execution ownership is still mandatory. */
    fun decode(root: JsonObject, knownExecutions: Set<String>): Snapshot {
        fields(root, "version", "preferences", "timers")
        version(root, 1)
        val p = child(root, "preferences")
        fields(p, "defaultSeconds", "suggestAfterSet", "notifications", "vibration", "audio")
        val preferences =
            GymRestTimer.Preferences(
                integer(p, "defaultSeconds"),
                bool(p, "suggestAfterSet"),
                bool(p, "notifications"),
                bool(p, "vibration"),
                bool(p, "audio"),
            )
        val seen = mutableSetOf<String>()
        val timers =
            array(root, "timers", 100000).map {
                val r = row(it)
                fields(
                    r,
                    "execution",
                    "trigger",
                    "phase",
                    "epoch",
                    "observed",
                    "remaining",
                    "duration",
                )
                val t =
                    GymRestTimer(
                        text(r, "execution", 128),
                        text(r, "trigger", 128),
                        choice<GymRestTimer.Phase>(r, "phase"),
                        text(r, "epoch", 128),
                        whole(r, "observed"),
                        whole(r, "remaining"),
                        whole(r, "duration"),
                    )
                require(t.executionId() in knownExecutions && seen.add(t.executionId())) {
                    "Orphan or duplicate rest timer."
                }
                t
            }
        return Snapshot(preferences, timers)
    }
}
