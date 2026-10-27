package org.racedaydreams.coach

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.GymProgrammeOperations
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.GymProgrammeGenerator
import org.racedaydreams.domain.strength.StrengthExercise

/** Exercise/programme previews preserve intentions; acceptance remains a separate command. */
@RunWith(AndroidJUnit4::class)
class GymProgrammeOperationsTest {
    private val now = Instant.parse("2026-10-20T08:00:00Z")
    private val today = LocalDate.of(2026, 10, 20)
    private val operations =
        GymProgrammeOperations(Clock.fixed(now, ZoneOffset.UTC)) { "new-programme" }

    @Test
    fun regenerationPreviewRetainsIdentityAndDoesNotRewriteTheOriginalVersion() {
        val original = plan()
        val proposed =
            operations.generate(original, original.settings(), emptyList(), emptyList(), emptySet())
        assertEquals(original.id(), proposed.id())
        assertEquals(original.version() + 1, proposed.version())
        assertEquals(now, proposed.createdAt())
        assertEquals(1, original.version())
        assertEquals("bench-press", original.sessions().first().exercises().first().exerciseId())
        assertEquals(80.0, original.sessions().first().exercises().first().referenceLoadKg()!!, 0.0)
        assertTrue(proposed.sessions().isNotEmpty())
    }

    @Test
    fun oneSessionPreviewPreservesEveryOtherPrescriptionAndRecurringPreferences() {
        val original = plan()
        val proposed = operations.reduce(original, "first", emptySet())
        assertEquals(original.settings(), proposed.settings())
        assertEquals(original.sessions()[1], proposed.sessions()[1])
        assertEquals(3, original.sessions().first().exercises().first().sets())
        assertEquals(2, proposed.sessions().first().exercises().first().sets())
        assertEquals(2, proposed.version())
    }

    @Test
    fun completedPrescriptionIsProtectedForEveryPreviewAction() {
        val original = plan()
        val completed = setOf(original.sessions().first().date())
        rejects { operations.move(original, "first", today.plusDays(2), completed) }
        rejects { operations.reduce(original, "first", completed) }
        rejects { operations.skip(original, "first", completed) }
        rejects {
            operations.swap(original, "first", "bench-press", "dumbbell-bench-press", completed)
        }
        assertEquals(1, original.version())
        assertEquals(2, original.sessions().size)
    }

    @Test
    fun substitutionPreviewDropsIncompatibleLoadReferenceAndKeepsHistoryIntact() {
        val original = plan()
        val proposed =
            operations.swap(original, "first", "bench-press", "dumbbell-bench-press", emptySet())
        val replacement = proposed.sessions().first().exercises().first()
        assertEquals("dumbbell-bench-press", replacement.exerciseId())
        assertNull(
            "A barbell reference must not be copied to dumbbells",
            replacement.referenceLoadKg(),
        )
        assertEquals(80.0, original.sessions().first().exercises().first().referenceLoadKg()!!, 0.0)
        assertEquals(original.sessions()[1], proposed.sessions()[1])
    }

    @Test
    fun alternativePreviewRespectsExistingMeasurementAndEquipmentRules() {
        val original = plan(setOf("dumbbells", "bench"))
        val exercises = StrengthExercise.starters()
        val alternatives = operations.alternatives(original, "bench-press", "", exercises)
        assertTrue(alternatives.any { it.id() == "dumbbell-bench-press" })
        assertTrue(alternatives.none { it.id() == "bench-press" })
        assertTrue(
            alternatives.all { it.measurement() == StrengthExercise.MeasurementProfile.LOAD_REPS }
        )
        assertTrue(alternatives.all { ExerciseCatalog.definition(it.id()).category() == "Chest" })
        assertTrue(
            alternatives.all {
                GymProgrammeGenerator.available(it.id(), original.settings().equipment())
            }
        )
    }

    @Test
    fun conflictPreviewUsesActualExerciseContentAndIncludesPostRunRecovery() {
        val original = plan()
        val keyRun = PlannedWorkout("key-run", today, WorkoutCategory.INTERVALS, 6.0, null, emptyList(), "Key run")
        val session = original.sessions().first()
        assertTrue(operations.conflicts(session, listOf(keyRun), emptyList()).isEmpty())
        val lower = session.copyExercise("back-squat")
        val warnings = operations.conflicts(lower, listOf(keyRun), emptyList())
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("day after"))
        assertTrue(warnings.single().contains("Review recovery"))
        val core = session.copyExercise("plank")
        assertTrue(operations.conflicts(core, listOf(keyRun), emptyList()).isEmpty())
        assertEquals("bench-press", session.exercises().first().exerciseId())
    }

    private fun GymProgramme.Session.copyExercise(id: String) =
        GymProgramme.Session(
            id(), date(), title(),
            listOf(GymProgramme.Prescription(id, 2, 6, 8, null, null, 6, 8, 90, "Retained work")),
            explanation(),
        )

    @Test
    fun retainedOperationsObserveTimezoneChangesWhileInjectedClocksStayFixed() {
        val originalZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+14:00"))
            val retained = GymProgrammeOperations(newId = { "timezone-programme" })
            val settings = plan().settings()
            val before = retained.generate(null, settings, emptyList(), emptyList(), emptySet())
            assertEquals(LocalDate.now(), before.start())

            TimeZone.setDefault(TimeZone.getTimeZone("GMT-12:00"))
            val changedDate = LocalDate.now()
            val after = retained.generate(null, settings, emptyList(), emptyList(), emptySet())
            assertEquals(changedDate, after.start())
            assertNotEquals(
                "The two zones must expose the cached-zone regression",
                before.start(),
                after.start(),
            )
            assertEquals(
                "An injected clock must ignore device timezone changes",
                today,
                operations.generate(null, settings, emptyList(), emptyList(), emptySet()).start(),
            )

            val original = plan()
            val current =
                GymProgramme(
                    original.id(),
                    original.version(),
                    Instant.now(),
                    original.reason(),
                    settings,
                    changedDate,
                    changedDate.plusDays(41),
                    listOf(
                        GymProgramme.Session(
                            "first",
                            changedDate,
                            "Today",
                            original.sessions().first().exercises(),
                            "Today's preserved prescription",
                        )
                    ),
                    emptyList(),
                )
            assertEquals(2, retained.move(current, "first", changedDate, emptySet()).version())
            assertEquals(2, retained.reduce(current, "first", emptySet()).version())
            assertTrue(retained.skip(current, "first", emptySet()).sessions().isEmpty())
            assertEquals(
                "dumbbell-bench-press",
                retained
                    .swap(current, "first", "bench-press", "dumbbell-bench-press", emptySet())
                    .sessions()
                    .first()
                    .exercises()
                    .first()
                    .exerciseId(),
            )
            assertEquals("bench-press", current.sessions().first().exercises().first().exerciseId())
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }

    private fun plan(equipment: Set<String> = setOf("full gym")): GymProgramme {
        val settings =
            GymProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.BEGINNER,
                setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY),
                2,
                equipment,
                45,
                GymProgramme.Effort.RPE,
            )
        val bench =
            GymProgramme.Prescription(
                "bench-press",
                3,
                5,
                8,
                null,
                null,
                6,
                8,
                120,
                "Preserved horizontal push",
                80.0,
            )
        val row =
            GymProgramme.Prescription(
                "dumbbell-row",
                3,
                8,
                12,
                null,
                null,
                6,
                8,
                90,
                "Preserved pull",
                null,
            )
        return GymProgramme(
            "preserved-programme",
            1,
            now.minusSeconds(3600),
            "Original intention",
            settings,
            today,
            today.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "first",
                    today.plusDays(1),
                    "First session",
                    listOf(bench, row),
                    "Original first prescription",
                ),
                GymProgramme.Session(
                    "second",
                    today.plusDays(4),
                    "Second session",
                    listOf(row),
                    "Original second prescription",
                ),
            ),
            emptyList(),
        )
    }

    private fun rejects(change: () -> Unit) {
        try {
            change()
            fail("A completed prescription was changed")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("cannot be rewritten"))
        }
    }
}
