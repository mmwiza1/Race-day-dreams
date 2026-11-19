package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.StrengthProgramme
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.text

/**
 * Original legacy-gym v1 archive and cross-section references. No activation or actual creation.
 */
object LegacyGymProgrammeArchiveJson {
    class Snapshot
    internal constructor(
        programmes: Map<String, GymProgramme>,
        val current: String?,
        decisions: List<LegacyGymProgrammePayloadJson.Decision>,
        internal val payload: JsonObject,
    ) {
        val programmes = Immutable.copyMap(programmes)
        val decisions = Immutable.copyList(decisions)
    }

    /** Parent envelope syntax and its whole-backup budget must already have been checked. */
    fun decode(root: JsonObject?): Snapshot {
        if (root == null)
            return decode(
                buildJsonObject {
                    put("version", 1)
                    put("versions", JsonArray(emptyList()))
                    put("current", JsonNull)
                }
            )
        require(root.requiredInteger("version") == 1) { "Unsupported gym backup." }
        val programmes = linkedMapOf<String, GymProgramme>()
        for (item in root.array("versions", 1000)) {
            val p = LegacyGymProgrammePayloadJson.decode(item.row())
            require(programmes.put(key(p), p) == null) { "Duplicate gym plan version." }
        }
        val current = root.text("current")
        require(current == null || current in programmes) { "Current gym version is missing." }
        val ids = mutableSetOf<String>()
        val decisions =
            if ("decisions" !in root) emptyList()
            else
                root.array("decisions", 100000).map { item ->
                    val d = LegacyGymProgrammePayloadJson.decodeDecision(item.row())
                    require(
                        ids.add(d.id) &&
                            d.baseVersion in programmes &&
                            d.resultingVersion in programmes
                    ) {
                        "Progression version reference is missing."
                    }
                    d
                }
        return Snapshot(
            programmes,
            current,
            decisions,
            Json.parseToJsonElement(root.toString()).row(),
        )
    }

    /** Retains older absent decisions and unknown legacy fields without rewriting prescriptions. */
    fun element(snapshot: Snapshot): JsonObject = snapshot.payload.also { decode(it) }

    fun validateLinks(
        snapshot: Snapshot,
        development: DevelopmentArchiveJson.Snapshot?,
        actualIds: Set<String>,
        authored: Map<String, StrengthProgramme> = emptyMap(),
    ) {
        for (record in development?.records.orEmpty()) {
            val link = record.evidence as? DevelopmentArchiveJson.Evidence.StrengthLink ?: continue
            val version = link.gymVersion ?: continue // Old unassociated gym evidence stays legal.
            val modern = authored[version]
            val matches =
                if (modern != null)
                    modern.workouts().any {
                        it.id() == link.gymSession && it.date() == link.plannedDate
                    }
                else
                    snapshot.programmes[version]?.sessions()?.any {
                        it.id() == link.gymSession && it.date() == link.plannedDate
                    } == true
            require(record.id in actualIds && matches) {
                "Gym actual/prescription reference is missing."
            }
        }
    }

    internal fun key(programme: GymProgramme) = "${programme.id()}:v${programme.version()}"
}
