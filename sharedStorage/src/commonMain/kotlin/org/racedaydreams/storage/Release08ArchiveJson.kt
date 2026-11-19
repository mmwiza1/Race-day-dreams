package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.polish.CustomWorkout
import org.racedaydreams.domain.support.DreamRoutes
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.SupportPayloadJson.array
import org.racedaydreams.storage.SupportPayloadJson.bool
import org.racedaydreams.storage.SupportPayloadJson.integer
import org.racedaydreams.storage.SupportPayloadJson.text
import org.racedaydreams.storage.SupportPayloadJson.version

/** Original 0.8 records only; no new feature data is stored in the legacy bucket. */
object Release08ArchiveJson {
    enum class Kind(val wire: String) {
        CUSTOM("custom"),
        ORIGIN("origin"),
        ASSOCIATION("association"),
        CHECKLIST("checklist"),
        CONFIG("config"),
    }

    class Record
    internal constructor(
        val kind: Kind,
        val id: String,
        val payload: JsonObject,
        val custom: CustomWorkout?,
    )

    class Snapshot internal constructor(records: List<Record>) {
        val records = Immutable.copyList(records)
    }

    fun decode(root: JsonObject): Snapshot {
        version(root)
        val ids = mutableSetOf<Pair<Kind, String>>()
        return Snapshot(
            array(root, "rows", 100000).map { item ->
                val r = item.row()
                val kind =
                    Kind.entries.firstOrNull { it.wire == text(r, "kind") }
                        ?: error("Unsupported 0.8 kind.")
                val id = text(r, "id")
                require(id.length in 1..256 && ids.add(kind to id)) {
                    "Invalid or duplicate 0.8 identity."
                }
                val p = Json.parseToJsonElement(r.getValue("payload").row().toString()).row()
                version(p)
                val custom =
                    when (kind) {
                        Kind.CUSTOM -> {
                            require(text(p, "id") == id) { "Custom workout identity differs." }
                            val groups = array(p, "groups", 500)
                            require(groups.isNotEmpty())
                            CustomWorkout(
                                text(p, "id"),
                                text(p, "title"),
                                groups.map { g ->
                                    val group = g.row()
                                    CustomWorkout.Group(
                                        integer(group, "repeats"),
                                        SupportPayloadJson.steps(group),
                                    )
                                },
                            )
                        }
                        Kind.ORIGIN -> {
                            require(
                                text(p, "mode") in
                                    listOf("free", "quick", "custom", "repeat", "workout", "race")
                            )
                            StoredInstant.parse(text(p, "at"))
                            if ("dreamRoute" in p)
                                text(p, "dreamRoute")
                                    .takeIf { it.isNotBlank() && it != "null" }
                                    ?.let { DreamRoutes.forId(it) }
                            null
                        }
                        Kind.ASSOCIATION -> {
                            require(
                                listOf("activity", "planVersion", "workout").all {
                                    text(p, it).length in 1..256
                                } && text(p, "activity") == id
                            )
                            StoredInstant.parse(text(p, "at"))
                            null
                        }
                        Kind.CHECKLIST -> {
                            require(text(p, "label").length in 1..120)
                            bool(p, "checked")
                            null
                        }
                        Kind.CONFIG -> {
                            val type = text(p, "type")
                            require(
                                mapOf(
                                    "dream" to "dream",
                                    "schedule" to "schedule",
                                    "shoe-feedback" to "shoe_feedback",
                                    "start" to "start",
                                )[id] == type
                            )
                            when (type) {
                                "dream" -> {
                                    bool(p, "enabled")
                                    val cities =
                                        array(p, "cities", 5).map {
                                            (it as? JsonPrimitive)?.content ?: it.toString()
                                        }
                                    require(
                                        cities.distinct().size == cities.size &&
                                            cities.all { c ->
                                                DreamRoutes.routes().any { it.id() == c }
                                            }
                                    )
                                }
                                "shoe_feedback" -> {
                                    val activities =
                                        array(p, "activities", 10000).map {
                                            (it as? JsonPrimitive)?.content ?: it.toString()
                                        }
                                    require(
                                        activities.distinct().size == activities.size &&
                                            activities.all { it.length in 1..256 }
                                    )
                                }
                                "schedule" -> {
                                    require(
                                        integer(p, "maximumMinutes") in 15..360 &&
                                            text(p, "distribution") in
                                                listOf("FEWER_LONGER", "BALANCED", "MORE_SHORTER")
                                    )
                                    for (d in array(p, "unavailable", 366)) StoredDate.parse(
                                        (d as? JsonPrimitive)?.content ?: d.toString()
                                    )
                                }
                                "start" ->
                                    require(text(p, "path") in listOf("record", "race", "hybrid"))
                                else -> error("Unsupported preference.")
                            }
                            null
                        }
                    }
                Record(kind, id, p, custom)
            }
        )
    }

    fun element(snapshot: Snapshot): JsonObject =
        buildJsonObject {
                put("version", 1)
                put(
                    "rows",
                    JsonArray(
                        snapshot.records.map { r ->
                            buildJsonObject {
                                put("kind", r.kind.wire)
                                put("id", r.id)
                                put("payload", r.payload)
                            }
                        }
                    ),
                )
            }
            .also { decode(it) }
}
