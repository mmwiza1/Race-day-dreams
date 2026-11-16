package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Choice
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Decision
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version

/** Original association graph validation before replacement; no destructive restore API. */
object GymAssociationArchiveJson {
    data class Archive(val decisions: List<Decision>, val current: Map<String, String>)

    data class Link(
        val actualId: String,
        val programmeKey: String?,
        val sessionId: String?,
        val plannedDate: String,
    )

    /** Standalone gym payloads retain their existing 16 MiB syntax budget. */
    fun encode(archive: Archive): String = GymStoredJson.write(element(archive)).also { decode(it) }

    /**
     * The enclosing validated backup owns its total budget; do not impose a second 16 MiB limit.
     */
    fun element(archive: Archive): JsonObject {
        require(archive.decisions.size <= 100000 && archive.current.size <= 100000) {
            "Too many association rows."
        }
        return buildJsonObject {
                put("version", 1)
                put(
                    "decisions",
                    JsonArray(
                        archive.decisions
                            .sortedBy { it.id() }
                            .map { read(GymAssociationDecisionJson.encode(it)) }
                    ),
                )
                put(
                    "current",
                    JsonArray(
                        archive.current.entries
                            .sortedBy { it.key }
                            .map { (id, decision) ->
                                buildJsonObject {
                                    put("actualId", id)
                                    put("decisionId", decision)
                                }
                            }
                    ),
                )
            }
            .also { decode(it) }
    }

    fun decode(payload: String): Archive = decode(read(payload))

    /** Nested archives arrive only after the enclosing backup's syntax/size validation. */
    fun decode(root: JsonObject): Archive {
        fields(root, "version", "decisions", "current")
        version(root, 1)
        val ids = mutableSetOf<String>()
        val decisions =
            array(root, "decisions", 100000).map {
                val encoded = row(it).toString()
                require(encoded.encodeToByteArray().size <= 524288) {
                    "Association snapshot is too large."
                }
                GymAssociationDecisionJson.decode(encoded).also {
                    require(ids.add(it.id())) { "Duplicated association decision." }
                }
            }
        val current = linkedMapOf<String, String>()
        for (value in array(root, "current", 100000)) {
            val r = row(value)
            fields(r, "actualId", "decisionId")
            require(current.put(text(r, "actualId", 256), text(r, "decisionId", 256)) == null) {
                "Duplicated current association."
            }
        }
        return Archive(decisions, current)
    }

    fun validate(
        archive: Archive,
        executions: Map<String, GymExecution>,
        actuals: Map<String, StrengthSession>,
        programmes: Map<String, GymProgramme>,
        links: Map<String, Link>,
        authored: Map<String, StrengthProgramme> = emptyMap(),
    ) {
        // Enforce syntax/row bounds even when an in-memory archive did not come through decode.
        element(archive)
        val decisions = mutableMapOf<String, Decision>()
        val actualIds = mutableSetOf<String>()
        for (d in archive.decisions) {
            val p = d.proposal()
            val id = p.actual().id()
            val e = executions[id]
            val key = "${p.programmeId()}:v${p.programmeRevision()}"
            require(
                decisions.put(d.id(), d) == null &&
                    id in actuals &&
                    e != null &&
                    e.status() == GymExecution.Status.FINISHED &&
                    e.origin() == p.origin() &&
                    if (p.authored()) key in authored else key in programmes
            ) {
                "Association history has a missing or inconsistent source."
            }
            if (p.authored()) {
                val plan = authored[key]
                GymWorkoutAssociation.validateAuthoredRetainedEvidence(
                    p,
                    p.actual(),
                    e!!.origin(),
                    plan,
                )
                if (d.choice() == Choice.COUNT_TOWARD_SESSION)
                    require(
                        p in
                            GymWorkoutAssociation.authoredProposals(
                                p.actual(),
                                p.origin(),
                                plan,
                                emptySet(),
                                p.reviewedAt(),
                            )
                    ) {
                        "No comparable nearby prescription was reviewed."
                    }
            } else {
                val plan = programmes[key]
                GymWorkoutAssociation.validateRetainedEvidence(p, p.actual(), e!!.origin(), plan)
                if (d.choice() == Choice.COUNT_TOWARD_SESSION)
                    require(
                        p in
                            GymWorkoutAssociation.proposals(
                                p.actual(),
                                p.origin(),
                                plan,
                                emptySet(),
                                p.reviewedAt(),
                            )
                    ) {
                        "No comparable nearby prescription was reviewed."
                    }
            }
            actualIds.add(id)
        }
        val occupied = mutableSetOf<Pair<String, String>>()
        for ((id, decisionId) in archive.current) {
            val d = decisions[decisionId]
            require(d != null && id == d.proposal().actual().id()) {
                "Invalid current association."
            }
            val link = links[id]
            if (d.choice() == Choice.KEEP_EXTRA)
                require(link == null) { "An extra-workout choice cannot claim a programme link." }
            else {
                val p = d.proposal()
                val key = "${p.programmeId()}:v${p.programmeRevision()}"
                require(
                    occupied.add(key to p.sessionId()) &&
                        link == Link(id, key, p.sessionId(), p.date().toString())
                ) {
                    "Association target is duplicated or differs from its retained link."
                }
            }
        }
        require(archive.current.keys == actualIds) {
            "Association history needs its current explicit choice."
        }
    }
}
