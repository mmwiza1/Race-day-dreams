package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class GymWarmupContractTest {
    private val now = Instants.ofEpochSecond(1793102400, 500000000)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val slot =
        GymWorkoutDefinition.Slot(
            "a",
            bench,
            GymWorkoutDefinition.Target(2, 4, 6, null, null, null, 90, 80.0),
            null,
        )

    private fun history(at: Instant = now, effort: Double? = 7.0, warmUp: Boolean = false) =
        listOf(
            StrengthSession(
                "past",
                at,
                Offsets.ofTotalSeconds(0),
                Durations.ofMinutes(5),
                null,
                "",
                listOf(
                    StrengthSession.ExerciseSets(
                        bench,
                        listOf(
                            StrengthSession.SetEntry("one", 6, 100.0, effort, warmUp),
                            StrengthSession.SetEntry("two", 6, 90.0, effort, warmUp),
                        ),
                    )
                ),
            )
        )

    @Test
    fun examplesUseRecentExactActualsAndNeverThePrescriptionAlone() {
        val suggestion =
            GymWarmup.suggest(slot, history(), emptyList(), GymProgramme.Experience.BEGINNER, now)
        assertEquals(listOf(32.0, 52.0), suggestion.ramps().map { it.kilograms() })
        assertEquals(listOf(6, 4), suggestion.ramps().map { it.repetitions() })
        assertEquals(
            3,
            GymWarmup.suggest(
                    slot,
                    history(),
                    emptyList(),
                    GymProgramme.Experience.EXPERIENCED,
                    now,
                )
                .ramps()
                .last()
                .repetitions(),
        )
        assertTrue(
            GymWarmup.suggest(slot, emptyList(), emptyList(), GymProgramme.Experience.BEGINNER, now)
                .ramps()
                .isEmpty()
        )
        assertEquals(80.0, slot.target()!!.referenceLoadKg())
        assertEquals(100.0, history().single().exercises().single().sets().first().kilograms())
    }

    @Test
    fun staleFutureUnknownEffortAndWarmupsCannotJustifyLoadExamples() {
        val oldest = Instants.ofEpochSecond(now.getEpochSecond() - 28L * 24 * 60 * 60, 500000000)
        assertEquals(
            2,
            GymWarmup.suggest(
                    slot,
                    history(oldest),
                    emptyList(),
                    GymProgramme.Experience.INTERMEDIATE,
                    now,
                )
                .ramps()
                .size,
        )
        for (history in
            listOf(
                history(Instants.ofEpochSecond(oldest.getEpochSecond(), 499999999)),
                history(Instants.ofEpochSecond(now.getEpochSecond(), 500000001)),
                history(effort = null),
                history(warmUp = true),
            )) {
            val suggestion =
                GymWarmup.suggest(slot, history, emptyList(), GymProgramme.Experience.BEGINNER, now)
            assertTrue(suggestion.ramps().isEmpty())
            assertTrue(suggestion.why()!!.contains("No kilograms are invented"))
        }
    }

    @Test
    fun completedMovementOrRelatedWorkingSetsSuppressAdditionalNumericalRamps() {
        val set = StrengthSession.SetEntry("today", 6, 80.0, 7.0, false)
        val done =
            GymWarmup.suggest(
                slot,
                history(),
                listOf(GymExecutionSet("a", bench, set)),
                GymProgramme.Experience.BEGINNER,
                now,
            )
        assertTrue(done.ramps().isEmpty())
        assertTrue(done.instruction()!!.contains("already recorded working sets"))
        val pushup = ExerciseCatalog.definition("push-up")!!.exercise()
        val related =
            GymWarmup.suggest(
                slot,
                history(),
                listOf(
                    GymExecutionSet("b", pushup, StrengthSession.SetEntry("bw", 6, 0.0, 7.0, false))
                ),
                GymProgramme.Experience.BEGINNER,
                now,
            )
        assertTrue(related.ramps().isEmpty())
        assertTrue(related.instruction()!!.contains("related movement"))
        val practice =
            GymWarmup.suggest(
                slot,
                history(),
                listOf(
                    GymExecutionSet(
                        "a",
                        bench,
                        StrengthSession.SetEntry("warm", 6, 20.0, 7.0, true),
                    )
                ),
                GymProgramme.Experience.BEGINNER,
                now,
            )
        assertEquals(2, practice.ramps().size)
    }

    @Test
    fun nonWeightedAndTimedTasksRetainGeneralPreparationWithoutInventedLoads() {
        for (id in listOf("push-up", "plank")) {
            val exercise = ExerciseCatalog.definition(id)!!.exercise()
            val suggestion =
                GymWarmup.suggest(
                    GymWorkoutDefinition.Slot("a", exercise, null, null),
                    history(),
                    emptyList(),
                    GymProgramme.Experience.EXPERIENCED,
                    now,
                )
            assertTrue(suggestion.ramps().isEmpty())
            assertTrue(suggestion.instruction()!!.contains("Practise the movement gently"))
        }
    }
}
