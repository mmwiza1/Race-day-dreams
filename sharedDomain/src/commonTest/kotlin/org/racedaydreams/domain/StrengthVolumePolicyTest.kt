package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class StrengthVolumePolicyTest {
    private val monday = CalendarDates.of(2026, 11, 9)
    private val at = Instants.ofEpochSecond(monday.toEpochDay() * 86400 + 43200)
    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()

    private fun source(
        exercise: StrengthExercise = bench,
        sets: List<StrengthSession.SetEntry> =
            listOf(StrengthSession.SetEntry("working", 5, 80.0, null, false)),
    ) =
        StrengthSession(
            "actual",
            at,
            Offsets.ofTotalSeconds(0),
            Durations.ofMinutes(30),
            null,
            null,
            listOf(StrengthSession.ExerciseSets(exercise, sets)),
        )

    @Test
    fun windowsKeepMondayAnchorsEmptyWeeksAndDuplicateEvidenceRules() {
        val rows = StrengthVolume.weekly(listOf(source(), source()), monday.plusDays(3), 3, at)
        assertEquals(3, rows.size)
        assertEquals(
            listOf(monday, monday.plusWeeks(1), monday.plusWeeks(2)),
            rows.map { it.monday() },
        )
        assertEquals(listOf(1, 0, 0), rows.map { it.workouts() })
        assertEquals(listOf(1, 0, 0), rows.map { it.workingSets() })
        assertFails { (rows as MutableList<StrengthVolume.Week>).clear() }
        assertFailsWith<IllegalArgumentException> {
            StrengthVolume.weekly(
                listOf(source(), source().copy(notes = "Conflicting")),
                monday,
                1,
                at,
            )
        }
        assertEquals(
            0,
            StrengthVolume.weekly(
                    listOf(source()),
                    monday,
                    1,
                    Instants.ofEpochSecond(at.getEpochSecond() - 1),
                )
                .single()
                .workouts(),
        )
        assertFailsWith<IllegalArgumentException> {
            StrengthVolume.weekly(emptyList(), monday, 2601, at)
        }
    }

    @Test
    fun workingSetAttributionAndExternalLoadStaySeparateFromWarmupsAndBodyMass() {
        val sets =
            listOf(
                StrengthSession.SetEntry("warm", 5, 120.0, null, true),
                StrengthSession.SetEntry("a", 5, 80.0, null, false),
                StrengthSession.SetEntry("b", 5, 80.0, null, false),
            )
        val week = StrengthVolume.weekly(listOf(source(sets = sets)), monday, 1, at).single()
        assertEquals(2, week.workingSets())
        assertEquals(2.0, week.primarySetShares().values.filterNotNull().sum(), 1e-12)
        assertEquals(10L, week.exercises().single().repetitions())
        assertEquals(800.0, week.exercises().single().loadRepetitions())
        assertEquals(0, week.unclassifiedSets())
        val push = requireNotNull(ExerciseCatalog.definition("push-up")).exercise()
        val body = source(push, listOf(StrengthSession.SetEntry("body", 10, 0.0, null, false)))
        assertNull(
            StrengthVolume.weekly(listOf(body), monday, 1, at)
                .single()
                .exercises()
                .single()
                .loadRepetitions()
        )
        val warmOnly =
            StrengthVolume.weekly(listOf(source(sets = listOf(sets.first()))), monday, 1, at)
                .single()
        assertEquals(1, warmOnly.workouts())
        assertEquals(0, warmOnly.workingSets())
        assertTrue(warmOnly.exercises().isEmpty())
    }

    @Test
    fun changedOrUnknownExerciseDefinitionsStayUnclassifiedWithoutLosingActualTasks() {
        val changed = bench.copy(name = "Retained older name")
        val week = StrengthVolume.weekly(listOf(source(changed)), monday, 1, at).single()
        assertEquals(1, week.unclassifiedSets())
        assertTrue(week.primarySetShares().isEmpty())
        assertEquals(changed, week.exercises().single().exercise())
        assertEquals(400.0, week.exercises().single().loadRepetitions())
        val hold =
            StrengthExercise(
                "custom-hold",
                "Hold",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_DURATION,
            )
        val actual =
            source(hold, listOf(StrengthSession.SetEntry("hold", 0, 20.0, null, false, 30, null)))
        val measured = StrengthVolume.weekly(listOf(actual), monday, 1, at).single()
        assertEquals(1, measured.unclassifiedSets())
        assertEquals(30L, measured.exercises().single().seconds())
        assertNull(measured.exercises().single().loadRepetitions())
    }

    @Test
    fun originalEnumMapSnapshotsRetainOrderAndNullableRawValuesWithoutMutationEscapes() {
        val primary =
            linkedMapOf(
                ExerciseMetadata.BodyArea.GLUTES to null,
                ExerciseMetadata.BodyArea.CHEST to 2.0,
            )
        val raw = StrengthVolume.Week(null, 0, 0, 0, primary, emptyMap(), emptyList())
        primary.clear()
        assertEquals(
            listOf(ExerciseMetadata.BodyArea.CHEST, ExerciseMetadata.BodyArea.GLUTES),
            raw.primarySetShares().keys.toList(),
        )
        assertNull(raw.primarySetShares()[ExerciseMetadata.BodyArea.GLUTES])
        assertNull(raw.monday())
        assertFails {
            (raw.primarySetShares() as MutableMap<ExerciseMetadata.BodyArea, Double?>).clear()
        }
        assertFails {
            (raw.primarySetShares().entries.first()
                    as MutableMap.MutableEntry<ExerciseMetadata.BodyArea, Double?>)
                .setValue(99.0)
        }
        assertTrue(StrengthVolume.attribution().contains("not exact muscle stimulus"))
    }
}
