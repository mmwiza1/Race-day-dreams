package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.time.DayOfWeek
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.integer
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.row

/** Original GymProgrammeStore payload. Its version is a programme revision, not a wire version. */
object LegacyGymProgrammePayloadJson {
    data class Decision(
        val id: String,
        val baseVersion: String,
        val resultingVersion: String,
        val prescribedSession: String,
        val exerciseId: String,
        val choice: String,
        val evidence: String,
        val at: Instant,
    )

    fun encode(programme: GymProgramme): String = element(programme).toString()

    fun element(programme: GymProgramme): JsonObject = buildJsonObject {
        val settings = programme.settings()
        put("id", programme.id())
        put("version", programme.version())
        put("createdAt", programme.createdAt().toString())
        put("reason", programme.reason())
        put("goal", settings.goal().name)
        put("priority", settings.priority().name)
        put("experience", settings.experience().name)
        put(
            "days",
            buildJsonArray {
                DayOfWeek.entries
                    .filter { it in settings.days() }
                    .forEach { add(JsonPrimitive(it.name)) }
            },
        )
        put("frequency", settings.frequency())
        put(
            "equipment",
            buildJsonArray { settings.equipment().sorted().forEach { add(JsonPrimitive(it)) } },
        )
        put("minutes", settings.minutes())
        put("effort", settings.effort().name)
        put("start", programme.start().toString())
        put("end", programme.end().toString())
        put(
            "sessions",
            buildJsonArray {
                programme.sessions().forEach {
                    add(GymAssociationDecisionJson.legacySessionElement(it))
                }
            },
        )
        put(
            "tradeoffs",
            buildJsonArray { programme.tradeoffs().forEach { add(JsonPrimitive(it)) } },
        )
    }

    fun decode(payload: String): GymProgramme = decode(StoredJson.objectValue(payload))

    /** The enclosing backup owns syntax/allocation limits for this already-validated DOM. */
    fun decode(value: JsonObject): GymProgramme {
        val days = value.array("days", 7).map { DayOfWeek.valueOf(string(it)) }
        require(days.distinct().size == days.size) { "Duplicate gym day." }
        val equipment = value.array("equipment", 20).map { string(it) }.toSet()
        val settings =
            GymProgramme.Settings(
                GymProgramme.Goal.valueOf(value.required("goal")),
                GymProgramme.Priority.valueOf(value.required("priority")),
                GymProgramme.Experience.valueOf(value.required("experience")),
                days.sortedBy { it.ordinal }.toSet(),
                value.requiredInteger("frequency"),
                equipment,
                value.requiredInteger("minutes"),
                GymProgramme.Effort.valueOf(value.required("effort")),
            )
        val sessions =
            value.array("sessions", 56).map { row ->
                val session = row.row()
                val exercises =
                    session.array("exercises", 10).map {
                        val p = it.row()
                        GymProgramme.Prescription(
                            p.required("exerciseId"),
                            p.requiredInteger("sets"),
                            p.requiredInteger("minReps"),
                            p.requiredInteger("maxReps"),
                            p.integer("seconds"),
                            p.number("metres"),
                            p.requiredInteger("minRpe"),
                            p.requiredInteger("maxRpe"),
                            p.requiredInteger("rest"),
                            p.required("intent"),
                            p.number("referenceKg"),
                        )
                    }
                GymProgramme.Session(
                    session.required("id"),
                    StoredDate.parse(session.required("date")),
                    session.required("title"),
                    exercises,
                    session.required("explanation"),
                )
            }
        return GymProgramme(
            value.required("id"),
            value.requiredInteger("version"),
            StoredInstant.parse(value.required("createdAt")),
            value.required("reason"),
            settings,
            StoredDate.parse(value.required("start")),
            StoredDate.parse(value.required("end")),
            sessions,
            value.array("tradeoffs", 300).map { string(it) },
        )
    }

    fun encodeDecision(decision: Decision): String = decisionElement(decision).toString()

    fun decisionElement(decision: Decision): JsonObject = buildJsonObject {
        put("id", decision.id)
        put("base", decision.baseVersion)
        put("result", decision.resultingVersion)
        put("session", decision.prescribedSession)
        put("exercise", decision.exerciseId)
        put("choice", decision.choice)
        put("evidence", decision.evidence)
        put("at", decision.at.toString())
    }

    fun decodeDecision(payload: String): Decision = decodeDecision(StoredJson.objectValue(payload))

    fun decodeDecision(value: JsonObject): Decision {
        val choice = value.required("choice")
        require(choice == "KEEP" || choice == "INCREASE") { "Invalid progression choice." }
        return Decision(
            value.required("id"),
            value.required("base"),
            value.required("result"),
            value.required("session"),
            value.required("exercise"),
            choice,
            value.required("evidence"),
            StoredInstant.parse(value.required("at")),
        )
    }

    private fun string(value: JsonElement): String {
        require(value is JsonPrimitive && value.isString) { "Expected retained gym text." }
        return value.content
    }
}
