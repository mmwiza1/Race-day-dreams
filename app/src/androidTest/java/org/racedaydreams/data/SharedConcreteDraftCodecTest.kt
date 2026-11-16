package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.ConcreteStrengthDraftPayloadJson

@RunWith(AndroidJUnit4::class)
class SharedConcreteDraftCodecTest {
    private val at = Instant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = LocalDate.of(2026, 11, 17)

    private fun programme(): StrengthProgramme {
        val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.TUESDAY),
                1,
                60,
                ExerciseMetadata.Equipment.values().toSet(),
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
        val original = programme()
        val old = original.workouts()[1]
        val from = old.definition().slots().single()
        val replacement = ExerciseCatalog.definition("dumbbell-bench-press")!!.exercise()
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
        val next =
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
            next,
            listOf(
                StrengthProgrammeEditor.Change(
                    old.id(),
                    from.id(),
                    from.exercise().id(),
                    replacement.id(),
                    "Equipment unavailable",
                )
            ),
        )
    }

    @Test
    fun originalAndSharedReadersPreserveNewLegacyReplacementAndAuthoredRevisionDrafts() {
        val drafts =
            listOf(
                ConcreteStrengthProgrammeDraft("initial", 1, null, programme(), emptyList()),
                ConcreteStrengthProgrammeDraft(
                    "replacement",
                    1,
                    StrengthProgrammingRepository.Reference(
                        StrengthProgrammingRepository.Owner.LEGACY,
                        "old-programme",
                        3,
                    ),
                    programme(),
                    emptyList(),
                ),
                forward(),
            )
        for (draft in drafts) {
            assertEquals(
                draft,
                ConcreteStrengthDraftPayloadJson.decode(
                    ConcreteStrengthProgrammeJson.encode(draft).toString()
                ),
            )
            assertEquals(
                draft,
                ConcreteStrengthProgrammeJson.decode(
                    JSONObject(ConcreteStrengthDraftPayloadJson.encode(draft))
                ),
            )
        }
        val decoded =
            ConcreteStrengthDraftPayloadJson.decode(
                ConcreteStrengthProgrammeJson.encode(forward()).toString()
            )
        assertEquals(5, decoded.revision())
        assertEquals(2, decoded.programme().revision())
        decoded.validateAgainst(programme())
        assertEquals(programme().workouts()[0], decoded.programme().workouts()[0])
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
    }

    @Test
    fun bothReadersRejectUnknownFieldsMissingReferencesAndCoercedDraftRevisions() {
        val original = ConcreteStrengthProgrammeJson.encode(forward()).toString()
        val missing = JSONObject(original).put("before", JSONObject.NULL).toString()
        val invalid =
            listOf(
                missing,
                original.dropLast(1) + ",\"unknown\":null}",
                original.replace("\"revision\":5", "\"revision\":5e0"),
                original.replace("\"kind\":\"concreteDraft\"", "\"kind\":\"draft\""),
            )
        for (payload in invalid) {
            assertNotEquals(original, payload)
            assertThrows(IllegalArgumentException::class.java) {
                ConcreteStrengthProgrammeJson.decode(JSONObject(payload))
            }
            assertThrows(IllegalArgumentException::class.java) {
                ConcreteStrengthDraftPayloadJson.decode(payload)
            }
        }
    }
}
