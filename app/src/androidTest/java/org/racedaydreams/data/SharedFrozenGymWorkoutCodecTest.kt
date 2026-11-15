package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.FrozenGymWorkoutJson

@RunWith(AndroidJUnit4::class)
class SharedFrozenGymWorkoutCodecTest {
    private fun slot(
        id: String = "slot",
        profile: StrengthExercise.MeasurementProfile = StrengthExercise.MeasurementProfile.LOAD_REPS,
    ): GymWorkoutDefinition.Slot {
        val exercise =
            StrengthExercise(
                "frozen-exercise",
                "Frozen historical definition",
                StrengthExercise.Region.FULL,
                if (profile.usesLoad()) StrengthExercise.Mode.WEIGHTED
                else StrengthExercise.Mode.BODYWEIGHT,
                profile == StrengthExercise.MeasurementProfile.LOAD_REPS,
                profile,
            )
        val target =
            GymWorkoutDefinition.Target(
                3,
                if (profile.usesReps()) 4 else null,
                if (profile.usesReps()) 6 else null,
                if (profile.usesDuration()) 30 else null,
                if (profile.usesDistance()) 125.5 else null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 5.5, 7.0),
                180,
                if (profile.usesLoad()) 82.25 else null,
            )
        return GymWorkoutDefinition.Slot(id, exercise, target, null)
    }

    private fun workout(slots: List<GymWorkoutDefinition.Slot> = listOf(slot())) =
        GymWorkoutDefinition(
            "workout",
            3,
            Instant.parse("2026-11-15T09:00:00.123456789Z"),
            "Frozen intention 🏋️\u0000",
            slots,
        )

    @Test
    fun originalAndSharedReadersAgreeAcrossAllModalitiesAndOpenTargets() {
        val all =
            StrengthExercise.MeasurementProfile.values().map {
                workout(listOf(slot(profile = it)))
            } + workout(listOf(slot().copy(target = null)))
        for (original in all) {
            assertEquals(original, FrozenGymWorkoutJson.decode(GymWorkoutJson.write(original)))
            assertEquals(original, GymWorkoutJson.read(FrozenGymWorkoutJson.encode(original)))
        }
    }

    @Test
    fun originalAndSharedReadersRejectDuplicateUnknownAndFractionalIntegerFields() {
        val original = GymWorkoutJson.write(workout())
        val invalid =
            listOf(
                original.replace("\"revision\":3", "\"revision\":3.0"),
                original.replace("\"revision\":3", "\"revision\":3e0"),
                original.replace("\"version\":1", "\"version\":2"),
                original.dropLast(1) + ",\"extra\":null}",
                "{\"x\":1,\"\\u0078\":2}",
                original + "{}",
                original.replace("\"sets\":3", "\"sets\":\"3\""),
            )
        for (payload in invalid) {
            assertNotEquals(original, payload)
            assertThrows(IllegalArgumentException::class.java) { GymWorkoutJson.read(payload) }
            assertThrows(IllegalArgumentException::class.java) {
                FrozenGymWorkoutJson.decode(payload)
            }
        }
    }

    @Test
    fun supersetsRirReferencesAndMaximumSlotCountKeepFrozenMeaning() {
        val first =
            slot("first")
                .copy(
                    supersetGroup = "pair",
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
                            ),
                )
        val second = slot("second").copy(supersetGroup = "pair")
        val original = workout(listOf(first, second) + List(98) { slot("slot-$it") })
        assertEquals(original, FrozenGymWorkoutJson.decode(GymWorkoutJson.write(original)))
        assertEquals(original, GymWorkoutJson.read(FrozenGymWorkoutJson.encode(original)))
        assertNull(
            FrozenGymWorkoutJson.decode(GymWorkoutJson.write(original))
                .slots()
                .first()
                .target()!!
                .referenceLoadKg()
        )
    }
}
