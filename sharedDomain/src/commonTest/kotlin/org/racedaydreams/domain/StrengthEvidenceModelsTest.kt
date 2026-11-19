package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthExercise.*
import org.racedaydreams.domain.time.*

class StrengthEvidenceModelsTest {
    @Test
    fun canonicalCatalogueKeepsAllOriginalIdentitiesAliasesAndFrozenOrdering() {
        val rows = ExerciseCatalog.definitions()
        assertEquals(220, rows.size)
        assertEquals(220, rows.map { it.exercise().id() }.toSet().size)
        assertEquals("back-squat", rows.first().exercise().id())
        assertEquals("cable-internal-rotation", rows.last().exercise().id())
        assertEquals(rows.map { it.exercise() }, StrengthExercise.starters())
        assertTrue(
            requireNotNull(ExerciseCatalog.definition("romanian-deadlift"))
                .aliases()
                .contains("RDL")
        )
        assertNull(ExerciseCatalog.definition("not-in-the-catalogue"))
        assertNull(ExerciseCatalog.definition(null))
        assertFails { (rows as MutableList<ExerciseCatalog.Definition>).clear() }
        assertFails {
            (requireNotNull(ExerciseCatalog.definition("romanian-deadlift")).aliases()
                    as MutableList<String>)
                .clear()
        }
    }

    @Test
    fun allMeasurementModalitiesRetainActualEvidenceAndProfileValidation() {
        for (profile in MeasurementProfile.values()) {
            val exercise =
                StrengthExercise(
                    "retained",
                    "Retained exercise",
                    Region.FULL,
                    Mode.WEIGHTED,
                    false,
                    profile,
                )
            val set =
                StrengthSession.SetEntry(
                    "actual",
                    if (profile.usesReps()) 8 else 0,
                    if (profile.usesLoad()) 20.0 else 0.0,
                    null,
                    false,
                    if (profile.usesDuration()) 30 else null,
                    if (profile.usesDistance()) 25.0 else null,
                    null,
                )
            assertEquals(set, StrengthSession.ExerciseSets(exercise, listOf(set)).sets().single())
            assertNull(set.coachingRpe())
            assertFailsWith<IllegalArgumentException> {
                StrengthSession.SetEntry("bad", 8, 0.0, null, false, 30, null)
            }
            if (!profile.usesLoad())
                assertFailsWith<IllegalArgumentException> {
                    StrengthSession.ExerciseSets(exercise, listOf(set.copy(kilograms = 20.0)))
                }
        }
        val bodyweight =
            StrengthExercise("body", "Bodyweight", Region.UPPER, Mode.BODYWEIGHT, false)
        assertEquals(MeasurementProfile.REPS_ONLY, bodyweight.measurement())
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.ExerciseSets(
                bodyweight,
                listOf(StrengthSession.SetEntry("a", 8, 5.0, null, false)),
            )
        }
        val weighted =
            bodyweight.copy(
                mode = Mode.WEIGHTED_BODYWEIGHT,
                measurement = MeasurementProfile.LOAD_REPS,
            )
        assertEquals(
            5.0,
            StrengthSession.ExerciseSets(
                    weighted,
                    listOf(StrengthSession.SetEntry("a", 8, 5.0, null, false)),
                )
                .sets()
                .single()
                .kilograms(),
        )
    }

    @Test
    fun warmupsRemainActualWarmupsAndMissingEffortIsNeverAssumedEasy() {
        val recorded = StrengthSession.SetEntry("warm", 5, 40.0, null, true)
        assertTrue(recorded.warmUp())
        assertNull(recorded.coachingRpe())
        val reserve = recorded.copy(id = "working", warmUp = false, repetitionsInReserve = 2.5)
        assertNull(reserve.rpe())
        assertEquals(7.5, reserve.coachingRpe())
        assertEquals(2.5, reserve.repetitionsInReserve())
        assertFailsWith<IllegalArgumentException> { reserve.copy(rpe = 7.0) }
        assertFailsWith<IllegalArgumentException> {
            reserve.copy(repetitionsInReserve = Double.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.SetEntry("timed", 0, 0.0, null, false, 30, null, 2.0)
        }
    }

    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()

    private fun session(exercises: List<StrengthSession.ExerciseSets>) =
        StrengthSession(
            " session ",
            Instants.ofEpochSecond(100, 123456789),
            Offsets.ofTotalSeconds(19800),
            Durations.ofMinutes(30),
            null,
            null,
            exercises,
        )

    @Test
    fun retainedSessionFreezesDefinitionsAndSetsAndRejectsDuplicateActualIdentities() {
        val set = StrengthSession.SetEntry("first", 5, 80.0, null, false)
        val sets = mutableListOf(set)
        val group = StrengthSession.ExerciseSets(bench, sets)
        val groups = mutableListOf(group)
        val recorded = session(groups)
        sets.clear()
        groups.clear()
        assertEquals(" session ", recorded.id())
        assertEquals(123456789, recorded.startedAt().getNano())
        assertEquals(19800, recorded.offset().getTotalSeconds())
        assertEquals("", recorded.notes())
        assertEquals(set, recorded.exercises().single().sets().single())
        assertEquals(bench, recorded.exercises().single().exercise())
        assertFails { (recorded.exercises() as MutableList<StrengthSession.ExerciseSets>).clear() }
        assertFails { (group.sets() as MutableList<StrengthSession.SetEntry>).clear() }
        assertFailsWith<IllegalArgumentException> { session(listOf(group, group)) }
    }

    @Test
    fun originalSetSessionAndEstimateBoundsRejectUnsupportedEvidence() {
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.SetEntry("a", 0, 0.0, null, false)
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.SetEntry("a", 501, 0.0, null, false)
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.SetEntry("a", 1, 1500.1, null, false)
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.SetEntry("a", 1, 0.0, Double.NaN, false)
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.SetEntry("a", 0, 0.0, null, false, 43201, null)
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthSession.SetEntry("a", 0, 0.0, null, false, null, 100001.0)
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthExercise("a", "Name", Region.FULL, Mode.BODYWEIGHT, true)
        }
        assertFailsWith<IllegalArgumentException> {
            bench.copy(measurement = MeasurementProfile.LOAD_DURATION)
        }
        val group =
            StrengthSession.ExerciseSets(
                bench,
                listOf(StrengthSession.SetEntry("a", 1, 0.0, null, false)),
            )
        assertFailsWith<IllegalArgumentException> {
            session(listOf(group)).copy(duration = Durations.ofNanos(0))
        }
        assertFailsWith<IllegalArgumentException> {
            session(listOf(group)).copy(duration = Durations.ofSeconds(43200, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            session(listOf(group)).copy(notes = "a".repeat(10001))
        }
    }
}
