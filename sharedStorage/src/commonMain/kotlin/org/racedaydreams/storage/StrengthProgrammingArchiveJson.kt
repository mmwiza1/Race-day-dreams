package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.write

/** Original v1/v2 authoring graph guards. Validating proposals is never permission to activate. */
object StrengthProgrammingArchiveJson {
    data class Snapshot(
        val programmes: Map<String, StrengthProgramme>,
        val drafts: List<StrengthProgrammeDraft>,
        val concreteDrafts: List<ConcreteStrengthProgrammeDraft>,
        val custom: List<CustomStrengthExercise>,
        val decisions: List<Decision>,
        val current: Reference?,
    )

    /** The enclosing backup validator must validate its legacy archive before supplying context. */
    fun decode(
        payload: String,
        legacy: List<GymProgramme> = emptyList(),
        legacyCurrent: String? = null,
    ): Snapshot = decode(read(payload), legacy, legacyCurrent)

    /** Parent backup syntax/size and legacy context must be validated before supplying this DOM. */
    fun decode(
        json: JsonObject,
        legacy: List<GymProgramme> = emptyList(),
        legacyCurrent: String? = null,
    ): Snapshot {
        val version = integer(json, "version")
        when (version) {
            1 ->
                fields(
                    json,
                    "version",
                    "programmes",
                    "drafts",
                    "customExercises",
                    "decisions",
                    "current",
                )
            2 ->
                fields(
                    json,
                    "version",
                    "programmes",
                    "drafts",
                    "concreteDrafts",
                    "customExercises",
                    "decisions",
                    "current",
                )
            else ->
                throw IllegalArgumentException("Unsupported strength programming archive version.")
        }
        val legacyIds = legacy.map { it.id() }.toSet()
        val legacyKeys = legacy.map { "${it.id()}:v${it.version()}" }.toSet()
        require(legacyCurrent == null || legacyCurrent in legacyKeys) {
            "Missing legacy current programme."
        }
        val programmes = linkedMapOf<String, StrengthProgramme>()
        for (item in array(json, "programmes", 1000)) {
            val p = StrengthProgrammingPayloadJson.decode(row(item))
            validateRows(p)
            require(p.id() !in legacyIds && programmes.put(key(p), p) == null) {
                "Duplicate or ambiguous programme identity."
            }
        }
        val customIds = mutableSetOf<String>()
        val custom =
            array(json, "customExercises", 10000).map {
                val e = StrengthProgrammingPayloadJson.decodeCustom(it.toString())
                bounded(read(StrengthProgrammingPayloadJson.encodeCustom(e)), 65536)
                require(customIds.add(e.exercise().id())) { "Duplicate custom exercise." }
                e
            }
        fun checkExercises(p: StrengthProgramme) {
            for (w in p.workouts()) for (s in w.definition().slots()) require(
                ExerciseCatalog.definition(s.exercise().id()) != null ||
                    s.exercise().id() in customIds
            ) {
                "Programme custom metadata reference."
            }
        }
        for (p in programmes.values) checkExercises(p)
        val draftIds = mutableSetOf<String>()
        val drafts =
            array(json, "drafts", 1000).map {
                val d = StrengthProgrammingPayloadJson.decodeDraft(it.toString())
                validateRows(d)
                require(
                    draftIds.add(d.id()) &&
                        d.programmeId() !in legacyIds &&
                        (d.baseRevision() == 0 ||
                            "${d.programmeId()}:v${d.baseRevision()}" in programmes)
                ) {
                    "Draft identity or base revision."
                }
                for (b in d.blocks()) for (day in b.days()) for (s in day.slots()) require(
                    ExerciseCatalog.definition(s.exercise().id()) != null ||
                        s.exercise().id() in customIds
                ) {
                    "Draft custom metadata reference."
                }
                d
            }
        fun checkRef(ref: Reference?) {
            require(
                ref == null ||
                    if (ref.owner() == Owner.AUTHORED) ref.key() in programmes
                    else ref.key() in legacyKeys
            ) {
                "Missing programme decision reference."
            }
        }
        val concreteIds = mutableSetOf<String>()
        val concreteDrafts =
            (if (version == 1) JsonArray(emptyList()) else array(json, "concreteDrafts", 1000))
                .map {
                    val d = ConcreteStrengthDraftPayloadJson.decode(it.toString())
                    validateRows(d)
                    require(concreteIds.add(d.id()) && d.programme().id() !in legacyIds) {
                        "Concrete draft identity."
                    }
                    checkRef(d.before())
                    if (d.programme().parentRevision() > 0)
                        d.validateAgainst(programmes.getValue(d.before()!!.key()))
                    // Competing saved previews remain proposals, even after another candidate is
                    // activated.
                    checkExercises(d.programme())
                    d
                }
        val decisionIds = mutableSetOf<String>()
        val activated = mutableSetOf<String>()
        var previous: Reference? = null
        val decisions =
            array(json, "decisions", 100000).mapIndexed { index, item ->
                val d = StrengthProgrammingDecisionJson.decode(row(item))
                bounded(StrengthProgrammingDecisionJson.element(d), 524288)
                require(decisionIds.add(d.id())) { "Duplicate programme decision." }
                checkRef(d.before())
                checkRef(d.after())
                require(index == 0 || previous == null || previous == d.before()) {
                    "Programme decision chain."
                }
                require(previous != null || d.before()?.owner() != Owner.AUTHORED) {
                    "Missing authored activation."
                }
                val after = d.after()
                if (after != null) {
                    val p = programmes.getValue(after.key())
                    require(!p.createdAt().isAfter(d.at()) && activated.add(key(p))) {
                        "Programme activation order."
                    }
                    if (p.revision() > 1) {
                        val base = programmes["${p.id()}:v${p.parentRevision()}"]
                        require(
                            base != null &&
                                Reference(Owner.AUTHORED, base.id(), base.revision()) == d.before()
                        ) {
                            "Programme revision lineage."
                        }
                        StrengthProgrammeEditor.Proposal(
                            base,
                            p,
                            d.protectedWorkoutIds().toSet(),
                            d.substitutions(),
                        )
                    } else
                        require(d.protectedWorkoutIds().isEmpty() && d.substitutions().isEmpty()) {
                            "Initial programme cannot claim earlier substitution evidence."
                        }
                } else
                    require(d.protectedWorkoutIds().isEmpty() && d.substitutions().isEmpty()) {
                        "Stop cannot rewrite a prescription."
                    }
                previous = after
                d
            }
        val current =
            if (isNull(json, "current")) null
            else ConcreteStrengthDraftPayloadJson.readReference(child(json, "current"))
        require(
            (current == null || current.owner() == Owner.AUTHORED) &&
                current == previous &&
                activated == programmes.keys
        ) {
            "Current programme or orphan revision."
        }
        require(current == null || legacyCurrent == null) { "Two active strength programmes." }
        return Snapshot(programmes.toMap(), drafts, concreteDrafts, custom, decisions, current)
    }

    fun encode(snapshot: Snapshot): String = write(element(snapshot))

    /** Nested export uses the enclosing backup budget, without truncating retained revisions. */
    fun element(snapshot: Snapshot): JsonObject = buildJsonObject {
        put("version", 2)
        put(
            "programmes",
            JsonArray(snapshot.programmes.values.map { StrengthProgrammingPayloadJson.element(it) }),
        )
        put(
            "drafts",
            JsonArray(snapshot.drafts.map { read(StrengthProgrammingPayloadJson.encodeDraft(it)) }),
        )
        put(
            "concreteDrafts",
            JsonArray(
                snapshot.concreteDrafts.map { read(ConcreteStrengthDraftPayloadJson.encode(it)) }
            ),
        )
        put(
            "customExercises",
            JsonArray(snapshot.custom.map { read(StrengthProgrammingPayloadJson.encodeCustom(it)) }),
        )
        put(
            "decisions",
            JsonArray(snapshot.decisions.map { StrengthProgrammingDecisionJson.element(it) }),
        )
        put(
            "current",
            snapshot.current?.let { ConcreteStrengthDraftPayloadJson.reference(it) } ?: JsonNull,
        )
    }

    /** Separate cross-archive logging-definition consistency check, required before replacement. */
    fun validateCoaching(snapshot: Snapshot, definitions: Map<String, StrengthExercise>) {
        for (custom in snapshot.custom) require(
            custom.exercise() == definitions[custom.exercise().id()]
        ) {
            "Custom authoring and logging definitions disagree."
        }
    }

    internal fun validateRows(p: StrengthProgramme) {
        val root = StrengthProgrammingPayloadJson.element(p)
        bounded(JsonObject(root - "blocks"), 262144)
        for (b in array(root, "blocks", 12)) {
            val block = row(b)
            bounded(JsonObject(block - "workouts"), 262144)
            for (w in array(block, "workouts", 156)) bounded(row(w), 262144)
        }
    }

    internal fun validateRows(d: StrengthProgrammeDraft) {
        val root = read(StrengthProgrammingPayloadJson.encodeDraft(d))
        bounded(JsonObject(root - "blocks"), 262144)
        for (b in array(root, "blocks", 12)) bounded(row(b), 262144)
    }

    internal fun validateRows(draft: ConcreteStrengthProgrammeDraft) {
        validateRows(draft.programme())
        val root = read(ConcreteStrengthDraftPayloadJson.encode(draft))
        bounded(
            JsonObject(root + ("programme" to JsonObject(child(root, "programme") - "blocks"))),
            262144,
        )
    }

    private fun bounded(row: JsonObject, max: Int) {
        require(row.toString().encodeToByteArray().size <= max) {
            "Strength programming row is too large."
        }
    }

    private fun key(p: StrengthProgramme): String = "${p.id()}:v${p.revision()}"
}
