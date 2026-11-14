package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class StrengthContextAndHistoryTest {
    private val bench = requireNotNull(ExerciseCatalog.definition("bench-press")).exercise()
    private val squat = requireNotNull(ExerciseCatalog.definition("back-squat")).exercise()

    private fun session(
        exercise: StrengthExercise = bench,
        rpe: Double? = null,
        sets: List<StrengthSession.SetEntry> =
            listOf(StrengthSession.SetEntry("working", 5, 80.0, null, false)),
    ) =
        StrengthSession(
            "source",
            Instants.ofEpochSecond(100, 123456789),
            Offsets.ofTotalSeconds(19800),
            Durations.ofMinutes(30),
            rpe,
            null,
            listOf(StrengthSession.ExerciseSets(exercise, sets)),
        )

    @Test
    fun absentEffortAndWarmupsNeverAcquireHardLowerBodyContext() {
        val missing = session(squat)
        assertEquals("Effort not recorded", StrengthAnalytics.context(missing).effort())
        assertEquals(
            StrengthAnalytics.Involvement.MODERATE,
            StrengthAnalytics.context(missing).lowerBody(),
        )
        assertNull(StrengthAnalytics.sessionLoad(missing))
        val warm =
            missing.copy(
                exercises =
                    listOf(
                        StrengthSession.ExerciseSets(
                            squat,
                            List(3) { StrengthSession.SetEntry("warm-$it", 5, 120.0, 9.0, true) },
                        )
                    )
            )
        assertEquals(StrengthAnalytics.Involvement.LOW, StrengthAnalytics.context(warm).lowerBody())
        assertEquals("Effort not recorded", StrengthAnalytics.context(warm).effort())
        assertTrue(StrengthAnalytics.progression(listOf(warm)).isEmpty())
    }

    @Test
    fun recordedReserveAndSessionEffortKeepTheOriginalQualifiedContext() {
        val sets =
            List(3) { StrengthSession.SetEntry("set-$it", 5, 80.0, null, false, null, null, 1.0) }
        val source = session(squat, sets = sets)
        val context = StrengthAnalytics.context(source)
        assertEquals("Hard", context.effort())
        assertEquals(StrengthAnalytics.Involvement.HIGH, context.lowerBody())
        assertTrue(context.reasons()!!.last().contains("approximate 10−RIR"))
        assertEquals(
            setOf(CalendarDates.of(1970, 1, 1)),
            StrengthAnalytics.hardLowerBodyDates(listOf(source)),
        )
        assertNull(StrengthAnalytics.sessionLoad(source))
        val core = requireNotNull(ExerciseCatalog.definition("plank")).exercise()
        val holds = List(3) { StrengthSession.SetEntry("hold-$it", 0, 0.0, 9.0, false, 30, null) }
        assertEquals(
            StrengthAnalytics.Involvement.LOW,
            StrengthAnalytics.context(session(core, sets = holds)).lowerBody(),
        )
    }

    @Test
    fun recordedSessionLoadRetainsMillisecondTruncationAndDoesNotInferSetAverages() {
        val source = session(rpe = 7.5).copy(duration = Durations.ofSeconds(1, 999999999))
        assertEquals(1999 / 60000.0 * 7.5, StrengthAnalytics.sessionLoad(source))
        assertEquals(0.0, StrengthAnalytics.sessionLoad(source.copy(rpe = 0.0)))
        assertNull(StrengthAnalytics.sessionLoad(source.copy(rpe = null)))
    }

    @Test
    fun historicalDatesKeepTheirOwnPrecisionAndDoNotCreateSourceWorkouts() {
        val unknown =
            HistoricalStrengthRecord(
                "unknown",
                bench,
                100.0,
                5,
                null,
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                "Entered",
            )
        val approximate =
            HistoricalStrengthRecord(
                "approximate",
                bench,
                90.0,
                5,
                CalendarDates.of(2020, 1, 2),
                HistoricalStrengthRecord.DatePrecision.APPROXIMATE,
                null,
            )
        val source = session()
        val timeline = StrengthProgression.timeline(listOf(source), listOf(unknown, approximate))
        assertEquals(1, source.exercises().size)
        assertTrue(
            timeline.takeLast(3).all {
                it.id() == "unknown" &&
                    it.date() == null &&
                    it.sessionId() == null &&
                    it.provenance() == "Historical/manual PB"
            }
        )
        assertEquals(
            HistoricalStrengthRecord.DatePrecision.APPROXIMATE,
            timeline.first { it.id() == "approximate" }.precision(),
        )
        assertEquals(
            HistoricalStrengthRecord.DatePrecision.EXACT,
            timeline.first { it.sessionId() == source.id() }.precision(),
        )
        assertEquals("unknown", StrengthProgression.best(timeline, bench.id(), "Load")!!.id())
        assertFails { (timeline as MutableList<StrengthProgression.Point>).clear() }
        assertFailsWith<IllegalArgumentException> {
            unknown.copy(date = CalendarDates.of(2020, 1, 1))
        }
        assertFailsWith<IllegalArgumentException> { approximate.copy(date = null) }
    }

    @Test
    fun legacyBucketsAndLoadedTasksKeepTheirOriginalComparability() {
        val seven = session(sets = listOf(StrengthSession.SetEntry("seven", 7, 80.0, null, false)))
        assertTrue(
            StrengthAnalytics.progression(listOf(seven)).none { it.category() == "Load × 7" }
        )
        assertTrue(
            StrengthAnalytics.progression(listOf(seven)).any { it.category() == "Estimated 1RM" }
        )
        val hold =
            StrengthExercise(
                "retained-hold",
                "Hold",
                StrengthExercise.Region.FULL,
                StrengthExercise.Mode.WEIGHTED,
                false,
                StrengthExercise.MeasurementProfile.LOAD_DURATION,
            )
        val points =
            StrengthAnalytics.progression(
                listOf(
                    session(
                        hold,
                        sets =
                            listOf(
                                StrengthSession.SetEntry("a", 0, 20.0, null, false, 30, null),
                                StrengthSession.SetEntry("b", 0, 30.0, null, false, 20, null),
                            ),
                    )
                )
            )
        assertEquals(
            listOf("Duration seconds at 20.0 kg", "Duration seconds at 30.0 kg"),
            points.map { it.category() },
        )
        assertEquals(listOf(30.0, 20.0), points.map { it.value() })
    }

    @Test
    fun historicalValidationAndBestSelectionRetainOriginalRules() {
        val push = requireNotNull(ExerciseCatalog.definition("push-up")).exercise()
        assertFailsWith<IllegalArgumentException> {
            HistoricalStrengthRecord(
                "body",
                push,
                5.0,
                5,
                null,
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                null,
            )
        }
        val first =
            StrengthProgression.Point(
                "a",
                "exercise",
                "Load",
                0.0,
                0.0,
                1,
                null,
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                "Manual",
                null,
                null,
            )
        val lower = first.copy(id = "b", value = -0.0)
        assertEquals(first, StrengthProgression.best(listOf(lower, first), "exercise", "Load"))
        assertEquals(
            first,
            StrengthProgression.best(listOf(first, first.copy(id = "tie")), "exercise", "Load"),
        )
        assertTrue(
            StrengthProgression.best(
                    listOf(first, first.copy(value = Double.NaN)),
                    "exercise",
                    "Load",
                )!!
                .value()
                .isNaN()
        )
        assertNull(StrengthProgression.best(listOf(first), "missing", "Load"))
    }
}
