package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.domain.time.*

class StrengthProgrammingArchiveJsonTest {
    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = CalendarDates.of(2026, 11, 17)
    private val ref = Reference(Owner.AUTHORED, "programme", 1)

    private fun programme(): StrengthProgramme {
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.TUESDAY),
                1,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
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
        val workouts =
            List(2) { i ->
                val id = "workout-$i"
                StrengthProgramme.Workout(
                    id,
                    start.plusWeeks(i.toLong()),
                    30,
                    GymWorkoutDefinition(
                        id,
                        1,
                        at,
                        "Upper",
                        listOf(
                            GymWorkoutDefinition.Slot(
                                "slot",
                                ExerciseCatalog.definition("bench-press")!!.exercise(),
                                target,
                                null,
                            )
                        ),
                    ),
                    null,
                )
            }
        return StrengthProgramme(
            "programme",
            1,
            0,
            at,
            "Original",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings,
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    start,
                    2,
                    workouts,
                    "Original explanation",
                )
            ),
            start,
            "Athlete authored",
            emptyList(),
        )
    }

    private fun decision() =
        Decision("activate", null, ref, at, "Athlete chose", emptyList(), emptyList())

    private fun snapshot() =
        StrengthProgrammingArchiveJson.Snapshot(
            mapOf(ref.key() to programme()),
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(decision()),
            ref,
        )

    private fun json(s: StrengthProgrammingArchiveJson.Snapshot = snapshot()) =
        Json.parseToJsonElement(StrengthProgrammingArchiveJson.encode(s)).jsonObject

    private fun decode(j: JsonObject) = StrengthProgrammingArchiveJson.decode(j.toString())

    private fun forward(): ConcreteStrengthProgrammeDraft {
        val p = programme()
        val old = p.workouts()[1]
        val slot = old.definition().slots().single()
        val replacement = ExerciseCatalog.definition("dumbbell-bench-press")!!.exercise()
        val updated =
            old.copy(
                definition =
                    old.definition()
                        .copy(
                            revision = 2,
                            slots =
                                listOf(
                                    slot.copy(
                                        exercise = replacement,
                                        target = slot.target()!!.withoutLoadReference(),
                                    )
                                ),
                        )
            )
        return ConcreteStrengthProgrammeDraft(
            "draft",
            5,
            ref,
            p.copy(
                revision = 2,
                parentRevision = 1,
                blocks =
                    listOf(p.blocks().single().copy(workouts = listOf(p.workouts()[0], updated))),
                effectiveFrom = old.date(),
                reason = "Athlete chose an alternative",
            ),
            listOf(
                StrengthProgrammeEditor.Change(
                    old.id(),
                    slot.id(),
                    slot.exercise().id(),
                    replacement.id(),
                    "Equipment unavailable",
                )
            ),
        )
    }

    @Test
    fun bothArchiveVersionsPreserveActivatedHistoryAndUnacceptedConcretePreviews() {
        val expected = snapshot()
        assertEquals(expected, decode(json()))
        assertEquals(
            expected,
            decode(JsonObject((json() - "concreteDrafts") + ("version" to JsonPrimitive(1)))),
        )
        val d = forward()
        val proposed = expected.copy(concreteDrafts = listOf(d))
        assertEquals(proposed, decode(json(proposed)))
        assertEquals(1, decode(json(proposed)).programmes.size)
        assertEquals(ref, decode(json(proposed)).current)
        assertNull(
            decode(json(proposed))
                .concreteDrafts
                .single()
                .programme()
                .workouts()[1]
                .definition()
                .slots()
                .single()
                .target()!!
                .referenceLoadKg()
        )
    }

    @Test
    fun permanentSubstitutionPreservesProtectedWorkoutAndSeparateDecisionProvenance() {
        val d = forward()
        val after = Reference(Owner.AUTHORED, "programme", 2)
        val choice =
            Decision(
                "swap",
                ref,
                after,
                at,
                "Athlete accepted",
                listOf("workout-0"),
                d.substitutions(),
            )
        val s =
            snapshot()
                .copy(
                    programmes =
                        linkedMapOf(ref.key() to programme(), after.key() to d.programme()),
                    decisions = listOf(decision(), choice),
                    current = after,
                )
        assertEquals(s, decode(json(s)))
        val historical = d.programme().workouts()[0]
        val altered =
            historical.copy(definition = historical.definition().copy(title = "Retroactive change"))
        val bad =
            d.programme()
                .copy(
                    blocks =
                        listOf(
                            d.programme()
                                .blocks()
                                .single()
                                .copy(workouts = listOf(altered, d.programme().workouts()[1]))
                        )
                )
        assertFailsWith<IllegalArgumentException> {
            decode(json(s.copy(programmes = s.programmes + (after.key() to bad))))
        }
    }

    @Test
    fun missingOrDuplicateRevisionsAndCurrentPointerMismatchReject() {
        val root = json()
        val p = root.getValue("programmes").jsonArray.single()
        val bad =
            listOf(
                JsonObject(root + ("programmes" to JsonArray(listOf(p, p)))),
                JsonObject(root + ("decisions" to JsonArray(emptyList()))),
                JsonObject(root + ("current" to JsonNull)),
                JsonObject(root + ("programmes" to JsonArray(emptyList()))),
            )
        for (r in bad) assertFailsWith<IllegalArgumentException> { decode(r) }
        assertEquals(snapshot(), decode(json()))
    }

    @Test
    fun decisionChainActivationTimeDuplicateIdentityAndOrphanRevisionsReject() {
        val d = decision()
        val cases =
            listOf(
                snapshot().copy(decisions = listOf(d, d)),
                snapshot()
                    .copy(
                        decisions = listOf(d.copy(at = StoredInstant.parse("2026-11-16T08:59:59Z")))
                    ),
                snapshot().copy(decisions = listOf(d.copy(before = ref))),
                snapshot()
                    .copy(
                        decisions = listOf(d.copy(after = Reference(Owner.AUTHORED, "missing", 1)))
                    ),
            )
        for (s in cases) assertFailsWith<IllegalArgumentException> { decode(json(s)) }
        val stopped =
            snapshot()
                .copy(
                    decisions =
                        listOf(
                            d,
                            Decision(
                                "stop",
                                ref,
                                null,
                                at,
                                "Athlete stopped",
                                emptyList(),
                                emptyList(),
                            ),
                        ),
                    current = null,
                )
        assertEquals(stopped, decode(json(stopped)))
        val invalidStop =
            stopped.copy(
                decisions =
                    listOf(
                        d,
                        stopped.decisions.last().copy(protectedWorkoutIds = listOf("workout-0")),
                    )
            )
        assertFailsWith<IllegalArgumentException> { decode(json(invalidStop)) }
    }

    @Test
    fun competingSavedPreviewsNeverReplaceAnActivatedRevision() {
        val d = forward()
        val after = Reference(Owner.AUTHORED, "programme", 2)
        val accepted =
            snapshot()
                .copy(
                    programmes =
                        linkedMapOf(ref.key() to programme(), after.key() to d.programme()),
                    decisions =
                        listOf(
                            decision(),
                            Decision(
                                "swap",
                                ref,
                                after,
                                at,
                                "Athlete accepted",
                                listOf("workout-0"),
                                d.substitutions(),
                            ),
                        ),
                    current = after,
                    concreteDrafts =
                        listOf(
                            d,
                            d.copy(
                                id = "other-preview",
                                programme = d.programme().copy(name = "Competing saved preview"),
                            ),
                        ),
                )
        assertEquals(accepted, decode(json(accepted)))
        assertEquals("Original", decode(json(accepted)).programmes.getValue(after.key()).name())
        assertFailsWith<IllegalArgumentException> {
            decode(json(accepted.copy(concreteDrafts = listOf(d, d))))
        }
    }

    @Test
    fun concreteBaseAndCustomMetadataMustExistButCustomTechniqueIsNeverInvented() {
        val d = forward()
        assertFailsWith<IllegalArgumentException> {
            decode(
                json(
                    snapshot()
                        .copy(
                            programmes = emptyMap(),
                            decisions = emptyList(),
                            current = null,
                            concreteDrafts = listOf(d),
                        )
                )
            )
        }
        val p = programme()
        val exercise =
            p.workouts()[1]
                .definition()
                .slots()
                .single()
                .exercise()
                .copy(id = "custom-unregistered", name = "Custom movement")
        val customWorkout =
            p.workouts()[1].copy(
                definition =
                    p.workouts()[1]
                        .definition()
                        .copy(
                            slots =
                                listOf(
                                    p.workouts()[1]
                                        .definition()
                                        .slots()
                                        .single()
                                        .copy(exercise = exercise)
                                )
                        )
            )
        val bad =
            p.copy(
                blocks =
                    listOf(
                        p.blocks().single().copy(workouts = listOf(p.workouts()[0], customWorkout))
                    )
            )
        assertFailsWith<IllegalArgumentException> {
            decode(json(snapshot().copy(programmes = mapOf(ref.key() to bad))))
        }
    }

    @Test
    fun independentlySavedInitialDraftsNeedNoActivationAndVersionOneHasNoConcreteDrafts() {
        val initial =
            ConcreteStrengthProgrammeDraft("new-preview", 3, null, programme(), emptyList())
        val empty =
            snapshot()
                .copy(
                    programmes = emptyMap(),
                    decisions = emptyList(),
                    current = null,
                    concreteDrafts = listOf(initial),
                )
        assertEquals(empty, decode(json(empty)))
        assertFailsWith<IllegalArgumentException> {
            decode(JsonObject(json() + ("version" to JsonPrimitive(1))))
        }
        assertFailsWith<IllegalArgumentException> {
            decode(JsonObject(json() + ("version" to JsonPrimitive(3))))
        }
    }

    @Test
    fun decisionParserPreservesNanosecondsAndRejectsDuplicateBlankUnknownOrCoercedFields() {
        val d =
            Decision(
                "swap",
                ref,
                Reference(Owner.AUTHORED, "programme", 2),
                at,
                "Athlete reviewed",
                listOf("workout-0", "\u00a0"),
                forward().substitutions(),
            )
        val payload = StrengthProgrammingDecisionJson.encode(d)
        assertEquals(d, StrengthProgrammingDecisionJson.decode(payload))
        for (bad in
            listOf(
                payload.replace("\"version\":1", "\"version\":1e0"),
                payload.replace("\"version\":1", "\"version\":1,\"extra\":0"),
                payload.replace("\"version\":1", "\"version\":1,\"version\":1"),
                payload.replace("workout-0", "\u2003"),
                payload.replace("[\"workout-0\",\"\u00a0\"]", "[\"workout-0\",\"workout-0\"]"),
            )) assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingDecisionJson.decode(bad)
        }
    }

    private fun legacy(id: String = "legacy") =
        GymProgramme(
            id,
            1,
            at,
            "Original",
            GymProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.BEGINNER,
                setOf(DayOfWeek.TUESDAY),
                1,
                setOf("full gym"),
                60,
                GymProgramme.Effort.RPE,
            ),
            start,
            start.plusDays(41),
            emptyList(),
            emptyList(),
        )

    @Test
    fun legacyReplacementRequiresItsPreservedReferenceAndCannotCreateTwoActiveProgrammes() {
        val old = legacy()
        val oldRef = Reference(Owner.LEGACY, old.id(), 1)
        val replacement = snapshot().copy(decisions = listOf(decision().copy(before = oldRef)))
        val payload = StrengthProgrammingArchiveJson.encode(replacement)
        assertEquals(replacement, StrengthProgrammingArchiveJson.decode(payload, listOf(old)))
        assertFailsWith<IllegalArgumentException> { StrengthProgrammingArchiveJson.decode(payload) }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingArchiveJson.decode(payload, listOf(old), oldRef.key())
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingArchiveJson.decode(
                StrengthProgrammingArchiveJson.encode(snapshot()),
                listOf(legacy("programme")),
            )
        }
        val preview =
            snapshot()
                .copy(
                    programmes = emptyMap(),
                    current = null,
                    decisions = emptyList(),
                    concreteDrafts =
                        listOf(
                            ConcreteStrengthProgrammeDraft(
                                "preview",
                                1,
                                oldRef,
                                programme(),
                                emptyList(),
                            )
                        ),
                )
        assertEquals(
            preview,
            StrengthProgrammingArchiveJson.decode(
                StrengthProgrammingArchiveJson.encode(preview),
                listOf(old),
                oldRef.key(),
            ),
        )
    }

    @Test
    fun customMetadataAndLoggingDefinitionsMustAgreeAndDuplicateCustomIdsReject() {
        val e =
            StrengthExercise(
                "custom-press",
                "Athlete press",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_REPS,
            )
        val custom =
            CustomStrengthExercise(
                e,
                ExerciseMetadata.BodyArea.CHEST,
                ExerciseMetadata.Role.HORIZONTAL_PUSH,
                ExerciseMetadata.EquipmentRequirement(
                    listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
                ),
                "Athlete notes only",
            )
        val base =
            snapshot()
                .copy(
                    programmes = emptyMap(),
                    current = null,
                    decisions = emptyList(),
                    custom = listOf(custom),
                )
        assertEquals(base, decode(json(base)))
        StrengthProgrammingArchiveJson.validateCoaching(base, mapOf(e.id() to e))
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingArchiveJson.validateCoaching(base, emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingArchiveJson.validateCoaching(
                base,
                mapOf(e.id() to e.copy(name = "Conflicting definition")),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            decode(json(base.copy(custom = listOf(custom, custom))))
        }
    }

    @Test
    fun archiveCollectionLimitsAndUnknownEnvelopeFieldsRejectWithoutTruncation() {
        val root = json()
        for (bad in
            listOf(
                JsonObject(root + ("programmes" to JsonArray(List(1001) { JsonNull }))),
                JsonObject(root + ("concreteDrafts" to JsonArray(List(1001) { JsonNull }))),
                JsonObject(root + ("decisions" to JsonArray(List(100001) { JsonNull }))),
                JsonObject(root + ("unknown" to JsonNull)),
            )) assertFailsWith<IllegalArgumentException> { decode(bad) }
    }

    @Test
    fun incompleteDraftsRemainUnacceptedAndMustRetainKnownUniqueBaseRevisions() {
        val draft =
            StrengthProgrammeDraft(
                "weekly-draft",
                3,
                "programme",
                1,
                "",
                programme().settings(),
                start,
                emptyList(),
                StrengthProgramme.Origin.CUSTOM,
                null,
            )
        val s = snapshot().copy(drafts = listOf(draft))
        assertEquals(s, decode(json(s)))
        assertFailsWith<IllegalArgumentException> {
            decode(json(s.copy(drafts = listOf(draft, draft))))
        }
        assertFailsWith<IllegalArgumentException> {
            decode(json(s.copy(drafts = listOf(draft.copy(baseRevision = 2)))))
        }
        val new =
            s.copy(
                programmes = emptyMap(),
                decisions = emptyList(),
                current = null,
                drafts = listOf(draft.copy(baseRevision = 0)),
            )
        assertEquals(new, decode(json(new)))
    }
}
