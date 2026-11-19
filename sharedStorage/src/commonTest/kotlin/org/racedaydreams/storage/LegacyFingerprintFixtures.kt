package org.racedaydreams.storage

import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

internal object LegacyFingerprintFixtures {
    fun numbers(): Sequence<Double> = sequence {
        yield(0.0)
        yield(-0.0)
        for (bits in 1L..2048) yield(Double.fromBits(bits))
        for (exponent in 1..1039) {
            val bits = exponent.toLong() shl 52
            for (candidate in listOf(bits - 1, bits, bits + 1)) {
                val value = Double.fromBits(candidate)
                if (value <= 100000) yield(value)
            }
        }
        var bits = 0x1234567812345678L
        repeat(20000) {
            bits = bits xor (bits shl 13)
            bits = bits xor (bits ushr 7)
            bits = bits xor (bits shl 17)
            val value = Double.fromBits(bits and Long.MAX_VALUE)
            if (value.isFinite() && value <= 100000) yield(value)
        }
        repeat(4096) {
            bits = bits xor (bits shl 13)
            bits = bits xor (bits ushr 7)
            bits = bits xor (bits shl 17)
            yield((bits ushr 11).toDouble() / 9007199254740992.0 * 100000)
        }
        for (base in listOf(0.0001, 0.001, 0.1, 0.5, 1.0, 2.5, 10.0, 100.0, 1000.0, 100000.0)) {
            for (delta in -16L..16L) {
                val value = Double.fromBits(base.toBits() + delta)
                if (value <= 100000) yield(value)
            }
        }
        for (v in listOf(0.1, 0.01, 0.001, 0.0001, 2.5, 82.25, 99999.99999999999)) yield(v)
    }

    fun session(
        notes: String = "Recorded / \\ \" 🏋️\u0000\t\b\n\r\u000c\u001f\u2028\u2029"
    ): StrengthSession {
        val profiles = StrengthExercise.MeasurementProfile.entries
        val exercises =
            profiles.mapIndexed { i, p ->
                val e =
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
                    e,
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
            StoredInstant.parse("2026-11-15T09:00:00.123456789Z"),
            Offsets.ofTotalSeconds(19815),
            Durations.ofMinutes(30),
            7.5,
            notes,
            exercises,
        )
    }
}
