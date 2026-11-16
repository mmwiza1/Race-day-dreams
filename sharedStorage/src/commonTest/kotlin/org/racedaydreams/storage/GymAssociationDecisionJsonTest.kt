package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class GymAssociationDecisionJsonTest {
    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val day = CalendarDates.of(2026, 11, 16)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    private fun time(minutes: Int) =
        Instants.ofEpochSecond(at.getEpochSecond() + minutes * 60L, at.getNano().toLong())

    private fun actual() =
        StrengthSession(
            "actual",
            at,
            Offsets.ofTotalSeconds(19815),
            Durations.ofMinutes(30),
            null,
            "Actual notes 🏋️\u0000",
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

    private fun legacy() =
        GymProgramme.Session(
            "legacy-session",
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
            "Original immutable prescription",
        )

    private fun authored(): StrengthProgramme.Workout {
        val target =
            GymWorkoutDefinition.Target(
                2,
                4,
                6,
                null,
                null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.5, 4.5),
                90,
                null,
            )
        val definition =
            GymWorkoutDefinition(
                "authored-session",
                3,
                at,
                "Authored Upper",
                listOf(GymWorkoutDefinition.Slot("slot", bench, target, null)),
            )
        return StrengthProgramme.Workout("authored-session", day, 30, definition, "Authored notes")
    }

    private fun decision(
        authored: Boolean = false,
        choice: GymWorkoutAssociation.Choice = GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION,
    ): GymWorkoutAssociation.Decision {
        val proposal =
            if (authored)
                GymWorkoutAssociation.Proposal.authored(
                    actual(),
                    GymExecution.Origin.FREE,
                    "programme",
                    3,
                    authored(),
                    time(31),
                )
            else
                GymWorkoutAssociation.Proposal(
                    actual(),
                    GymExecution.Origin.FREE,
                    "programme",
                    3,
                    legacy(),
                    time(31),
                )
        return GymWorkoutAssociation.Decision("decision", proposal, choice, time(32))
    }

    private fun root(authored: Boolean = false) =
        Json.parseToJsonElement(GymAssociationDecisionJson.encode(decision(authored))).jsonObject

    private fun withProposal(change: (JsonObject) -> JsonObject): String {
        val row = root()
        return JsonObject(row + ("proposal" to change(row.getValue("proposal").jsonObject)))
            .toString()
    }

    @Test
    fun bothFamiliesAndAthleteChoicesRetainActualsWithoutApplyingAnyPlanLink() {
        for (authored in listOf(false, true)) for (choice in GymWorkoutAssociation.Choice.entries) {
            val original = decision(authored, choice)
            val encoded = GymAssociationDecisionJson.encode(original)
            val decoded = GymAssociationDecisionJson.decode(encoded)
            assertEquals(original, decoded)
            assertEquals(actual(), decoded.proposal().actual())
            assertEquals(choice, decoded.choice())
            assertEquals(authored, decoded.proposal().authored())
            assertEquals(
                if (authored) 2 else 1,
                Json.parseToJsonElement(encoded).jsonObject.getValue("version").jsonPrimitive.int,
            )
        }
    }

    @Test
    fun authoredFractionalRirAndOptionalReferenceNeverBecomeLegacyIntegerTargets() {
        val decoded =
            GymAssociationDecisionJson.decode(GymAssociationDecisionJson.encode(decision(true)))
                .proposal()
        assertNull(decoded.prescription())
        val target = decoded.authoredPrescription()!!.definition().slots().single().target()!!
        assertEquals(GymWorkoutDefinition.EffortScale.RIR, target.effort()!!.scale())
        assertEquals(2.5, target.effort()!!.minimum())
        assertEquals(90, target.restSeconds())
        assertNull(target.referenceLoadKg())
        assertEquals(80.0, decoded.actual().exercises().single().sets()[1].kilograms())
    }

    @Test
    fun legacyReferenceIsNotAnActualLiftAndWarmUpsKeepTheirRecordedMeaning() {
        val decoded =
            GymAssociationDecisionJson.decode(GymAssociationDecisionJson.encode(decision()))
                .proposal()
        assertNull(decoded.authoredPrescription())
        assertEquals(82.25, decoded.prescription()!!.exercises().single().referenceLoadKg())
        assertTrue(decoded.actual().exercises().single().sets()[0].warmUp())
        assertEquals(20.0, decoded.actual().exercises().single().sets()[0].kilograms())
        assertNull(decoded.actual().rpe())
    }

    @Test
    fun familyVersionsUnknownFieldsAndCoercedRevisionsFailClosed() {
        val original = root()
        for (payload in
            listOf(
                JsonObject(original + ("version" to JsonPrimitive(3))),
                JsonObject(original + ("version" to JsonPrimitive(2))),
                JsonObject(original + ("unknown" to JsonNull)),
            )) assertFailsWith<IllegalArgumentException> {
            GymAssociationDecisionJson.decode(payload.toString())
        }
        assertFailsWith<IllegalArgumentException> {
            GymAssociationDecisionJson.decode(
                withProposal { JsonObject(it + ("programmeRevision" to JsonPrimitive(3.0))) }
            )
        }
        val encoded = GymAssociationDecisionJson.encode(decision(true))
        assertFailsWith<IllegalArgumentException> {
            GymAssociationDecisionJson.decode(encoded.replace("\"version\":2", "\"version\":1"))
        }
    }

    @Test
    fun incompleteAcceptedEvidenceIsRejectedInsteadOfUsingLegacyDefaults() {
        val invalid = withProposal { proposal ->
            val actual = proposal.getValue("actual").jsonObject
            val block = actual.getValue("exercises").jsonArray.single().jsonObject
            val exercise = JsonObject(block.getValue("exercise").jsonObject - "measurement")
            JsonObject(
                proposal +
                    ("actual" to
                        JsonObject(
                            actual +
                                ("exercises" to
                                    JsonArray(listOf(JsonObject(block + ("exercise" to exercise)))))
                        ))
            )
        }
        assertFailsWith<IllegalArgumentException> { GymAssociationDecisionJson.decode(invalid) }
    }

    @Test
    fun decisionsCannotPrecedeReviewAndReviewsCannotPrecedeActualCompletion() {
        assertFailsWith<IllegalArgumentException> {
            GymAssociationDecisionJson.decode(
                JsonObject(root() + ("decidedAt" to JsonPrimitive(time(30).toString()))).toString()
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymAssociationDecisionJson.decode(
                withProposal {
                    JsonObject(it + ("reviewedAt" to JsonPrimitive(time(29).toString())))
                }
            )
        }
    }

    @Test
    fun duplicateFieldsInvalidDatesAndOversizeLegacyPrescriptionsRejectWithoutTruncation() {
        val encoded = GymAssociationDecisionJson.encode(decision())
        assertFailsWith<IllegalArgumentException> {
            GymAssociationDecisionJson.decode(encoded.replaceFirst("{", "{\"version\":1,"))
        }
        assertFailsWith<IllegalArgumentException> {
            GymAssociationDecisionJson.decode(
                encoded.replace("\"date\":\"2026-11-16\"", "\"date\":\"not-a-date\"")
            )
        }
        val invalid = withProposal {
            val prescription = it.getValue("prescription").jsonObject
            val exercise = prescription.getValue("exercises").jsonArray.single()
            JsonObject(
                it +
                    ("prescription" to
                        JsonObject(
                            prescription + ("exercises" to JsonArray(List(11) { exercise }))
                        ))
            )
        }
        assertFailsWith<IllegalArgumentException> { GymAssociationDecisionJson.decode(invalid) }
    }
}
