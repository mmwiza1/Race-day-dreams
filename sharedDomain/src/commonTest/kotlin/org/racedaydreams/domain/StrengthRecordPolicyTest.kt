package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class StrengthRecordPolicyTest {
    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()

    private fun session(
        id: String = "session",
        exercise: StrengthExercise = bench,
        sets: List<StrengthSession.SetEntry> =
            listOf(StrengthSession.SetEntry("working", 5, 80.0, null, false)),
        second: Long = 0,
        offset: Int = 19800,
    ) =
        StrengthSession(
            id,
            Instants.ofEpochSecond(second, 123456789),
            Offsets.ofTotalSeconds(offset),
            Durations.ofMinutes(30),
            null,
            null,
            listOf(StrengthSession.ExerciseSets(exercise, sets)),
        )

    private fun history(vararg rows: StrengthSession) =
        StrengthRecords.progression(rows.toList(), Instants.ofEpochSecond(100000))

    @Test
    fun measuredSinglesAreActualsWhileWarmupsAndUnsupportedEstimatesRemainExcluded() {
        val warm = StrengthSession.SetEntry("warm", 5, 120.0, null, true)
        val single = StrengthSession.SetEntry("single", 1, 90.0, null, false)
        val source = session(sets = listOf(warm, single))
        val records = history(source)
        assertEquals(3, records.size)
        assertTrue(records.none { it.estimated() })
        assertTrue(records.all { it.actual() === single && it.workoutId() == source.id() })
        assertEquals(90.0, records.first().value())
        assertNull(StrengthRecordPolicy.estimatedOneRepMax(bench, warm))
        assertNull(StrengthRecordPolicy.estimatedOneRepMax(bench, single))
        assertNull(StrengthRecordPolicy.estimatedOneRepMax(bench, single.copy(reps = 11)))
        val working = single.copy(reps = 5, kilograms = 80.0)
        assertNull(working.rpe())
        assertEquals(80.0 * (1 + 5 / 30.0), StrengthRecordPolicy.estimatedOneRepMax(bench, working))
        val estimate = history(session(sets = listOf(working))).single { it.estimated() }
        assertSame(working, estimate.actual())
        assertTrue(StrengthRecords.estimateExplanation().contains("not a measured lift"))
        assertFails { (records as MutableList<StrengthRecords.Point>).clear() }
    }

    @Test
    fun identicalEvidenceIsIdempotentAndConflictsCannotHideBehindAsOfFiltering() {
        val source = session()
        assertEquals(history(source), history(source, source))
        assertFailsWith<IllegalArgumentException> {
            history(source, source.copy(notes = "Different"))
        }
        assertTrue(
            StrengthRecords.progression(listOf(source), Instants.ofEpochSecond(-1)).isEmpty()
        )
        assertTrue(StrengthRecords.progression(emptyList(), null).isEmpty())
        val future = source.copy(startedAt = Instants.ofEpochSecond(200000))
        assertFailsWith<IllegalArgumentException> {
            history(future, future.copy(notes = "Different"))
        }
    }

    @Test
    fun recordedExerciseDefinitionsAndMeasurementTasksStayDistinct() {
        val body =
            bench.copy(
                mode = StrengthExercise.Mode.BODYWEIGHT,
                measurement = StrengthExercise.MeasurementProfile.REPS_ONLY,
                estimateOneRepMax = false,
            )
        val bodyActual = StrengthSession.SetEntry("body", 12, 0.0, null, false)
        val records = history(session(), session("body", body, listOf(bodyActual)))
        val bodyPoint =
            records.single { it.key()!!.kind() == StrengthRecords.Kind.BODYWEIGHT_REPETITIONS }
        assertEquals(12.0, bodyPoint.value())
        assertSame(bodyActual, bodyPoint.actual())
        assertNull(bodyPoint.key()!!.load())
        assertNull(StrengthRecordPolicy.estimatedOneRepMax(body, bodyActual))
        val timed =
            StrengthExercise(
                "hold",
                "Hold",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_DURATION,
            )
        val carry =
            timed.copy(
                id = "carry",
                name = "Carry",
                measurement = StrengthExercise.MeasurementProfile.LOAD_DISTANCE,
            )
        val timedPoint =
            history(
                    session(
                        "hold",
                        timed,
                        listOf(StrengthSession.SetEntry("t", 0, 20.0, null, false, 30, null)),
                    )
                )
                .single()
        val carryPoint =
            history(
                    session(
                        "carry",
                        carry,
                        listOf(StrengthSession.SetEntry("d", 0, 20.0, null, false, null, 25.5)),
                    )
                )
                .single()
        assertEquals(StrengthRecords.Kind.DURATION, timedPoint.key()!!.kind())
        assertEquals(StrengthRecords.Kind.DISTANCE, carryPoint.key()!!.kind())
        assertEquals(20.0, timedPoint.key()!!.load())
        assertEquals(20.0, carryPoint.key()!!.load())
        assertEquals(30.0, timedPoint.value())
        assertEquals(25.5, carryPoint.value())
    }

    @Test
    fun bestRecordsKeepFirstTiesActualProvenanceAndOriginalImprovementTolerance() {
        val first = session()
        val near =
            session(
                "near",
                sets = listOf(StrengthSession.SetEntry("near", 5, 80.00000001, null, false)),
                second = 1,
            )
        val better =
            session(
                "better",
                sets = listOf(StrengthSession.SetEntry("better", 5, 82.0, null, false)),
                second = 2,
            )
        val rows = history(better, near, first)
        val key =
            StrengthRecords.Key(
                bench.id(),
                bench.mode(),
                bench.measurement(),
                StrengthRecords.Kind.HEAVIEST_LOAD,
                null,
                null,
            )
        assertEquals(listOf(80.0, 82.0), rows.filter { it.key() == key }.map { it.value() })
        assertEquals("better", StrengthRecords.best(rows).single { it.key() == key }.workoutId())
        val positiveZero =
            StrengthRecords.Key(
                "same",
                bench.mode(),
                bench.measurement(),
                StrengthRecords.Kind.REPETITIONS_AT_LOAD,
                null,
                0.0,
            )
        assertNotEquals(positiveZero, positiveZero.copy(load = -0.0))
        val point = rows.first()
        assertEquals(
            listOf(point),
            StrengthRecords.best(listOf(point, point.copy(workoutId = "equal later"))),
        )
    }

    @Test
    fun sourceOffsetControlsRecordedDayWithoutInventingADeviceTimezone() {
        assertEquals(
            CalendarDates.of(1970, 1, 2),
            history(session(second = 86399, offset = 3600)).first().date(),
        )
        assertEquals(
            CalendarDates.of(1969, 12, 31),
            history(session(second = 1000, offset = -3600)).first().date(),
        )
        val zero = Instants.ofEpochSecond(-1, 999999999)
        assertEquals(
            CalendarDates.of(1969, 12, 31),
            CalendarDates.atOffset(zero, Offsets.ofTotalSeconds(0)),
        )
        assertEquals(
            CalendarDates.of(1970, 1, 1),
            CalendarDates.atOffset(zero, Offsets.ofTotalSeconds(1)),
        )
    }
}
