package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*

class FrozenGymWorkoutJsonTest {
    private val at = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")

    private fun exercise(
        profile: StrengthExercise.MeasurementProfile = StrengthExercise.MeasurementProfile.LOAD_REPS
    ) =
        StrengthExercise(
            "frozen",
            "Frozen historical definition",
            StrengthExercise.Region.FULL,
            if (profile.usesLoad()) StrengthExercise.Mode.WEIGHTED
            else StrengthExercise.Mode.BODYWEIGHT,
            profile == StrengthExercise.MeasurementProfile.LOAD_REPS,
            profile,
        )

    private fun slot(
        id: String = "slot",
        profile: StrengthExercise.MeasurementProfile = StrengthExercise.MeasurementProfile.LOAD_REPS,
    ) =
        GymWorkoutDefinition.Slot(
            id,
            exercise(profile),
            GymWorkoutDefinition.Target(
                3,
                if (profile.usesReps()) 4 else null,
                if (profile.usesReps()) 6 else null,
                if (profile.usesDuration()) 30 else null,
                if (profile.usesDistance()) 125.5 else null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 5.5, 7.0),
                180,
                if (profile.usesLoad()) 82.25 else null,
            ),
            null,
        )

    private fun workout(slots: List<GymWorkoutDefinition.Slot> = listOf(slot())) =
        GymWorkoutDefinition("workout", 3, at, "Frozen intention 🏋️\u0000", slots)

    private fun row() = Json.parseToJsonElement(FrozenGymWorkoutJson.encode(workout())).jsonObject

    private fun mutateSlot(change: (JsonObject) -> JsonObject): String {
        val row = row()
        return JsonObject(
                row +
                    ("slots" to
                        JsonArray(
                            listOf(change(row.getValue("slots").jsonArray.single().jsonObject))
                        ))
            )
            .toString()
    }

    @Test
    fun frozenWorkoutsAcrossAllMeasurementProfilesKeepTargetsSeparateFromActuals() {
        for (profile in StrengthExercise.MeasurementProfile.entries) {
            val original = workout(listOf(slot(profile = profile)))
            assertEquals(
                original,
                FrozenGymWorkoutJson.decode(FrozenGymWorkoutJson.encode(original)),
            )
        }
    }

    @Test
    fun unknownTargetsAndRpeRirScalesStayDistinct() {
        val original =
            workout(
                listOf(
                    slot().copy(id = "open", target = null),
                    slot()
                        .copy(
                            target =
                                slot()
                                    .target()!!
                                    .copy(
                                        effort =
                                            GymWorkoutDefinition.EffortRange(
                                                GymWorkoutDefinition.EffortScale.RIR,
                                                2.5,
                                                4.5,
                                            ),
                                        referenceLoadKg = null,
                                        restSeconds = 0,
                                    )
                        ),
                )
            )
        val decoded = FrozenGymWorkoutJson.decode(FrozenGymWorkoutJson.encode(original))
        assertEquals(original, decoded)
        assertNull(decoded.slots()[0].target())
        assertEquals(
            GymWorkoutDefinition.EffortScale.RIR,
            decoded.slots()[1].target()!!.effort()!!.scale(),
        )
        assertNull(decoded.slots()[1].target()!!.referenceLoadKg())
    }

    @Test
    fun supersetGroupingAndFrozenExerciseIdentityDoNotConsultCatalogue() {
        val original =
            workout(
                listOf(
                    slot("first").copy(supersetGroup = "pair"),
                    slot("second").copy(supersetGroup = "pair"),
                    slot("third"),
                )
            )
        assertEquals(original, FrozenGymWorkoutJson.decode(FrozenGymWorkoutJson.encode(original)))
        val bad = mutateSlot { JsonObject(it + ("supersetGroup" to JsonPrimitive("orphan"))) }
        assertFailsWith<IllegalArgumentException> { FrozenGymWorkoutJson.decode(bad) }
    }

    @Test
    fun missingUnknownAndFutureFieldsFailClosedAtEveryLevel() {
        for (payload in
            listOf(
                JsonObject(row() - "title"),
                JsonObject(row() + ("extra" to JsonNull)),
                JsonObject(row() + ("version" to JsonPrimitive(2))),
            )) assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutJson.decode(payload.toString())
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutJson.decode(mutateSlot { JsonObject(it + ("unexpected" to JsonNull)) })
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutJson.decode(
                mutateSlot {
                    JsonObject(
                        it +
                            ("exercise" to
                                JsonObject(it.getValue("exercise").jsonObject - "measurement"))
                    )
                }
            )
        }
    }

    @Test
    fun integerTokensRemainStrictAndCannotBecomeLossyOrCoercedTargets() {
        val input = FrozenGymWorkoutJson.encode(workout())
        for (value in
            listOf("3.0", "3e0", "\"3\"", "2147483648", "9223372036854775808")) assertFailsWith<
            IllegalArgumentException
        > {
            FrozenGymWorkoutJson.decode(input.replace("\"revision\":3", "\"revision\":" + value))
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutJson.decode(input.replace("\"sets\":3", "\"sets\":3.5"))
        }
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutJson.decode(
                input.replace("\"referenceLoadKg\":82.25", "\"referenceLoadKg\":\"82.25\"")
            )
        }
    }

    @Test
    fun duplicateObjectKeysIncludingEscapedNamesAreRejectedBeforeDomAllocation() {
        for (payload in
            listOf(
                "{\"version\":1,\"version\":2}",
                "{\"x\":1,\"\\u0078\":2}",
                "{\"nested\":{\"x\":0,\"x\":1}}",
            )) {
            val error = assertFailsWith<IllegalArgumentException> { GymStoredJson.read(payload) }
            assertTrue(error.message!!.contains("duplicate field"))
        }
    }

    @Test
    fun lenientJsonCommentsTrailingDataAndInvalidEscapesAreRejected() {
        val inputs =
            listOf(
                "[]",
                "{x:1}",
                "{'x':1}",
                "{\"x\":01}",
                "{\"x\":1,}",
                "{\"x\":[1,]}",
                "{\"x\":NaN}",
                "{}{}",
                "{} //comment",
                "{\"x\":\"\\q\"}",
                "{\"x\":\"\\u00qq\"}",
                "{\"x\":truee}",
                "{\"x\":\"raw\ncontrol\"}",
            )
        for (input in inputs) assertFailsWith<IllegalArgumentException>(input) {
            GymStoredJson.read(input)
        }
    }

    @Test
    fun realStringEscapesAndLargeExactIntegersSurviveTheStrictSyntaxGate() {
        val input =
            """ {"quoted":"\"\\\/\b\f\n\r\t\u0000","clock":9007199254741017,"negative":-0,"decimal":-0.0} """
        val row = GymStoredJson.read(input)
        assertEquals(9007199254741017L, GymStoredJson.whole(row, "clock"))
        assertEquals(0L, GymStoredJson.whole(row, "negative"))
        assertEquals((-0.0).toBits(), GymStoredJson.number(row, "decimal").toBits())
        assertEquals("\"\\/\b\u000c\n\r\t\u0000", GymStoredJson.text(row, "quoted", 100))
    }

    @Test
    fun sizeDepthTokenAndStringLimitsAreBoundedBeforeReadingPayloads() {
        val tooDeep = "{\"x\":" + "[".repeat(21) + "0" + "]".repeat(21) + "}"
        val tooMany = "{\"x\":[" + "0,".repeat(1000000) + "0]}"
        val tooLong = "{\"x\":\"" + "x".repeat(100001) + "\"}"
        for (input in listOf(tooDeep, tooMany, tooLong)) assertFailsWith<IllegalArgumentException> {
            GymStoredJson.read(input)
        }
        val tooLarge = "{\"x\":\"" + "é".repeat(GymStoredJson.MAX_BYTES / 2) + "\"}"
        val error = assertFailsWith<IllegalArgumentException> { GymStoredJson.read(tooLarge) }
        assertTrue(error.message!!.contains("payload size"))
        assertFailsWith<IllegalArgumentException> {
            GymStoredJson.write(buildJsonObject { put("x", tooLarge) })
        }
    }

    @Test
    fun slotLimitsDuplicatesAndIncompatibleMeasurementsNeverGetSilentlyTruncated() {
        val row = row()
        val slot = row.getValue("slots").jsonArray.single()
        for (rows in
            listOf(
                JsonArray(emptyList()),
                JsonArray(listOf(slot, slot)),
                JsonArray(List(101) { slot }),
            )) assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutJson.decode(JsonObject(row + ("slots" to rows)).toString())
        }
        val valid = workout(List(100) { slot("slot-$it") })
        assertEquals(valid, FrozenGymWorkoutJson.decode(FrozenGymWorkoutJson.encode(valid)))
        assertFailsWith<IllegalArgumentException> {
            FrozenGymWorkoutJson.decode(
                mutateSlot {
                    JsonObject(
                        it +
                            ("target" to
                                JsonObject(
                                    it.getValue("target").jsonObject +
                                        ("seconds" to JsonPrimitive(30))
                                ))
                    )
                }
            )
        }
    }
}
