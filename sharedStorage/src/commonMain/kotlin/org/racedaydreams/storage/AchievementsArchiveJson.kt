package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Offsets
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.bool
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.nullableInteger
import org.racedaydreams.storage.GymStoredJson.nullableNumber
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version

/** Retained milestone proof, never a request to recalculate awards or trigger celebrations. */
object AchievementsArchiveJson {
    class Snapshot(val celebrations: Boolean, awards: List<SqliteTrainingMilestones.Stored>) {
        val awards = Immutable.copyList(awards)

        init {
            require(this.awards.map { it.award.kind() }.toSet().size == this.awards.size) {
                "Duplicate milestone."
            }
        }
    }

    fun decode(input: JsonObject?): Snapshot {
        if (input == null) return Snapshot(false, emptyList())
        fields(input, "version", "celebrations", "awards")
        version(input, 1)
        val awards =
            array(input, "awards", Kind.entries.size).map { element ->
                val r = row(element)
                fields(r, "kind", "earnedAt", "recognizedAt", "evidence")
                val sources =
                    array(r, "evidence", 50).map { item ->
                        val p = row(item)
                        fields(p, "source", "id", "at", "offset", "distance")
                        Proof(
                            choice<Source>(p, "source"),
                            text(p, "id", 256),
                            StoredInstant.parse(text(p, "at", 128)),
                            nullableInteger(p, "offset")?.let { Offsets.ofTotalSeconds(it) },
                            nullableNumber(p, "distance"),
                        )
                    }
                SqliteTrainingMilestones.Stored(
                    Award(
                        choice<Kind>(r, "kind"),
                        StoredInstant.parse(text(r, "earnedAt", 128)),
                        sources,
                    ),
                    StoredInstant.parse(text(r, "recognizedAt", 128)),
                )
            }
        return Snapshot(bool(input, "celebrations"), awards)
    }

    fun element(snapshot: Snapshot): JsonObject =
        buildJsonObject {
                put("version", 1)
                put("celebrations", snapshot.celebrations)
                put(
                    "awards",
                    buildJsonArray {
                        for (r in snapshot.awards) add(
                            buildJsonObject {
                                val a = r.award
                                put("kind", a.kind().name)
                                put("earnedAt", a.earnedAt().toString())
                                put("recognizedAt", r.recognizedAt.toString())
                                put(
                                    "evidence",
                                    buildJsonArray {
                                        for (p in a.evidence()) add(
                                            buildJsonObject {
                                                put("source", p.source().name)
                                                put("id", p.recordId())
                                                put("at", p.at().toString())
                                                put(
                                                    "offset",
                                                    p.offset()?.let {
                                                        JsonPrimitive(it.getTotalSeconds())
                                                    } ?: JsonNull,
                                                )
                                                put(
                                                    "distance",
                                                    StoredJson.nullable(p.distanceMetres()),
                                                )
                                            }
                                        )
                                    },
                                )
                            }
                        )
                    },
                )
            }
            .also { decode(it) }
}
