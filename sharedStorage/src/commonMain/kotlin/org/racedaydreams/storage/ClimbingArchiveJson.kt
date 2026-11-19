package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.climbing.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.nullableLong
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version
import org.racedaydreams.storage.GymStoredJson.whole

/**
 * Bouldering payloads and the backup "climbing" section (root backup 14+). Every problem keeps the
 * grade and grading system exactly as entered. Older backups have no section and restore empty.
 */
object ClimbingArchiveJson {
    /** Saved sessions plus the athlete's preferred grading system. Unsaved drafts stay local. */
    class Snapshot(sessions: List<ClimbingSession>, val gradeSystem: GradeSystem) {
        val sessions = Immutable.copyList(sessions)

        init {
            require(this.sessions.size <= MAX_SESSIONS) {
                "Climbing history exceeds the format limit; nothing was truncated."
            }
            require(this.sessions.map { it.id }.toSet().size == this.sessions.size) {
                "Duplicate climbing session identity."
            }
        }

        companion object {
            fun empty() = Snapshot(emptyList(), GradeSystem.V_SCALE)
        }
    }

    const val MAX_SESSIONS = 100000

    fun decode(input: JsonObject?): Snapshot {
        if (input == null) return Snapshot.empty()
        fields(input, "version", "gradeSystem", "sessions")
        version(input, 1)
        return Snapshot(
            array(input, "sessions", MAX_SESSIONS).map { session(row(it)) },
            choice<GradeSystem>(input, "gradeSystem"),
        )
    }

    fun element(snapshot: Snapshot): JsonObject =
        buildJsonObject {
                put("version", 1)
                put("gradeSystem", snapshot.gradeSystem.name)
                put("sessions", buildJsonArray { snapshot.sessions.forEach { add(session(it)) } })
            }
            .also { decode(it) }

    fun session(value: ClimbingSession): JsonObject = buildJsonObject {
        put("version", 1)
        put("id", value.id)
        put("startedAt", value.startedAt.toString())
        put("offset", value.offset.toString())
        put("durationSeconds", value.durationSeconds)
        put("location", value.location)
        put("notes", value.notes)
        put("problems", problems(value.problems))
    }

    fun session(input: JsonObject): ClimbingSession {
        fields(
            input,
            "version",
            "id",
            "startedAt",
            "offset",
            "durationSeconds",
            "location",
            "notes",
            "problems",
        )
        version(input, 1)
        return ClimbingSession(
            text(input, "id", 256),
            StoredInstant.parse(text(input, "startedAt", 128)),
            StoredOffset.parse(text(input, "offset", 32)),
            whole(input, "durationSeconds"),
            text(input, "location", ClimbingSession.MAX_LOCATION),
            text(input, "notes", ClimbingSession.MAX_NOTES),
            problems(input),
        )
    }

    fun draft(value: ClimbingDraft): JsonObject = buildJsonObject {
        put("version", 1)
        put("id", value.id)
        put("startedAt", value.startedAt.toString())
        put("offset", value.offset.toString())
        put("durationSeconds", value.durationSeconds?.let { JsonPrimitive(it) } ?: JsonNull)
        put("location", value.location)
        put("notes", value.notes)
        put("problems", problems(value.problems))
    }

    fun draft(input: JsonObject): ClimbingDraft {
        fields(
            input,
            "version",
            "id",
            "startedAt",
            "offset",
            "durationSeconds",
            "location",
            "notes",
            "problems",
        )
        version(input, 1)
        return ClimbingDraft(
            text(input, "id", 256),
            StoredInstant.parse(text(input, "startedAt", 128)),
            StoredOffset.parse(text(input, "offset", 32)),
            nullableLong(input, "durationSeconds"),
            text(input, "location", ClimbingSession.MAX_LOCATION),
            text(input, "notes", ClimbingSession.MAX_NOTES),
            problems(input),
        )
    }

    private fun problems(values: List<ClimbingProblem>) = buildJsonArray {
        for (p in values) add(
            buildJsonObject {
                put("id", p.id)
                put("system", p.grade?.system?.name?.let { JsonPrimitive(it) } ?: JsonNull)
                put("grade", p.grade?.label?.let { JsonPrimitive(it) } ?: JsonNull)
                put("attempts", p.attempts)
                put("outcome", p.outcome.name)
                put("name", p.name)
            }
        )
    }

    private fun problems(input: JsonObject): List<ClimbingProblem> =
        array(input, "problems", ClimbingSession.MAX_PROBLEMS).map { element ->
            val r = row(element)
            fields(r, "id", "system", "grade", "attempts", "outcome", "name")
            val grade =
                if (isNull(r, "system") && isNull(r, "grade")) null
                else ClimbingGrade(choice<GradeSystem>(r, "system"), text(r, "grade", 8))
            ClimbingProblem(
                text(r, "id", 256),
                grade,
                integer(r, "attempts"),
                choice<ClimbOutcome>(r, "outcome"),
                text(r, "name", ClimbingProblem.MAX_NAME),
            )
        }
}
