package org.racedaydreams.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.racedaydreams.domain.TrainingPhase
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.time.CalendarDates

class PlanJourneyTest {
    private val start = CalendarDates.of(2026, 11, 2)
    private val end = start.plusDays(27) // four weeks, race on the last day
    private val phases =
        listOf(
            TrainingPhase(TrainingPhase.Kind.FOUNDATION, start, start.plusDays(13), null),
            TrainingPhase(TrainingPhase.Kind.TAPER, start.plusDays(14), start.plusDays(26), null),
            TrainingPhase(TrainingPhase.Kind.RACE, end, end, null),
        )

    @Test
    fun phasesBecomeProportionalSegmentsAndTodayNamesTheCurrentPhase() {
        val journey = planJourney(start, end, phases, start.plusDays(15), end)
        assertEquals(3, journey.segments.size)
        assertEquals(0f, journey.segments[0].start)
        assertEquals(0.5f, journey.segments[0].end)
        assertEquals(0.5f, journey.segments[1].start)
        assertEquals(1f, journey.segments.last().end)
        assertEquals(TrainingPhase.Kind.TAPER, journey.currentPhase?.kind())
        assertTrue(journey.segments[1].current)
        assertTrue(journey.todayFraction > 0.5f && journey.todayFraction < 0.6f)
        assertEquals(1f, journey.raceFraction)
        assertEquals("Taper", phaseName(TrainingPhase.Kind.TAPER))
        assertEquals("Race specific", phaseName(TrainingPhase.Kind.RACE_SPECIFIC))
    }

    @Test
    fun datesOutsideTheProgrammeStayOnTheTrackAndRaceOutsideIsNotMarked() {
        val before = planJourney(start, end, phases, start.minusDays(5), end.plusDays(3))
        assertEquals(0f, before.todayFraction)
        assertEquals(TrainingPhase.Kind.FOUNDATION, before.currentPhase?.kind())
        assertNull(before.raceFraction)
        val after = planJourney(start, end, phases, end.plusDays(10), null)
        assertEquals(1f, after.todayFraction)
        assertEquals(TrainingPhase.Kind.RACE, after.currentPhase?.kind())
        val none = planJourney(start, end, emptyList(), start, null)
        assertEquals(1, none.segments.size)
        assertNull(none.currentPhase)
    }

    @Test
    fun calendarEntriesMapToDisciplineGlyphAndStateTone() {
        fun entry(c: CalendarData.Category, s: CalendarData.State) =
            CalendarData.Entry(c, s, "Session", null, null)
        val long = entry(CalendarData.Category.LONG, CalendarData.State.PLANNED)
        assertEquals(Discipline.RUNNING, long.discipline())
        assertTrue(long.isKey())
        assertEquals(DiscTone.TONAL, long.tone())
        val gym = entry(CalendarData.Category.STRENGTH, CalendarData.State.COMPLETED)
        assertEquals(Discipline.STRENGTH, gym.discipline())
        assertEquals(Glyph.DUMBBELL, gym.glyph())
        assertEquals(DiscTone.FILLED, gym.tone())
        val missed = entry(CalendarData.Category.EASY, CalendarData.State.NOT_COMPLETED)
        assertEquals(DiscTone.MUTED, missed.tone())
        assertTrue(!missed.isKey())
        assertNull(entry(CalendarData.Category.REST, CalendarData.State.PLANNED).discipline())
    }
}
