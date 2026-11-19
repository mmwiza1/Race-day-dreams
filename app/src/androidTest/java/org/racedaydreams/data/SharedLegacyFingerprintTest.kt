package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.MessageDigest
import java.time.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedLegacyFingerprintTest {
    private val at = Instant.parse("2026-11-15T09:00:00.123456789Z")

    private fun session(notes: String): StrengthSession {
        val exercises =
            StrengthExercise.MeasurementProfile.entries.mapIndexed { i, p ->
                val exercise =
                    StrengthExercise(
                        "exercise-$i",
                        "Exercise / $i",
                        StrengthExercise.Region.UPPER,
                        if (p.usesLoad()) StrengthExercise.Mode.WEIGHTED
                        else StrengthExercise.Mode.BODYWEIGHT,
                        p == StrengthExercise.MeasurementProfile.LOAD_REPS,
                        p,
                    )
                StrengthSession.ExerciseSets(
                    exercise,
                    listOf(
                        StrengthSession.SetEntry(
                            "set-$i",
                            if (p.usesReps()) 6 else 0,
                            if (p.usesLoad()) 82.25 else 0.0,
                            if (p.usesReps()) null else 7.5,
                            i == 0,
                            if (p.usesDuration()) 45L else null,
                            if (p.usesDistance()) 123.5 else null,
                            if (p.usesReps()) 2.5 else null,
                        )
                    ),
                )
            } +
                StrengthSession.ExerciseSets(
                    StrengthExercise(
                        "weighted-pull-up",
                        "Weighted pull-up",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED_BODYWEIGHT,
                        false,
                    ),
                    listOf(StrengthSession.SetEntry("weighted", 8, 10.0, 8.5, false)),
                )
        return StrengthSession(
            "actual",
            at,
            ZoneOffset.ofTotalSeconds(19815),
            Duration.ofMinutes(30),
            7.5,
            notes,
            exercises,
        )
    }

    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            (it.toInt() and 255).toString(16).padStart(2, '0')
        }

    private fun compare(actual: StrengthSession) {
        val expected = CoachingJson.strength(actual).toString()
        assertEquals(expected, PortableLegacyStrengthFingerprint.input(actual))
        assertArrayEquals(
            expected.toByteArray(Charsets.UTF_8),
            PortableLegacyStrengthFingerprint.inputBytes(actual),
        )
        assertEquals(
            sha(expected.toByteArray(Charsets.UTF_8)),
            PortableLegacyStrengthFingerprint.fingerprint(actual),
        )
    }

    @Test
    fun originalWireBytesMatchAllModalitiesEscapesUnicodeNullsAndPreciseTime() {
        val notes = "Recorded / \\ \" 🏋️\u0000\t\b\n\r\u000c\u001f\u2028\u2029"
        val normal = session(notes)
        compare(normal)
        assertEquals(
            "1c7caee335838b43cfb111bffb0b90fcda0ec12bcc2a1d61cbd8998bcef69cf0",
            PortableLegacyStrengthFingerprint.fingerprint(normal),
        )
        compare(session("\ud800/\udc00/💪"))
        compare(
            session("")
                .copy(
                    id = "id-\ud800",
                    offset = ZoneOffset.MIN,
                    duration = Duration.ofSeconds(1, 42),
                )
        )
        val sets = normal.exercises().first()
        for (kg in listOf(0.0, -0.0, Double.MIN_VALUE, 0.0001, 0.001, 0.1, 1500.0)) compare(
            normal.copy(
                exercises =
                    listOf(sets.copy(sets = listOf(sets.sets().single().copy(kilograms = kg))))
            )
        )
        compare(
            normal.copy(
                exercises =
                    listOf(
                        sets.copy(
                            sets =
                                listOf(
                                    sets
                                        .sets()
                                        .single()
                                        .copy(rpe = 8.5, repetitionsInReserve = null)
                                )
                        )
                    )
            )
        )
    }

    @Test
    fun originalAndroidNumberSpellingMatchesSubnormalsBoundariesAndEverydayValues() {
        val exercise =
            StrengthExercise(
                "distance",
                "Distance",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.BODYWEIGHT,
                false,
                StrengthExercise.MeasurementProfile.DISTANCE,
            )
        fun verify(value: Double) {
            if (!value.isFinite() || value <= 0 || value > 100000) return
            val actual =
                StrengthSession(
                    "actual",
                    at,
                    ZoneOffset.UTC,
                    Duration.ofSeconds(1),
                    null,
                    "",
                    listOf(
                        StrengthSession.ExerciseSets(
                            exercise,
                            listOf(
                                StrengthSession.SetEntry("set", 0, 0.0, null, false, null, value)
                            ),
                        )
                    ),
                )
            assertEquals(
                CoachingJson.strength(actual).toString(),
                PortableLegacyStrengthFingerprint.input(actual),
            )
            val encoded =
                JSONObject(PortableLegacyStrengthFingerprint.input(actual))
                    .getJSONArray("exercises")
                    .getJSONObject(0)
                    .getJSONArray("sets")
                    .getJSONObject(0)
                    .get("metres")
            assertEquals(value.toBits(), (encoded as Number).toDouble().toBits())
        }
        for (bits in 1L..2048) verify(Double.fromBits(bits))
        for (exponent in 1..1039) for (delta in -1L..1L) verify(
            Double.fromBits((exponent.toLong() shl 52) + delta)
        )
        var bits = 0x1234567812345678L
        repeat(4096) {
            bits = bits xor (bits shl 13)
            bits = bits xor (bits ushr 7)
            bits = bits xor (bits shl 17)
            verify((bits ushr 11).toDouble() / 9007199254740992.0 * 100000)
        }
    }

    @Test
    fun validatingOriginalReceiptsDoesNotRehashOrRewriteRetainedProofStrings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        try {
            val db = ProfileDatabase.get(context).writableDatabase
            val actual = session("Original / source")
            CoachingStore(context).saveStrength(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            val original = SqliteGymExecutionRepository(context)
            original.saveWorkout(repeat.definition(), repeat.source())
            fun payload(): String =
                db.rawQuery(
                        "SELECT payload FROM strength_workout_versions WHERE workout_id='repeat' AND revision=1",
                        null,
                    )
                    .use {
                        assertTrue(it.moveToFirst())
                        it.getString(0)
                    }
            val before = payload()
            val shared =
                SqliteGymExecutions(
                    AndroidSqlTestConnection(db),
                    { Instant.now() },
                    PortableLegacyStrengthFingerprint,
                )
            shared.saveWorkout(repeat.definition(), repeat.source())
            assertEquals(before, payload())
            val oldHash =
                FrozenGymWorkoutRecordJson.decode(before).sourceSnapshot!!.originalSourceFingerprint
            val edited = actual.copy(notes = "Explicit edit / source")
            shared.saveActual(edited)
            original.saveWorkout(repeat.definition(), repeat.source())
            val proof = FrozenGymWorkoutRecordJson.decode(payload()).sourceSnapshot!!
            assertEquals(oldHash, proof.originalSourceFingerprint)
            assertEquals(
                sha(CoachingJson.strength(edited).toString().toByteArray(Charsets.UTF_8)),
                proof.currentSourceFingerprint,
            )
        } finally {
            ProfileDatabase.get(context).close()
            context.deleteDatabase(ProfileDatabase.NAME)
        }
    }
}
