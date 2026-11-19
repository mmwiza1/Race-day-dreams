package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.climbing.*
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
import org.racedaydreams.domain.time.*

class ClimbingContractTest {
    private val utc = Offsets.ofTotalSeconds(0)
    // Wednesday 18 November 2026.
    private val today = CalendarDates.of(2026, 11, 18)

    private fun at(date: LocalDate, hour: Int = 18) =
        Instants.ofEpochSecond(date.toEpochDay() * 86_400 + hour * 3600L)

    private fun v(label: String) = ClimbingGrade(GradeSystem.V_SCALE, label)

    private fun font(label: String) = ClimbingGrade(GradeSystem.FONTAINEBLEAU, label)

    private fun problem(id: String, grade: ClimbingGrade?, attempts: Int, outcome: ClimbOutcome) =
        ClimbingProblem(id, grade, attempts, outcome)

    private fun session(
        id: String,
        date: LocalDate,
        minutes: Long,
        vararg problems: ClimbingProblem,
    ) = ClimbingSession(id, at(date), utc, minutes * 60, "", "", problems.toList())

    @Test
    fun gradesAreValidatedAndOrderedWithinTheirOwnSystem() {
        assertTrue(v("V5").rank > v("V4").rank)
        assertTrue(v("V0").rank > v("VB").rank)
        assertTrue(font("6C+").rank > font("6C").rank)
        assertTrue(font("7A").rank > font("6C+").rank)
        assertFailsWith<IllegalArgumentException> { v("V18") }
        assertFailsWith<IllegalArgumentException> { font("6D") }
        assertFailsWith<IllegalArgumentException> { ClimbingGrade(GradeSystem.V_SCALE, "6A") }
        assertEquals(19, ClimbingGrade.V_SCALE.size)
        assertEquals(24, ClimbingGrade.FONTAINEBLEAU.size)
    }

    @Test
    fun approximateConversionNeverReplacesTheOriginalGrade() {
        val original = font("6C+")
        val converted = ClimbingGrade.approximately(original, GradeSystem.V_SCALE)
        assertEquals(v("V5"), converted)
        assertEquals(font("6C+"), original)
        assertEquals(font("7A"), ClimbingGrade.approximately(v("V6"), GradeSystem.FONTAINEBLEAU))
        assertSame(original, ClimbingGrade.approximately(original, GradeSystem.FONTAINEBLEAU))
        // Every grade has an approximate partner, and order is never inverted by conversion.
        for (system in GradeSystem.values()) {
            val other = GradeSystem.values().first { it != system }
            val ranks =
                ClimbingGrade.labels(system).map {
                    ClimbingGrade.approximately(ClimbingGrade(system, it), other).rank
                }
            assertEquals(ranks.sorted(), ranks)
        }
    }

    @Test
    fun aFlashIsOnlyASendOnTheFirstAttempt() {
        assertTrue(problem("a", v("V3"), 1, ClimbOutcome.FLASH).sent())
        assertFailsWith<IllegalArgumentException> { problem("b", v("V3"), 2, ClimbOutcome.FLASH) }
        assertTrue(problem("c", v("V3"), 1, ClimbOutcome.SEND).sent())
        assertFalse(problem("d", null, 5, ClimbOutcome.NOT_SENT).sent())
        assertFailsWith<IllegalArgumentException> {
            problem("e", v("V3"), 0, ClimbOutcome.NOT_SENT)
        }
    }

    @Test
    fun sessionsRejectDuplicateProblemsAndImplausibleTimes() {
        val p = problem("same", v("V1"), 1, ClimbOutcome.FLASH)
        assertFailsWith<IllegalArgumentException> { session("s", today, 60, p, p) }
        assertFailsWith<IllegalArgumentException> { session("s", today, 0) }
        assertFailsWith<IllegalArgumentException> {
            ClimbingSession("s", at(today), utc, 25 * 3600, "", "", emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            ClimbingSession("s", at(today), utc, 3600, "x".repeat(81), "", emptyList())
        }
    }

    @Test
    fun liveDraftsMeasureElapsedTimeAndBecomeSessions() {
        val started = at(today, 18)
        val draft = ClimbingDraft("d", started, utc, null, " Local wall ", "", emptyList())
        assertTrue(draft.live())
        assertEquals(
            5400,
            draft.elapsedSeconds(Instants.ofEpochSecond(started.getEpochSecond() + 5400)),
        )
        assertEquals(0, draft.elapsedSeconds(Instants.ofEpochSecond(started.getEpochSecond() - 10)))
        val saved = draft.toSession(5400)
        assertEquals("Local wall", saved.location)
        assertEquals(ClimbingDraft.from(saved).toSession(5400), saved)
    }

    @Test
    fun summaryCountsSessionsAttemptsSendsAndFlashesInThePeriod() {
        val sessions =
            listOf(
                session(
                    "recent",
                    today.minusDays(1),
                    90,
                    problem("r1", v("V4"), 1, ClimbOutcome.FLASH),
                    problem("r2", v("V5"), 4, ClimbOutcome.SEND),
                    problem("r3", v("V6"), 6, ClimbOutcome.NOT_SENT),
                    problem("r4", font("7A"), 3, ClimbOutcome.SEND),
                    problem("r5", null, 2, ClimbOutcome.SEND),
                ),
                session(
                    "last-week",
                    today.minusDays(8),
                    60,
                    problem("l1", v("V3"), 2, ClimbOutcome.SEND),
                    problem("l2", v("V4"), 3, ClimbOutcome.NOT_SENT),
                ),
                session(
                    "old",
                    today.minusDays(60),
                    45,
                    problem("o1", v("V7"), 1, ClimbOutcome.FLASH),
                ),
            )
        val summary =
            ClimbingAnalytics.summary(sessions, today, Period.FOUR_WEEKS, GradeSystem.V_SCALE)

        assertEquals(2, summary.sessions)
        assertEquals(150 * 60L, summary.climbingSeconds)
        assertEquals(7, summary.problems)
        assertEquals(21, summary.attempts)
        assertEquals(5, summary.sends)
        assertEquals(1, summary.flashes)
        // Highest actual send per system; the unsent V6 and the out-of-period V7 do not count.
        assertEquals(v("V5"), summary.highestSend[GradeSystem.V_SCALE])
        assertEquals(font("7A"), summary.highestSend[GradeSystem.FONTAINEBLEAU])
        // Distribution keeps original grades; other-system and ungraded problems are not converted.
        assertEquals(
            listOf(
                ClimbingAnalytics.GradeCount(v("V3"), 1, 1),
                ClimbingAnalytics.GradeCount(v("V4"), 2, 1),
                ClimbingAnalytics.GradeCount(v("V5"), 1, 1),
                ClimbingAnalytics.GradeCount(v("V6"), 1, 0),
            ),
            summary.distribution,
        )
        assertEquals(1, summary.otherSystemProblems)
        assertEquals(1, summary.ungradedProblems)
        // Weekly progression: highest V-scale send per week, the last week still in progress.
        assertEquals(4, summary.progression.size)
        assertEquals(v("V5"), summary.progression.last().highestSend)
        assertEquals(v("V3"), summary.progression[2].highestSend)
        assertNull(summary.progression.first().highestSend)
        assertEquals(listOf("recent", "last-week", "old"), summary.recent.map { it.id })

        val longer =
            ClimbingAnalytics.summary(sessions, today, Period.TWELVE_WEEKS, GradeSystem.V_SCALE)
        assertEquals(v("V7"), longer.highestSend[GradeSystem.V_SCALE])
        assertEquals(3, longer.sessions)
    }

    @Test
    fun duplicatedSessionRecordsAreCountedOnce() {
        val one = session("dup", today, 60, problem("p", v("V2"), 1, ClimbOutcome.FLASH))
        val summary =
            ClimbingAnalytics.summary(
                listOf(one, one),
                today,
                Period.FOUR_WEEKS,
                GradeSystem.V_SCALE,
            )
        assertEquals(1, summary.sessions)
        assertEquals(1, summary.sends)
    }

    @Test
    fun noSessionsReportNothingRatherThanInventedGrades() {
        val summary =
            ClimbingAnalytics.summary(
                emptyList(),
                today,
                Period.ONE_YEAR,
                GradeSystem.FONTAINEBLEAU,
            )
        assertFalse(summary.hasAnySession())
        assertTrue(summary.highestSend.isEmpty())
        assertTrue(summary.distribution.isEmpty())
        assertEquals(52, summary.progression.size)
        assertTrue(summary.progression.all { it.highestSend == null && it.sends == 0 })
    }
}
