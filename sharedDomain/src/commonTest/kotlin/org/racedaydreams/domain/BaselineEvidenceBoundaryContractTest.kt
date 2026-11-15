package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.time.*

class BaselineEvidenceBoundaryContractTest {
    private fun manual(
        distance: Double? = null,
        runs: Double? = null,
        longest: Double? = null,
        time: Duration? = null,
        easyPace: Double? = null,
        weeks: Int? = null,
    ) = ManualBaselineInput(distance, runs, longest, time, null, null, easyPace, weeks)

    private fun result(
        id: String? = "entered",
        assessment: BaselineAssessment? = BaselineAssessment(1, AssessmentMode.CONTROLLED),
        duration: Duration? = Durations.ofSeconds(300),
        effort: Double? = null,
        at: Instant? = Instants.ofEpochSecond(0),
        activity: String? = null,
    ) = AssessmentResult(id, assessment, duration, effort, at, activity)

    @Test
    fun missingAnswersRemainUnknownAndKnownZeroRemainsAnAnswer() {
        val empty = ManualBaselineInput.empty()
        assertFalse(empty.hasAnyValue())
        assertNull(empty.weeklyDistanceKm())
        assertNull(empty.runsPerWeek())
        assertNull(empty.longestRunKm())
        assertNull(empty.recent5k())
        assertNull(empty.recent10k())
        assertNull(empty.recentHalf())
        assertNull(empty.easyPaceSecondsPerKm())
        assertNull(empty.consistentWeeks())
        assertTrue(manual(distance = 0.0).hasAnyValue())
        assertTrue(manual(weeks = 0).hasAnyValue())
        assertNull(result().activityId())
        assertNull(result().rpe())
    }

    @Test
    fun manualPerformanceNeedsWholeSecondsButRecordedAssessmentRetainsNanoseconds() {
        val fraction = Durations.ofNanos(1)
        for (field in 0..2) assertFailsWith<IllegalArgumentException> {
            ManualBaselineInput(
                null,
                null,
                null,
                if (field == 0) fraction else null,
                if (field == 1) fraction else null,
                if (field == 2) fraction else null,
                null,
                null,
            )
        }
        assertEquals(fraction, result(duration = fraction).duration())
        assertEquals(1, result(duration = fraction).duration().getNano())
        assertEquals(Durations.ofHours(48), manual(time = Durations.ofHours(48)).recent5k())
        assertEquals(Durations.ofHours(48), result(duration = Durations.ofHours(48)).duration())
        for (time in
            listOf(Durations.ofSeconds(0), Durations.ofNanos(-1), Durations.ofSeconds(172800, 1))) {
            assertFailsWith<IllegalArgumentException> { manual(time = time) }
            assertFailsWith<IllegalArgumentException> { result(duration = time) }
        }
    }

    @Test
    fun manualDistanceFrequencyAndPaceKeepTheirDistinctLimits() {
        for (bad in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { manual(distance = bad) }
            assertFailsWith<IllegalArgumentException> { manual(runs = bad) }
            assertFailsWith<IllegalArgumentException> { manual(longest = bad) }
            assertFailsWith<IllegalArgumentException> { manual(easyPace = bad) }
        }
        assertEquals(21.0, manual(runs = 21.0).runsPerWeek())
        assertFailsWith<IllegalArgumentException> { manual(runs = 21.01) }
        assertEquals(0.0, manual(longest = 0.0).longestRunKm())
        assertFailsWith<IllegalArgumentException> { manual(easyPace = 0.0) }
        assertFailsWith<IllegalArgumentException> { manual(weeks = -1) }
    }

    @Test
    fun originalAsciiTrimValidationDoesNotNormaliseStoredIdentity() {
        for (id in listOf(null, "", " \t\n")) assertFailsWith<IllegalArgumentException> {
            result(id = id)
        }
        assertEquals(" entered ", result(id = " entered ").id())
        assertEquals("\u2003", result(id = "\u2003").id())
        assertEquals(" original-run ", result(activity = " original-run ").activityId())
    }

    @Test
    fun athleteMayChooseAnySupportedDistanceAndEitherMode() {
        for (distance in 1..5) for (mode in AssessmentMode.entries) {
            val chosen = BaselineAssessment(distance, mode)
            assertEquals(distance, chosen.distanceKm())
            assertEquals(mode, chosen.mode())
        }
        for (distance in listOf(0, 6)) assertFailsWith<IllegalArgumentException> {
            BaselineAssessment(distance, AssessmentMode.CONTROLLED)
        }
        assertFailsWith<NullPointerException> { BaselineAssessment(1, null) }
    }

    @Test
    fun effortIsOptionalAndFractionalRatingsAreNotRoundedOrInvented() {
        assertNull(result().rpe())
        for (effort in listOf(0.0, 5.5, 10.0)) assertEquals(effort, result(effort = effort).rpe())
        for (effort in listOf(-0.1, 10.1, Double.NaN, Double.POSITIVE_INFINITY)) assertFailsWith<
            IllegalArgumentException
        > {
            result(effort = effort)
        }
    }

    @Test
    fun requiredActualEvidenceCannotBeFabricatedFromMissingFields() {
        assertFailsWith<NullPointerException> { result(assessment = null) }
        assertFailsWith<NullPointerException> { result(duration = null) }
        assertFailsWith<NullPointerException> { result(at = null) }
        val at = Instants.ofEpochSecond(500, 123)
        assertEquals(at, result(at = at).completedAt())
    }

    @Test
    fun recordValueSemanticsPreserveSignedZeroAndSeparateActualFromManual() {
        assertEquals(ManualBaselineInput.empty(), ManualBaselineInput.empty())
        assertNotEquals(manual(distance = 0.0), manual(distance = -0.0))
        assertEquals(result(effort = 5.5), result(effort = 5.5))
        assertNotEquals(result(), result(activity = "run"))
        val assessment = BaselineAssessment(3, AssessmentMode.CONTROLLED)
        assertEquals("BaselineAssessment[distanceKm=3, mode=CONTROLLED]", assessment.toString())
    }
}
