package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.bool
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.nullableText
import org.racedaydreams.storage.GymStoredJson.number
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version

/**
 * Original optional body archive. Reading measurements does not enable guidance or import health.
 */
object BodyMeasurementsArchiveJson {
    class Snapshot(heights: List<BodyHeight>, val preferences: Preferences) {
        val heights = Immutable.copyList(heights)

        init {
            require(this.heights.size <= 100000) {
                "Height history exceeds the format limit; nothing was truncated."
            }
            require(
                this.heights.map { it.id() }.toSet().size == this.heights.size &&
                    this.heights.map { it.provider() to it.sourceId() }.toSet().size ==
                        this.heights.size
            ) {
                "Duplicate height identity."
            }
        }
    }

    fun decode(input: JsonObject?): Snapshot {
        if (input == null) return Snapshot(emptyList(), Preferences.defaults())
        fields(input, "version", "heights", "preferences")
        version(input, 1)
        val heights =
            array(input, "heights", 100000).map { element ->
                val r = row(element)
                fields(
                    r,
                    "id",
                    "metres",
                    "at",
                    "offset",
                    "provider",
                    "sourceId",
                    "origin",
                    "importedAt",
                )
                BodyHeight(
                    text(r, "id", 256),
                    number(r, "metres"),
                    StoredInstant.parse(text(r, "at", 128)),
                    nullableText(r, "offset", 32)?.let { StoredOffset.parse(it) },
                    text(r, "provider", 256),
                    text(r, "sourceId", 512),
                    nullableText(r, "origin", 512),
                    nullableText(r, "importedAt", 128)?.let { StoredInstant.parse(it) },
                )
            }
        val p = child(input, "preferences")
        fields(p, "proteinEnabled", "heightUnit", "basis")
        val basis =
            if (isNull(p, "basis")) null
            else {
                val b = child(p, "basis")
                fields(b, "id", "at", "kg")
                ProteinGuidance.Basis(
                    text(b, "id", 256),
                    StoredInstant.parse(text(b, "at", 128)),
                    number(b, "kg"),
                )
            }
        return Snapshot(
            heights,
            Preferences(bool(p, "proteinEnabled"), choice<HeightUnit>(p, "heightUnit"), basis),
        )
    }

    fun element(snapshot: Snapshot): JsonObject =
        buildJsonObject {
                put("version", 1)
                put(
                    "heights",
                    buildJsonArray {
                        for (r in snapshot.heights) add(
                            buildJsonObject {
                                put("id", r.id())
                                put("metres", r.metres())
                                put("at", r.at().toString())
                                put("offset", StoredJson.nullable(r.offset()?.toString()))
                                put("provider", r.provider())
                                put("sourceId", r.sourceId())
                                put("origin", StoredJson.nullable(r.origin()))
                                put("importedAt", StoredJson.nullable(r.importedAt()?.toString()))
                            }
                        )
                    },
                )
                val p = snapshot.preferences
                put(
                    "preferences",
                    buildJsonObject {
                        put("proteinEnabled", p.proteinEnabled())
                        put("heightUnit", p.heightUnit().name)
                        put(
                            "basis",
                            p.basis()?.let { b ->
                                buildJsonObject {
                                    put("id", b.id())
                                    put("at", b.at().toString())
                                    put("kg", b.kilograms())
                                }
                            } ?: JsonNull,
                        )
                    },
                )
            }
            .also { decode(it) }
}
