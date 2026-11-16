package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.storage.RemovedRecordingDetail
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.requiredNumber
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string
import org.racedaydreams.storage.StoredJson.text

/**
 * Streaming graph guard for original raw archives 1–3. Keeps bounded per-session metadata, never
 * all route points. This does NOT verify ZIP entries/checksums or permit database writes. The file
 * adapter must verify the manifest and require a newline after every complete row.
 */
class RecordingArchiveRows(private val version: Int) {
    companion object {
        const val MAX_BYTES = 512L * 1024 * 1024
        const val MAX_LINE = 8 * 1024 * 1024
    }

    data class Session(
        val id: String,
        val state: String,
        val startedAt: Instant,
        val startedMono: Long,
        val lastMono: Long,
        val activityId: String?,
        val points: Int,
        val events: Long,
        val hasContext: Boolean,
        val hasActivity: Boolean,
        val removal: RemovedRecordingDetail?,
    )

    class Snapshot internal constructor(val version: Int, sessions: Map<String, Session>) {
        val sessions = Immutable.copyMap(sessions)
    }

    private class Pending(val header: Session) {
        var points = 0
        var events = 0L
        var lastPointMono = -1L
        var lastDistance = 0.0
        var hasContext = false
        var hasActivity = false
        var removal: RemovedRecordingDetail? = null

        fun freeze() =
            header.copy(
                points = points,
                events = events,
                hasContext = hasContext,
                hasActivity = hasActivity,
                removal = removal,
            )
    }

    private val sessions = linkedMapOf<String, Pending>()
    private var bytes = 0L
    private var closed = false
    private var failed = false

    init {
        require(version in 1..3) { "Unsupported recording archive version." }
    }

    /** One complete UTF-8 line, excluding its terminal newline; blank lines remain legal. */
    fun acceptLine(line: String): Unit = guarded {
        require(line.length <= MAX_LINE && '\n' !in line) { "Archive row too large or incomplete." }
        val size = line.encodeToByteArray(throwOnInvalidSequence = true).size.toLong() + 1
        require(size <= MAX_BYTES - bytes) { "Recording archive exceeds 512 MiB." }
        bytes += size
        if (line.isEmpty()) return@guarded
        // Original JSONTokener permits one initial UTF-8 BOM for each complete JSON document.
        val text = line.removePrefix("\uFEFF")
        StoredJsonSyntax(text, 32, 2000000, MAX_LINE) {
                throw IllegalArgumentException("Invalid recording archive row: $it")
            }
            .validate()
        val row = Json.parseToJsonElement(text).row()
        val id = row.string("id")
        require(id.length in 1..256) { "Invalid recording identity." }
        val kind = row.string("kind")
        if (kind == "session") {
            require(id !in sessions && sessions.size < if (version >= 3) 100000 else 1000) {
                "Duplicate recording or archive session limit exceeded."
            }
            val state = row.string("state")
            require(state in listOf("ACTIVE", "PAUSED", "RECOVERABLE", "FINISHED")) {
                "Unsupported recording state."
            }
            val mono = row.exactLong("mono")
            val last = row.exactLong("last")
            require(mono >= 0 && last >= mono) { "Invalid recording clock bounds." }
            val activity = if (state == "FINISHED") row.string("activity") else row.text("activity")
            sessions[id] =
                Pending(
                    Session(
                        id,
                        state,
                        StoredInstant.parse(row.string("start")),
                        mono,
                        last,
                        activity,
                        0,
                        0,
                        false,
                        false,
                        null,
                    )
                )
            return@guarded
        }
        val pending = requireNotNull(sessions[id]) { "Recording row precedes its session." }
        val header = pending.header
        val payload =
            row["payload"]?.row() ?: throw IllegalArgumentException("Missing recording payload.")
        when (kind) {
            "context" -> {
                require(!pending.hasContext) { "Duplicate recording context." }
                SupportArchiveJson.decode(
                    buildJsonObject {
                        put("version", 1)
                        put(
                            "rows",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("kind", "execution")
                                        put("id", id)
                                        put("payload", payload)
                                    }
                                )
                            },
                        )
                    }
                )
                pending.hasContext = true
            }
            "point" -> {
                require(
                    row.exactLong("seq") == pending.points.toLong() &&
                        pending.points < if (version >= 3) 500000 else 100000
                ) {
                    "Invalid recording point sequence or point limit exceeded."
                }
                val point = RecordingPointJson.decode(payload.toString())
                val mono = point.fix().monotonicMillis()
                require(
                    mono in header.startedMono..header.lastMono &&
                        mono > pending.lastPointMono &&
                        point.activeMillis() <= mono - header.startedMono &&
                        point.cumulativeMetres() >= pending.lastDistance
                ) {
                    "Recording point disagrees with retained clock/distance evidence."
                }
                pending.points++
                pending.lastPointMono = mono
                pending.lastDistance = point.cumulativeMetres()
            }
            "event" -> {
                require(row.exactLong("seq") == pending.events) {
                    "Invalid recording event sequence."
                }
                require(
                    payload.string("type").length in 1..64 &&
                        payload.exactLong("mono") in header.startedMono..header.lastMono
                ) {
                    "Invalid recording event."
                }
                if (payload["value"] != null && payload["value"] != JsonNull)
                    payload.requiredNumber("value")
                StoredInstant.parse(payload.string("wall"))
                pending.events++
            }
            "activity" -> {
                require(!pending.hasActivity) { "Duplicate recording summary." }
                val activity = ActivityJson.decode(payload.toString())
                require(
                    activity.id() == header.activityId &&
                        activity.source().recordId() == id &&
                        activity.source().provider() in listOf("rdd", "gpx")
                ) {
                    "Recording summary disagrees with its owned source."
                }
                pending.hasActivity = true
            }
            "removed" -> {
                require(version >= 3 && header.state == "FINISHED" && pending.removal == null) {
                    "Unsupported or duplicate removed-detail provenance."
                }
                val proof = DetailRetentionArchiveJson.removal(payload)
                require(
                    proof.sessionId() == id &&
                        proof.activityId() == header.activityId &&
                        proof.startedAt() == header.startedAt &&
                        proof.startedMono() == header.startedMono &&
                        proof.lastMono() == header.lastMono
                ) {
                    "Removed detail disagrees with its session."
                }
                pending.removal = proof
            }
            else -> throw IllegalArgumentException("Unsupported recording row.")
        }
    }

    /** Call only after the file adapter has verified EOF framing and the complete file checksum. */
    fun finish(): Snapshot = guarded {
        for (pending in sessions.values) {
            require(pending.header.state != "FINISHED" || pending.hasActivity) {
                "Finished recording metadata is missing."
            }
            require(pending.removal == null || pending.points == 0 && pending.events == 0L) {
                "Removed detail cannot contain raw rows."
            }
        }
        closed = true
        Snapshot(version, sessions.mapValues { it.value.freeze() })
    }

    private inline fun <T> guarded(block: () -> T): T {
        check(!closed && !failed) { "Recording archive validation is already closed or failed." }
        return try {
            block()
        } catch (failure: Throwable) {
            failed = true
            throw failure
        }
    }
}
