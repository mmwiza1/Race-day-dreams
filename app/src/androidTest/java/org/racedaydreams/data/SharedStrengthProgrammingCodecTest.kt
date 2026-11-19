package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.StrengthProgrammingPayloadJson

@RunWith(AndroidJUnit4::class)
class SharedStrengthProgrammingCodecTest {
    private val at = Instant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = LocalDate.of(2026, 11, 17)

    private fun settings(
        goal: GymProgramme.Goal = GymProgramme.Goal.GENERAL,
        experience: GymProgramme.Experience = GymProgramme.Experience.BEGINNER,
        frequency: Int = 2,
    ) =
        StrengthProgramme.Settings(
            goal,
            GymProgramme.Priority.BALANCED,
            experience,
            DayOfWeek.values().toSet(),
            frequency,
            60,
            ExerciseMetadata.Equipment.values().toSet(),
        )

    private fun generate(
        settings: StrengthProgramme.Settings = settings(),
        template: String? = null,
    ) =
        StrengthProgrammeGenerator()
            .generate(
                StrengthProgrammeGenerator.Request(
                    "programme",
                    at,
                    "Original programme",
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

    @Test
    fun originalAndSharedProgrammeReadersAgreeAcrossGoalsExperienceAndAllTemplates() {
        val configurations =
            GymProgramme.Goal.values().flatMap { goal ->
                GymProgramme.Experience.values().map { settings(goal, it) to null }
            } +
                StrengthProgrammeTemplates.all().map {
                    settings(
                        it.defaultGoal()!!,
                        GymProgramme.Experience.EXPERIENCED,
                        it.frequency(),
                    ) to it.id()
                }
        for ((settings, template) in configurations) {
            val original = generate(settings, template)
            assertEquals(
                original,
                StrengthProgrammingPayloadJson.decode(StrengthProgrammingJson.write(original)),
            )
            assertEquals(
                original,
                StrengthProgrammingJson.read(StrengthProgrammingPayloadJson.encode(original)),
            )
        }
    }

    @Test
    fun incompleteDraftAndCustomMetadataReadBothWaysWithoutActivatingOrInventingGuidance() {
        val draft =
            StrengthProgrammeDraft(
                "draft",
                1,
                "programme",
                0,
                "",
                settings(),
                start,
                listOf(
                    StrengthProgrammeDraft.WeekBlock(
                        "block",
                        StrengthProgramme.Phase.FOUNDATION,
                        "Foundation",
                        3,
                        listOf(
                            StrengthProgrammeDraft.Day(
                                "day",
                                DayOfWeek.TUESDAY,
                                "Session",
                                emptyList(),
                                null,
                            )
                        ),
                    )
                ),
                StrengthProgramme.Origin.CUSTOM,
                null,
            )
        assertEquals(
            draft,
            StrengthProgrammingPayloadJson.decodeDraft(StrengthProgrammingJson.writeDraft(draft)),
        )
        assertEquals(
            draft,
            StrengthProgrammingJson.readDraft(StrengthProgrammingPayloadJson.encodeDraft(draft)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            draft.preview(at, emptySet(), emptyMap())
        }
        for (profile in StrengthExercise.MeasurementProfile.values()) {
            val custom =
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
                        listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
                    ),
                    "Own notes 🏋️\u0000",
                )
            assertEquals(
                custom,
                StrengthProgrammingPayloadJson.decodeCustom(
                    StrengthProgrammingJson.writeCustom(custom)
                ),
            )
            assertEquals(
                custom,
                StrengthProgrammingJson.readCustom(
                    StrengthProgrammingPayloadJson.encodeCustom(custom)
                ),
            )
        }
    }

    @Test
    fun frozenDefinitionsEnumOrderAndRevisionLineageArePreservedAcrossReaders() {
        val encoded = StrengthProgrammingJson.encode(generate())
        encoded
            .getJSONArray("blocks")
            .getJSONObject(0)
            .getJSONArray("workouts")
            .getJSONObject(0)
            .getJSONObject("definition")
            .getJSONArray("slots")
            .getJSONObject(0)
            .getJSONObject("exercise")
            .put("name", "Historical retained name")
        val original = StrengthProgrammingJson.read(encoded.toString())
        val shared = StrengthProgrammingPayloadJson.decode(encoded.toString())
        assertEquals(original, shared)
        assertEquals(original.settings().days().toList(), shared.settings().days().toList())
        assertEquals(
            original.settings().equipment().toList(),
            shared.settings().equipment().toList(),
        )
        assertEquals(
            original,
            StrengthProgrammingJson.read(StrengthProgrammingPayloadJson.encode(shared)),
        )
    }

    @Test
    fun bothReadersRejectFutureUnknownDuplicateAndCoercedAuthoringFields() {
        val source = StrengthProgrammingJson.write(generate())
        val badDays = JSONObject(source)
        badDays.getJSONObject("settings").put("days", JSONArray().put("MONDAY").put("MONDAY"))
        val payloads =
            listOf(
                source.replace("\"version\":1", "\"version\":2"),
                source.replace("\"revision\":1", "\"revision\":1e0"),
                source.dropLast(1) + ",\"unknown\":null}",
                source.replaceFirst("{", "{\"version\":1,"),
                badDays.toString(),
            )
        for (payload in payloads) {
            assertNotEquals(source, payload)
            assertThrows(IllegalArgumentException::class.java) {
                StrengthProgrammingJson.read(payload)
            }
            assertThrows(IllegalArgumentException::class.java) {
                StrengthProgrammingPayloadJson.decode(payload)
            }
        }
    }
}
