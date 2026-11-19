package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class StrengthProgrammingPayloadJsonTest {
    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = CalendarDates.of(2026, 11, 17)

    private fun settings(
        goal: GymProgramme.Goal = GymProgramme.Goal.GENERAL,
        experience: GymProgramme.Experience = GymProgramme.Experience.BEGINNER,
        frequency: Int = 2,
    ) =
        StrengthProgramme.Settings(
            goal,
            GymProgramme.Priority.BALANCED,
            experience,
            DayOfWeek.entries.toSet(),
            frequency,
            60,
            ExerciseMetadata.Equipment.entries.toSet(),
        )

    private fun slot(): GymWorkoutDefinition.Slot =
        GymWorkoutDefinition.Slot(
            "slot",
            StrengthExercise(
                "bench-press",
                "Historical retained name",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_REPS,
            ),
            GymWorkoutDefinition.Target(
                2,
                4,
                6,
                null,
                null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.5, 4.5),
                180,
                82.25,
            ),
            null,
        )

    private fun programme(count: Int = 4): StrengthProgramme {
        val blocks =
            List(count) { index ->
                val date = start.plusWeeks(index.toLong())
                val id = "workout-$index"
                val definition = GymWorkoutDefinition(id, 2, at, "Session $index", listOf(slot()))
                StrengthProgramme.Block(
                    "block-$index",
                    StrengthProgramme.Phase.entries[index % 4],
                    "Block $index",
                    date,
                    1,
                    listOf(StrengthProgramme.Workout(id, date, 30, definition, null)),
                    "Frozen explanation",
                )
            }
        return StrengthProgramme(
            "programme",
            2,
            1,
            at,
            "Retained programme",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings(),
            blocks,
            start,
            "Athlete chose",
            listOf("Retained trade-off"),
        )
    }

    private fun draft() =
        StrengthProgrammeDraft(
            "draft",
            1,
            "programme",
            0,
            "",
            settings(),
            start,
            emptyList(),
            StrengthProgramme.Origin.CUSTOM,
            null,
        )

    private fun custom(
        profile: StrengthExercise.MeasurementProfile = StrengthExercise.MeasurementProfile.LOAD_REPS
    ) =
        CustomStrengthExercise(
            StrengthExercise(
                "custom-${profile.name.lowercase().replace('_','-')}",
                "My movement",
                StrengthExercise.Region.UPPER,
                if (profile.usesLoad()) StrengthExercise.Mode.WEIGHTED
                else StrengthExercise.Mode.BODYWEIGHT,
                false,
                profile,
            ),
            ExerciseMetadata.BodyArea.CHEST,
            ExerciseMetadata.Role.HORIZONTAL_PUSH,
            ExerciseMetadata.EquipmentRequirement(
                if (profile.usesLoad())
                    listOf(
                        setOf(ExerciseMetadata.Equipment.DUMBBELLS),
                        setOf(ExerciseMetadata.Equipment.KETTLEBELLS),
                    )
                else listOf(emptySet())
            ),
            "Original notes 🏋️\u0000",
        )

    private fun root() =
        Json.parseToJsonElement(StrengthProgrammingPayloadJson.encode(programme())).jsonObject

    @Test
    fun programmeKeepsFrozenDefinitionsRevisionLineageAndOptionalReferences() {
        val original = programme()
        val decoded =
            StrengthProgrammingPayloadJson.decode(StrengthProgrammingPayloadJson.encode(original))
        assertEquals(original, decoded)
        assertEquals(1, decoded.parentRevision())
        assertEquals(4, decoded.blocks().size)
        assertEquals(
            "Historical retained name",
            decoded.workouts().first().definition().slots().single().exercise().name(),
        )
        assertEquals(
            2.5,
            decoded.workouts().first().definition().slots().single().target()!!.effort()!!.minimum(),
        )
        assertEquals(
            82.25,
            decoded.workouts().first().definition().slots().single().target()!!.referenceLoadKg(),
        )
        assertNull(decoded.workouts().first().notes())
    }

    @Test
    fun existingGeneratorGoalsExperienceAndEveryTemplateRoundTripWithoutReprogramming() {
        val generator = StrengthProgrammeGenerator()
        val configurations =
            GymProgramme.Goal.entries.flatMap { goal ->
                GymProgramme.Experience.entries.map { experience ->
                    settings(goal, experience) to null
                }
            } +
                StrengthProgrammeTemplates.all().map {
                    settings(
                        it.defaultGoal()!!,
                        GymProgramme.Experience.EXPERIENCED,
                        it.frequency(),
                    ) to it.id()
                }
        for ((settings, template) in configurations) {
            val original =
                generator.generate(
                    StrengthProgrammeGenerator.Request(
                        "generated",
                        at,
                        "Original",
                        settings,
                        start,
                        10,
                        template,
                        PrimaryFocus.STRENGTH,
                        emptySet(),
                        null,
                        emptySet(),
                        emptySet(),
                        true,
                    )
                )
            assertEquals(
                original,
                StrengthProgrammingPayloadJson.decode(
                    StrengthProgrammingPayloadJson.encode(original)
                ),
            )
        }
    }

    @Test
    fun incompleteDraftsRemainSaveableWithoutBecomingAnActivatedProgramme() {
        val empty = draft()
        assertEquals(
            empty,
            StrengthProgrammingPayloadJson.decodeDraft(
                StrengthProgrammingPayloadJson.encodeDraft(empty)
            ),
        )
        val partial =
            empty.copy(
                blocks =
                    listOf(
                        StrengthProgrammeDraft.WeekBlock(
                            "b",
                            StrengthProgramme.Phase.FOUNDATION,
                            "Foundation",
                            3,
                            listOf(
                                StrengthProgrammeDraft.Day(
                                    "d",
                                    DayOfWeek.TUESDAY,
                                    "Session",
                                    emptyList(),
                                    null,
                                )
                            ),
                        )
                    )
            )
        val decoded =
            StrengthProgrammingPayloadJson.decodeDraft(
                StrengthProgrammingPayloadJson.encodeDraft(partial)
            )
        assertEquals(partial, decoded)
        assertFailsWith<IllegalArgumentException> { decoded.preview(at, emptySet(), emptyMap()) }
    }

    @Test
    fun customModalitiesEquipmentAlternativesAndNotesStayAthleteAuthored() {
        for (profile in StrengthExercise.MeasurementProfile.entries) {
            val original = custom(profile)
            val decoded =
                StrengthProgrammingPayloadJson.decodeCustom(
                    StrengthProgrammingPayloadJson.encodeCustom(original)
                )
            assertEquals(original, decoded)
            assertFalse(decoded.exercise().estimateOneRepMax())
        }
    }

    @Test
    fun sortedWireEnumsRetainOriginalSettingsSetContractAndRejectDuplicates() {
        val original = root()
        val configured = original.getValue("settings").jsonObject
        val wireDays = configured.getValue("days").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(wireDays.sorted(), wireDays)
        val decoded = StrengthProgrammingPayloadJson.decode(original.toString())
        assertEquals(settings().days(), decoded.settings().days())
        assertEquals(settings().days().toList(), decoded.settings().days().toList())
        assertEquals(settings().equipment(), decoded.settings().equipment())
        assertEquals(settings().equipment().toList(), decoded.settings().equipment().toList())
        for (value in
            listOf(
                JsonArray(listOf(JsonPrimitive("MONDAY"), JsonPrimitive("MONDAY"))),
                JsonArray(listOf(JsonPrimitive(true))),
            )) {
            assertFailsWith<IllegalArgumentException> {
                StrengthProgrammingPayloadJson.decode(
                    JsonObject(
                            original + ("settings" to JsonObject(configured + ("days" to value)))
                        )
                        .toString()
                )
            }
        }
    }

    @Test
    fun versionsKindsMissingAndUnknownFieldsFailClosed() {
        val original = root()
        for (payload in
            listOf(
                JsonObject(original + ("version" to JsonPrimitive(2))),
                JsonObject(original + ("kind" to JsonPrimitive("draft"))),
                JsonObject(original - "effectiveFrom"),
                JsonObject(original + ("unknown" to JsonNull)),
            )) assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingPayloadJson.decode(payload.toString())
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingPayloadJson.decodeDraft(original.toString())
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingPayloadJson.decodeCustom(original.toString())
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingPayloadJson.decode(
                StrengthProgrammingPayloadJson.encodeCustom(custom())
            )
        }
    }

    @Test
    fun programmeCollectionAndExplanationLimitsNeverSilentlyTruncate() {
        val original = root()
        val block = original.getValue("blocks").jsonArray.first().jsonObject
        val workout = block.getValue("workouts").jsonArray.first()
        for (payload in
            listOf(
                JsonObject(original + ("blocks" to JsonArray(List(13) { block }))),
                JsonObject(
                    original + ("tradeoffs" to JsonArray(List(101) { JsonPrimitive("Note") }))
                ),
                JsonObject(
                    original + ("tradeoffs" to JsonArray(listOf(JsonPrimitive("x".repeat(2001)))))
                ),
                JsonObject(
                    original +
                        ("blocks" to
                            JsonArray(
                                listOf(
                                    JsonObject(
                                        block + ("workouts" to JsonArray(List(157) { workout }))
                                    )
                                )
                            ))
                ),
            )) assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingPayloadJson.decode(payload.toString())
        }
        val largest = programme(12)
        assertEquals(
            largest,
            StrengthProgrammingPayloadJson.decode(StrengthProgrammingPayloadJson.encode(largest)),
        )
    }

    @Test
    fun malformedNestedCustomMetadataAndInventedTechniqueAreRejected() {
        val original =
            Json.parseToJsonElement(StrengthProgrammingPayloadJson.encodeCustom(custom()))
                .jsonObject
        for (payload in
            listOf(
                JsonObject(original + ("technique" to JsonPrimitive("invented"))),
                JsonObject(
                    original +
                        ("equipmentAlternatives" to
                            JsonArray(
                                listOf(
                                    JsonArray(
                                        listOf(
                                            JsonPrimitive("DUMBBELLS"),
                                            JsonPrimitive("DUMBBELLS"),
                                        )
                                    )
                                )
                            ))
                ),
                JsonObject(
                    original +
                        ("equipmentAlternatives" to JsonArray(List(17) { JsonArray(emptyList()) }))
                ),
                JsonObject(
                    original +
                        ("equipmentAlternatives" to JsonArray(listOf(JsonPrimitive("DUMBBELLS"))))
                ),
            )) assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingPayloadJson.decodeCustom(payload.toString())
        }
    }

    @Test
    fun duplicateJsonKeysFractionalIntegerTokensAndInvalidDatesAreRejected() {
        val encoded = StrengthProgrammingPayloadJson.encode(programme())
        for (payload in
            listOf(
                encoded.replaceFirst("{", "{\"version\":1,"),
                encoded.replace("\"revision\":2", "\"revision\":2e0"),
                encoded.replace("\"parentRevision\":1", "\"parentRevision\":\"1\""),
                encoded.replace("2026-11-17", "invalid-date"),
                encoded + "{}",
            )) assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingPayloadJson.decode(payload)
        }
    }
}
