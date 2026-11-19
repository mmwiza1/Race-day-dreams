package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.write

/**
 * Existing frozen workout row and repeat receipts. Hashes are retained exactly, never regenerated
 * from differently formatted JSON. Linked source content still needs transactional validation.
 */
object FrozenGymWorkoutRecordJson {
    enum class TemplateKind {
        ACTUAL,
        EXECUTION,
    }

    data class RepeatProof(
        val templateKind: TemplateKind,
        val originalSourceSlots: List<GymWorkoutDefinition.Slot>,
        val originalSourceFingerprint: String,
        val currentSourceFingerprint: String?,
        val sourceEditedAt: Instant?,
        val sourceDeletedAt: Instant?,
    )

    data class Record(
        val definition: GymWorkoutDefinition,
        val source: GymExecution.SourceReference?,
        val sourceSnapshot: RepeatProof?,
    )

    fun encode(record: Record): String = write(element(record))

    fun element(record: Record): JsonObject {
        validate(record)
        return buildJsonObject {
            put("definition", FrozenGymWorkoutJson.element(record.definition))
            put("source", record.source?.let { GymExecutionSnapshotJson.source(it) } ?: JsonNull)
            val proof = record.sourceSnapshot
            put(
                "sourceSnapshot",
                if (proof == null) JsonNull
                else
                    buildJsonObject {
                        put("kind", "REPEATED_TEMPLATE")
                        put("actualSessionId", record.source!!.id())
                        put("definition", FrozenGymWorkoutJson.element(record.definition))
                        put("templateKind", proof.templateKind.name)
                        put(
                            "originalSourceSlots",
                            FrozenGymWorkoutJson.slots(proof.originalSourceSlots),
                        )
                        put("originalSourceFingerprint", proof.originalSourceFingerprint)
                        put(
                            "currentSourceFingerprint",
                            proof.currentSourceFingerprint?.let { JsonPrimitive(it) } ?: JsonNull,
                        )
                        put(
                            "sourceEditedAt",
                            proof.sourceEditedAt?.let { JsonPrimitive(it.toString()) } ?: JsonNull,
                        )
                        put(
                            "sourceDeletedAt",
                            proof.sourceDeletedAt?.let { JsonPrimitive(it.toString()) } ?: JsonNull,
                        )
                    },
            )
        }
    }

    fun decode(payload: String): Record = decode(read(payload))

    /** Parent syntax/size must be checked before decoding this already-bounded DOM. */
    fun decode(root: JsonObject): Record {
        fields(root, "definition", "source", "sourceSnapshot")
        val definition = FrozenGymWorkoutJson.decode(child(root, "definition"))
        val source =
            if (isNull(root, "source")) null
            else GymExecutionSnapshotJson.readSource(child(root, "source"))
        val proof =
            if (isNull(root, "sourceSnapshot")) null
            else {
                val p = child(root, "sourceSnapshot")
                fields(
                    p,
                    "kind",
                    "actualSessionId",
                    "definition",
                    "templateKind",
                    "originalSourceSlots",
                    "originalSourceFingerprint",
                    "currentSourceFingerprint",
                    "sourceEditedAt",
                    "sourceDeletedAt",
                )
                require(
                    text(p, "kind", 64) == "REPEATED_TEMPLATE" &&
                        text(p, "actualSessionId", 256) == source?.id() &&
                        FrozenGymWorkoutJson.decode(child(p, "definition")) == definition
                ) {
                    "Repeated-source evidence differs from its frozen template."
                }
                RepeatProof(
                    choice<TemplateKind>(p, "templateKind"),
                    FrozenGymWorkoutJson.readSlots(array(p, "originalSourceSlots", 100)),
                    text(p, "originalSourceFingerprint", 64),
                    if (isNull(p, "currentSourceFingerprint")) null
                    else text(p, "currentSourceFingerprint", 64),
                    if (isNull(p, "sourceEditedAt")) null
                    else StoredInstant.parse(text(p, "sourceEditedAt", 64)),
                    if (isNull(p, "sourceDeletedAt")) null
                    else StoredInstant.parse(text(p, "sourceDeletedAt", 64)),
                )
            }
        return Record(definition, source, proof).also { validate(it) }
    }

    internal fun validateAvailable(
        row: Record,
        actual: StrengthSession?,
        execution: GymExecution?,
        fingerprints: LegacyStrengthFingerprint,
    ) {
        validate(row)
        val proof = requireNotNull(row.sourceSnapshot)
        if (proof.sourceDeletedAt != null) {
            require(actual == null && execution == null) {
                "A deleted repeat source still appears as retained evidence."
            }
            return
        }
        require(
            actual != null &&
                compatibleFingerprint(actual, fingerprints) == proof.currentSourceFingerprint
        ) {
            "The repeat source changed without an explicit retained decision."
        }
        if (proof.templateKind == TemplateKind.EXECUTION) {
            require(execution != null && execution.slots() == proof.originalSourceSlots) {
                "The original repeat structure changed."
            }
            GymWorkoutSourceValidator.repeatedExecution(row.definition, row.source, execution)
        } else if (proof.sourceEditedAt == null)
            GymWorkoutSourceValidator.repeatedActual(row.definition, row.source, actual)
    }

    internal fun compatibleFingerprint(
        actual: StrengthSession,
        fingerprints: LegacyStrengthFingerprint,
    ): String =
        fingerprints.fingerprint(actual).also {
            require(Regex("[0-9a-f]{64}").matches(it)) { "Invalid compatible source fingerprint." }
        }

    private fun validate(record: Record) {
        require(record.source?.kind() != GymExecution.SourceKind.SAVED_WORKOUT) {
            "A saved revision cannot create itself as a source."
        }
        if (record.source?.kind() != GymExecution.SourceKind.ACTUAL_SESSION) {
            require(record.sourceSnapshot == null) { "Unexpected repeated-source evidence." }
            return
        }
        val proof =
            requireNotNull(record.sourceSnapshot) { "The frozen repeat receipt is required." }
        val expected =
            proof.originalSourceSlots.mapIndexed { i, s ->
                if (proof.templateKind == TemplateKind.ACTUAL)
                    require(
                        s.target() == null && s.supersetGroup() == null && s.id() == "slot-$i"
                    ) {
                        "An actual repeat must preserve its original exercise order without invented targets."
                    }
                s.copy(target = s.target()?.withoutLoadReference())
            }
        require(expected == record.definition.slots()) {
            "The repeated definition differs from its frozen source slots."
        }
        requireFingerprint(proof.originalSourceFingerprint)
        if (proof.sourceDeletedAt == null) {
            requireFingerprint(proof.currentSourceFingerprint)
            require(
                proof.sourceEditedAt != null ||
                    proof.originalSourceFingerprint == proof.currentSourceFingerprint
            ) {
                "Changed repeat evidence needs an explicit retained edit."
            }
        } else
            require(proof.currentSourceFingerprint == null) {
                "Deleted source evidence cannot claim a current fingerprint."
            }
    }

    private fun requireFingerprint(value: String?) {
        require(value != null && Regex("[0-9a-f]{64}").matches(value)) {
            "Invalid retained source fingerprint."
        }
    }
}
