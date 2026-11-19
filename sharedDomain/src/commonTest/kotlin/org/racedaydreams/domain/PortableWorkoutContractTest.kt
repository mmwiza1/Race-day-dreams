package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.polish.*
import org.racedaydreams.domain.portability.*
import org.racedaydreams.domain.time.*

class PortableWorkoutContractTest {
    private val now = Instants.ofEpochSecond(2000000000)
    private val source = SourceIdentity("local", "record", null)

    private fun point(second: Long = 0) =
        TrackPoint(Instants.ofEpochSecond(second), 51.0, -0.1, null, null, null, null)

    private fun step(metres: Double? = null, millis: Long? = null) =
        WorkoutExecutionEngine.Step(
            "Run",
            WorkoutSegment.Kind.RUN,
            metres,
            millis,
            null,
            null,
            null,
        )

    private fun run(
        days: Long,
        distance: Double? = 5000.0,
        effort: Double? = null,
        moving: Long? = null,
        shoe: String? = null,
    ): ActivityRecord {
        val start = Instants.ofEpochSecond(now.getEpochSecond() - days * 86400)
        return ActivityRecord(
            "run-$days",
            source,
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + 7200),
            null,
            null,
            distance,
            moving?.let { Durations.ofSeconds(it) },
            null,
            null,
            null,
            null,
            effort,
            null,
            emptyList(),
            emptyList(),
            null,
            shoe,
        )
    }

    @Test
    fun structuredDeliveryRetainsThePrescriptionWithoutClaimingActualCompletion() {
        val workout =
            PlannedWorkout(
                "p",
                CalendarDates.of(2026, 11, 15),
                WorkoutCategory.EASY_RUN,
                5.0,
                null,
                emptyList(),
                "original",
            )
        val delivery = StructuredWorkoutDelivery(workout, 3)
        assertSame(workout, delivery.workout())
        assertEquals(3L, delivery.revision())
        assertFailsWith<IllegalArgumentException> { StructuredWorkoutDelivery(workout, 0) }
        assertFailsWith<NullPointerException> { StructuredWorkoutDelivery(null, 1) }
    }

    @Test
    fun missingSensorsStayUnknownAndBelowSeaLevelAltitudeRemainsValid() {
        val p = point()
        assertNull(p.altitudeMetres())
        assertNull(p.accuracyMetres())
        assertNull(p.heartRate())
        assertNull(p.cadence())
        assertEquals(-30.0, TrackPoint(now, -90.0, 180.0, -30.0, 0.0, 0.0, 0.0).altitudeMetres())
    }

    @Test
    fun invalidCoordinatesOrSensorValuesCannotBecomeRouteEvidence() {
        for (latitude in
            listOf(Double.NaN, Double.POSITIVE_INFINITY, 90.01, -90.01)) assertFailsWith<
            IllegalArgumentException
        > {
            TrackPoint(now, latitude, 0.0, null, null, null, null)
        }
        assertFailsWith<IllegalArgumentException> {
            TrackPoint(now, 0.0, 180.01, null, null, null, null)
        }
        assertFailsWith<IllegalArgumentException> {
            TrackPoint(now, 0.0, 0.0, Double.NaN, null, null, null)
        }
        assertFailsWith<IllegalArgumentException> {
            TrackPoint(now, 0.0, 0.0, null, -1.0, null, null)
        }
        assertFailsWith<NullPointerException> { TrackPoint(null, 0.0, 0.0, null, null, null, null) }
    }

    @Test
    fun routeSnapshotsRetainIndependentSegmentsAndRawIdentity() {
        val first = mutableListOf(point(10), point(11))
        val segments = mutableListOf<List<TrackPoint>>(first, listOf(point(0)), emptyList())
        val route = RecordedRoute(" route ", null, source, segments, null)
        first.clear()
        segments.clear()
        assertEquals(listOf(2, 1, 0), route.segments().map { it.size })
        assertEquals(" route ", route.id())
        assertEquals("", route.name())
        assertNull(route.activityId())
        assertSame(source, route.source())
        assertTrue(RecordedRoute("empty", null, source, emptyList(), null).segments().isEmpty())
    }

    @Test
    fun duplicateOrReversedObservationsAreRejectedWithinOneSegment() {
        for (points in
            listOf(listOf(point(1), point(1)), listOf(point(2), point(1)))) assertFailsWith<
            IllegalArgumentException
        > {
            RecordedRoute("r", null, source, listOf(points), null)
        }
        assertFailsWith<IllegalArgumentException> {
            RecordedRoute(" ", null, source, emptyList(), null)
        }
    }

    @Test
    fun mixedTargetsRetainKnownTotalsWithoutInventingMissingMeasurements() {
        val workout =
            CustomWorkout(
                "w",
                "Mixed",
                listOf(
                    CustomWorkout.Group(2, listOf(step(400.0), step(millis = 60000))),
                    CustomWorkout.Group(1, listOf(step())),
                ),
            )
        assertEquals(5, workout.flattened().size)
        val totals = workout.totals()
        assertEquals(800.0, totals.knownMetres())
        assertEquals(120000L, totals.knownMillis())
        assertFalse(totals.distanceComplete())
        assertFalse(totals.durationComplete())
        val complete =
            CustomWorkout("c", "Both", listOf(CustomWorkout.Group(1, listOf(step(100.0, 30000)))))
                .totals()
        assertTrue(complete.distanceComplete())
        assertTrue(complete.durationComplete())
    }

    @Test
    fun prescriptionReorderingCreatesANewSnapshotWithoutEditingTheOriginal() {
        val steps = mutableListOf(step(100.0))
        val a = CustomWorkout.Group(1, steps)
        val b = CustomWorkout.Group(2, listOf(step(200.0)))
        val groups = mutableListOf(a, b)
        val original = CustomWorkout("w", "Workout", groups)
        steps.clear()
        groups.clear()
        val moved = original.move(0, 1)
        assertEquals(listOf(a, b), original.groups())
        assertEquals(listOf(b, a), moved.groups())
        assertEquals(listOf(200.0, 200.0, 100.0), moved.flattened().map { it.metres() })
    }

    @Test
    fun repeatedStepsAndPerStepBoundsMatchTheExistingWorkoutLimits() {
        assertEquals(
            500,
            CustomWorkout(
                    "w",
                    "Limit",
                    listOf(CustomWorkout.Group(100, List(5) { step(300000.0, 21600000) })),
                )
                .flattened()
                .size,
        )
        assertFailsWith<IllegalArgumentException> {
            CustomWorkout("w", "Too many", listOf(CustomWorkout.Group(100, List(6) { step() })))
        }
        assertFailsWith<IllegalArgumentException> { CustomWorkout.Group(101, listOf(step())) }
        assertFailsWith<IllegalArgumentException> {
            CustomWorkout("w", "Too far", listOf(CustomWorkout.Group(1, listOf(step(300000.1)))))
        }
        assertFailsWith<IllegalArgumentException> {
            CustomWorkout(
                "w",
                "Too long",
                listOf(CustomWorkout.Group(1, listOf(step(millis = 21600001)))),
            )
        }
    }

    @Test
    fun longElapsedDurationAloneCannotBeAssumedToBeMovingTrainingDemand() {
        assertFalse(TrainingInsights.extraRunDemand(run(0, null)))
        assertFalse(TrainingInsights.extraRunDemand(run(0, 7999.0, 6.9, 2699)))
        assertTrue(TrainingInsights.extraRunDemand(run(0, 8000.0)))
        assertTrue(TrainingInsights.extraRunDemand(run(0, null, 7.0)))
        assertTrue(TrainingInsights.extraRunDemand(run(0, null, moving = 2700)))
    }

    @Test
    fun insightWindowsIncludeTheirExactBoundariesButNotFutureOrMissingDistances() {
        val rows = listOf(0L, 1, 2, 28, 29, 30, 31, 56).map { run(it) }
        assertEquals(
            "Recorded running volume is broadly stable",
            TrainingInsights.from(rows, now).single().title(),
        )
        val withUnknown = rows + run(-1, 999999.0, 10.0) + run(3, null, 10.0) + run(57, 999999.0)
        assertEquals(TrainingInsights.from(rows, now), TrainingInsights.from(withUnknown, now))
        assertTrue(TrainingInsights.from(rows.filter { it.id() != "run-28" }, now).isEmpty())
        assertTrue(TrainingInsights.from(rows.filter { it.id() != "run-56" }, now).isEmpty())
    }

    @Test
    fun demandingEffortRequiresRecordedEvidenceAndDoesNotActivateChanges() {
        val prior = (29L..32L).map { run(it) }
        assertEquals(1, TrainingInsights.from(prior + (0L..3L).map { run(it) }, now).size)
        val insights = TrainingInsights.from(prior + (0L..3L).map { run(it, effort = 8.0) }, now)
        assertEquals(2, insights.size)
        assertTrue(insights.last().evidence()!!.contains("no automatic change"))
    }

    @Test
    fun shoeContextUsesExplicitAssignmentsAndDoesNotDiagnoseDiscomfort() {
        val shoe = Shoe("shoe", "Brand", "Model", null, 0.0, false, "", false)
        val rows =
            listOf(
                run(0, 13000.0, shoe = "shoe"),
                run(1, null, shoe = "shoe"),
                run(2, 15000.0, shoe = "other"),
                run(40, 14000.0, shoe = "shoe"),
            )
        val text =
            TrainingInsights.shoes(shoe, rows, now, setOf("run-0", "run-2")).joinToString(" ")
        assertTrue(text.contains("2 assigned runs"))
        assertTrue(text.contains("13.0 km"))
        assertTrue(text.contains("2 of 3"))
        assertTrue(text.contains("1 run while wearing this shoe. This does not establish a cause."))
        assertFalse(
            TrainingInsights.shoes(shoe, listOf(run(0, null, shoe = "shoe")), now, emptySet()).any {
                it.contains("Longest")
            }
        )
    }
}
