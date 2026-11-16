package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.nullableInteger
import org.racedaydreams.storage.GymStoredJson.nullableNumber
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.write

/** Original v1 legacy/v2 authored association decisions. Reading never applies a plan link. */
object GymAssociationDecisionJson {
    fun encode(decision: GymWorkoutAssociation.Decision): String =
        write(element(decision)).also { decode(it) }

    fun decode(payload: String): GymWorkoutAssociation.Decision = decode(read(payload))

    private fun element(decision: GymWorkoutAssociation.Decision): JsonObject = buildJsonObject {
        val proposal = decision.proposal()
        put("version", if (proposal.authored()) 2 else GymWorkoutAssociation.FORMAT_VERSION)
        put("id", decision.id())
        put("choice", decision.choice().name)
        put("decidedAt", decision.decidedAt().toString())
        put(
            "proposal",
            buildJsonObject {
                put("actual", StrengthEvidenceJson.session(proposal.actual()))
                put("origin", proposal.origin().name)
                put("programmeId", proposal.programmeId())
                put("programmeRevision", proposal.programmeRevision())
                put("reviewedAt", proposal.reviewedAt().toString())
                put(
                    "prescription",
                    if (proposal.authored())
                        StrengthProgrammingPayloadJson.workoutElement(
                            proposal.authoredPrescription()!!
                        )
                    else legacySession(proposal.prescription()!!),
                )
            },
        )
    }

    private fun decode(root: JsonObject): GymWorkoutAssociation.Decision {
        fields(root, "version", "id", "choice", "decidedAt", "proposal")
        val format = integer(root, "version")
        require(format == GymWorkoutAssociation.FORMAT_VERSION || format == 2) {
            "Unsupported gym payload version."
        }
        val proposal = child(root, "proposal")
        fields(
            proposal,
            "actual",
            "origin",
            "programmeId",
            "programmeRevision",
            "reviewedAt",
            "prescription",
        )
        val actual = StrengthEvidenceJson.readAcceptedSession(child(proposal, "actual"))
        val origin = choice<GymExecution.Origin>(proposal, "origin")
        val programmeId = text(proposal, "programmeId", 256)
        val revision = integer(proposal, "programmeRevision")
        val reviewed = StoredInstant.parse(text(proposal, "reviewedAt", 64))
        val frozen =
            if (format == 2)
                GymWorkoutAssociation.Proposal.authored(
                    actual,
                    origin,
                    programmeId,
                    revision,
                    StrengthProgrammingPayloadJson.readWorkout(child(proposal, "prescription")),
                    reviewed,
                )
            else
                GymWorkoutAssociation.Proposal(
                    actual,
                    origin,
                    programmeId,
                    revision,
                    readLegacySession(child(proposal, "prescription")),
                    reviewed,
                )
        return GymWorkoutAssociation.Decision(
            text(root, "id", 256),
            frozen,
            choice<GymWorkoutAssociation.Choice>(root, "choice"),
            StoredInstant.parse(text(root, "decidedAt", 64)),
        )
    }

    private fun legacySession(session: GymProgramme.Session): JsonObject = buildJsonObject {
        put("id", session.id())
        put("date", session.date().toString())
        put("title", session.title())
        put("explanation", session.explanation())
        put(
            "exercises",
            buildJsonArray {
                for (p in session.exercises()) add(
                    buildJsonObject {
                        put("exerciseId", p.exerciseId())
                        put("sets", p.sets())
                        put("minReps", p.minimumReps())
                        put("maxReps", p.maximumReps())
                        put("seconds", p.seconds()?.let { JsonPrimitive(it) } ?: JsonNull)
                        put("metres", StoredJson.nullable(p.metres()))
                        put("minRpe", p.minimumRpe())
                        put("maxRpe", p.maximumRpe())
                        put("rest", p.restSeconds())
                        put("intent", p.intent())
                        put("referenceKg", StoredJson.nullable(p.referenceLoadKg()))
                    }
                )
            },
        )
    }

    private fun readLegacySession(session: JsonObject): GymProgramme.Session {
        fields(session, "id", "date", "title", "explanation", "exercises")
        val exercises =
            array(session, "exercises", 10).map {
                val p = row(it)
                fields(
                    p,
                    "exerciseId",
                    "sets",
                    "minReps",
                    "maxReps",
                    "seconds",
                    "metres",
                    "minRpe",
                    "maxRpe",
                    "rest",
                    "intent",
                    "referenceKg",
                )
                GymProgramme.Prescription(
                    text(p, "exerciseId", 256),
                    integer(p, "sets"),
                    integer(p, "minReps"),
                    integer(p, "maxReps"),
                    nullableInteger(p, "seconds"),
                    nullableNumber(p, "metres"),
                    integer(p, "minRpe"),
                    integer(p, "maxRpe"),
                    integer(p, "rest"),
                    text(p, "intent", 10000),
                    nullableNumber(p, "referenceKg"),
                )
            }
        return GymProgramme.Session(
            text(session, "id", 256),
            StoredDate.parse(text(session, "date", 32)),
            text(session, "title", 120),
            exercises,
            text(session, "explanation", 10000),
        )
    }
}
