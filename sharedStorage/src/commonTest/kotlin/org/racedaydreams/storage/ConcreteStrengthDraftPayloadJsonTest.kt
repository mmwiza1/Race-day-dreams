package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class ConcreteStrengthDraftPayloadJsonTest {
    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = CalendarDates.of(2026, 11, 17)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val replacement = ExerciseCatalog.definition("dumbbell-bench-press")!!.exercise()

    private fun original(): StrengthProgramme {
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
            List(2) { index ->
                val id = "workout-$index"
                StrengthProgramme.Workout(
                    id,
                    start.plusWeeks(index.toLong()),
                    30,
                    GymWorkoutDefinition(
                        id,
                        1,
                        at,
                        "Upper",
                        listOf(GymWorkoutDefinition.Slot("slot", bench, target, null)),
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

    private fun forward(): ConcreteStrengthProgrammeDraft {
        val original = original()
        val old = original.workouts()[1]
        val from = old.definition().slots().single()
        val updated =
            old.copy(
                definition =
                    old.definition()
                        .copy(
                            revision = 2,
                            slots =
                                listOf(
                                    from.copy(
                                        exercise = replacement,
                                        target = from.target()!!.withoutLoadReference(),
                                    )
                                ),
                        )
            )
        val programme =
            original.copy(
                revision = 2,
                parentRevision = 1,
                blocks =
                    listOf(
                        original
                            .blocks()
                            .single()
                            .copy(workouts = listOf(original.workouts()[0], updated))
                    ),
                effectiveFrom = old.date(),
                reason = "Athlete chose an alternative",
            )
        return ConcreteStrengthProgrammeDraft(
            "draft",
            5,
            StrengthProgrammingRepository.Reference(
                StrengthProgrammingRepository.Owner.AUTHORED,
                original.id(),
                1,
            ),
            programme,
            listOf(
                StrengthProgrammeEditor.Change(
                    old.id(),
                    from.id(),
                    bench.id(),
                    replacement.id(),
                    "Equipment unavailable",
                )
            ),
        )
    }

    private fun root() =
        Json.parseToJsonElement(ConcreteStrengthDraftPayloadJson.encode(forward())).jsonObject

    @Test
    fun newAndReplacementPreviewsKeepBeforeOwnerWithoutBecomingActive() {
        for (before in
            listOf(
                null,
                StrengthProgrammingRepository.Reference(
                    StrengthProgrammingRepository.Owner.LEGACY,
                    "old-programme",
                    3,
                ),
            )) {
            val draft =
                ConcreteStrengthProgrammeDraft("initial", 1, before, original(), emptyList())
            assertEquals(
                draft,
                ConcreteStrengthDraftPayloadJson.decode(
                    ConcreteStrengthDraftPayloadJson.encode(draft)
                ),
            )
        }
    }

    @Test
    fun forwardDraftPreservesDistinctDraftRevisionProtectedWorkoutAndSubstitutionProvenance() {
        val draft = forward()
        val decoded =
            ConcreteStrengthDraftPayloadJson.decode(ConcreteStrengthDraftPayloadJson.encode(draft))
        assertEquals(draft, decoded)
        assertEquals(5, decoded.revision())
        assertEquals(2, decoded.programme().revision())
        assertEquals(1, decoded.before()!!.revision())
        assertEquals(original().workouts()[0], decoded.programme().workouts()[0])
        assertNull(
            decoded
                .programme()
                .workouts()[1]
                .definition()
                .slots()
                .single()
                .target()!!
                .referenceLoadKg()
        )
        assertEquals(
            82.25,
            original().workouts()[1].definition().slots().single().target()!!.referenceLoadKg(),
        )
        decoded.validateAgainst(original())
    }

    @Test
    fun futureUnknownMissingAndCoercedFieldsAreRejected() {
        val root = root()
        for (invalid in
            listOf(
                JsonObject(root + ("version" to JsonPrimitive(2))),
                JsonObject(root + ("kind" to JsonPrimitive("draft"))),
                JsonObject(root - "before"),
                JsonObject(root + ("unknown" to JsonNull)),
                JsonObject(root + ("revision" to JsonPrimitive(5.0))),
            )) assertFailsWith<IllegalArgumentException> {
            ConcreteStrengthDraftPayloadJson.decode(invalid.toString())
        }
    }

    @Test
    fun staleMissingOrWrongFamilyReferencesCannotBecomeForwardDrafts() {
        val root = root()
        val before = root.getValue("before").jsonObject
        val invalid =
            listOf(
                JsonObject(root + ("before" to JsonNull)),
                JsonObject(
                    root + ("before" to JsonObject(before + ("revision" to JsonPrimitive(3))))
                ),
                JsonObject(
                    root + ("before" to JsonObject(before + ("owner" to JsonPrimitive("LEGACY"))))
                ),
            )
        for (payload in invalid) assertFailsWith<IllegalArgumentException> {
            ConcreteStrengthDraftPayloadJson.decode(payload.toString())
        }
    }

    @Test
    fun duplicateOversizeOrMalformedChangesFailWithoutInventingHistory() {
        val root = root()
        val change = root.getValue("substitutions").jsonArray.single()
        val invalid =
            listOf(
                JsonObject(root + ("substitutions" to JsonArray(listOf(change, change)))),
                JsonObject(root + ("substitutions" to JsonArray(List(312 * 12 + 1) { change }))),
                JsonObject(
                    root +
                        ("substitutions" to
                            JsonArray(
                                listOf(JsonObject(change.jsonObject + ("unknown" to JsonNull)))
                            ))
                ),
            )
        for (payload in invalid) assertFailsWith<IllegalArgumentException> {
            ConcreteStrengthDraftPayloadJson.decode(payload.toString())
        }
        val encoded = ConcreteStrengthDraftPayloadJson.encode(forward())
        assertFailsWith<IllegalArgumentException> {
            ConcreteStrengthDraftPayloadJson.decode(encoded.replaceFirst("{", "{\"version\":1,"))
        }
    }
}
