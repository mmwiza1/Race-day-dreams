package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.GymAssociationDecisionJson

@RunWith(AndroidJUnit4::class)
class SharedGymAssociationCodecTest {
    private val at = Instant.parse("2026-11-16T09:00:00.123456789Z")
    private val day = LocalDate.of(2026, 11, 16)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    private fun actual() =
        StrengthSession(
            "actual",
            at,
            ZoneOffset.ofTotalSeconds(19815),
            Duration.ofMinutes(30),
            null,
            "Retained actual 🏋️\u0000",
            listOf(
                StrengthSession.ExerciseSets(
                    bench,
                    listOf(
                        StrengthSession.SetEntry("warm", 6, 20.0, 3.5, true),
                        StrengthSession.SetEntry("working", 6, 80.0, null, false, null, null, 2.5),
                    ),
                )
            ),
        )

    private fun decision(
        authored: Boolean,
        choice: GymWorkoutAssociation.Choice,
    ): GymWorkoutAssociation.Decision {
        val proposal =
            if (authored) {
                val target =
                    GymWorkoutDefinition.Target(
                        2,
                        4,
                        6,
                        null,
                        null,
                        GymWorkoutDefinition.EffortRange(
                            GymWorkoutDefinition.EffortScale.RIR,
                            2.5,
                            4.5,
                        ),
                        90,
                        null,
                    )
                val workout =
                    StrengthProgramme.Workout(
                        "authored",
                        day,
                        30,
                        GymWorkoutDefinition(
                            "authored",
                            3,
                            at,
                            "Upper",
                            listOf(GymWorkoutDefinition.Slot("slot", bench, target, null)),
                        ),
                        null,
                    )
                GymWorkoutAssociation.Proposal.authored(
                    actual(),
                    GymExecution.Origin.FREE,
                    "programme",
                    3,
                    workout,
                    at.plusSeconds(31 * 60),
                )
            } else {
                val session =
                    GymProgramme.Session(
                        "legacy",
                        day,
                        "Upper",
                        listOf(
                            GymProgramme.Prescription(
                                "bench-press",
                                2,
                                4,
                                6,
                                null,
                                null,
                                6,
                                8,
                                180,
                                "Retained intention",
                                82.25,
                            )
                        ),
                        "Retained explanation",
                    )
                GymWorkoutAssociation.Proposal(
                    actual(),
                    GymExecution.Origin.FREE,
                    "programme",
                    3,
                    session,
                    at.plusSeconds(31 * 60),
                )
            }
        return GymWorkoutAssociation.Decision("decision", proposal, choice, at.plusSeconds(32 * 60))
    }

    @Test
    fun originalAndSharedReadersRetainBothFamiliesAndBothAthleteChoices() {
        for (authored in listOf(false, true)) for (choice in
            GymWorkoutAssociation.Choice.values()) {
            val original = decision(authored, choice)
            assertEquals(
                original,
                GymAssociationDecisionJson.decode(GymAssociationJson.write(original)),
            )
            assertEquals(
                original,
                GymAssociationJson.read(GymAssociationDecisionJson.encode(original)),
            )
        }
    }

    @Test
    fun authoredRirReferenceAndRecordedWeightsRemainDistinctFromLegacyPrescription() {
        val authored =
            GymAssociationDecisionJson.decode(
                    GymAssociationJson.write(
                        decision(true, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)
                    )
                )
                .proposal()
        assertNull(authored.prescription())
        assertEquals(
            GymWorkoutDefinition.EffortScale.RIR,
            authored
                .authoredPrescription()!!
                .definition()
                .slots()
                .single()
                .target()!!
                .effort()!!
                .scale(),
        )
        assertNull(
            authored
                .authoredPrescription()!!
                .definition()
                .slots()
                .single()
                .target()!!
                .referenceLoadKg()
        )
        val legacy =
            GymAssociationDecisionJson.decode(
                    GymAssociationJson.write(
                        decision(false, GymWorkoutAssociation.Choice.KEEP_EXTRA)
                    )
                )
                .proposal()
        assertNull(legacy.authoredPrescription())
        assertEquals(82.25, legacy.prescription()!!.exercises().single().referenceLoadKg()!!, 0.0)
        assertEquals(80.0, legacy.actual().exercises().single().sets()[1].kilograms(), 0.0)
        assertTrue(legacy.actual().exercises().single().sets()[0].warmUp())
    }

    @Test
    fun bothReadersRejectIncompleteActualEvidenceFutureFormatsAndInvalidDecisionTime() {
        val original =
            GymAssociationJson.write(
                decision(false, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)
            )
        val missing = JSONObject(original)
        missing
            .getJSONObject("proposal")
            .getJSONObject("actual")
            .getJSONArray("exercises")
            .getJSONObject(0)
            .getJSONObject("exercise")
            .remove("measurement")
        val early =
            JSONObject(original).put("decidedAt", at.plusSeconds(30 * 60).toString()).toString()
        val invalid =
            listOf(
                missing.toString(),
                early,
                original.replace("\"programmeRevision\":3", "\"programmeRevision\":3e0"),
                original.replace("\"version\":1", "\"version\":3"),
                original.dropLast(1) + ",\"unknown\":null}",
            )
        for (payload in invalid) {
            assertNotEquals(original, payload)
            assertThrows(IllegalArgumentException::class.java) { GymAssociationJson.read(payload) }
            assertThrows(IllegalArgumentException::class.java) {
                GymAssociationDecisionJson.decode(payload)
            }
        }
    }
}
