package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.StrengthEvidenceJson

/**
 * Real original Android parser parity; actual definitions are never replaced with today's
 * catalogue.
 */
@RunWith(AndroidJUnit4::class)
class SharedStrengthEvidenceCodecTest {
    private fun session(
        profile: StrengthExercise.MeasurementProfile,
        mode: StrengthExercise.Mode =
            if (profile.usesLoad()) StrengthExercise.Mode.WEIGHTED
            else StrengthExercise.Mode.BODYWEIGHT,
    ): StrengthSession {
        val exercise =
            StrengthExercise(
                "old-recorded-exercise",
                "Frozen original definition",
                StrengthExercise.Region.FULL,
                mode,
                profile == StrengthExercise.MeasurementProfile.LOAD_REPS &&
                    mode == StrengthExercise.Mode.WEIGHTED,
                profile,
            )
        val working =
            StrengthSession.SetEntry(
                "working",
                if (profile.usesReps()) 8 else 0,
                if (profile.usesLoad()) 82.25 else 0.0,
                null,
                false,
                if (profile.usesDuration()) 30 else null,
                if (profile.usesDistance()) 125.5 else null,
                if (profile.usesReps()) 2.5 else null,
            )
        val warm = working.copy(id = "warm", warmUp = true, rpe = 3.5, repetitionsInReserve = null)
        return StrengthSession(
            "actual-session",
            Instant.parse("2026-11-15T09:00:00.123456789Z"),
            ZoneOffset.ofTotalSeconds(19815),
            Duration.ofSeconds(5400, 123456789),
            null,
            "Retained actual 🏋️\u0000 note",
            listOf(StrengthSession.ExerciseSets(exercise, listOf(warm, working))),
        )
    }

    @Test
    fun allModalityAndEffortPayloadsReadBothWaysWithoutChangingRecordedEvidence() {
        val profiles =
            StrengthExercise.MeasurementProfile.values().map { session(it) } +
                session(
                    StrengthExercise.MeasurementProfile.LOAD_REPS,
                    StrengthExercise.Mode.WEIGHTED_BODYWEIGHT,
                )
        for (original in profiles) {
            assertEquals(
                original,
                StrengthEvidenceJson.decodeSession(CoachingJson.strength(original).toString()),
            )
            assertEquals(
                original,
                CoachingJson.strength(JSONObject(StrengthEvidenceJson.encodeSession(original))),
            )
            val exercise = original.exercises().single().exercise()
            assertEquals(
                exercise,
                StrengthEvidenceJson.decodeExercise(CoachingJson.exercise(exercise).toString()),
            )
            assertEquals(
                exercise,
                CoachingJson.exercise(JSONObject(StrengthEvidenceJson.encodeExercise(exercise))),
            )
        }
    }

    @Test
    fun legacyMissingMeasurementsRemainUnknownAndMalformedActualsFailBothReaders() {
        val original = session(StrengthExercise.MeasurementProfile.LOAD_REPS)
        val payload = CoachingJson.strength(original)
        val exercise = payload.getJSONArray("exercises").getJSONObject(0)
        exercise.getJSONObject("exercise").remove("measurement")
        val sets = exercise.getJSONArray("sets")
        for (i in 0 until sets.length()) {
            sets.getJSONObject(i).remove("seconds")
            sets.getJSONObject(i).remove("metres")
            sets.getJSONObject(i).remove("rir")
        }
        val expected = CoachingJson.strength(payload)
        val decoded = StrengthEvidenceJson.decodeSession(payload.toString())
        assertEquals(expected, decoded)
        assertNull(decoded.exercises().single().sets()[1].rpe())
        assertNull(decoded.exercises().single().sets()[1].repetitionsInReserve())
        sets.getJSONObject(1).put("seconds", 30.5)
        assertThrows(IllegalArgumentException::class.java) { CoachingJson.strength(payload) }
        assertThrows(IllegalArgumentException::class.java) {
            StrengthEvidenceJson.decodeSession(payload.toString())
        }
    }
}
