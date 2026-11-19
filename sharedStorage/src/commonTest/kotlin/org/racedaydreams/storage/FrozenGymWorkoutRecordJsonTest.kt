package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class FrozenGymWorkoutRecordJsonTest {
    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val originalHash = "a".repeat(64)
    private val changedHash = "b".repeat(64)

    private fun source(kind: GymExecution.SourceKind = GymExecution.SourceKind.ACTUAL_SESSION) =
        GymExecution.SourceReference(
            kind,
            "actual",
            if (kind == GymExecution.SourceKind.ACTUAL_SESSION) 0 else 1,
            if (kind == GymExecution.SourceKind.PROGRAMME) "session" else null,
        )

    private fun actual(): FrozenGymWorkoutRecordJson.Record {
        val slots = listOf(GymWorkoutDefinition.Slot("slot-0", bench, null, null))
        return FrozenGymWorkoutRecordJson.Record(
            GymWorkoutDefinition("repeat", 1, at, "Repeat", slots),
            source(),
            FrozenGymWorkoutRecordJson.RepeatProof(
                FrozenGymWorkoutRecordJson.TemplateKind.ACTUAL,
                slots,
                originalHash,
                originalHash,
                null,
                null,
            ),
        )
    }

    private fun root(value: FrozenGymWorkoutRecordJson.Record = actual()) =
        Json.parseToJsonElement(FrozenGymWorkoutRecordJson.encode(value)).jsonObject

    private fun patchProof(vararg changes: Pair<String, JsonElement>): String {
        val root = root()
        return JsonObject(
                root +
                    ("sourceSnapshot" to
                        JsonObject(root.getValue("sourceSnapshot").jsonObject + changes))
            )
            .toString()
    }

    @Test
    fun sourceFreeAndProgrammeRecordsHaveNoRepeatReceiptAndSavedSourceCannotSelfCreate() {
        for (source in listOf(null, source(GymExecution.SourceKind.PROGRAMME))) {
            val value = actual().copy(source = source, sourceSnapshot = null)
            assertEquals(
                value,
                FrozenGymWorkoutRecordJson.decode(FrozenGymWorkoutRecordJson.encode(value)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.encode(
                actual()
                    .copy(
                        source = source(GymExecution.SourceKind.SAVED_WORKOUT),
                        sourceSnapshot = null,
                    )
            )
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.encode(actual().copy(source = null))
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.encode(actual().copy(sourceSnapshot = null))
        }
    }

    @Test
    fun actualRepeatPreservesFrozenHistoricalNamesAndExactOriginalFingerprints() {
        val original = actual()
        val slot =
            original.definition
                .slots()
                .single()
                .copy(exercise = bench.copy(name = "Recorded old name"))
        val value =
            original.copy(
                definition = original.definition.copy(slots = listOf(slot)),
                sourceSnapshot = original.sourceSnapshot!!.copy(originalSourceSlots = listOf(slot)),
            )
        assertEquals(
            value,
            FrozenGymWorkoutRecordJson.decode(FrozenGymWorkoutRecordJson.encode(value)),
        )
        assertEquals(
            originalHash,
            FrozenGymWorkoutRecordJson.decode(FrozenGymWorkoutRecordJson.encode(value))
                .sourceSnapshot!!
                .currentSourceFingerprint,
        )
    }

    @Test
    fun executionRepeatKeepsSupersetsAndEffortButDropsStartingLoadReferences() {
        val target =
            GymWorkoutDefinition.Target(
                2,
                4,
                6,
                null,
                null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.5, 4.5),
                180,
                82.25,
            )
        val originals =
            listOf(
                GymWorkoutDefinition.Slot("first", bench, target, "pair"),
                GymWorkoutDefinition.Slot(
                    "second",
                    ExerciseCatalog.definition("dumbbell-row")!!.exercise(),
                    target,
                    "pair",
                ),
            )
        val definition =
            actual()
                .definition
                .copy(
                    slots = originals.map { it.copy(target = it.target()!!.withoutLoadReference()) }
                )
        val proof =
            actual()
                .sourceSnapshot!!
                .copy(
                    templateKind = FrozenGymWorkoutRecordJson.TemplateKind.EXECUTION,
                    originalSourceSlots = originals,
                )
        val value = actual().copy(definition = definition, sourceSnapshot = proof)
        val restored = FrozenGymWorkoutRecordJson.decode(FrozenGymWorkoutRecordJson.encode(value))
        assertEquals(value, restored)
        assertEquals(
            82.25,
            restored.sourceSnapshot!!.originalSourceSlots[0].target()!!.referenceLoadKg(),
        )
        assertNull(restored.definition.slots()[0].target()!!.referenceLoadKg())
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.encode(
                value.copy(definition = definition.copy(slots = originals))
            )
        }
    }

    @Test
    fun explicitEditedAndDeletedReceiptsRemainDistinctWithoutRecomputingHashes() {
        val original = actual()
        val edited =
            original.copy(
                sourceSnapshot =
                    original.sourceSnapshot!!.copy(
                        currentSourceFingerprint = changedHash,
                        sourceEditedAt = at,
                    )
            )
        assertEquals(
            edited,
            FrozenGymWorkoutRecordJson.decode(FrozenGymWorkoutRecordJson.encode(edited)),
        )
        val deleted =
            edited.copy(
                sourceSnapshot =
                    edited.sourceSnapshot!!.copy(
                        currentSourceFingerprint = null,
                        sourceDeletedAt = at,
                    )
            )
        assertEquals(
            deleted,
            FrozenGymWorkoutRecordJson.decode(FrozenGymWorkoutRecordJson.encode(deleted)),
        )
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.encode(
                original.copy(
                    sourceSnapshot =
                        original.sourceSnapshot!!.copy(currentSourceFingerprint = changedHash)
                )
            )
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.encode(
                deleted.copy(
                    sourceSnapshot =
                        deleted.sourceSnapshot!!.copy(currentSourceFingerprint = changedHash)
                )
            )
        }
    }

    @Test
    fun actualTemplatesCannotInventTargetsGroupsOrExerciseOrder() {
        val original = actual()
        val target = GymWorkoutDefinition.Target(2, 4, 6, null, null, null, 90, null)
        for (slot in
            listOf(
                original.definition.slots()[0].copy(id = "wrong"),
                original.definition.slots()[0].copy(target = target),
            )) {
            val value =
                original.copy(
                    definition = original.definition.copy(slots = listOf(slot)),
                    sourceSnapshot =
                        original.sourceSnapshot!!.copy(originalSourceSlots = listOf(slot)),
                )
            assertFailsWith<IllegalArgumentException> { FrozenGymWorkoutRecordJson.encode(value) }
        }
    }

    @Test
    fun strictReceiptFieldsIdentityAndFingerprintSyntaxRejectBeforeAnySourceDecision() {
        val invalid =
            listOf(
                patchProof("actualSessionId" to JsonPrimitive("other")),
                patchProof("kind" to JsonPrimitive("OTHER")),
                patchProof("templateKind" to JsonPrimitive("OTHER")),
                patchProof("currentSourceFingerprint" to JsonNull),
                patchProof("currentSourceFingerprint" to JsonPrimitive("A".repeat(64))),
                patchProof("originalSourceFingerprint" to JsonPrimitive("a".repeat(63))),
                patchProof("sourceEditedAt" to JsonPrimitive("unknown")),
                patchProof("extra" to JsonNull),
                patchProof(
                    "definition" to
                        FrozenGymWorkoutJson.element(actual().definition.copy(title = "Different"))
                ),
            )
        for (payload in invalid) assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.decode(payload)
        }
        val root = root()
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.decode(JsonObject(root - "sourceSnapshot").toString())
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutRecordJson.decode(JsonObject(root + ("extra" to JsonNull)).toString())
        }
    }
}
