package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.time.DayOfWeek

class TrainingPreferencesTest {
    private fun preferences(
        running: Set<DayOfWeek?>,
        gym: Set<DayOfWeek?>,
        count: Int = 2,
        longDay: DayOfWeek? = DayOfWeek.SUNDAY,
    ) =
        TrainingPreferences(
            TrainingStrategy.BALANCED,
            running,
            count,
            longDay,
            gym,
            ScheduleType.FLEXIBLE,
            UnitSystem.METRIC,
        )

    @Test
    fun isoDaysKeepStoredNamesOrderingAndNumbersOnBothPlatforms() {
        assertEquals(
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
            DayOfWeek.entries.map { it.name },
        )
        assertEquals((1..7).toList(), DayOfWeek.entries.map { it.getValue() })
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.valueOf("SUNDAY"))
    }

    @Test
    fun availabilityCopiesInputAndRetainsIndependentOverlappingGymDays() {
        val running = linkedSetOf<DayOfWeek?>(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        val gym = linkedSetOf<DayOfWeek?>(DayOfWeek.SUNDAY)
        val saved = preferences(running, gym)
        running.clear()
        gym.clear()
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
            saved.runningDays().toList(),
        )
        assertEquals(setOf(DayOfWeek.SUNDAY), saved.gymDays())
        assertEquals(2, saved.runsPerWeek())
        assertFails { (saved.runningDays() as MutableSet<DayOfWeek>).clear() }
        assertFails { (saved.gymDays().iterator() as MutableIterator<DayOfWeek>).remove() }
        assertEquals(setOf(DayOfWeek.SUNDAY), saved.gymDays())
    }

    @Test
    fun unavailableDaysAndMissingInputsAreRejectedWithoutInventingAvailability() {
        assertFailsWith<IllegalArgumentException> {
            preferences(setOf(DayOfWeek.MONDAY), emptySet())
        }
        assertFailsWith<IllegalArgumentException> {
            preferences(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), emptySet())
        }
        assertFailsWith<IllegalArgumentException> {
            preferences(setOf(DayOfWeek.SUNDAY), emptySet(), 0)
        }
        assertFailsWith<NullPointerException> {
            preferences(setOf(DayOfWeek.SUNDAY, null), emptySet())
        }
        assertFailsWith<NullPointerException> {
            TrainingPreferences(
                null,
                emptySet(),
                1,
                null,
                emptySet(),
                ScheduleType.FIXED,
                UnitSystem.METRIC,
            )
        }
        assertEquals(emptySet(), preferences(setOf(DayOfWeek.SUNDAY), emptySet(), 1).gymDays())
    }

    @Test
    fun retainedPreferencesKeepStructuralEqualityAndRecordPresentation() {
        val a = preferences(linkedSetOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), emptySet())
        val b = preferences(linkedSetOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), emptySet())
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(
            "TrainingPreferences[strategy=BALANCED, runningDays=[MONDAY, SUNDAY], runsPerWeek=2, longRunDay=SUNDAY, gymDays=[], scheduleType=FLEXIBLE, units=METRIC]",
            a.toString(),
        )
    }
}
