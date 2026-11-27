package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class StrengthEvidenceJsonTest {
    private fun exercise(
        profile: StrengthExercise.MeasurementProfile =
            StrengthExercise.MeasurementProfile.LOAD_REPS,
        mode: StrengthExercise.Mode =
            if (profile.usesLoad()) StrengthExercise.Mode.WEIGHTED
            else StrengthExercise.Mode.BODYWEIGHT,
    ) =
        StrengthExercise(
            "recorded-exercise",
            "Original recorded exercise",
            StrengthExercise.Region.FULL,
            mode,
            profile == StrengthExercise.MeasurementProfile.LOAD_REPS &&
                mode == StrengthExercise.Mode.WEIGHTED,
            profile,
        )

    private fun set(
        profile: StrengthExercise.MeasurementProfile =
            StrengthExercise.MeasurementProfile.LOAD_REPS,
        id: String = "set",
    ) =
        StrengthSession.SetEntry(
            id,
            if (profile.usesReps()) 8 else 0,
            if (profile.usesLoad()) 82.25 else 0.0,
            null,
            false,
            if (profile.usesDuration()) 30L else null,
            if (profile.usesDistance()) 125.5 else null,
            if (profile.usesReps()) 2.5 else null,
        )

    private fun session(
        exercises: List<StrengthSession.ExerciseSets> =
            listOf(StrengthSession.ExerciseSets(exercise(), listOf(set())))
    ) =
        StrengthSession(
            "actual-session",
            StoredInstant.parse("2026-11-15T09:00:00.123456789Z"),
            Offsets.ofTotalSeconds(19815),
            Durations.ofSeconds(5400, 123456789),
            null,
            "Actual note 🏋️\u0000 not a prescription",
            exercises,
        )

    private fun row() =
        Json.parseToJsonElement(StrengthEvidenceJson.encodeSession(session())).jsonObject

    private fun withSets(sets: JsonArray): String {
        val row = row()
        val item = row.getValue("exercises").jsonArray.single().jsonObject
        return JsonObject(
                row + ("exercises" to JsonArray(listOf(JsonObject(item + ("sets" to sets)))))
            )
            .toString()
    }

    @Test
    fun dateOnlyLogRoundTripsWithoutInventingMeasuredDurationOrEffortLoad() {
        val original = session().copy(duration = StrengthSession.UNRECORDED_DURATION, rpe = 7.0)
        val restored =
            StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeSession(original))
        assertEquals(original, restored)
        assertFalse(restored.hasRecordedDuration())
        assertNull(StrengthAnalytics.sessionLoad(restored))
        assertTrue(session().hasRecordedDuration())
    }

    @Test
    fun everySupportedModalityAndWeightedBodyweightPreserveTheirActualMeasurements() {
        for (profile in StrengthExercise.MeasurementProfile.entries) {
            val original =
                session(
                    listOf(StrengthSession.ExerciseSets(exercise(profile), listOf(set(profile))))
                )
            assertEquals(
                original,
                StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeSession(original)),
            )
        }
        val weighted =
            exercise(
                StrengthExercise.MeasurementProfile.LOAD_REPS,
                StrengthExercise.Mode.WEIGHTED_BODYWEIGHT,
            )
        val original = session(listOf(StrengthSession.ExerciseSets(weighted, listOf(set()))))
        val decoded =
            StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeSession(original))
        assertEquals(original, decoded)
        assertFalse(decoded.exercises().single().exercise().estimateOneRepMax())
    }

    @Test
    fun legacyExerciseWithoutMeasurementKeepsOriginalModeSpecificFallback() {
        for (mode in StrengthExercise.Mode.entries) {
            val profile =
                if (mode == StrengthExercise.Mode.BODYWEIGHT)
                    StrengthExercise.MeasurementProfile.REPS_ONLY
                else StrengthExercise.MeasurementProfile.LOAD_REPS
            val value = exercise(profile, mode)
            val original =
                Json.parseToJsonElement(StrengthEvidenceJson.encodeExercise(value)).jsonObject
            assertEquals(
                value,
                StrengthEvidenceJson.decodeExercise(JsonObject(original - "measurement").toString()),
            )
            assertFailsWith<IllegalArgumentException> {
                StrengthEvidenceJson.decodeExercise(
                    JsonObject(original + ("measurement" to JsonNull)).toString()
                )
            }
        }
    }

    @Test
    fun fractionalEffortSourceOffsetNanosecondsAndWarmUpsStayDistinctFromWorkingSets() {
        val warm =
            set()
                .copy(
                    id = "warm",
                    kilograms = 20.0,
                    warmUp = true,
                    repetitionsInReserve = null,
                    rpe = 3.5,
                )
        val working = set()
        val original =
            session(listOf(StrengthSession.ExerciseSets(exercise(), listOf(warm, working))))
        val decoded =
            StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeSession(original))
        assertEquals(original, decoded)
        assertNull(decoded.rpe())
        assertTrue(decoded.exercises().single().sets()[0].warmUp())
        assertEquals(3.5, decoded.exercises().single().sets()[0].rpe())
        assertEquals(2.5, decoded.exercises().single().sets()[1].repetitionsInReserve())
        assertNull(decoded.exercises().single().sets()[1].rpe())
    }

    @Test
    fun frozenDefinitionIsReadWithoutResolvingAgainstCurrentCatalogue() {
        val historical =
            StrengthExercise(
                "bench",
                "Old retained definition",
                StrengthExercise.Region.LOWER,
                StrengthExercise.Mode.BODYWEIGHT,
                false,
                StrengthExercise.MeasurementProfile.DURATION,
            )
        val original =
            session(
                listOf(
                    StrengthSession.ExerciseSets(
                        historical,
                        listOf(set(StrengthExercise.MeasurementProfile.DURATION)),
                    )
                )
            )
        assertEquals(
            original,
            StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeSession(original)),
        )
    }

    @Test
    fun legacyRepetitionSetsRetainUnknownOptionalMeasurementsAndEffort() {
        val original =
            row()
                .getValue("exercises")
                .jsonArray
                .single()
                .jsonObject
                .getValue("sets")
                .jsonArray
                .single()
                .jsonObject
        val old = JsonObject(original - setOf("rpe", "seconds", "metres", "rir"))
        val decoded =
            StrengthEvidenceJson.decodeSession(withSets(JsonArray(listOf(old))))
                .exercises()
                .single()
                .sets()
                .single()
        assertNull(decoded.rpe())
        assertNull(decoded.repetitionsInReserve())
        assertNull(decoded.durationSeconds())
        assertNull(decoded.distanceMetres())
        assertEquals(82.25, decoded.kilograms())
        assertEquals(8, decoded.reps())
    }

    @Test
    fun malformedTypesAndIncompatibleActualMeasurementsFailInsteadOfCoercingEvidence() {
        val original =
            row()
                .getValue("exercises")
                .jsonArray
                .single()
                .jsonObject
                .getValue("sets")
                .jsonArray
                .single()
                .jsonObject
        val invalid =
            listOf(
                "kg" to JsonPrimitive("82"),
                "reps" to JsonPrimitive(8.5),
                "warmUp" to JsonPrimitive("false"),
                "rpe" to JsonPrimitive(7.0),
                "seconds" to JsonPrimitive(30.0),
                "metres" to JsonPrimitive(100.0),
            )
        for (change in invalid) assertFails {
            StrengthEvidenceJson.decodeSession(
                withSets(JsonArray(listOf(JsonObject(original + change))))
            )
        }
        assertFails {
            StrengthEvidenceJson.decodeSession(JsonObject(row() + ("notes" to JsonNull)).toString())
        }
        assertFails {
            StrengthEvidenceJson.decodeSession(
                JsonObject(row() + ("exercises" to JsonArray(emptyList()))).toString()
            )
        }
    }

    @Test
    fun duplicateSetIdentitiesAndOversizeCollectionsAreRejectedWithoutTruncation() {
        val original = row()
        val item = original.getValue("exercises").jsonArray.single().jsonObject
        val set = item.getValue("sets").jsonArray.single()
        assertFails { StrengthEvidenceJson.decodeSession(withSets(JsonArray(listOf(set, set)))) }
        assertFails { StrengthEvidenceJson.decodeSession(withSets(JsonArray(List(101) { set }))) }
        assertFails {
            StrengthEvidenceJson.decodeSession(
                JsonObject(original + ("exercises" to JsonArray(List(101) { item }))).toString()
            )
        }
    }

    @Test
    fun largestSupportedActualWorkoutRetainsAllTenThousandUniqueSets() {
        val rows =
            List(100) { e ->
                StrengthSession.ExerciseSets(
                    exercise().copy(id = "exercise-$e"),
                    List(100) { s -> set(id = "actual-$e-$s") },
                )
            }
        val original = session(rows)
        val decoded =
            StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeSession(original))
        assertEquals(original, decoded)
        assertEquals(10000, decoded.exercises().sumOf { it.sets().size })
    }

    @Test
    fun acceptedActualsRoundTripEachModalityWithoutReinterpretingWorkingEvidence() {
        for (profile in StrengthExercise.MeasurementProfile.entries) {
            val actual =
                session(
                    listOf(StrengthSession.ExerciseSets(exercise(profile), listOf(set(profile))))
                )
            assertEquals(
                actual,
                StrengthEvidenceJson.decodeAcceptedSession(
                    StrengthEvidenceJson.encodeAcceptedSession(actual)
                ),
            )
        }
    }

    @Test
    fun acceptedActualsRequireCompleteFieldsButLegacyDefaultsStaySupported() {
        val original = row()
        val block = original.getValue("exercises").jsonArray.single().jsonObject
        val exercise = block.getValue("exercise").jsonObject
        val oldDefinition = JsonObject(block + ("exercise" to JsonObject(exercise - "measurement")))
        val old =
            JsonObject(original + ("exercises" to JsonArray(listOf(oldDefinition)))).toString()
        assertEquals(session(), StrengthEvidenceJson.decodeSession(old))
        assertFailsWith<IllegalArgumentException> {
            StrengthEvidenceJson.decodeAcceptedSession(old)
        }
        val unknown = JsonObject(original + ("unknown" to JsonNull)).toString()
        assertEquals(session(), StrengthEvidenceJson.decodeSession(unknown))
        assertFailsWith<IllegalArgumentException> {
            StrengthEvidenceJson.decodeAcceptedSession(unknown)
        }
        val originalSet = block.getValue("sets").jsonArray.single().jsonObject
        val missingRir = withSets(JsonArray(listOf(JsonObject(originalSet - "rir"))))
        assertNull(
            StrengthEvidenceJson.decodeSession(missingRir)
                .exercises()
                .single()
                .sets()
                .single()
                .repetitionsInReserve()
        )
        assertFailsWith<IllegalArgumentException> {
            StrengthEvidenceJson.decodeAcceptedSession(missingRir)
        }
    }

    @Test
    fun strictAcceptedRepetitionTokensDifferFromLegacyWholeNumericCoachingValues() {
        val source = StrengthEvidenceJson.encodeSession(session())
        val exponent = source.replace("\"reps\":8", "\"reps\":8e0")
        assertNotEquals(source, exponent)
        assertEquals(session(), StrengthEvidenceJson.decodeSession(exponent))
        assertFailsWith<IllegalArgumentException> {
            StrengthEvidenceJson.decodeAcceptedSession(exponent)
        }
    }

    @Test
    fun acceptedEncodingValidatesRepresentabilityBeforeFutureStorageWrites() {
        val tooLong = session().copy(id = "x".repeat(257))
        assertFailsWith<IllegalArgumentException> {
            StrengthEvidenceJson.encodeAcceptedSession(tooLong)
        }
        val actual = session()
        assertEquals(
            actual,
            StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeAcceptedSession(actual)),
        )
    }
}
