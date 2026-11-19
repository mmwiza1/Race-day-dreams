package org.racedaydreams.domain.execution

import kotlin.test.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.time.*

class RunningExecutionContractTest {
    private fun step(metres: Double? = null, millis: Long? = null) =
        WorkoutExecutionEngine.Step(
            "Run",
            WorkoutSegment.Kind.RUN,
            metres,
            millis,
            240.0,
            360.0,
            7.0,
        )

    private fun input(
        metres: Double,
        active: Long,
        elapsed: Long = active,
        paused: Boolean = false,
    ) = WorkoutExecutionEngine.Input(metres, active, elapsed, null, false, paused)

    @Test
    fun interpolationUsesTheLatestObservationAndDoesNotCountOvershootTwice() {
        val e = WorkoutExecutionEngine(listOf(step(1000.0), step(400.0)))
        e.update(input(0.0, 0))
        e.update(input(500.0, 200000))
        val state = e.update(input(1100.0, 260000))
        assertEquals(1, state.index())
        assertEquals(300.0, state.metresRemaining()!!, 1e-9)
        val boundary = e.events().first { it.type() == "STEP_COMPLETED" }
        assertEquals(1000.0, boundary.metres(), 1e-9)
        assertEquals(250000, boundary.activeMillis())
        assertTrue(e.update(input(1400.0, 290000)).complete())
        assertEquals(2, e.events().count { it.type() == "STEP_COMPLETED" })
        assertEquals(1, e.events().count { it.type() == "WORKOUT_COMPLETED" })
        e.update(input(1400.0, 291000))
        assertEquals(6, e.events().size)
    }

    @Test
    fun mixedDistanceAndTimedStepsInterpolateInOrderAcrossOneObservation() {
        val e = WorkoutExecutionEngine(listOf(step(1000.0), step(millis = 30000), step(200.0)))
        val result = e.update(input(1400.0, 140000))
        assertEquals(2, result.index())
        assertEquals(100.0, result.metresRemaining()!!, 1e-9)
        val completed = e.events().filter { it.type() == "STEP_COMPLETED" }
        assertEquals(listOf(100000L, 130000L), completed.map { it.activeMillis() })
        assertEquals(listOf(1000.0, 1300.0), completed.map { it.metres() })
    }

    @Test
    fun legacyDistanceWinsOverEstimatedDurationAndPauseDoesNotAutoAdvance() {
        val e = WorkoutExecutionEngine(listOf(step(1000.0, 1000)))
        assertFalse(e.update(input(100.0, 5000)).complete())
        assertNull(e.state().millisRemaining())
        assertFalse(e.update(input(1000.0, 5000, 10000, paused = true)).complete())
        assertTrue(e.state().paused())
        e.skip()
        assertTrue(e.state().complete())
        assertEquals(1, e.events().count { it.type() == "STEP_SKIPPED" })
    }

    @Test
    fun openAndEarlyActionsRemainExplicitAndNeverInventCompletion() {
        val open = WorkoutExecutionEngine(listOf(step()))
        open.completeOpen()
        open.skip()
        assertTrue(open.events().isEmpty())
        assertFalse(open.update(input(100.0, 10000)).complete())
        open.completeOpen()
        assertTrue(open.state().complete())
        val early = WorkoutExecutionEngine(listOf(step(1000.0)))
        early.finishEarly()
        early.finishEarly()
        assertTrue(early.state().early())
        assertFalse(early.state().complete())
        early.update(input(2000.0, 500000))
        early.skip()
        early.completeOpen()
        assertEquals(0, early.state().index())
        assertEquals(1, early.events().count { it.type() == "WORKOUT_FINISHED_EARLY" })
        assertTrue(early.events().none { it.type() == "WORKOUT_COMPLETED" })
    }

    @Test
    fun backwardsMetricsAreRejectedBeforeMutatingTheLastGoodProjection() {
        val e = WorkoutExecutionEngine(listOf(step(1000.0)))
        e.update(input(100.0, 10000, 20000))
        val before = e.state()
        val events = e.events()
        for (bad in
            listOf(
                input(99.0, 10000, 20000),
                input(100.0, 9999, 20000),
                input(100.0, 10000, 19999),
            )) {
            assertFailsWith<IllegalArgumentException> { e.update(bad) }
            assertEquals(before, e.state())
            assertEquals(events, e.events())
        }
        assertFailsWith<IllegalArgumentException> { input(Double.NaN, 0) }
        assertFailsWith<IllegalArgumentException> { input(0.0, 2, 1) }
    }

    @Test
    fun factoriesKeepPrescriptionsUnknownsRepetitionOrderAndOriginalMutableResults() {
        val date = CalendarDates.of(2026, 11, 14)
        val segment =
            WorkoutSegment(
                WorkoutSegment.Kind.RUN,
                .4,
                Durations.ofSeconds(90),
                null,
                null,
                null,
                7.0,
                "My instruction",
            )
        val plan =
            PlannedWorkout(
                "p",
                date,
                WorkoutCategory.INTERVALS,
                1.0,
                null,
                listOf(segment),
                "Retained",
            )
        val generated = WorkoutExecutionEngine.from(plan)
        assertEquals("My instruction", generated.single().label())
        assertEquals(400.0, generated.single().metres())
        assertEquals(90000, generated.single().millis())
        assertNull(generated.single().fastPace())
        val recovery = step(millis = 30000)
        val reps = WorkoutExecutionEngine.repetitions(3, generated.single(), recovery)
        assertEquals(
            listOf(generated.single(), recovery, generated.single(), recovery, generated.single()),
            reps,
        )
        (reps as MutableList).clear()
        (generated as MutableList).clear()
        assertEquals(1, plan.segments().size)
        val open = WorkoutExecutionEngine.from(plan.copy(segments = emptyList(), distanceKm = null))
        assertNull(open.single().metres())
        assertNull(open.single().millis())
        assertFailsWith<IllegalArgumentException> {
            WorkoutExecutionEngine.repetitions(0, step(), null)
        }
        assertFailsWith<IllegalArgumentException> {
            WorkoutExecutionEngine.from(
                plan.copy(
                    segments = emptyList(),
                    distanceKm = null,
                    duration = Durations.ofNanos(500000),
                )
            )
        }
        assertFailsWith<ArithmeticException> {
            WorkoutExecutionEngine.from(
                plan.copy(
                    segments = emptyList(),
                    distanceKm = null,
                    duration = Durations.ofSeconds(Long.MAX_VALUE),
                )
            )
        }
    }

    @Test
    fun engineAndEventSnapshotsAreFrozenWithoutAddingRawNullElementValidation() {
        val source = mutableListOf(step(100.0))
        val e = WorkoutExecutionEngine(source)
        source.clear()
        assertEquals(1, e.steps().size)
        assertFails { (e.steps() as MutableList).clear() }
        e.update(input(0.0, 0))
        val old = e.events()
        assertFails { (old as MutableList).clear() }
        e.update(input(100.0, 30000))
        assertEquals(2, old.size)
        assertEquals(4, e.events().size)
        val raw = WorkoutExecutionEngine(WorkoutExecutionEngine.repetitions(1, null, null))
        assertNull(raw.state().current())
        assertFalse(raw.state().complete())
        assertNull((raw.steps() as List<*>).single())
        assertFailsWith<NullPointerException> { raw.update(input(0.0, 0)) }
        assertNull(WorkoutExecutionEngine.Event(null, -1, Double.NaN, -1).type())
    }

    @Test
    fun interpolationKeepsMathRoundBoundaryInsteadOfRoundingTwice() {
        val cases = listOf(.49999999999999994 to 0L, .5 to 1L, .5000000000000001 to 1L)
        for ((distance, expected) in cases) {
            val e = WorkoutExecutionEngine(listOf(step(distance)))
            e.update(input(1.0, 1))
            assertEquals(
                expected,
                e.events().first { it.type() == "STEP_COMPLETED" }.activeMillis(),
            )
        }
    }

    @Test
    fun longCountersAndManyObservationsKeepBoundedEventsAndSingleCompletion() {
        val target = 2147484647L
        val e = WorkoutExecutionEngine(listOf(step(millis = target)))
        e.update(input(0.0, 0))
        assertFalse(e.update(input(1000.0, target - 1)).complete())
        assertEquals(1, e.state().millisRemaining())
        assertTrue(e.update(input(1000.0, target)).complete())
        for (n in 1L..20000L) e.update(input(1000.0, target + n))
        assertEquals(4, e.events().size)
        assertEquals(target, e.events().first { it.type() == "STEP_COMPLETED" }.activeMillis())
    }
}
